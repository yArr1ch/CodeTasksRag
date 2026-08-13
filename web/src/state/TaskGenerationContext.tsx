import {createContext, useContext, useEffect, useMemo, useState} from 'react';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {TaskGeneration, tasksApi} from '../api/taskApi';

type GenerationVariables = { prompt: string };
type GenerationSession = { id: string; prompt: string };

const GENERATION_STORAGE_KEY = 'algocoach.task-generation';

function readSession(): GenerationSession | null {
    try {
        const stored = localStorage.getItem(GENERATION_STORAGE_KEY);
        return stored ? JSON.parse(stored) as GenerationSession : null;
    } catch {
        return null;
    }
}

function isTerminal(status?: TaskGeneration['status']) {
    return status === 'SIMILAR_TASKS_FOUND'
        || status === 'READY'
        || status === 'FAILED'
        || status === 'CANCELED';
}

type TaskGenerationContextValue = {
    prompt: string;
    setPrompt: (prompt: string) => void;
    generation?: TaskGeneration;
    isPending: boolean;
    isCancelling: boolean;
    error: Error | null;
    generate: () => void;
    retry: () => void;
    cancelGeneration: () => void;
    clearGeneration: () => void;
};

const TaskGenerationContext = createContext<TaskGenerationContextValue | null>(null);

export function TaskGenerationProvider({children}: { children: React.ReactNode }) {
    const queryClient = useQueryClient();
    const [session, setSession] = useState<GenerationSession | null>(readSession);
    const [prompt, setPrompt] = useState(() => session?.prompt ?? '');
    const [generation, setGeneration] = useState<TaskGeneration>();
    const mutation = useMutation({
        mutationKey: ['task-generation'],
        mutationFn: ({prompt: requestPrompt}: GenerationVariables) =>
            tasksApi.generate(requestPrompt),
        onSuccess: (result) => {
            const nextSession = {id: result.id, prompt: result.prompt};
            setSession(nextSession);
            setPrompt(result.prompt);
            setGeneration(result);
            localStorage.setItem(GENERATION_STORAGE_KEY, JSON.stringify(nextSession));
        },
    });
    const cancelMutation = useMutation({
        mutationFn: (id: string) => tasksApi.cancelGeneration(id),
        onSuccess: (_, id) => {
            if (session?.id === id) {
                const canceled = (previous?: TaskGeneration) => previous
                    ? {...previous, status: 'CANCELED' as const, error: 'Task generation canceled by user'}
                    : previous;
                setGeneration(canceled);
                queryClient.setQueryData(['task-generation', id], canceled);
            }
        },
    });
    const generationQuery = useQuery({
        queryKey: ['task-generation', session?.id],
        queryFn: () => tasksApi.generation(session!.id),
        enabled: Boolean(session?.id),
        refetchInterval: query => isTerminal(query.state.data?.status) ? false : 1_000,
    });

    useEffect(() => {
        if (generationQuery.data) {
            setGeneration(generationQuery.data);
        }
    }, [generationQuery.data]);

    const value = useMemo(() => ({
        prompt,
        setPrompt,
        generation,
        isPending: mutation.isPending || Boolean(session && (!generation || !isTerminal(generation.status))),
        isCancelling: cancelMutation.isPending,
        error: mutation.error instanceof Error
            ? mutation.error
            : generationQuery.error instanceof Error
                ? generationQuery.error
                : generation?.status === 'FAILED'
                    ? new Error(generation.error ?? 'Task generation failed')
                    : null,
        generate: () => {
            const requestPrompt = prompt.trim();
            if (requestPrompt) {
                setSession(null);
                localStorage.removeItem(GENERATION_STORAGE_KEY);
                setGeneration(undefined);
                mutation.mutate({prompt: requestPrompt});
            }
        },
        retry: () => {
            const requestPrompt = session?.prompt ?? prompt.trim();
            if (requestPrompt) {
                setSession(null);
                localStorage.removeItem(GENERATION_STORAGE_KEY);
                setGeneration(undefined);
                mutation.mutate({prompt: requestPrompt});
            }
        },
        cancelGeneration: () => {
            if (session?.id) {
                cancelMutation.mutate(session.id);
            }
        },
        clearGeneration: () => {
            setSession(null);
            setGeneration(undefined);
            localStorage.removeItem(GENERATION_STORAGE_KEY);
        },
    }), [cancelMutation.isPending, generation, generationQuery.error, mutation, prompt, queryClient, session]);

    return <TaskGenerationContext.Provider value={value}>{children}</TaskGenerationContext.Provider>;
}

export function useTaskGeneration() {
    const context = useContext(TaskGenerationContext);
    if (!context) {
        throw new Error('useTaskGeneration must be used inside TaskGenerationProvider');
    }
    return context;
}

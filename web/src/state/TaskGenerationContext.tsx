import {createContext, useContext, useMemo, useState} from 'react';
import {useMutation} from '@tanstack/react-query';
import {TaskGenerationResult, tasksApi} from '../api/taskApi';

type GenerationDecision = 'CHECK' | 'CONTINUE';
type GenerationVariables = {prompt: string; decision: GenerationDecision};

type TaskGenerationContextValue = {
    prompt: string;
    setPrompt: (prompt: string) => void;
    generation?: TaskGenerationResult;
    isPending: boolean;
    error: Error | null;
    generate: (decision: GenerationDecision) => void;
};

const TaskGenerationContext = createContext<TaskGenerationContextValue | null>(null);

export function TaskGenerationProvider({children}: {children: React.ReactNode}) {
    const [prompt, setPrompt] = useState('');
    const [generation, setGeneration] = useState<TaskGenerationResult>();
    const mutation = useMutation({
        mutationKey: ['task-generation'],
        mutationFn: ({prompt: requestPrompt, decision}: GenerationVariables) =>
            tasksApi.generate(requestPrompt, decision),
        onSuccess: setGeneration,
    });
    const value = useMemo(() => ({
        prompt,
        setPrompt,
        generation,
        isPending: mutation.isPending,
        error: mutation.error instanceof Error ? mutation.error : null,
        generate: (decision: GenerationDecision) => mutation.mutate({prompt: prompt.trim(), decision}),
    }), [generation, mutation.error, mutation.isPending, prompt]);

    return <TaskGenerationContext.Provider value={value}>{children}</TaskGenerationContext.Provider>;
}

export function useTaskGeneration() {
    const context = useContext(TaskGenerationContext);
    if (!context) {
        throw new Error('useTaskGeneration must be used inside TaskGenerationProvider');
    }
    return context;
}

import {get, patch, post} from './client';

export type Task = {
    id: string;
    title: string;
    description: string;
    constraints: string[];
    testCases: { input: string; expectedOutput: string }[];
    status: string
};
export type TaskSolution = { id: string; sourceCode: string; type: 'REFERENCE' | 'COMMUNITY'; similarity: number };
export type TaskReview = {
    valid: boolean;
    warnings: { field: string; severity: 'INFO' | 'WARNING' | 'ERROR'; message: string; evidence?: string; impact?: string; suggestion?: string; correctedValue?: string | null }[]
};
export type TaskGenerationResult = {
    status: 'SIMILAR_TASKS_FOUND' | 'GENERATED';
    task?: Task;
    similarTasks: { id: string; title: string; description: string; similarity: number }[];
    referenceSolutions: string[]
};

export const tasksApi = {
    list: () => get<Task[]>('/tasks'),
    get: (id: string) => get<Task>(`/tasks/${id}`),
    generate: (prompt: string, decision: 'CHECK' | 'CONTINUE' = 'CHECK') => post<TaskGenerationResult>('/tasks/generate', {
        prompt,
        decision
    }),
    review: (id: string) => post<TaskReview>(`/tasks/${id}/review`),
    applyCorrection: (id: string, field: string, correctedValue: string) => patch<Task>(`/tasks/${id}/corrections`, {field, correctedValue}),
    hint: (id: string, level: number) => post<{ level: number; hint: string }>(`/tasks/${id}/hints`, {level}),
    solutions: (id: string, query?: string) => get<TaskSolution[]>(`/tasks/${id}/solutions${query ? `?query=${encodeURIComponent(query)}` : ''}`),
    publish: (id: string) => post<void>(`/tasks/${id}/publish`),
    reject: (id: string) => post<void>(`/tasks/${id}/reject`)
};

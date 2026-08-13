import {get, post} from './client';

export type Task = {
    id: string;
    title: string;
    description: string;
    constraints: string[];
    testCases: { input: string; expectedOutput: string }[];
    status: string
};
export type TaskPage = {
    tasks: Task[];
    nextCursor?: string | null;
    hasMore: boolean;
};
export type TaskSolution = { id: string; sourceCode: string; type: 'REFERENCE' | 'COMMUNITY'; similarity: number };
export type TaskReview = {
    valid: boolean;
    warnings: {
        field: string;
        severity: 'INFO' | 'WARNING' | 'ERROR';
        message: string;
        evidence?: string;
        impact?: string;
        suggestion?: string;
    }[]
};
export type TaskGenerationStatus =
    | 'GENERATING'
    | 'VALIDATING'
    | 'REPAIRING'
    | 'SIMILAR_TASKS_FOUND'
    | 'READY'
    | 'FAILED'
    | 'CANCELED';

export type TaskGeneration = {
    id: string;
    prompt: string;
    status: TaskGenerationStatus;
    attempt: number;
    maxAttempts: number;
    taskId?: string;
    similarTasks: { id: string; title: string; description: string; similarity: number }[];
    error?: string | null;
};

export const tasksApi = {
    list: (cursor?: string | null, limit = 3) => get<TaskPage>(`/tasks?limit=${limit}${cursor ? `&cursor=${encodeURIComponent(cursor)}` : ''}`),
    get: (id: string) => get<Task>(`/tasks/${id}`),
    generate: (prompt: string) => post<TaskGeneration>('/tasks/generate', {prompt}),
    generation: (id: string) => get<TaskGeneration>(`/tasks/generations/${id}`),
    cancelGeneration: (id: string) => post<void>(`/tasks/generations/${id}/cancel`),
    review: (id: string) => post<TaskReview>(`/tasks/${id}/review`),
    hint: (id: string, code: string, executionFeedback: string) =>
        post<{ hint: string }>(`/tasks/${id}/hints`, {code, executionFeedback}),
    solutions: (id: string, query?: string) => get<TaskSolution[]>(`/tasks/${id}/solutions${query ? `?query=${encodeURIComponent(query)}` : ''}`),
    publish: (id: string) => post<void>(`/tasks/${id}/publish`),
    reject: (id: string) => post<void>(`/tasks/${id}/reject`)
};

import {get, post} from './client';

export type Submission = {
    id: string;
    taskId: string;
    sourceCode: string;
    status: string;
    passedTests: number;
    totalTests: number;
    error?: string
};

export const submissionsApi = {
    submit: (taskId: string, sourceCode: string) => post<Submission>('/submissions', {taskId, sourceCode}),
    get: (id: string) => get<Submission>(`/submissions/${id}`)
};

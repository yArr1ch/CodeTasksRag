import {get} from './client';

export type UserProfile = {
    id: string;
    displayName: string;
    admin: boolean;
    points: number;
    totalEarned: number;
};

export type PointTransaction = {
    type: 'PASS_REWARD' | 'HINT_CHARGE' | 'REFERENCE_UNLOCK';
    amount: number;
    taskId?: string;
    submissionId?: string;
    createdAt: string;
};

export type PointSummary = {
    points: number;
    totalEarned: number;
    transactions: PointTransaction[];
};

export type LeaderboardEntry = {
    rank: number;
    displayName: string;
    points: number;
    totalEarned: number;
};

export const userApi = {
    me: () => get<UserProfile>('/me'),
    points: () => get<PointSummary>('/me/points'),
    leaderboard: () => get<LeaderboardEntry[]>('/leaderboard')
};

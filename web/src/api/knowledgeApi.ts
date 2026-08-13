import {get, upload} from './client';

export type KnowledgeDocument = {
    id: string;
    title: string;
    source: string;
    topic: string;
};

export type KnowledgePage = {
    documents: KnowledgeDocument[];
    nextCursor?: string | null;
    hasMore: boolean;
};

export const knowledgeApi = {
    list: (cursor?: string | null, limit = 12) => get<KnowledgePage>(`/knowledge?limit=${limit}${cursor ? `&cursor=${encodeURIComponent(cursor)}` : ''}`),
    create: (file: File) => upload<{ id: string; title: string; content: string; source: string; topic: string }>('/knowledge', file)
};

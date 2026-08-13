import axios from 'axios';

export const http = axios.create({
    baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api',
    headers: {'Content-Type': 'application/json'}
});

export async function get<T>(path: string) {
    return (await http.get<T>(path)).data;
}

export async function post<T>(path: string, body?: unknown) {
    return (await http.post<T>(path, body)).data;
}

export async function patch<T>(path: string, body?: unknown) {
    return (await http.patch<T>(path, body)).data;
}

export async function upload<T>(path: string, file: File) {
    const body = new FormData();
    body.append('file', file);
    return (await http.post<T>(path, body, {
        headers: {'Content-Type': 'multipart/form-data'}
    })).data;
}

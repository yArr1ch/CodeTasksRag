import {useEffect, useMemo, useRef, useState} from 'react';
import {BookOpen, Check, FileUp, Save, UploadCloud} from 'lucide-react';
import {useInfiniteQuery, useMutation, useQueryClient} from '@tanstack/react-query';
import Layout from '../components/Layout';
import {knowledgeApi} from '../api/knowledgeApi';

function errorMessage(error: unknown, fallback: string) {
    const detail = (error as { response?: { data?: { detail?: string } } })?.response?.data?.detail;
    return detail ?? (error instanceof Error ? error.message : fallback);
}

export default function KnowledgePage() {
    const queryClient = useQueryClient();
    const listScrollRef = useRef<HTMLDivElement>(null);
    const loadMoreRef = useRef<HTMLDivElement>(null);
    const [selectedFile, setSelectedFile] = useState<File>();
    const [message, setMessage] = useState<string>();
    const [error, setError] = useState<string>();
    const {data, isLoading, isFetchingNextPage, hasNextPage, fetchNextPage, error: listError} = useInfiniteQuery({
        queryKey: ['knowledge-documents'],
        queryFn: ({pageParam}) => knowledgeApi.list(pageParam),
        initialPageParam: null as string | null,
        getNextPageParam: lastPage => lastPage.hasMore ? lastPage.nextCursor : undefined
    });
    const documents = useMemo(() => data?.pages.flatMap(page => page.documents) ?? [], [data]);

    useEffect(() => {
        const target = loadMoreRef.current;
        const scrollContainer = listScrollRef.current;
        if (!target || !scrollContainer) {
            return;
        }
        const observer = new IntersectionObserver(entries => {
            if (entries[0]?.isIntersecting && hasNextPage && !isFetchingNextPage) {
                fetchNextPage();
            }
        }, {root: scrollContainer, rootMargin: '120px'});
        observer.observe(target);
        return () => observer.disconnect();
    }, [fetchNextPage, hasNextPage, isFetchingNextPage]);
    const upload = useMutation({
        mutationFn: () => {
            if (!selectedFile) {
                throw new Error('Choose a Markdown file first.');
            }
            return knowledgeApi.create(selectedFile);
        },
        onSuccess: result => {
            setError(undefined);
            setMessage(`${result.title} was saved and indexed for AI retrieval.`);
            setSelectedFile(undefined);
            queryClient.invalidateQueries({queryKey: ['knowledge-documents']});
        },
        onError: failure => {
            setMessage(undefined);
            setError(errorMessage(failure, 'The algorithm could not be uploaded.'));
        }
    });

    function selectFile(file?: File) {
        if (!file) {
            return;
        }
        setMessage(undefined);
        setError(file.name.endsWith('.md') ? undefined : 'Choose a Markdown file with a .md extension.');
        setSelectedFile(file.name.endsWith('.md') ? file : undefined);
    }

    return <Layout>
        <section className="knowledge-page">
            <div className="knowledge-heading">
                <div>
                    <p className="eyebrow">KNOWLEDGE LIBRARY</p>
                    <h1>Load an algorithm</h1>
                    <p>Upload a curated Markdown document. AlgoCoach will validate its format, store it in PostgreSQL,
                        and index it for task generation and coaching.</p>
                </div>
                <div className="create-mark"><BookOpen size={28}/></div>
            </div>

            {(message || error) &&
                <div className={error ? 'knowledge-notice error' : 'knowledge-notice success'}>{error ?? message}</div>}

            <div className="knowledge-upload-layout">
                <div className="panel knowledge-upload-card">
                    <div className="panel-title"><span><UploadCloud size={15}/> Upload Markdown</span><span
                        className="status">.md only</span></div>
                    <label className="knowledge-dropzone">
                        <FileUp size={28}/>
                        <b>{selectedFile?.name ?? 'Choose an algorithm file'}</b>
                        <span>{selectedFile ? `${Math.ceil(selectedFile.size / 1024)} KB selected` : 'Click to browse your Markdown file'}</span>
                        <input type="file" accept=".md,text/markdown"
                               onChange={event => selectFile(event.target.files?.[0])}/>
                    </label>
                    <div className="knowledge-upload-actions">
                        <button className="secondary" disabled={!selectedFile || upload.isPending}
                                onClick={() => setSelectedFile(undefined)}>Clear
                        </button>
                        <button className="primary" disabled={!selectedFile || Boolean(error) || upload.isPending}
                                onClick={() => upload.mutate()}><Save
                            size={15}/> {upload.isPending ? 'Uploading…' : 'Upload algorithm'}</button>
                    </div>
                </div>

                <div className="knowledge-upload-guide">
                    <div className="panel knowledge-format-card">
                        <div className="panel-title"><span>Accepted format</span><Check size={15}/></div>
                        <pre>{`---\ntitle: AsterFold Internal Algorithm\nsource: algocoach-internal\ntopic: fictional-algorithm\n---\n\n# AsterFold\n\nDescribe the algorithm, rules, examples,\ncanonical Java method, and complexity.`}</pre>
                        <p>Files must contain <code>title</code>, <code>source</code>, and <code>topic</code> in the
                            front matter. The Markdown body becomes the trusted retrieval context.</p>
                    </div>
                </div>
            </div>

            <div className="stored-knowledge">
                <div className="section-head">
                    <div><p className="eyebrow">STORED IN DATABASE</p><h2>Algorithm context</h2></div>
                    <span className="task-count">{documents.length} documents</span></div>
                {isLoading ? <div className="loading"><span className="spin">◌</span></div> : listError ?
                    <div className="error">Couldn’t load the algorithm library. Is the API running on port
                        8080?</div> : documents.length === 0 ?
                        <div className="empty-state">No algorithms have been uploaded yet.</div> : <>
                            <div ref={listScrollRef} className="knowledge-list-scroll">
                                <div className="knowledge-list">{documents.map(document => <div
                                    className="knowledge-card" key={document.id}><BookOpen size={16}/>
                                    <div><b>{document.title}</b><span>{document.source} · {document.topic}</span></div>
                                </div>)}</div>
                                <div ref={loadMoreRef} className="task-feed-status" aria-live="polite">
                                    {isFetchingNextPage ? <><span className="spin small">◌</span> Loading more
                                        algorithms…</> : hasNextPage ? 'Scroll for more algorithms' : 'You reached the end of the algorithm library.'}
                                </div>
                            </div>
                        </>}
            </div>
        </section>
    </Layout>;
}

import {useInfiniteQuery} from '@tanstack/react-query';
import {useEffect, useMemo, useRef} from 'react';
import {Plus} from 'lucide-react';
import {Link} from 'react-router-dom';
import Layout from '../components/Layout';
import TaskCard from '../components/TaskCard';
import {tasksApi} from '../api/taskApi';

export default function Home() {
    const loadMoreRef = useRef<HTMLDivElement>(null);
    const {data, isLoading, isFetchingNextPage, hasNextPage, fetchNextPage, error} = useInfiniteQuery({
        queryKey: ['tasks'],
        queryFn: ({pageParam}) => tasksApi.list(pageParam),
        initialPageParam: null as string | null,
        getNextPageParam: lastPage => lastPage.hasMore ? lastPage.nextCursor : undefined
    });
    const tasks = useMemo(() => data?.pages.flatMap(page => page.tasks) ?? [], [data]);

    useEffect(() => {
        const target = loadMoreRef.current;
        if (!target) {
            return;
        }
        const observer = new IntersectionObserver(entries => {
            if (entries[0]?.isIntersecting && hasNextPage && !isFetchingNextPage) {
                fetchNextPage();
            }
        }, {rootMargin: '0px'});
        observer.observe(target);
        return () => observer.disconnect();
    }, [fetchNextPage, hasNextPage, isFetchingNextPage]);

    return <Layout>
        <section className="hero"><div><p className="eyebrow">YOUR DAILY PRACTICE</p><h1>Sharpen your<br/><em>problem-solving.</em></h1><p className="hero-copy">Build strong algorithmic thinking, one thoughtful problem at a time.</p></div><div className="hero-art"><div className="orbit one"/><div className="orbit two"/><span>∑</span></div></section>
        <div className="section-head"><div><p className="eyebrow">CURATED FOR YOU</p><h2>Choose a challenge</h2></div><div className="section-actions"><span className="task-count">{tasks.length} loaded</span><Link className="primary create-task-button" to="/tasks/create"><Plus size={15}/> Create task</Link></div></div>
        {isLoading ? <div className="loading"><span className="spin">◌</span></div> : error ? <div className="error">Couldn’t load tasks. Is the API running on port 8080?</div> : <>
            <div className="task-grid">{tasks.map((task, i) => <TaskCard task={task} index={i} key={task.id}/>)}</div>
            <div ref={loadMoreRef} className="task-feed-status" aria-live="polite">
                {isFetchingNextPage ? <><span className="spin small">◌</span> Loading more challenges…</> : hasNextPage ? 'Scroll for more challenges' : tasks.length ? 'You reached the end of the challenge library.' : 'No published challenges yet.'}
            </div>
        </>}
    </Layout>;
}

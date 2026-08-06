import {useQuery} from '@tanstack/react-query';
import {Plus} from 'lucide-react';
import {Link} from 'react-router-dom';
import Layout from '../components/Layout';
import TaskCard from '../components/TaskCard';
import {tasksApi} from '../api/taskApi';

export default function Home() {
    const {data, isLoading, error} = useQuery({queryKey: ['tasks'], queryFn: tasksApi.list});
    return <Layout><section className="hero"><div><p className="eyebrow">YOUR DAILY PRACTICE</p><h1>Sharpen your<br/><em>problem-solving.</em></h1><p className="hero-copy">Build strong algorithmic thinking, one thoughtful problem at a time.</p></div><div className="hero-art"><div className="orbit one"/><div className="orbit two"/><span>∑</span></div></section><div className="section-head"><div><p className="eyebrow">CURATED FOR YOU</p><h2>Choose a challenge</h2></div><div className="section-actions"><span className="task-count">{data?.length ?? 0} problems</span><Link className="primary create-task-button" to="/tasks/create"><Plus size={15}/> Create task</Link></div></div>{isLoading ? <div className="loading"><span className="spin">◌</span></div> : error ? <div className="error">Couldn’t load tasks. Is the API running on port 8080?</div> : <div className="task-grid">{data?.map((task, i) => <TaskCard task={task} index={i} key={task.id}/>)}</div>}</Layout>;
}

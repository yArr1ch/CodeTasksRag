import {ArrowRight} from 'lucide-react';
import {Link} from 'react-router-dom';
import {Task} from '../api/taskApi';

export default function TaskCard({task, index}: { task: Task; index: number }) {
    return <Link className="task-card" to={`/tasks/${task.id}`}>
        <div className="card-top"><span
            className={'difficulty d' + (index % 3)}>● {index % 3 === 0 ? 'Warm up' : index % 3 === 1 ? 'Intermediate' : 'Stretch'}</span><ArrowRight
            size={18}/></div>
        <h3>{task.title}</h3><p>{task.description}</p>
        <div className="card-bottom"><span>{task.testCases?.length ?? 0} test cases</span><span
            className="progress-dot"><i/></span></div>
    </Link>;
}

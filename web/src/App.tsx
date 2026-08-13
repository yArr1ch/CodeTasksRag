import {Route, Routes} from 'react-router-dom';
import Home from './pages/Home';
import TaskPage from './pages/TaskPage';
import CreateTask from './pages/CreateTask';
import KnowledgePage from './pages/KnowledgePage';

export default function App() {
    return <Routes>
        <Route path="/" element={<Home/>}/>
        <Route path="/tasks/create" element={<CreateTask/>}/>
        <Route path="/knowledge" element={<KnowledgePage/>}/>
        <Route path="/tasks/:id" element={<TaskPage/>}/>
    </Routes>;
}

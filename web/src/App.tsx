import {Route, Routes} from 'react-router-dom';
import Home from './pages/Home';
import TaskPage from './pages/TaskPage';
import CreateTask from './pages/CreateTask';
import KnowledgePage from './pages/KnowledgePage';
import ProfilePage from './pages/ProfilePage';
import LeaderboardPage from './pages/LeaderboardPage';
import {useAuth} from './auth/AuthContext';

export default function App() {
    const auth = useAuth();
    if (!auth.ready) return <div className="loading">Connecting to AlgoCoach…</div>;
    if (!auth.authenticated) return <div className="loading">Sign-in is required.</div>;
    return <Routes>
        <Route path="/" element={<Home/>}/>
        <Route path="/tasks/create" element={<CreateTask/>}/>
        <Route path="/knowledge" element={<KnowledgePage/>}/>
        <Route path="/tasks/:id" element={<TaskPage/>}/>
        <Route path="/profile" element={<ProfilePage/>}/>
        <Route path="/leaderboard" element={<LeaderboardPage/>}/>
    </Routes>;
}

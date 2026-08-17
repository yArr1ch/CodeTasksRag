import {useState} from 'react';
import {NavLink} from 'react-router-dom';
import {BookOpen, BrainCircuit, ChevronRight, Code2, Menu, Sparkles, Trophy, UserRound, X} from 'lucide-react';
import {useAuth} from '../auth/AuthContext';
import {useQuery} from '@tanstack/react-query';
import {userApi} from '../api/userApi';

export default function Layout({children}: { children: React.ReactNode }) {
    const [open, setOpen] = useState(false);
    const auth = useAuth();
    const profile = useQuery({queryKey: ['me'], queryFn: userApi.me});
    return <div className="app">
        <aside className={open ? 'open' : ''}>
            <div className="brand"><span className="brand-mark"><BrainCircuit
                size={21}/></span><span>Algo<span>Coach</span></span>
                <button className="close" onClick={() => setOpen(false)}><X size={18}/></button>
            </div>
            <nav>
                <NavLink to="/" end onClick={() => setOpen(false)}
                         className={({isActive}) => isActive ? 'active-nav' : ''}><Code2 size={17}/> Practice</NavLink>
                {auth.admin && <NavLink to="/knowledge" onClick={() => setOpen(false)}
                                        className={({isActive}) => isActive ? 'active-nav' : ''}><BookOpen
                    size={17}/> Knowledge library</NavLink>}
                <NavLink to="/leaderboard" onClick={() => setOpen(false)}
                         className={({isActive}) => isActive ? 'active-nav' : ''}><Trophy
                    size={17}/> Leaderboard</NavLink>
                <a href="#" className="muted"><Sparkles size={17}/> AI Coach <small>soon</small></a>
            </nav>
            <div className="sidebar-footer">
                <div
                    className="avatar">{(profile.data?.displayName ?? auth.username ?? 'U').slice(0, 2).toUpperCase()}</div>
                <div><b>{profile.data?.points ?? 0} points</b><span><NavLink
                    to="/profile">View progress</NavLink></span></div>
                <ChevronRight size={15}/></div>
        </aside>
        <main>
            <header>
                <button className="menu" onClick={() => setOpen(true)}><Menu/></button>
                <div className="crumb">Practice <span>/</span> <b>Algorithms</b></div>
                <div className="header-right"><span className="streak">✦ {profile.data?.points ?? 0} points</span>
                    <NavLink className="avatar" to="/profile" aria-label="Open profile"><UserRound size={16}/></NavLink>
                    <button className="ghost logout-button" onClick={auth.logout}>Log out</button>
                </div>
            </header>
            {children}</main>
    </div>;
}

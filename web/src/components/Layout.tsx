import {useState} from 'react';
import {NavLink} from 'react-router-dom';
import {BookOpen, BrainCircuit, ChevronRight, Code2, Menu, Sparkles, X} from 'lucide-react';

export default function Layout({children}: { children: React.ReactNode }) {
    const [open, setOpen] = useState(false);
    return <div className="app">
        <aside className={open ? 'open' : ''}>
            <div className="brand"><span className="brand-mark"><BrainCircuit
                size={21}/></span><span>Algo<span>Coach</span></span>
                <button className="close" onClick={() => setOpen(false)}><X size={18}/></button>
            </div>
            <nav><NavLink to="/" end onClick={() => setOpen(false)}
                          className={({isActive}) => isActive ? 'active-nav' : ''}><Code2 size={17}/> Practice</NavLink><NavLink
                to="/knowledge" onClick={() => setOpen(false)} className={({isActive}) => isActive ? 'active-nav' : ''}><BookOpen
                size={17}/> Knowledge library</NavLink><a href="#"
                                                          className="muted"><Sparkles
                size={17}/> AI Coach <small>soon</small></a></nav>
            <div className="sidebar-footer">
                <div className="avatar">YC</div>
                <div><b>Your progress</b><span>Keep the streak alive</span></div>
                <ChevronRight size={15}/></div>
        </aside>
        <main>
            <header>
                <button className="menu" onClick={() => setOpen(true)}><Menu/></button>
                <div className="crumb">Practice <span>/</span> <b>Algorithms</b></div>
                <div className="header-right"><span className="streak">✦ 3 day streak</span>
                    <div className="avatar">YC</div>
                </div>
            </header>
            {children}</main>
    </div>;
}

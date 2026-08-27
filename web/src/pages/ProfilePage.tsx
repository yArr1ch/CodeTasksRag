import {useQuery} from '@tanstack/react-query';
import Layout from '../components/Layout';
import {userApi} from '../api/userApi';

export default function ProfilePage() {
    const profile = useQuery({queryKey: ['me'], queryFn: userApi.me});
    const points = useQuery({queryKey: ['points'], queryFn: userApi.points});
    if (profile.isLoading || points.isLoading) return <Layout><div className="loading">Loading profile…</div></Layout>;
    if (profile.error || points.error || !profile.data || !points.data) return <Layout><div className="error">Could not load your profile.</div></Layout>;
    return <Layout>
        <section className="account-page">
            <p className="eyebrow">YOUR PROGRESS</p>
            <h1>{profile.data.displayName}</h1>
            <div className="account-summary">
                <div className="panel account-stat"><span>Available points</span><b>{profile.data.points}</b></div>
                <div className="panel account-stat"><span>Total earned</span><b>{profile.data.totalEarned}</b></div>
                <div className="panel account-stat"><span>Role</span><b>{profile.data.admin ? 'Admin' : 'Learner'}</b></div>
            </div>
            <div className="panel point-history">
                <div className="panel-title"><span>Point history</span></div>
                {points.data.transactions.length === 0 ? <p className="empty-state">No point activity yet.</p> :
                    points.data.transactions.map((transaction, index) => <div className="point-row" key={`${transaction.createdAt}-${index}`}>
                        <span>{transaction.type.replaceAll('_', ' ')}</span>
                        <b className={transaction.amount > 0 ? 'points-positive' : 'points-negative'}>{transaction.amount > 0 ? '+' : ''}{transaction.amount}</b>
                    </div>)}
            </div>
        </section>
    </Layout>;
}

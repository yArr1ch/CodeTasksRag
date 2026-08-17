import {useQuery} from '@tanstack/react-query';
import Layout from '../components/Layout';
import {userApi} from '../api/userApi';

export default function LeaderboardPage() {
    const leaderboard = useQuery({queryKey: ['leaderboard'], queryFn: userApi.leaderboard});
    return <Layout>
        <section className="account-page">
            <p className="eyebrow">COMMUNITY</p>
            <h1>Leaderboard</h1>
            {leaderboard.isLoading ? <div className="loading">Loading leaderboard…</div> : leaderboard.error ?
                <div className="error">Could not load the leaderboard.</div> :
                <div className="panel leaderboard">
                    {leaderboard.data?.map(entry => <div className="leaderboard-row" key={entry.rank}>
                        <span className="leaderboard-rank">#{entry.rank}</span>
                        <b>{entry.displayName}</b>
                        <span>{entry.points} points</span>
                    </div>)}
                </div>}
        </section>
    </Layout>;
}

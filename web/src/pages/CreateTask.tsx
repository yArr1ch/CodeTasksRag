import {useEffect} from 'react';
import {ArrowLeft, LoaderCircle, Sparkles} from 'lucide-react';
import {Link, useNavigate} from 'react-router-dom';
import Layout from '../components/Layout';
import {useTaskGeneration} from '../state/TaskGenerationContext';

export default function CreateTask() {
    const navigate = useNavigate();
    const {prompt, setPrompt, generation, isPending, error, generate} = useTaskGeneration();
    useEffect(() => {
        if (generation?.task) navigate(`/tasks/${generation.task.id}`);
    }, [generation, navigate]);
    const hasSimilar = generation?.status === 'SIMILAR_TASKS_FOUND';

    return <Layout>
        <section className="create-page">
            <Link className="back" to="/"> <ArrowLeft size={15}/> All challenges</Link>
            <div className="create-heading">
                <div>
                    <p className="eyebrow">TASK WORKSHOP</p>
                    <h1>Create a challenge</h1>
                    <p>Describe the algorithm you want to practice and let AlgoCoach prepare a validated draft.</p>
                </div>
                <div className="create-mark"><Sparkles size={28}/></div>
            </div>

            <div className={'generate panel' + (hasSimilar ? ' has-similar' : '')}>
                <div className="generator-prompt">
                    <p className="eyebrow">AI TASK GENERATOR</p>
                    <b>Describe a problem you want to practice</b>
                    {!hasSimilar && <div className="generate-row">
                        <input
                            value={prompt}
                            onChange={event => setPrompt(event.target.value)}
                            placeholder="e.g. a sliding-window problem about sensor readings"
                            onKeyDown={event => {
                                if (event.key === 'Enter' && prompt.trim()) generate('CHECK');
                            }}
                        />
                        <button className="primary" disabled={!prompt.trim() || isPending} onClick={() => generate('CHECK')}>
                            <Sparkles size={15}/> {isPending ? 'Generating…' : 'Generate task'}
                        </button>
                    </div>}
                    {isPending && <div className="generation-status"><LoaderCircle size={14} className="spin"/> Generating and validating the task…</div>}
                    {error && <div className="error generator-error">{error.message}</div>}
                    {hasSimilar && <p className="pasted-prompt">“{prompt}”</p>}
                </div>
                {hasSimilar && <div className="similar-results">
                    <div className="similar-heading">
                        <div><p className="eyebrow">SIMILAR TASKS FOUND</p><b>Related challenges already exist</b></div>
                        <button className="primary" disabled={isPending} onClick={() => generate('CONTINUE')}>
                            {isPending ? 'Generating…' : 'Generate anyway'}
                        </button>
                    </div>
                    <div className="similar-grid">
                        {generation.similarTasks.map(similar => <div className="similar-card" key={similar.id}>
                            <div className="match-line"><span>{Math.round(similar.similarity * 100)}% match</span><span>Existing challenge</span></div>
                            <div className="match-bar"><i style={{width: `${Math.round(similar.similarity * 100)}%`}}/></div>
                            <b>{similar.title}</b>
                            <p>{similar.description}</p>
                            <small>Algorithm practice · Similarity result</small>
                            <button className="existing-button" onClick={() => navigate(`/tasks/${similar.id}`)}>Practice this task <span>→</span></button>
                        </div>)}
                    </div>
                </div>}
            </div>
        </section>
    </Layout>;
}

import {useEffect} from 'react';
import {ArrowLeft, LoaderCircle, Sparkles} from 'lucide-react';
import {Link, useNavigate} from 'react-router-dom';
import Layout from '../components/Layout';
import {useTaskGeneration} from '../state/TaskGenerationContext';

export default function CreateTask() {
    const navigate = useNavigate();
    const {
        prompt,
        setPrompt,
        generation,
        isPending,
        isCancelling,
        error,
        generate,
        retry,
        cancelGeneration,
        clearGeneration,
    } = useTaskGeneration();
    useEffect(() => {
        if (generation?.taskId) {
            clearGeneration();
            navigate(`/tasks/${generation.taskId}`);
        }
    }, [clearGeneration, generation?.taskId, navigate]);
    const hasSimilar = generation?.status === 'SIMILAR_TASKS_FOUND';
    const generationFailed = generation?.status === 'FAILED';
    const generationCanceled = generation?.status === 'CANCELED';
    const stages = [
        {status: 'GENERATING', label: 'Generating draft'},
        {status: 'VALIDATING', label: 'Validating solution'},
        {status: 'REPAIRING', label: 'Repairing reference'},
        {status: 'READY', label: 'Ready'},
    ];
    const activeStage = Math.max(0, stages.findIndex(stage => stage.status === generation?.status));

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
                    <div className="generate-row">
                        <input
                            value={prompt}
                            onChange={event => setPrompt(event.target.value)}
                            placeholder="e.g. a sliding-window problem about sensor readings"
                            onKeyDown={event => {
                                if (event.key === 'Enter' && prompt.trim()) generate();
                            }}
                        />
                        <button className="primary" disabled={!prompt.trim() || isPending} onClick={generate}>
                            <Sparkles size={15}/> {isPending ? 'Generating…' : 'Generate task'}
                        </button>
                    </div>
                    {isPending && <div className="generation-progress">
                        <div className="generation-status"><LoaderCircle size={14} className="spin"/> Task generation in progress</div>
                        <div className="generation-steps" aria-label="Task generation progress">
                            {stages.map((stage, index) => <span
                                className={index < activeStage ? 'complete' : index === activeStage ? 'active' : ''}
                                key={stage.status}
                            >{stage.label}</span>)}
                        </div>
                        {generation?.attempt ? <small>Repair attempt {generation.attempt} of {generation.maxAttempts}</small> : null}
                        <button
                            className="secondary generation-cancel"
                            disabled={isCancelling}
                            onClick={cancelGeneration}
                        >
                            {isCancelling ? 'Cancelling…' : 'Cancel generation'}
                        </button>
                    </div>}
                    {(error || generationFailed) && <div className="generation-error">
                        <div>
                            <b>Generation failed</b>
                            <p>{error?.message ?? generation?.error ?? 'The task could not be generated.'}</p>
                        </div>
                        <button className="secondary" disabled={isPending} onClick={retry}>Retry generation</button>
                    </div>}
                    {generationCanceled && <div className="generation-error canceled">
                        <div>
                            <b>Generation canceled</b>
                            <p>You can start a new generation whenever you are ready.</p>
                        </div>
                    </div>}
                    {hasSimilar && <p className="pasted-prompt">“{prompt}”</p>}
                </div>
                {hasSimilar && <div className="similar-results">
                    <div className="similar-heading">
                        <div>
                            <p className="eyebrow">TASK ALREADY EXISTS</p>
                            <b>We found an existing challenge for this request</b>
                        </div>
                    </div>
                    <div className="similar-grid">
                        {generation.similarTasks.map(similar => <div className="similar-card" key={similar.id}>
                            <div className="match-line">
                                <span>{Math.round(similar.similarity * 100)}% match</span>
                                <span>Existing challenge</span>
                            </div>
                            <div className="match-bar"><i style={{width: `${Math.round(similar.similarity * 100)}%`}}/></div>
                            <b>{similar.title}</b>
                            <p>{similar.description}</p>
                            <button className="existing-button" onClick={() => navigate(`/tasks/${similar.id}`)}>
                                Practice this task <span>→</span>
                            </button>
                        </div>)}
                    </div>
                </div>}
            </div>
        </section>
    </Layout>;
}

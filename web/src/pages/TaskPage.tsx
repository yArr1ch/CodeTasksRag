import {useEffect, useState} from "react";
import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {
    CheckCircle2,
    Lightbulb,
    LoaderCircle,
    Play,
    Terminal,
} from "lucide-react";
import {useNavigate, useParams} from "react-router-dom";
import Layout from "../components/Layout";
import ReferenceSolution from "../components/ReferenceSolution";
import {submissionsApi, Submission} from "../api/submissionApi";
import {tasksApi} from "../api/taskApi";

function ReviewFindings({
                            review,
                            onApply,
                            applied,
                            applying,
                        }: {
    review: NonNullable<ReturnType<typeof useMutation>["data"]> & {
        valid: boolean;
        warnings: any[];
    };
    onApply: (field: string, value: string) => void;
    applied: Set<string>;
    applying: boolean;
}) {
    return (
        <div className={review.valid ? "review-ok" : "review-panel"}>
            <div className="review-summary">
                <b>
                    {review.valid
                        ? "Review passed"
                        : `${review.warnings.length} review findings`}
                </b>
                <span>
          {review.valid
              ? "No issues found in this generated task."
              : "Check each finding before publishing."}
        </span>
            </div>
            {review.warnings.map((warning, index) => (
                <div
                    className={
                        "review-finding severity-" + warning.severity.toLowerCase()
                    }
                    key={warning.field + index}
                >
                    <div className="finding-top">
                        <span className="severity">{warning.severity}</span>
                        <b>{warning.field}</b>
                    </div>
                    <p>{warning.message}</p>
                    {warning.evidence && (
                        <div>
                            <small>Evidence</small>
                            <blockquote>{warning.evidence}</blockquote>
                        </div>
                    )}
                    {warning.impact && (
                        <div>
                            <small>Impact</small>
                            <p>{warning.impact}</p>
                        </div>
                    )}
                    {warning.suggestion && (
                        <div>
                            <small>Suggested change</small>
                            <p>{warning.suggestion}</p>
                        </div>
                    )}
                    {warning.correctedValue && (
                        <button
                            className="use-correction"
                            disabled={applying || applied.has(warning.field)}
                            onClick={() => onApply(warning.field, warning.correctedValue)}
                        >
                            {applied.has(warning.field)
                                ? "Applied to task"
                                : applying
                                    ? "Applying…"
                                    : "Apply correction"}
                        </button>
                    )}
                </div>
            ))}
        </div>
    );
}

export default function TaskPage() {
    const {id = ""} = useParams();
    const nav = useNavigate();
    const qc = useQueryClient();
    const {
        data: task,
        isLoading,
        error,
    } = useQuery({queryKey: ["task", id], queryFn: () => tasksApi.get(id)});
    const {data: solutions} = useQuery({
        queryKey: ["solutions", id],
        queryFn: () => tasksApi.solutions(id),
    });
    const [activeTab, setActiveTab] = useState<"problem" | "solutions">(
        "problem",
    );
    const [code, setCode] = useState("// Write your solution here\n\n");
    const [hint, setHint] = useState<{ hint: string }>();
    const [submission, setSubmission] = useState<Submission>();
    const [applied, setApplied] = useState<Set<string>>(new Set());
    const [moderationAction, setModerationAction] = useState<"publish" | "reject">();

    const hintMutation = useMutation({
        mutationFn: (level: number) => tasksApi.hint(id, level),
        onSuccess: setHint,
    });
    const review = useMutation({mutationFn: () => tasksApi.review(id)});
    const applyCorrection = useMutation({
        mutationFn: ({field, value}: { field: string; value: string }) =>
            tasksApi.applyCorrection(id, field, value),
        onSuccess: (_, correction) => {
            setApplied((previous) => new Set(previous).add(correction.field));
            qc.invalidateQueries({queryKey: ["task", id]});
            qc.invalidateQueries({queryKey: ["solutions", id]});
        },
    });
    const moderate = useMutation({
        mutationFn: (action: "publish" | "reject") => tasksApi[action](id),
        onSuccess: () => qc.invalidateQueries({queryKey: ["task", id]}),
    });
    const confirmModeration = (action: "publish" | "reject") => {
        setModerationAction(action);
    };
    const executeModeration = () => {
        if (!moderationAction) return;
        moderate.mutate(moderationAction, {
            onSettled: () => setModerationAction(undefined),
        });
    };
    const submit = useMutation({
        mutationFn: () => submissionsApi.submit(id, code),
        onSuccess: (value) => {
            setSubmission(value);
            qc.setQueryData(["submission", value.id], value);

        },
    });
    useEffect(() => {
        if (
            !submission ||
            ["PASSED", "FAILED", "COMPILE_ERROR", "TIMEOUT"].includes(
                submission.status,
            )
        )
            return;
        const timer = setInterval(
            async () => setSubmission(await submissionsApi.get(submission.id)),
            1500,
        );
        return () => clearInterval(timer);
    }, [submission]);
    useEffect(() => {
        if (!moderationAction) return;
        const closeOnEscape = (event: KeyboardEvent) => {
            if (event.key === "Escape") setModerationAction(undefined);
        };
        window.addEventListener("keydown", closeOnEscape);
        return () => window.removeEventListener("keydown", closeOnEscape);
    }, [moderationAction]);
    if (isLoading)
        return (
            <Layout>
                <LoaderCircle className="spin"/>
            </Layout>
        );
    if (error || !task)
        return (
            <Layout>
                <div className="error">Task not found.</div>
            </Layout>
        );
    return (
        <Layout>
            <div className="task-header">
                <button className="back" onClick={() => nav("/")}>
                    ← All challenges
                </button>
                <span className="difficulty d1">● {task.status}</span>
                <h1>{task.title}</h1>
                {task.status === "DRAFT" && (
                    <>
                        <div className="task-actions">
                            <button
                                className="ghost"
                                onClick={() => nav("/")}
                            >
                                View all tasks
                            </button>
                            <button
                                className="ghost"
                                onClick={() => review.mutate()}
                                disabled={review.isPending}
                            >
                                {review.isPending ? "Reviewing…" : "Review task"}
                            </button>
                            <button
                                className="ghost"
                                onClick={() => confirmModeration("reject")}
                            >
                                Reject
                            </button>
                            <button
                                className="primary"
                                onClick={() => confirmModeration("publish")}
                            >
                                Publish
                            </button>
                        </div>
                        {review.data && (
                            <ReviewFindings
                                review={review.data}
                                onApply={(field, value) =>
                                    applyCorrection.mutate({field, value})
                                }
                                applied={applied}
                                applying={applyCorrection.isPending}
                            />
                        )}
                    </>
                )}
            </div>
            <div className={`workspace${task.status === "PUBLISHED" ? "" : " moderation-workspace"}`}>
                <div className="problem panel">
                    <div className="panel-title task-tabs">
                        <div>
                            <button
                                className={activeTab === "problem" ? "tab active" : "tab"}
                                onClick={() => setActiveTab("problem")}
                            >
                                Problem
                            </button>
                            <button
                                className={activeTab === "solutions" ? "tab active" : "tab"}
                                onClick={() => setActiveTab("solutions")}
                            >
                                Reference solutions
                            </button>
                        </div>
                        <span className="status">{task.testCases?.length} test cases</span>
                    </div>
                    {activeTab === "problem" ? (
                        <>
                            <p>{task.description}</p>
                            <h4>Constraints</h4>
                            <ul>
                                {task.constraints?.map((c) => (
                                    <li key={c}>{c}</li>
                                ))}
                            </ul>
                            <h4>Examples</h4>
                            {task.testCases?.slice(0, 2).map((test, index) => (
                                <div className="example" key={index}>
                                    <b>Example {index + 1}</b>
                                    <code>
                                        Input: {test.input}
                                        <br/>
                                        Output: {test.expectedOutput}
                                    </code>
                                </div>
                            ))}
                        </>
                    ) : (
                        <div className="solutions-list">
                            {solutions?.length ? (
                                solutions.map((solution) => (
                                    <div className="example" key={solution.id}>
                                        <b>
                                            {solution.type} · {(solution.similarity * 100).toFixed(0)}
                                            % match
                                        </b>
                                        <ReferenceSolution
                                            source={solution.sourceCode}
                                            editable={
                                                task.status === "DRAFT" && solution.type === "REFERENCE"
                                            }
                                            onAutoSave={(sourceCode) =>
                                                applyCorrection.mutateAsync({
                                                    field: "referenceSolutions",
                                                    value: sourceCode,
                                                })
                                            }
                                        />
                                    </div>
                                ))
                            ) : (
                                <p className="empty-state">
                                    No reference solutions available yet.
                                </p>
                            )}
                        </div>
                    )}
                </div>
                {task.status === "PUBLISHED" && <div className="editor-column">
                    <div className="editor panel">
                        <div className="panel-title">
              <span>
                <Terminal size={15}/> Java 25
              </span>
                            <span className="editor-dot">● Ready</span>
                        </div>
                        <textarea
                            value={code}
                            onChange={(event) => setCode(event.target.value)}
                            spellCheck={false}
                        />
                        <div className="editor-actions">
                            <button
                                className="ghost"
                                onClick={() => setCode("// Write your solution here\n\n")}
                            >
                                Reset
                            </button>
                            <button
                                className="primary"
                                disabled={submit.isPending}
                                onClick={() => submit.mutate()}
                            >
                                <Play size={15} fill="currentColor"/>{" "}
                                {submit.isPending ? "Running…" : "Run solution"}
                            </button>
                        </div>
                    </div>
                    <div className="coach panel">
                        <div className="coach-title">
                            <Lightbulb size={18}/>
                            <div>
                                <b>Need a nudge?</b>
                                <span>Ask your AI coach for a hint</span>
                            </div>
                        </div>
                        <div className="hint-levels">
                            {[1, 2, 3].map((level) => (
                                <button
                                    key={level}
                                    onClick={() => hintMutation.mutate(level)}
                                    disabled={hintMutation.isPending}
                                >
                                    Hint {level}
                                </button>
                            ))}
                        </div>
                        {hint && <p className="hint">{hint.hint}</p>}
                    </div>
                    {submission && (
                        <div className={"result panel " + submission.status.toLowerCase()}>
                            <div>
                                <b>
                                    {submission.status === "PASSED"
                                        ? "All tests passed!"
                                        : submission.status}
                                </b>
                                <span>
                  {submission.passedTests ?? 0} / {submission.totalTests ?? 0}{" "}
                                    tests passed
                </span>
                            </div>
                            <CheckCircle2 size={22}/>
                            {submission.error && <code>{submission.error}</code>}
                        </div>
                    )}
                </div>}
            </div>
            {moderationAction && (
                <div
                    className="modal-backdrop"
                    role="presentation"
                    onMouseDown={() => setModerationAction(undefined)}
                >
                    <div
                        className="confirm-modal"
                        role="dialog"
                        aria-modal="true"
                        aria-labelledby="moderation-title"
                        onMouseDown={(event) => event.stopPropagation()}
                    >
                        <p className="eyebrow">TASK MODERATION</p>
                        <h2 id="moderation-title">
                            {moderationAction === "publish"
                                ? "Publish this task?"
                                : "Reject this task?"}
                        </h2>
                        <p>
                            {moderationAction === "publish"
                                ? "This task will become visible as an active challenge."
                                : "This task will be marked as rejected and cannot be published."}
                        </p>
                        <div className="confirm-actions">
                            <button
                                className="ghost"
                                onClick={() => setModerationAction(undefined)}
                                disabled={moderate.isPending}
                            >
                                Cancel
                            </button>
                            <button
                                className={moderationAction === "reject" ? "danger" : "primary"}
                                onClick={executeModeration}
                                disabled={moderate.isPending}
                            >
                                {moderate.isPending
                                    ? "Saving…"
                                    : moderationAction === "publish"
                                        ? "Publish task"
                                        : "Reject task"}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </Layout>
    );
}

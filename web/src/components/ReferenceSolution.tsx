import {useEffect, useRef, useState} from 'react';
import * as prettier from 'prettier/standalone';
import javaPlugin from 'prettier-plugin-java';

function normalizeSource(source: string) {
    const decoded = source.includes('\\n') && !source.includes('\n') ? source.replaceAll('\\n', '\n') : source;
    return decoded
        .replaceAll('\r\n', '\n')
        .replaceAll('\r', '\n')
        .split('\n')
        .filter(line => line.trim().length > 0)
        .join('\n')
        .trim();
}

type ReferenceSolutionProps = {
    source: string;
    editable?: boolean;
    onAutoSave?: (source: string) => Promise<unknown>;
};

export default function ReferenceSolution({source, editable = false, onAutoSave}: ReferenceSolutionProps) {
    const [formatted, setFormatted] = useState(source);
    const [draft, setDraft] = useState(source);
    const [savedDraft, setSavedDraft] = useState(source);
    const [saveState, setSaveState] = useState<'saved' | 'pending' | 'saving' | 'failed'>('saved');
    const onAutoSaveRef = useRef(onAutoSave);

    useEffect(() => {
        onAutoSaveRef.current = onAutoSave;
    }, [onAutoSave]);

    useEffect(() => {
        let active = true;
        const displaySource = normalizeSource(source);
        setDraft(current => current === savedDraft ? source : current);
        setSavedDraft(source);
        prettier.format(displaySource, {parser: 'java', plugins: [javaPlugin], printWidth: 100, tabWidth: 4, trailingComma: 'none'})
            .then(value => {if (active) setFormatted(value);})
            .catch(() => {if (active) setFormatted(displaySource);});
        return () => { active = false; };
    }, [source]);

    useEffect(() => {
        if (!editable || draft === savedDraft || !onAutoSaveRef.current) {
            return;
        }
        setSaveState('pending');
        const sourceToSave = draft;
        const timer = window.setTimeout(() => {
            setSaveState('saving');
            void onAutoSaveRef.current?.(sourceToSave)
                .then(() => {
                    setSavedDraft(sourceToSave);
                    setSaveState('saved');
                })
                .catch(() => setSaveState('failed'));
        }, 700);
        return () => window.clearTimeout(timer);
    }, [draft, editable, savedDraft]);

    if (editable) {
        return <div className="reference-editor">
            <div className="reference-editor-actions">
                <span className={`autosave-state ${saveState}`}>
                    {saveState === 'saved' ? 'Saved automatically' : saveState === 'saving' ? 'Saving…' : saveState === 'failed' ? 'Could not save' : 'Changes will save automatically'}
                </span>
                <div>
                    <button className="ghost" onClick={() => setDraft(savedDraft)} disabled={saveState === 'saving'}>Discard changes</button>
                </div>
            </div>
            <textarea
                aria-label="Reference solution source code"
                value={draft}
                onChange={event => setDraft(event.target.value)}
                spellCheck={false}
            />
        </div>;
    }

    return <pre className="reference-code"><code>{formatted}</code></pre>;
}

import {useEffect, useState} from 'react';
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
};

export default function ReferenceSolution({source}: ReferenceSolutionProps) {
    const [formatted, setFormatted] = useState(source);

    useEffect(() => {
        let active = true;
        const displaySource = normalizeSource(source);
        prettier.format(displaySource, {parser: 'java', plugins: [javaPlugin], printWidth: 100, tabWidth: 4, trailingComma: 'none'})
            .then(value => {if (active) setFormatted(value);})
            .catch(() => {if (active) setFormatted(displaySource);});
        return () => { active = false; };
    }, [source]);

    return <pre className="reference-code"><code>{formatted}</code></pre>;
}

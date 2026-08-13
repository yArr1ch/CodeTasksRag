# AlgoCoach development instructions

Before working on this repository, inspect the available Codex skills:

- `~/.codex/skills/`
- `.codex/skills/`

When a task matches a skill, read that skill's complete `SKILL.md` before taking implementation actions. Prefer the project's local skill when one exists, and use the smallest set of relevant skills.

For every task:

- preserve all existing user changes, including unstaged, staged, and untracked files;
- only edit files directly required by the user's request;
- do not revert, reset, rewrite, reformat, or "clean up" unrelated changes;
- inspect `git status` and the relevant diff before editing, and keep unrelated hunks intact;
- if a required change overlaps an existing user edit, preserve the edit and make the smallest targeted patch;
- never delete a file or change a dependency/configuration outside the requested scope without explicit approval.

For backend changes:

- preserve the API/Worker service boundary;
- keep deterministic validation and code execution separate from AI behavior;
- use Java 25, Gradle Kotlin DSL, Spring Boot, PostgreSQL, Kafka, and Ollama conventions already defined by the project;
- inspect the current code before introducing new abstractions;
- avoid duplicate interfaces, services, repositories, or controllers when an existing component already owns the responsibility;
- validate changes with the appropriate Gradle tests or compilation checks.

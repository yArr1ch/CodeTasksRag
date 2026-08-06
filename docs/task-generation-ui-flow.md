# Task generation UI flow

## Goal

Let a user review an AI-generated task before publishing it.

## Flow

1. The user submits a natural-language prompt.
2. The UI calls `POST /api/tasks/generate`.
3. The API generates the task and saves it as `DRAFT`.
4. The UI displays the returned draft immediately.
5. The UI offers `Review`, `Edit`, `Publish`, and `Reject` actions.

## Review

The UI calls:

```http
POST /api/tasks/{id}/review
```

The review can be shown in a modal or side panel. It should contain actionable
field-level feedback, including the issue, evidence, impact, and suggestion.

After the user edits the draft, the UI must request a new review because the
previous review may no longer match the task.

## Lifecycle actions

Publish:

```http
POST /api/tasks/{id}/publish
```

Reject:

```http
POST /api/tasks/{id}/reject
```

`DRAFT` is editable. `PUBLISHED` is available to learners and should normally
be protected from casual editing. `REJECTED` is discarded from the publishing
flow.

## Backend work still required

Add a draft update endpoint:

```http
PATCH /api/tasks/{id}
```

It should update only editable draft fields and reject updates to published
tasks unless an explicit administrative workflow is introduced.

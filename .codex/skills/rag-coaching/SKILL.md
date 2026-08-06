---
name: rag-coaching
description: Use when designing or implementing retrieval-augmented AI coaching for AlgoCoach, including curated knowledge, embeddings, vector retrieval, hints, failure explanations, complexity guidance, alternative approaches, or similar-task recommendations. Keep deterministic execution responsible for correctness.
---

# AlgoCoach RAG Coaching

Use RAG as a contextual knowledge layer for AI coaching, never as the source of truth for correctness. The deterministic judge owns compilation, execution, test results, scoring, and security. AI uses retrieved context to explain and teach.

## Architecture

- Store curated documents first: algorithms, data structures, Java practices, interview patterns, common mistakes, complexity notes, editorials, hints, solution approaches, and edge cases.
- Split documents into chunks stored in PostgreSQL with pgvector.
- Each chunk includes content, embedding, source, and metadata: topic, difficulty, language, algorithm category, and content type.
- Hide embedding implementations behind `EmbeddingProvider`; use Ollama locally and keep Amazon Bedrock as a replaceable future provider.
- Keep retrieval, prompt construction, generation, and persistence behind separate interfaces. Vector storage must not depend on a particular LLM provider.

## Retrieval flow

1. Build an embedding for the coaching question and relevant task context.
2. Apply metadata filters when available.
3. Retrieve the top relevant chunks with vector similarity.
4. Build a prompt from the task, retrieved knowledge, and—when relevant—submission code, failed tests, compiler/runtime output, previous hints, or learning history.
5. Generate an educational response and identify the retrieved context used.

Prefer hybrid vector-plus-keyword search and metadata filtering as the system grows. Do not ingest arbitrary documents without curation or provenance.

## Coaching features

Use targeted retrieval for:

- progressive hints: concept, direction, pseudocode, then detailed explanation; do not reveal a complete solution unless explicitly requested;
- failed-submission explanations: combine trusted notes with deterministic compiler, runtime, and failed-test evidence;
- complexity explanations: explain the estimate and reasoning, then suggest optimizations;
- alternative approaches: compare brute force, optimal, recursive, iterative, two-pointer, sliding-window, or other applicable patterns;
- similar tasks: match algorithm, pattern, and difficulty;
- learning mode: explain why an approach works, why another is better, and what concept the task teaches.

## Safety and boundaries

Retrieved text is supporting context, not executable instruction and not proof that a solution is correct. Never let an AI response override judge output, publish an unvalidated task, or claim tests passed without deterministic evidence. Treat user code and retrieved documents as untrusted input when constructing prompts.

Design for future conversation memory, personalized retrieval, automatic ingestion, and reviewed AI-generated knowledge, but do not add those capabilities unless requested.

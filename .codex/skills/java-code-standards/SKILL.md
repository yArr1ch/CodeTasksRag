---
name: java-code-standards
description: >
  Java/Spring code-writing standards that mirror how this codebase is reviewed, so code
  is written to pass review the first time. Covers domain-reflective naming, minimal
  @Transactional, avoiding needless classes/abstractions, preferring derived Spring Data
  repository methods over @Query, and keeping logic scoped to the flow that needs it.
  Apply when writing, refactoring, or reviewing Java/Spring backend code (entities,
  repositories, services, controllers). Also invocable as /java-code-standards.
---

# Java Code Standards

One ruleset for **writing** and **reviewing** Java/Spring code here. The point: write code so
review has nothing to flag. Each rule below states how to write it, *why*, and what review
catches when it's violated.

Scope: Java 25 / Spring Boot 3.5, Gradle, Spring Data JPA, records + `@With`, Liquibase
(forward-only). Blocking MVC, not reactive.

When reviewing, keep comments terse and actionable (one line: location, problem, fix) — see
the `caveman-review` skill for format.

---

## 1. Naming reflects the domain, not the implementation

Names mirror the domain concept, read straight, and don't restate the type they live on.

- A repository/storage for `Dashboard` exposes `getById(...)` — call site reads `dashboard.getById(id)`.
- **Don't** prefix methods with the holder's type: `getByStorageId`, `dashboardStorageFindDashboard`.
- Prefer the plain, expected verb (`getById`, `findBySlug`, `save`) over clever or redundant names.

```java
// ✗ name restates the class / leaks the implementation type
dashboardStorage.getByStorageId(id);

// ✓ reads as the domain action
dashboardStorage.getById(id);
```

**Why:** redundant qualifiers add noise and drift from the domain language.
**Review flags:** method/variable names that repeat the enclosing type or describe the
mechanism instead of the domain.

---

## 2. `@Transactional` only where a transaction is actually needed

Annotate when there's a real boundary: multiple writes that must commit/rollback together,
or a read that needs a consistent snapshot / lazy-loading inside the unit of work.

- A single repository call (`repo.save(x)`, `repo.findById(id)`) does **not** need `@Transactional`.
- Don't blanket-annotate every service method.
- Use `@Transactional(readOnly = true)` only when a read path genuinely needs it (lazy
  associations, multi-query consistency) — not reflexively.

```java
// ✗ single delegated call, no boundary to manage
@Transactional
public Dashboard getById(UUID id) { return repo.getById(id); }

// ✓ annotate the multi-step write that must be atomic
@Transactional
public Dashboard rename(UUID id, String name) {
    var entity = repo.getById(id);
    entity.setName(name);
    return repo.save(entity);
}
```

**Why:** needless `@Transactional` adds proxy overhead, hides the real boundaries, and
misleads the next reader about what's atomic.
**Review flags:** `@Transactional` on single-call or read-only-by-accident methods; missing
it on genuine multi-step writes.

---

## 3. Don't create a class/interface until it's needed

Build the minimum that solves the task now (YAGNI). Don't split out a class, interface, or
helper "in case" it's reused.

- One implementation and one caller → inline it; introduce the abstraction when a *second*
  real use appears.
- Before adding a type, ask: does this reduce complexity *today*, or just add indirection?
- Prefer collapsing small single-use helpers back into their caller over scattering them.

**Why:** speculative types create mess and uncertainty — readers can't tell if an
abstraction is load-bearing or aspirational.
**Review flags:** new interface with one impl, wrapper/util class with one caller, premature
generalization. Suggest inlining.

---

## 4. Prefer derived Spring Data methods; `@Query` only when needed

Let Spring Data derive the query from the method name. Reach for `@Query` only when the
query genuinely can't be expressed that way.

- `findBySlug`, `findByStatus`, `existsByEmail`, `deleteByDashboardId` → derived.
- `@Query` (JPQL) when you need explicit `JOIN FETCH` (N+1), DTO/projection construction,
  or predicates the method-name DSL can't express.

```java
// ✓ derived — no @Query needed
Optional<DashboardEntity> findBySlug(String slug);
List<ArtifactEntity> findByDashboardId(UUID dashboardId);

// ✓ @Query justified — JOIN FETCH to avoid N+1
@Query("select d from DashboardEntity d join fetch d.artifacts where d.id = :id")
Optional<DashboardEntity> findWithArtifacts(@Param("id") UUID id);
```

**Why:** derived methods are self-documenting and less error-prone; `@Query` is for cases
that earn it.
**Review flags:** a `@Query` that exactly reproduces what a derived method would do.

---

## 5. Keep logic scoped to the flow that needs it

If logic only matters to one flow, keep it inside that flow. Don't widen visibility or
promote something to a shared/public/general utility until more than one caller actually
needs it.

- Default to the **narrowest visibility**: package-private for classes/methods/fields unless
  broader access is explicitly required.
- A private helper used by one flow stays private and local — don't lift it into a shared
  service or `public static` util on spec.
- Promote to shared scope only when a second real consumer arrives.

**Why:** prematurely exposing logic enlarges the surface area, invites accidental coupling,
and obscures where behavior really belongs (favor logical locality).
**Review flags:** `public`/shared placement for single-flow logic; helpers hoisted out of
their only caller; visibility wider than the actual usage.

---

## 6. Explicit imports, no wildcards

Import each type explicitly. Don't use `import java.util.*` (or any `.*` wildcard).

```java
// ✗
import java.util.*;

// ✓
import java.util.List;
import java.util.Optional;
```

**Why:** explicit imports make dependencies visible and avoid ambiguity; it's the house
style here.
**Review flags:** any `.*` wildcard import. Replace with the explicit types.

---

## 7. Package-private by default for classes

A class only used within its own package should be package-private (no `public` modifier).
Make it `public` only when something in another package actually uses it.

```java
// ✗ public but only referenced inside this package
public class DashboardMapper { ... }

// ✓ narrowest visibility that works
class DashboardMapper { ... }
```

**Why:** the narrowest visibility keeps the module's surface area small and signals what's
truly part of the package's contract. (Reinforces rule 5.)
**Review flags:** `public` class with no cross-package usage. Drop the modifier.

---

## 8. Add a DB index only when current requirements need it

Don't add an index speculatively. Every index slows inserts/updates and grows with the
table — add one only when a real query/requirement justifies it now.

- Tie each index to an actual read path (a filter, join, or uniqueness constraint we use).
- "Might query by this later" is not a reason — add it when that query exists.
- Indexes go in forward-only Liquibase migrations.

**Why:** unnecessary indexes cost write throughput and storage as the table grows, for no
current benefit.
**Review flags:** a new index with no corresponding query/requirement. Ask what reads
justify it; if none, drop it.

---

## 9. Test naming: `methodName_stateUnderTest_expectedBehavior`

Name test methods in three underscore-separated parts: the method/unit under test, the
state/condition, and the expected outcome. It says what's tested and what should happen
without reading the body.

```java
// ✓
void getById_unknownId_throwsNotFound() { ... }
void rename_blankName_rejectsWithValidationError() { ... }
void findBySlug_existingSlug_returnsDashboard() { ... }

// ✗ — opaque, no state/expectation
void testGetById() { ... }
void worksCorrectly() { ... }
```

**Why:** a self-describing name makes failures readable in test reports and documents the
behavior contract.
**Review flags:** test names missing state or expected behavior (`testX`, `xWorks`).

---

## 10. No unused / leftover imports

Remove imports that are no longer referenced. After refactors or deletions, check that no
orphaned imports remain.

**Why:** stale imports are dead code, add noise, and can mask real dependencies. The build
treats warnings as errors, so they shouldn't ship.
**Review flags:** any imported type not used in the file. Delete it.

---

## 11. Use existing methods/utils consistently — don't reinvent

Before writing a helper or inline snippet, check whether the codebase already has a method,
util, or mapper for it, and use it. When several call sites do the same thing, they should
go through the same one.

- Reuse the existing helper instead of hand-rolling a parallel one.
- If two paths solve the same problem differently, converge them on one approach.
- Match the established idiom in the touched module rather than introducing a new style.

```java
// ✗ re-deriving what a shared util already does
var slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-");

// ✓ use the canonical helper everyone else uses
var slug = SlugUtils.toSlug(name);
```

**Why:** duplicate/divergent implementations drift apart and create inconsistent behavior
and more to maintain. One canonical path is easier to reason about and change.
**Review flags:** a new helper duplicating an existing one; the same operation done
differently across call sites. Point to the existing util.

---

## 12. Pass long/unbounded values as request params or body, not path variables

When a value can be long or unbounded — free-form ids, keys, job names, tokens — take it as
a query param or request body, not a `@PathVariable`. Reserve path variables for short,
bounded identifiers that read naturally in the URL.

```java
// ✗ long/free-form value crammed into the path
@GetMapping("/jobs/{jobName}")
Job get(@PathVariable String jobName) { ... }

// ✓ query param for the unbounded value
@GetMapping("/jobs")
Job get(@RequestParam String jobName) { ... }
```

**Why:** long path segments hit URL-length limits, complicate encoding of special
characters, and leak values into logs/history. Params/body handle length and encoding
cleanly.
**Review flags:** long/free-form or user-supplied strings (keys, names, tokens) as
`@PathVariable`. Move to `@RequestParam` or the request body.

---

## 13. Put logic in the service; keep controllers thin

Business logic, validation, and verification live in the service layer. The controller only
adapts HTTP (bind, delegate, return) and calls the service method.

- When behavior changes, update the service — the controller keeps calling the same method.
- Don't verify/branch business rules in the controller; the service owns them.
- Multiple entry points (controller, worker, another service) should call the **same**
  service method so the rules run once, in one place.

```java
// ✗ verification logic leaking into the controller
@PostMapping
Dashboard create(@RequestBody DashboardRequest req) {
    if (repo.existsBySlug(req.slug())) throw new ConflictException();
    return service.create(req);
}

// ✓ controller delegates; service verifies
@PostMapping
Dashboard create(@RequestBody DashboardRequest req) {
    return service.create(req);   // service checks slug uniqueness, etc.
}
```

**Why:** centralizing logic in the service keeps controllers substitutable, avoids rule
duplication across entry points, and keeps behavior testable without the web layer.
**Review flags:** validation/business branching in a controller; logic that should be
shared but lives in one caller. Push it into the service.

---

## 14. Keep response mapping on the response type, not in the service

`to`/`from` conversion (entity ↔ DTO) belongs on the response class itself — e.g. a static
`from(...)` factory on the record — not scattered as mapping methods in the service.

```java
// ✓ mapping lives on the response record
public record DashboardResponse(UUID id, String name) {
    static DashboardResponse from(DashboardEntity entity) {
        return new DashboardResponse(entity.getId(), entity.getName());
    }
}

// service just calls it
return DashboardResponse.from(service.getById(id));

// ✗ mapping method living in the service
class DashboardService {
    DashboardResponse toResponse(DashboardEntity e) { ... }
}
```

**Why:** the response type owns its own shape, so the mapping is discoverable, reusable
across callers, and keeps the service focused on behavior (behavior/invariants stay with the
record). Matches the repo's "keep behavior in domain records" convention.
**Review flags:** `toResponse`/`fromEntity` mapping methods defined in the service or a
loose mapper when they could be a `from(...)` factory on the response class.

---

## 15. Prefer streams over imperative loops for transformations

Use the Streams API for map/filter/collect/reduce-style work instead of hand-written `for`
loops. It reads as *what* is computed, not *how* to iterate.

```java
// ✗ imperative loop building a list
List<DashboardResponse> result = new ArrayList<>();
for (DashboardEntity e : entities) {
    if (e.isActive()) result.add(DashboardResponse.from(e));
}

// ✓ stream
List<DashboardResponse> result = entities.stream()
    .filter(DashboardEntity::isActive)
    .map(DashboardResponse::from)
    .toList();
```

Keep it pragmatic: a plain loop is fine when the stream would be *less* clear — heavy
side effects, early `break`/`return`, or index juggling. Don't force a convoluted stream
just to avoid a loop.

**Why:** streams make transformations declarative and consistent with the rest of the
codebase; less boilerplate, fewer mutable accumulators.
**Review flags:** a `for`/`while` loop doing a straightforward map/filter/collect. Suggest
the stream equivalent.

---

## 16. Response type must match what the endpoint actually returns

The declared response type has to reflect the data the endpoint returns. Don't reuse a
domain-specific DTO for an API that returns different data — return a DTO whose shape
matches the payload.

- If an endpoint's returned data changes, update its response type (or introduce the right
  one) — don't leave a stale/borrowed DTO.
- Don't stretch one DTO across endpoints with genuinely different shapes just to reuse it.

**Why:** a mismatched response type misleads clients and the generated OpenAPI/Swagger
contract, and hides fields that are actually returned (or advertises ones that aren't).
**Review flags:** endpoint returning a DTO that doesn't match its real payload; a
domain/other-feature DTO reused for an API with a different shape.

---

## 17. Keep Swagger/OpenAPI summaries in sync with the method

When a method's behavior, params, or return change, update its Swagger annotations
(`@Operation` summary, `@Parameter`, response schema) in the same change. The doc must
describe what the method now does.

**Why:** Swagger is the served contract external clients rely on; a stale summary is worse
than none — it actively misleads.
**Review flags:** a changed endpoint whose `@Operation`/`@Parameter`/summary still
describes the old behavior. Update it alongside the code.

---

## Quick review checklist

- [ ] Names read as the domain action; no type-name echo (`getByStorageId`).
- [ ] `@Transactional` present only on real multi-step writes / needed reads.
- [ ] No single-use class/interface/helper added speculatively.
- [ ] Derived repo method used unless `@Query` is genuinely required.
- [ ] Single-flow logic kept local and at narrowest visibility.
- [ ] Explicit imports, no `.*` wildcards.
- [ ] Classes package-private unless used cross-package.
- [ ] New DB index tied to a real current query, not speculative.
- [ ] Test names follow `methodName_stateUnderTest_expectedBehavior`.
- [ ] No unused / leftover imports.
- [ ] Reuses existing methods/utils; no duplicate or divergent implementations.
- [ ] Long/unbounded values passed as request param/body, not path variable.
- [ ] Logic lives in the service; controller only delegates.
- [ ] Response mapping (`from`/`to`) lives on the response type, not the service.
- [ ] Streams used for straightforward map/filter/collect instead of manual loops.
- [ ] Response type matches the endpoint's actual payload; no borrowed/mismatched DTO.
- [ ] Swagger `@Operation`/`@Parameter` summaries updated to match the method.

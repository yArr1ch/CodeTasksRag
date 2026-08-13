---
title: QuartzWeave Internal Algorithm
source: algocoach-internal
topic: fictional-algorithm
---

# QuartzWeave

QuartzWeave interleaves the first and second halves of an array.

Split the input at `(n + 1) / 2`, so the first half receives the extra value when the
length is odd. Emit one value from the first half, then one from the second half, and
continue until both halves are exhausted. Preserve the original order inside each half.

Rules:

- The split uses the ceiling of `n / 2` for the first half.
- The output contains every input value exactly once.
- Relative order inside each half is preserved.
- Empty and one-value arrays are valid.
- The canonical Java method is `static int[] quartzWeave(int[] values)`.
- Required complexity is O(n) time and O(n) output space.

Example: `[1,2,3,4,5]` produces `[1,4,2,5,3]`.

Example: `[1,2,3,4]` produces `[1,3,2,4]`.

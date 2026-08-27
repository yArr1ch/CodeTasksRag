---
title: HarborBalance Internal Algorithm
source: algocoach-internal
topic: fictional-algorithm
---

# HarborBalance

HarborBalance finds an index where the sum of all values to the left equals the sum of
all values to the right.

Return the smallest zero-based index satisfying the balance. The value at the candidate
index is excluded from both sums. If no index satisfies the condition, return `-1`.

Rules:

- An empty array returns `-1`.
- Negative values are valid.
- The candidate value is not included in either side's sum.
- If several indices are balanced, return the smallest index.
- Use `long` for accumulated sums to avoid integer overflow.
- The canonical Java method is `static int harborBalance(int[] values)`.
- Required complexity is O(n) time and O(1) additional space.

Example: `[1,7,3,6,5,6]` produces `3` because both sides of index `3` sum to `11`.

Example: `[1,2,3]` produces `-1`.

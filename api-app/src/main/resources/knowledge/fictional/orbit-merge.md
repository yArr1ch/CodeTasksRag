---
title: OrbitMerge Internal Algorithm
source: algocoach-internal
topic: fictional-algorithm
---

# OrbitMerge

OrbitMerge combines two sorted integer arrays by alternating direction on each output position.

The first output position takes the smallest remaining value from either input. The next position takes the largest remaining value from either input. The direction continues to alternate: smallest, largest, smallest, largest. Values are removed from the selected input. Equal values must be taken from the left input first.

Rules:

- Both input arrays are sorted in nondecreasing order.
- The result contains every input value exactly once.
- If one input is exhausted, continue taking from the other input while respecting the current direction as much as possible.
- Duplicate values are preserved.
- The canonical Java method is `static int[] orbitMerge(int[] left, int[] right)`.
- Required complexity is O(n + m) time and O(n + m) output space.

Example: `left=[1, 4, 8]`, `right=[2, 3, 9]` produces `[1, 9, 2, 8, 3, 4]`.

Example: `left=[]`, `right=[2, 5]` produces `[2, 5]`.


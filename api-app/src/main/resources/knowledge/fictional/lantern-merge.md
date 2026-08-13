---
title: LanternMerge Internal Algorithm
source: algocoach-internal
topic: fictional-algorithm
---

# LanternMerge

LanternMerge produces the sorted union of two sorted integer arrays.

Read both arrays from left to right and emit the smallest value that has not been
processed. When the same value appears in either array, emit it only once and advance
past every copy of that value in both arrays. The result must remain sorted.

Rules:

- Both input arrays are sorted in nondecreasing order.
- Duplicate values in one or both inputs appear once in the result.
- Negative values are valid.
- Empty input arrays are valid.
- The result contains every distinct value from either input.
- The canonical Java method is `static int[] lanternMerge(int[] left, int[] right)`.
- Required complexity is O(n + m) time and O(n + m) output space.

Example: `left=[1,2,2,6]` and `right=[2,3,6,9]` produce `[1,2,3,6,9]`.

Example: `left=[]` and `right=[-2,-2,4]` produce `[-2,4]`.

---
title: RuneWindow Internal Algorithm
source: algocoach-internal
topic: fictional-algorithm
---

# RuneWindow

RuneWindow finds the length of the shortest contiguous subarray whose sum is at least a
given limit.

Every input value is nonnegative, so after a window reaches the limit, move its left
boundary rightward while the window remains valid. Return the minimum valid length. If no
window reaches the limit, return `0`.

Rules:

- Input values are nonnegative integers.
- A zero or negative limit returns `0`.
- A valid window has sum greater than or equal to the limit.
- A subarray must be non-empty.
- An empty array returns `0`.
- The canonical Java method is `static int runeWindow(int[] values, long limit)`.
- Required complexity is O(n) time and O(1) additional space.

Example: `[2,3,1,2,4,3]` with limit `7` produces `2` because `[4,3]` is the shortest
valid window.

Example: `[1,1,1]` with limit `5` produces `0`.

---
title: CinderGate Internal Algorithm
source: algocoach-internal
topic: fictional-algorithm
---

# CinderGate

CinderGate counts contiguous subarrays whose values add up to an exact target.

The target and array values may be positive, zero, or negative. Count every pair of
inclusive boundaries separately, even when two subarrays contain the same values. The
answer is the number of matching subarrays, not the longest length or the list of
boundaries.

Rules:

- An empty array returns `0`.
- Negative values and a negative target are valid.
- A subarray must be non-empty.
- Use `long` for prefix sums and for the returned count.
- The canonical Java method is `static long cinderGate(int[] values, long target)`.
- Required complexity is O(n) expected time and O(n) additional space.

Example: `[1,-1,0,2,-2]` with target `0` produces `6`.

Example: `[2,2,2]` with target `4` produces `2` because the matching subarrays are the
first two values and the last two values.

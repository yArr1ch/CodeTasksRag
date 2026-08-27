---
title: BeaconWindow Internal Algorithm
source: algocoach-internal
topic: fictional-algorithm
---

# BeaconWindow

BeaconWindow finds the longest contiguous segment whose beacon score is strictly below a limit.

Each input value is a nonnegative beacon score. The window is valid when the sum of all values inside it is less than the supplied limit. When the sum reaches or exceeds the limit, move the left boundary until the window is valid again. Return the length of the longest valid segment. A zero or negative limit produces zero.

Rules:

- Input values are nonnegative, so the two-pointer method is valid.
- An empty array produces zero.
- The comparison is strictly `< limit`, not `<= limit`.
- The canonical Java method is `static int beaconWindow(int[] scores, long limit)`.
- Required complexity is O(n) time and O(1) extra space.

Example: scores `[2,1,2,1]` with limit `4` produces `2` because `[2,1]` is valid while `[2,1,2]` is not.

Example: scores `[0,0,1]` with limit `1` produces `2`.


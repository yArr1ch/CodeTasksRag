---
title: Two Pointers
source: internal-algorithm-notes
topic: two-pointers
---

# Two Pointers

## What it is

The two-pointers technique uses two indexes to scan a sequence. The indexes may start at opposite ends or move through the sequence at different speeds.

The key idea is to eliminate impossible candidates after each comparison instead of checking every pair.

## When to consider it

Look for these signals:

- the input is sorted or can safely be sorted;
- the task asks about pairs or intervals;
- the answer depends on values at the left and right boundaries;
- a brute-force pair search has O(n²) complexity.

## General approach for a sorted array

1. Place one pointer at the beginning and one at the end.
2. Evaluate the values at both pointers.
3. If the current result is too small, move the left pointer forward.
4. If the current result is too large, move the right pointer backward.
5. Stop when the pointers meet or the required result is found.

## Important invariant

Moving a pointer must discard only candidates that cannot produce a valid answer. The reason for moving the pointer should be explained by the ordering of the input.

## Common mistakes

- applying the technique to an unsorted input without preserving the original indexes when they matter;
- moving both pointers when only one movement is justified;
- skipping duplicate values incorrectly;
- using two pointers when the condition requires arbitrary, non-contiguous elements.

## Complexity

If each pointer only moves forward or backward through the input, the time complexity is usually O(n), with O(1) additional space apart from any required result storage.

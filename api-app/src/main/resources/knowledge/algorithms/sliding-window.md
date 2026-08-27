---
title: Sliding Window
source: internal-algorithm-notes
topic: sliding-window
---

# Sliding Window

## What it is

Sliding window is a technique for processing a contiguous part of an array or string while maintaining information about the current range.

Instead of recomputing every range from the beginning, move the right boundary forward and remove elements from the left when the range becomes invalid.

## When to consider it

Look for these signals:

- the task asks about a contiguous subarray or substring;
- the task asks for the longest, shortest, or number of valid ranges;
- a range must satisfy a condition while it grows or shrinks;
- a brute-force solution checks many overlapping ranges.

## General approach

1. Start both boundaries at the beginning of the input.
2. Extend the right boundary and include the new element in the window state.
3. While the window violates the required condition, remove the leftmost element and move the left boundary.
4. Update the answer using the valid window.

## Example pattern

For the longest substring without repeated characters, keep a set of characters in the current window. Add characters from the right. When a duplicate appears, remove characters from the left until the duplicate is removed.

## Common mistakes

- forgetting to remove the leftmost element from the state;
- updating the answer before restoring the window invariant;
- confusing a contiguous range with a subsequence;
- using a nested scan that makes the solution quadratic.

## Complexity

When each element enters and leaves the window at most once, the time complexity is O(n) and the additional space complexity is O(k), where k is the number of tracked values.

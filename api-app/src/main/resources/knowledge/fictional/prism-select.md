---
title: PrismSelect Internal Algorithm
source: algocoach-internal
topic: fictional-algorithm
---

# PrismSelect

PrismSelect chooses one representative from each consecutive run of equal strings.

The input is already sorted lexicographically. For each run, keep the lexicographically smallest string with an odd length; if the run has no odd-length string, keep the first string in that run. The output keeps the order of runs and never sorts again.

Rules:

- An empty input produces an empty output.
- Strings are compared case-sensitively using Java natural ordering.
- Empty strings have even length.
- The canonical Java method is `static List<String> prismSelect(List<String> values)`.
- Required complexity is O(n) comparisons plus the cost of scanning string lengths.

Example: `["aa", "aa", "abc", "abc", "xyz"]` produces `["aa", "abc", "xyz"]`; `abc` is selected from its run because it has odd length.

Example: `["cat", "cat", "dog", "dog"]` produces `["cat", "dog"]` because both runs contain an odd-length value.


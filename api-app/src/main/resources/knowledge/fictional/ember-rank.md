---
title: EmberRank Internal Algorithm
source: algocoach-internal
topic: fictional-algorithm
---

# EmberRank

EmberRank replaces every integer with its zero-based rank among the distinct values in
the array.

Rank `0` belongs to the smallest distinct value, rank `1` to the next distinct value,
and so on. Equal input values always receive the same rank. The output preserves the
original positions of the input values.

Rules:

- Ranking uses numeric ascending order.
- Duplicate values share one rank.
- Negative values are valid.
- An empty array produces an empty array.
- The canonical Java method is `static int[] emberRank(int[] values)`.
- Required complexity is O(n log n) time and O(n) additional space.

Example: `[40,10,40,20]` produces `[2,0,2,1]`.

Example: `[-5,-5,0,7]` produces `[0,0,1,2]`.

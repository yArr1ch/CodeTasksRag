---
title: AsterFold Internal Algorithm
source: algocoach-internal
topic: fictional-algorithm
---

# AsterFold

AsterFold transforms an integer array in fixed blocks of three elements.

For every complete block `[a, b, c]`, exchange the second and third values and produce `[a, c, b]`. An incomplete final block is copied without modification. Blocks are processed from left to right and are never merged.

Rules:

- An empty array is valid and produces an empty array.
- A one- or two-element final block remains unchanged.
- Negative values and duplicate values are processed normally.
- The transformation must be in-place.
- Required complexity is O(n) time and O(1) extra space.
- The canonical Java method is `static void asterFold(int[] values)`.

Example: `[1, 2, 3, 4, 5, 6, 7]` becomes `[1, 3, 2, 4, 6, 5, 7]`.

Example: `[9, 8]` remains `[9, 8]` because the final block is incomplete.


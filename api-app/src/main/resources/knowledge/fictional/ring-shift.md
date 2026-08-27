---
title: RingShift Internal Algorithm
source: algocoach-internal
topic: fictional-algorithm
---

# RingShift

RingShift rotates each concentric ring of a rectangular matrix by a different amount.

The outer ring rotates right by one position. The next ring rotates right by two positions. The rotation amount increases by one for every inward ring. A ring is listed clockwise starting at its top-left corner, rotated, and written back along the same perimeter. Single-cell center rings are unchanged.

Rules:

- The matrix must be rectangular.
- A one-row or one-column matrix is one ring.
- Rotation is to the right, not to the left.
- Rotation amounts are reduced modulo the ring length.
- The canonical Java method is `static void ringShift(int[][] matrix)`.
- Required extra space is O(max(rows, columns)) for one ring buffer.

Example: a one-row matrix `[1,2,3,4]` becomes `[4,1,2,3]`.

For a three-by-three matrix, the outer ring rotates by one and the center cell remains unchanged.


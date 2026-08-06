---
title: SpiralLatch Internal Algorithm
source: algocoach-internal
topic: fictional-algorithm
---

# SpiralLatch

SpiralLatch reads a rectangular matrix in clockwise spiral order, but locks every second layer before reading it.

Layer zero is read normally. Layer one is read in reverse clockwise order, which is equivalent to traversing its perimeter counterclockwise. Layer two is normal again, and the direction alternates for every layer. A single-row or single-column layer is read once, never twice.

Rules:

- The matrix has at least one row and one column.
- Rows may have different lengths only if the input is normalized into a rectangle first; the task input is always rectangular.
- The output contains every matrix cell exactly once.
- The canonical Java method is `static List<Integer> spiralLatch(int[][] matrix)`.
- Required complexity is O(rows * columns) time.

Example for `[[1,2,3],[4,5,6],[7,8,9]]`: the output is `[1,2,3,6,9,8,7,4,5]` because the outer layer is normal and the inner layer contains only `5`.

For a four-by-four matrix, the second perimeter must be traversed in the opposite direction from the outer perimeter.


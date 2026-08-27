---
title: EchoPartition Internal Algorithm
source: algocoach-internal
topic: fictional-algorithm
---

# EchoPartition

EchoPartition rearranges an integer array around a pivot while preserving the relative order inside both partitions.

Values strictly smaller than the pivot appear first, values equal to the pivot appear second, and values strictly greater than the pivot appear last. The pivot is a value, not an index. The algorithm must be stable: two values from the same partition retain their original order.

Rules:

- The pivot may be absent from the input.
- Negative values are valid.
- The result must contain the same number of values as the input.
- The canonical Java method is `static int[] echoPartition(int[] values, int pivot)`.
- Required complexity is O(n) time and O(n) additional space because stability is required.

Example: values `[4,2,5,2,3]` and pivot `2` produce `[2,2,4,5,3]`. The equal values come first in their original order, followed by the greater values in their original order.

Values equal to the pivot belong in the middle partition, not in either outer partition.

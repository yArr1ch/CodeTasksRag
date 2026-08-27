---
title: TideCompress Internal Algorithm
source: algocoach-internal
topic: fictional-algorithm
---

# TideCompress

TideCompress keeps the first value and the turning points of a numeric sequence.

For every new value, compare the direction from the previous input value to the current input value with the previous nonzero direction. If the direction is unchanged, replace the last retained value with the current value. If the direction changes, retain the previous turning value and then retain the current value. Equal values are always retained and reset the direction. A direction is positive when the next value is greater and negative when it is smaller.

Rules:

- An empty input produces an empty output.
- Equal adjacent values are retained and reset the direction.
- The first value is always retained.
- The canonical Java method is `static int[] tideCompress(int[] values)`.
- The output must preserve retained values in original order.

Example: `[1,2,3,2,2,5]` produces `[1,3,2,2,5]`. The direction changes from increasing to decreasing at `3`, the equal `2` is retained and resets the direction, and the final increase retains `5`.

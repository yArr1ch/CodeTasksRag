---
title: MosaicReverse Internal Algorithm
source: algocoach-internal
topic: fictional-algorithm
---

# MosaicReverse

MosaicReverse reverses every maximal contiguous block of integers that have the same
parity.

Scan from left to right. A block starts at the current value and continues while every
next value is either even like the first value or odd like the first value. Reverse the
values inside that block, then continue with the next block. Parity is based on the
mathematical remainder: negative even numbers are even and negative odd numbers are odd.

Rules:

- A block contains adjacent values with the same even/odd classification.
- A parity change ends the current block.
- A one-value block is unchanged.
- The transformation must be performed in-place.
- The result contains the same values and has the same length as the input.
- The canonical Java method is `static void mosaicReverse(int[] values)`.
- Required complexity is O(n) time and O(1) additional space.

Example: `[1,3,2,4,6,5,7]` becomes `[3,1,6,4,2,7,5]`.

Example: `[-4,-2,-3,0,1]` becomes `[-2,-4,-3,0,1]`.

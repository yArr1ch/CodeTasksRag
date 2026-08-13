---
title: GlyphLatch Internal Algorithm
source: algocoach-internal
topic: fictional-algorithm
---

# GlyphLatch

GlyphLatch run-length encodes a string of lowercase English letters.

For every maximal consecutive run of the same character, append the decimal run length,
then a colon, then the character. Runs are emitted from left to right. The colon is part
of the output format and makes multi-digit counts unambiguous.

Rules:

- The input contains only lowercase English letters from `a` through `z`.
- An empty string produces an empty string.
- Each run is encoded exactly once.
- The original character order is preserved.
- The canonical Java method is `static String glyphLatch(String text)`.
- Required complexity is O(n) time and O(n) output space.

Example: `"aaabbcaaaa"` produces `"3:a2:b1:c4:a"`.

Example: `"z"` produces `"1:z"`.

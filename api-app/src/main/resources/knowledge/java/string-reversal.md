---
title: Reversing a String in Java
source: internal-java-notes
topic: java-strings
---

# Reversing a String in Java

## Basic approach

To reverse a string, process its characters from the last position to the first position and append them to a result.

In Java, `StringBuilder` is appropriate when characters are appended repeatedly because it is mutable and avoids creating a new string for every concatenation.

## Step-by-step reasoning

1. Create an empty `StringBuilder`.
2. Start at `input.length() - 1`.
3. Append each character while moving toward index zero.
4. Convert the builder to a string.

## Edge cases

- an empty string should produce an empty string;
- a one-character string is already reversed;
- spaces are characters and should be preserved unless the task says otherwise;
- do not assume the input contains only letters.

## Alternative approach

`StringBuilder` also provides a `reverse()` operation. It is concise, but a manual loop can be useful when the task is intended to teach traversal or when additional character-level rules are required.

## Complexity

Reversing a string requires O(n) time and O(n) additional space for the result.

## Common mistake

Repeatedly concatenating strings inside a loop may create many intermediate objects. Prefer `StringBuilder` for repeated appends.

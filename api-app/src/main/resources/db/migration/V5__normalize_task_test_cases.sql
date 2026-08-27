UPDATE tasks
SET test_cases = normalized.test_cases
FROM (
    SELECT id,
           COALESCE(
               jsonb_agg(
                   CASE
                       WHEN jsonb_typeof(item) = 'object' AND item ? 'input'
                           THEN CASE jsonb_typeof(item -> 'input')
                               WHEN 'array' THEN to_jsonb(
                                   (SELECT string_agg(value, ' ')
                                    FROM jsonb_array_elements_text(item -> 'input'))
                               )
                               ELSE item -> 'input'
                           END
                       ELSE item
                   END
               ),
               '[]'::jsonb
           ) AS test_cases
    FROM tasks
    CROSS JOIN LATERAL jsonb_array_elements(tasks.test_cases) AS test_case(item)
    GROUP BY tasks.id
) AS normalized
WHERE tasks.id = normalized.id
  AND tasks.test_cases IS NOT NULL;

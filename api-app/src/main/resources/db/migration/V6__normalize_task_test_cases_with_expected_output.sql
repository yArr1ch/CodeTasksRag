UPDATE tasks
SET test_cases = normalized.test_cases
FROM (
    SELECT tasks.id,
           jsonb_agg(
               CASE
                   WHEN jsonb_typeof(test_case.item) = 'string'
                       THEN jsonb_build_object(
                           'input', test_case.item,
                           'expectedOutput', NULL
                       )
                   ELSE test_case.item
               END
           ) AS test_cases
    FROM tasks
    CROSS JOIN LATERAL jsonb_array_elements(tasks.test_cases) AS test_case(item)
    GROUP BY tasks.id
) normalized
WHERE tasks.id = normalized.id
  AND tasks.test_cases IS NOT NULL;

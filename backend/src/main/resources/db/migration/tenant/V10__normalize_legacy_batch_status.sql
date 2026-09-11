-- Convert legacy ordinal enum values written by the previous Batch mapping.
UPDATE batches
SET status = CASE TRIM(status)
    WHEN '0' THEN 'PENDING'
    WHEN '1' THEN 'PROCESSED'
    WHEN '2' THEN 'FAILED'
    WHEN '3' THEN 'REVIEWED'
    ELSE status
END
WHERE TRIM(status) IN ('0', '1', '2', '3');

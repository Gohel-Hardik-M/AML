package com.aml.system.batch;

import com.aml.system.repository.TransactionBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Custom SkipPolicy for Resilient 1 Crore Record Batch Execution.
 *
 * ARCHITECTURAL PURPOSE (POISON PILL ISOLATION):
 * In 10M record batch files, data quality anomalies (e.g. malformed dates, truncated delimiters,
 * unparseable amounts, unescaped commas) are guaranteed to occur.
 *
 * If uncaught, a single malformed row ("poison pill") would abort the entire multi-hour batch job.
 * This SkipPolicy:
 * 1. Catches FlatFileParseException and NumberFormatException.
 * 2. Writes the raw bad record and exception stack to the `aml_invalid_transactions_dlq` Dead Letter Queue table.
 * 3. Increments the skip metric and allows the job to proceed uninterrupted.
 * 4. Enforces a maximum skip ceiling (e.g. 50,000 corrupt rows) before safely failing the job.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AmlSkipPolicy implements SkipPolicy {

    private final TransactionBatchRepository transactionBatchRepository;
    private static final int MAX_ALLOWABLE_SKIPS = 50000;

    @Override
    public boolean shouldSkip(Throwable t, long skipCount) throws SkipLimitExceededException {
        if (skipCount >= MAX_ALLOWABLE_SKIPS) {
            log.error("Maximum allowable skip limit [{}] exceeded! Aborting batch job to prevent widespread data corruption.", MAX_ALLOWABLE_SKIPS);
            return false;
        }

        if (t instanceof FlatFileParseException ffpe) {
            log.warn("Encountered Poison Pill record at line {}: Input: '{}'. Error: {}",
                    ffpe.getLineNumber(), ffpe.getInput(), ffpe.getMessage());

            try {
                // Log to Dead Letter Queue (DLQ) in isolated transaction
                transactionBatchRepository.logPoisonPill(
                        UUID.randomUUID(),
                        "UNKNOWN_TENANT",
                        ffpe.getInput(),
                        "FlatFileParseException at line " + ffpe.getLineNumber() + ": " + ffpe.getMessage()
                );
            } catch (Exception dlqEx) {
                log.error("Failed to write poison pill to DLQ table: {}", dlqEx.getMessage());
            }

            return true;
        }

        if (t instanceof NumberFormatException nfe) {
            log.warn("Encountered number parsing error during record conversion: {}", nfe.getMessage());
            return true;
        }

        if (t instanceof IllegalArgumentException iae) {
            log.warn("Encountered invalid argument during item transformation: {}", iae.getMessage());
            return true;
        }

        // Do not skip unexpected database crashes, network disconnects, or OutOfMemoryErrors
        log.error("Fatal non-skippable exception encountered during batch step: {}", t.getMessage(), t);
        return false;
    }
}

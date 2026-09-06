package com.aml.system.batch;

import com.aml.system.service.AmlEltService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobCompletionNotificationListener implements JobExecutionListener {

    private final AmlEltService amlEltService;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Starting Spring Batch Job. Execution ID: {}", jobExecution.getId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String batchIdStr = jobExecution.getJobParameters().getString("batchId");
        String tenantId = jobExecution.getJobParameters().getString("tenantId");
        String localFilePath = jobExecution.getJobParameters().getString("localFilePath");

        // 1. O(1) Storage Cleanup: Delete the temporary local file immediately
        if (localFilePath != null) {
            try {
                Files.deleteIfExists(Path.of(localFilePath));
                log.info("Disk cleanup successful. Deleted temp file: {}", localFilePath);
            } catch (IOException e) {
                log.error("Storage leak warning: Failed to delete temp file: {}", localFilePath, e);
            }
        }

        // 2. Post-Job Actions based on Success or Failure
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("!!! BATCH INGESTION COMPLETED SUCCESSFULLY !!!");

            if (batchIdStr != null && tenantId != null) {
                UUID batchId = UUID.fromString(batchIdStr);
                log.info("Triggering PostgreSQL ELT Stored Procedure for Batch: {}", batchId);

                // Trigger the massive merge on the database layer
                amlEltService.executeEltPipeline(batchId, tenantId);
            }

        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            log.error("BATCH INGESTION FAILED for Batch ID: {}", batchIdStr);
        }
    }
}
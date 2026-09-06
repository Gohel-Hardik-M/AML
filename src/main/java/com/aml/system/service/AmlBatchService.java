package com.aml.system.service;

import com.aml.system.exception.AmlBusinessException;
import com.aml.system.model.BatchEntity;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.repository.BatchRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmlBatchService {

    private final JobLauncher jobLauncher;
    private final Job amlTransactionBatchJob;

    private final BatchRepository batchRepository;
    private final Cloudinary cloudinary;

    // Extracted constants to avoid hardcoding
    private static final String STATUS_PENDING = "PENDING";
    private static final String RESOURCE_TYPE_RAW = "raw";

    @Transactional
    public BatchEntity processAndUploadBatch(MultipartFile file, LocalDate batchDate, String channel) throws Exception {
        String tenantId = TenantContextHolder.getTenantId();

        if (tenantId == null || tenantId.isBlank()) {
            throw new AmlBusinessException("Tenant context missing for upload.", HttpStatus.UNAUTHORIZED);
        }

        // 1. Generate Checksum to prevent duplicate identical files
        String checksum = generateChecksum(file.getBytes());
        if (batchRepository.existsByTenantIdAndFileChecksum(tenantId, checksum)) {
            throw new IllegalArgumentException("This exact file has already been uploaded for tenant: " + tenantId);
        }

        // 2. Dynamic folder path formatting (e.g., icci/batches/2026-09-06)
        String folderPath = String.format("%s/batches/%s", tenantId.toLowerCase(), batchDate.toString());

        // 3. Upload to Cloudinary
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", RESOURCE_TYPE_RAW,
                "folder", folderPath,
                "overwrite", true
        ));

        String relativeCloudinaryPath = uploadResult.get("public_id").toString();

        // 4. Save or Update the Batch Entity
        Optional<BatchEntity> existingBatch = batchRepository.findByTenantIdAndBatchDate(tenantId, batchDate);

        BatchEntity batchToSave;
        if (existingBatch.isPresent()) {
            batchToSave = existingBatch.get();
            batchToSave.setFileName(relativeCloudinaryPath);
            batchToSave.setFileChecksum(checksum);
            batchToSave.setStatus(STATUS_PENDING);
            batchToSave.setChannel(channel);
            batchToSave.setRetryCount(batchToSave.getRetryCount() + 1);
        } else {
            batchToSave = BatchEntity.builder()
                    .tenantId(tenantId)
                    .batchDate(batchDate)
                    .fileName(relativeCloudinaryPath)
                    .fileChecksum(checksum)
                    .channel(channel)
                    .status(STATUS_PENDING)
                    .build();
        }

        log.info("Batch file uploaded to Cloudinary successfully for Tenant: {}, Date: {}", tenantId, batchDate);
        return batchRepository.save(batchToSave);
    }

    private String generateChecksum(byte[] fileBytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(fileBytes);
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Downloads the file from Cloudinary and triggers the Spring Batch Job.
     * NOTE: @Transactional has been REMOVED to prevent Spring Batch metadata collisions!
     */
    public void triggerBatchProcessing(LocalDate batchDate) throws Exception {
        String tenantId = TenantContextHolder.getTenantId();

        if (tenantId == null || tenantId.isBlank()) {
            throw new AmlBusinessException("Tenant context missing.", HttpStatus.UNAUTHORIZED);
        }

        // 1. Fetch the batch record
        BatchEntity batch = batchRepository.findByTenantIdAndBatchDate(tenantId, batchDate)
                .orElseThrow(() -> new AmlBusinessException("No batch found for date: " + batchDate, HttpStatus.NOT_FOUND));

        if (!STATUS_PENDING.equals(batch.getStatus())) {
            throw new AmlBusinessException("Batch must be in PENDING status to process.", HttpStatus.BAD_REQUEST);
        }

        log.info("Preparing to process batch {} for tenant {}", batch.getBatchId(), tenantId);

        // 2. Generate Cloudinary Download URL (Raw files)
        String cloudinaryUrl = cloudinary.url()
                .resourceType(RESOURCE_TYPE_RAW)
                .secure(true)
                .generate(batch.getFileName());

        // 3. Download to a fast local temporary file
        Path tempFile = Files.createTempFile("aml_batch_", "_" + batch.getBatchId() + ".csv");
        try (InputStream in = new URL(cloudinaryUrl).openStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Downloaded Cloudinary file to local temp storage: {}", tempFile.toAbsolutePath());
        } catch (Exception e) {
            throw new AmlBusinessException("Failed to download file from Cloudinary: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 4. Update status to prevent double-processing (Spring Data JPA saves are naturally transactional)
        batch.setStatus("PROCESSING");
        batchRepository.save(batch);

        // 5. Trigger the Spring Batch Job asynchronously
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("localFilePath", tempFile.toAbsolutePath().toString())
                .addString("tenantId", tenantId)
                .addString("batchId", batch.getBatchId().toString())
                .addLong("timestamp", System.currentTimeMillis()) // Ensures uniqueness
                .toJobParameters();

        try {
            // Handoff to Spring Batch!
            jobLauncher.run(amlTransactionBatchJob, jobParameters);
        } catch (Exception e) {
            batch.setStatus("FAILED");
            batchRepository.save(batch);

            // MASSIVE LOG TO CATCH THE EXACT ERROR

            log.error("CRITICAL BATCH LAUNCH ERROR: ", e);


            throw new AmlBusinessException("Failed to launch Spring Batch execution.", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}
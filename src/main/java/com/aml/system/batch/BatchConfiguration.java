package com.aml.system.batch;

import com.aml.system.model.Alert;
import com.aml.system.model.Transaction;
import com.aml.system.model.enums.TransactionType;
import com.aml.system.repository.TransactionBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Spring Batch Configuration for 10M Record AML Ingestion & Rule Evaluation Pipeline.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RuleEngineProcessor ruleEngineProcessor;
    private final AmlSkipPolicy amlSkipPolicy;
    private final TransactionBatchRepository transactionBatchRepository;

    public static final int CHUNK_SIZE = 5000; // Optimal chunk size for PostgreSQL bulk insert throughput

    @Bean
    public Job amlTransactionBatchJob(Step amlTransactionBatchStep) {
        return new JobBuilder("amlTransactionBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(amlTransactionBatchStep)
                .build();
    }

    @Bean
    public Step amlTransactionBatchStep(
            FlatFileItemReader<Transaction> transactionItemReader,
            ItemWriter<List<Alert>> alertBatchItemWriter,
            TaskExecutor batchTaskExecutor
    ) {
        return new StepBuilder("amlTransactionBatchStep", jobRepository)
                .<Transaction, List<Alert>>chunk(CHUNK_SIZE, transactionManager)
                .reader(transactionItemReader)
                .processor(ruleEngineProcessor)
                .writer(alertBatchItemWriter)
                .faultTolerant()
                .skipPolicy(amlSkipPolicy)
                .taskExecutor(batchTaskExecutor) // Multi-threaded step execution for high core utilization
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<Transaction> transactionItemReader(
            @Value("#{jobParameters['filePath']}") String filePath,
            @Value("#{jobParameters['tenantId']}") String tenantId,
            @Value("#{jobParameters['batchId']}") String batchId
    ) {
        log.info("Configuring FlatFileItemReader for tenant [{}] from path [{}]", tenantId, filePath);

        return new FlatFileItemReaderBuilder<Transaction>()
                .name("transactionCsvReader")
                .resource(new FileSystemResource(filePath != null ? filePath : "data/sample_transactions.csv"))
                .delimited()
                .names("transactionId", "sourceAccountId", "destinationAccountId", "customerId",
                        "amount", "currency", "transactionType", "countryCode", "counterpartyCountryCode",
                        "counterpartyName", "channel", "timestamp")
                .linesToSkip(1) // Skip CSV Header
                .fieldSetMapper(new TransactionFieldSetMapper(tenantId, batchId))
                .saveState(false) // Required when running multi-threaded step
                .build();
    }

    @Bean
    public ItemWriter<List<Alert>> alertBatchItemWriter() {
        return chunk -> {
            List<Alert> allAlertsInChunk = new ArrayList<>();
            for (List<Alert> alertList : chunk.getItems()) {
                if (alertList != null && !alertList.isEmpty()) {
                    allAlertsInChunk.addAll(alertList);
                }
            }

            if (!allAlertsInChunk.isEmpty()) {
                log.info("Persisting {} detected alerts to database via JdbcTemplate batchUpdate", allAlertsInChunk.size());
                transactionBatchRepository.insertAlertsBatch(allAlertsInChunk, CHUNK_SIZE);
            }
        };
    }

    @Bean
    public TaskExecutor batchTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("aml-batch-worker-");
        executor.setConcurrencyLimit(8); // Utilize 8 concurrent worker threads
        return executor;
    }

    private static class TransactionFieldSetMapper implements FieldSetMapper<Transaction> {
        private final String tenantId;
        private final UUID batchId;
        private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        public TransactionFieldSetMapper(String tenantId, String batchId) {
            this.tenantId = tenantId != null ? tenantId : "DEFAULT_TENANT";
            this.batchId = batchId != null ? UUID.fromString(batchId) : UUID.randomUUID();
        }

        @Override
        public Transaction mapFieldSet(FieldSet fs) {
            return Transaction.builder()
                    .transactionId(fs.readString("transactionId") != null && !fs.readString("transactionId").isBlank() ?
                            UUID.fromString(fs.readString("transactionId")) : UUID.randomUUID())
                    .tenantId(tenantId)
                    .batchId(batchId)
                    .sourceAccountId(fs.readString("sourceAccountId"))
                    .destinationAccountId(fs.readString("destinationAccountId"))
                    .customerId(fs.readString("customerId"))
                    .amount(new BigDecimal(fs.readString("amount")))
                    .currency(fs.readString("currency"))
                    .transactionType(TransactionType.valueOf(fs.readString("transactionType")))
                    .countryCode(fs.readString("countryCode"))
                    .counterpartyCountryCode(fs.readString("counterpartyCountryCode"))
                    .counterpartyName(fs.readString("counterpartyName"))
                    .channel(fs.readString("channel"))
                    .timestamp(LocalDateTime.parse(fs.readString("timestamp"), formatter))
                    .build();
        }
    }
}

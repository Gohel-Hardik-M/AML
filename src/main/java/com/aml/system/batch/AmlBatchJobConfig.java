package com.aml.system.batch;

import com.aml.system.dto.batch.TransactionCsvInput;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class AmlBatchJobConfig {

    @Bean
    @StepScope
    public FlatFileItemReader<TransactionCsvInput> csvItemReader(
            @Value("#{jobParameters['localFilePath']}") String localFilePath) {

        return new FlatFileItemReaderBuilder<TransactionCsvInput>()
                .name("csvItemReader")
                .resource(new FileSystemResource(localFilePath))
                .linesToSkip(1) // Skip the CSV header row
                .delimited()
                .names("transactionId", "sourceAccountId", "destinationAccountId", "customerId",
                        "amount", "currency", "transactionType", "countryCode",
                        "counterpartyCountryCode", "counterpartyName", "channel", "txnTimestamp")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(TransactionCsvInput.class);
                }})
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<TransactionCsvInput> jdbcBatchItemWriter(DataSource dataSource) {
        String sql = """
            INSERT INTO aml_transactions_staging (
                transaction_id, source_account_id, destination_account_id, customer_id, 
                amount, currency, transaction_type, country_code, 
                counterparty_country_code, counterparty_name, channel, txn_timestamp, 
                tenant_id, batch_id
            ) VALUES (
                CAST(:transactionId AS UUID), :sourceAccountId, :destinationAccountId, :customerId, 
                :amount, :currency, :transactionType, :countryCode, 
                :counterpartyCountryCode, :counterpartyName, :channel, CAST(:txnTimestamp AS TIMESTAMP), 
                :tenantId, CAST(:batchId AS UUID)
            )
            """;

        return new JdbcBatchItemWriterBuilder<TransactionCsvInput>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql(sql)
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public Step processCsvStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<TransactionCsvInput> csvItemReader,
            TransactionItemProcessor processor,
            JdbcBatchItemWriter<TransactionCsvInput> jdbcBatchItemWriter) {

        return new StepBuilder("processCsvStep", jobRepository)
                .<TransactionCsvInput, TransactionCsvInput>chunk(10000, transactionManager)
                .reader(csvItemReader)
                .processor(processor)
                .writer(jdbcBatchItemWriter)
                .build();
    }

    @Bean
    public Job amlTransactionBatchJob(
            JobRepository jobRepository,
            Step processCsvStep,
            JobCompletionNotificationListener listener) {

        return new JobBuilder("amlTransactionBatchJob", jobRepository)
                .start(processCsvStep)
                .listener(listener)
                .build();
    }
}
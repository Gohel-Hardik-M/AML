package com.aml.system.batch;

import com.aml.system.dto.batch.TransactionCsvInput;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope // This allows us to dynamically inject JobParameters at runtime
public class TransactionItemProcessor implements ItemProcessor<TransactionCsvInput, TransactionCsvInput> {

    @Value("#{jobParameters['tenantId']}")
    private String tenantId;

    @Value("#{jobParameters['batchId']}")
    private String batchId;

    @Override
    public TransactionCsvInput process(TransactionCsvInput item) throws Exception {
        // Stamp every single row with the correct tenant and batch ID
        item.setTenantId(tenantId);
        item.setBatchId(batchId);

        return item;
    }
}
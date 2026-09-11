package com.aml.system.rule.impl;

import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;
import com.aml.system.rule.AmlRule;
import com.aml.system.rule.RuleEvaluationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CrossBorderRule implements AmlRule {

    @Override
    public String getRuleCode() {
        return "CROSS_BORDER_001";
    }

    @Override
    public String getRuleName() {
        return "Cross-Border High Value Transfer";
    }

    @Override
    public RuleEvaluationResult evaluate(Transaction transaction, TenantRuleConfig config) {
        if (transaction.getAmount() == null || config == null || config.getThresholdAmount() == null) {
            return RuleEvaluationResult.notTriggered(getRuleCode(), getRuleName());
        }

        String country = transaction.getCountryCode();
        String counterpartyCountry = transaction.getCounterpartyCountryCode();

        if (country == null || counterpartyCountry == null) {
            return RuleEvaluationResult.notTriggered(getRuleCode(), getRuleName());
        }

        BigDecimal threshold = config.getThresholdAmount();

        boolean differentCountries = !country.equalsIgnoreCase(counterpartyCountry);
        boolean largeAmount = transaction.getAmount().compareTo(threshold) >= 0;

        if (differentCountries && largeAmount) {
            return RuleEvaluationResult.triggered(
                    getRuleCode(),
                    getRuleName(),
                    "MEDIUM",
                    transaction.getAmount(),
                    "Cross-border transfer (" + country + " -> " + counterpartyCountry + ") of "
                            + transaction.getAmount() + " meets or exceeds the configured threshold of " + threshold + "."
            );
        }

        return RuleEvaluationResult.notTriggered(getRuleCode(), getRuleName());
    }
}

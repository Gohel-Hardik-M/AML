package com.aml.system.rule.impl;

import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;
import com.aml.system.rule.AmlRule;
import com.aml.system.rule.RuleEvaluationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
public class GeographicRiskRule implements AmlRule {

    private static final Set<String> HIGH_RISK_COUNTRIES = Set.of(
            "XX", "YY", "ZZ", "IR", "KP", "SY"
    );

    @Override
    public String getRuleCode() {
        return "GEO_RISK_001";
    }

    @Override
    public String getRuleName() {
        return "High-Risk Geographic Route";
    }

    @Override
    public RuleEvaluationResult evaluate(Transaction transaction, TenantRuleConfig config) {
        if (config == null || config.getThresholdAmount() == null) {
            return RuleEvaluationResult.notTriggered(getRuleCode(), getRuleName());
        }

        String country = transaction.getCountryCode();
        String counterpartyCountry = transaction.getCounterpartyCountryCode();

        boolean countryRisk = country != null && HIGH_RISK_COUNTRIES.contains(country.toUpperCase());
        boolean counterpartyRisk = counterpartyCountry != null && HIGH_RISK_COUNTRIES.contains(counterpartyCountry.toUpperCase());

        if (countryRisk || counterpartyRisk) {
            BigDecimal threshold = config.getThresholdAmount();
            if (transaction.getAmount() != null && transaction.getAmount().compareTo(threshold) >= 0) {
                String riskJurisdiction = countryRisk ? country : counterpartyCountry;
                return RuleEvaluationResult.triggered(
                        getRuleCode(),
                        getRuleName(),
                        "HIGH",
                        transaction.getAmount(),
                        "Transaction involves configured high-risk jurisdiction (" + riskJurisdiction + ")."
                );
            }
        }

        return RuleEvaluationResult.notTriggered(getRuleCode(), getRuleName());
    }
}

package com.aml.system.service;

import com.aml.system.model.Alert;
import com.aml.system.model.AlertSeverity;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertPdfServiceTest {

    @Test
    void wrapsLongFieldsAcrossPagesWithoutDroppingContent() throws Exception {
        String longNarrative = "Narrative start " + "review detail ".repeat(500) + " NARRATIVE-END";
        String longMetadata = "{\"metadata\":\"" + "value ".repeat(500) + "METADATA-END\"}";
        Alert alert = Alert.builder()
                .alertId(UUID.randomUUID())
                .transactionId(UUID.randomUUID())
                .customerId("CUSTOMER-1")
                .ruleCode("VELOCITY_001")
                .ruleName("Velocity monitoring")
                .severity(AlertSeverity.HIGH)
                .triggeredAmount(new BigDecimal("1250.50"))
                .narrative(longNarrative)
                .detectionMetadataJson(longMetadata)
                .reviewed(true)
                .reviewDecision("CLOSED")
                .reviewedBy("officer")
                .reviewNotes("Reviewed and closed after investigation.")
                .build();

        byte[] pdf = new AlertPdfService().generate(alert);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(document.getNumberOfPages() > 1);
            assertTrue(text.contains("NARRATIVE-END"));
            assertTrue(text.contains("METADATA-END"));
            assertTrue(text.contains("Review notes:"));
        }
    }
}

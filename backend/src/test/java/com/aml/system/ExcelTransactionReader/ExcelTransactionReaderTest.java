package com.aml.system.ExcelTransactionReader;

import com.aml.system.Validation.TransactionValidator;
import com.aml.system.exception.TransactionException;
import com.aml.system.model.TransactionType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ExcelTransactionReaderTest {
    private static final String[] HEADERS = {
            "transaction_id", "source_account_id", "destination_account_id", "customer_id", "amount", "currency",
            "transaction_type", "country_code", "counterparty_country_code", "counterparty_name", "channel", "timestamp"
    };

    private final ExcelTransactionReader reader = new ExcelTransactionReader(new TransactionValidator());

    @AfterEach
    void clearThreadState() {
        com.aml.system.multitenancy.TenantContextHolder.clear();
    }

    @Test
    void readsNumericAmountFormulaAndExcelDate() throws Exception {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = workbook(workbook -> {
            header(workbook);
            Row row = workbook.getSheetAt(0).createRow(1);
            row.createCell(0).setCellValue(id.toString());
            row.createCell(1).setCellValue("SRC-1");
            row.createCell(2).setCellValue("DST-1");
            row.createCell(3).setCellValue("CUSTOMER-1");
            row.createCell(4).setCellValue(1250.50);
            row.createCell(5).setCellValue("inr");
            row.createCell(6).setCellValue("wire-transfer");
            row.createCell(7).setCellValue("IN");
            row.createCell(8).setCellValue("US");
            row.createCell(9).setCellValue("Counterparty");
            row.createCell(10).setCellValue("ONLINE");
            row.createCell(11).setCellValue("2026-09-11T12:30:00");
        });

        var transactions = reader.read(file);

        assertEquals(1, transactions.size());
        assertEquals(id, transactions.get(0).getTransactionId());
        assertEquals("INR", transactions.get(0).getCurrency());
        assertEquals(TransactionType.WIRE_TRANSFER, transactions.get(0).getTransactionType());
        assertEquals("2026-09-11T12:30", transactions.get(0).getTimestamp().toString());
    }

    @Test
    void rejectsMissingOrWrongHeaderBeforeReadingRows() throws Exception {
        MockMultipartFile file = workbook(workbook -> {
            Row row = workbook.getSheetAt(0).createRow(0);
            row.createCell(0).setCellValue("wrong_header");
        });

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> reader.read(file));

        assertTrue(error.getMessage().contains("Invalid Excel header"));
    }

    @Test
    void rejectsUsdCurrencyWithRowNumber() throws Exception {
        MockMultipartFile file = standardRow("USD", "WIRE_TRANSFER", "2026-09-11T12:30:00", UUID.randomUUID().toString());

        TransactionException error = assertThrows(TransactionException.class, () -> reader.read(file));

        assertTrue(error.getMessage().contains("row 2"));
        assertTrue(error.getMessage().contains("Only INR"));
    }

    @Test
    void rejectsCryptoTransactionTypeEvenWhenCurrencyIsInr() throws Exception {
        MockMultipartFile file = standardRow("INR", "CRYPTO_PURCHASE", "2026-09-11T12:30:00", UUID.randomUUID().toString());

        TransactionException error = assertThrows(TransactionException.class, () -> reader.read(file));

        assertTrue(error.getMessage().contains("Cryptocurrency"));
    }

    @Test
    void rejectsMalformedUuidAmountAndTimestamp() throws Exception {
        MockMultipartFile file = standardRow("INR", "WIRE_TRANSFER", "not-a-date", "not-a-uuid");

        TransactionException error = assertThrows(TransactionException.class, () -> reader.read(file));

        assertTrue(error.getMessage().contains("Transaction ID must be a valid UUID"));
    }

    @Test
    void skipsBlankRowsAndRejectsAnUploadWithNoDataRows() throws Exception {
        MockMultipartFile file = workbook(workbook -> {
            header(workbook);
            workbook.getSheetAt(0).createRow(1);
        });

        assertTrue(reader.read(file).isEmpty());
    }

    private MockMultipartFile standardRow(String currency, String type, String timestamp, String id) throws Exception {
        return workbook(workbook -> {
            header(workbook);
            Row row = workbook.getSheetAt(0).createRow(1);
            String[] values = {id, "SRC", "DST", "CUSTOMER", "100", currency, type, "IN", "IN", "Name", "ONLINE", timestamp};
            for (int i = 0; i < values.length; i++) row.createCell(i).setCellValue(values[i]);
        });
    }

    private MockMultipartFile workbook(java.util.function.Consumer<Workbook> writer) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.createSheet("Transactions");
            writer.accept(workbook);
            workbook.write(output);
            return new MockMultipartFile("file", "transactions.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }

    private void header(Workbook workbook) {
        Row row = workbook.getSheetAt(0).getRow(0);
        if (row == null) row = workbook.getSheetAt(0).createRow(0);
        for (int i = 0; i < HEADERS.length; i++) row.createCell(i).setCellValue(HEADERS[i]);
    }
}

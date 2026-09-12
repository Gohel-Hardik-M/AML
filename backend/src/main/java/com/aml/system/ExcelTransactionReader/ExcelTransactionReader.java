package com.aml.system.ExcelTransactionReader;

import com.aml.system.Validation.TransactionValidator;
import com.aml.system.model.Transaction;
import com.aml.system.model.TransactionType;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.UUID;


@Component
public class ExcelTransactionReader {

        private  final TransactionValidator transactionValidator;

        public  ExcelTransactionReader(TransactionValidator transactionValidator){
            this.transactionValidator = transactionValidator;
        }

        public List<Transaction> read(MultipartFile file) throws IOException {

            List<Transaction> transactions = new ArrayList<>();
            try(InputStream inputStream = file.getInputStream();
                Workbook workbook = WorkbookFactory.create(inputStream)){

                Sheet sheet = workbook.getSheetAt(0);
                FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();

                Row headerRow = sheet.getRow(0);

                if(headerRow == null){
                    throw new  IllegalArgumentException("Excel Header is missing");
                }
                validateHeader(headerRow, formulaEvaluator);

                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++){

                    Row row = sheet.getRow(rowIndex);

                    if(row==null || isEmptyRow(row, formulaEvaluator)){
                        continue;
                    }


                    int excelRowNumber = rowIndex +1;
                    Transaction transaction = mapRowToTransaction(row, formulaEvaluator);

                    transactionValidator.validate(transaction, excelRowNumber);

                    transactions.add(transaction);

                }
            }
            return  transactions;
        }


        private Transaction mapRowToTransaction(Row row, FormulaEvaluator formulaEvaluator){
            return  Transaction.builder()
                .transactionId(parseUUID(getCellValue(row.getCell(0), formulaEvaluator)))
                .sourceAccountId(getCellValue(row.getCell(1), formulaEvaluator))
                .destinationAccountId(getCellValue(row.getCell(2), formulaEvaluator))
                .customerID(getCellValue(row.getCell(3), formulaEvaluator))
                .amount(parseAmount(row.getCell(4), formulaEvaluator))
                .currency(normalizeCode(getCellValue(row.getCell(5), formulaEvaluator)))
                    .transactionType(parseTransactionType(
                    getCellValue(row.getCell(6), formulaEvaluator)
                    ))
                .countryCode(normalizeCode(getCellValue(row.getCell(7), formulaEvaluator)))
                .counterpartyCountryCode(normalizeCode(getCellValue(row.getCell(8), formulaEvaluator)))
                .counterpartyName(getCellValue(row.getCell(9), formulaEvaluator))
                .channel(getCellValue(row.getCell(10), formulaEvaluator))
                .timestamp(parseTimestamp(row.getCell(11), formulaEvaluator))
                    .build();
        }


        private String getCellValue(Cell cell, FormulaEvaluator formulaEvaluator) {

            if (cell == null) {
                return null;
            }

            DataFormatter formatter = new DataFormatter();

            String value = formatter.formatCellValue(cell, formulaEvaluator);

            if (value == null || value.isBlank()) {
                return null;
            }

            return value.trim();
        }

        private BigDecimal parseAmount(Cell cell, FormulaEvaluator formulaEvaluator) {

            if (cell != null) {
                CellValue evaluated = cell.getCellType() == CellType.FORMULA
                        ? formulaEvaluator.evaluate(cell)
                        : null;
                CellType effectiveType = evaluated == null ? cell.getCellType() : evaluated.getCellType();
                if (effectiveType == CellType.NUMERIC) {
                    double numericValue = evaluated == null ? cell.getNumericCellValue() : evaluated.getNumberValue();
                    return BigDecimal.valueOf(numericValue);
                }
            }

            String value = getCellValue(cell, formulaEvaluator);

            if (value == null) {
                return null;
            }

            try {
                return new BigDecimal(normalizeAmount(value));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private String normalizeAmount(String value) {
            String normalized = value.trim()
                    .replace("\u00A0", "")
                    .replace("\u202F", "")
                    .replaceAll("[\\s\\$€£₹]", "");
            boolean accountingNegative = normalized.startsWith("(") && normalized.endsWith(")");
            if (accountingNegative) {
                normalized = normalized.substring(1, normalized.length() - 1);
            }

            int lastComma = normalized.lastIndexOf(',');
            int lastDot = normalized.lastIndexOf('.');
            if (lastComma >= 0 && lastDot >= 0) {
                if (lastComma > lastDot) {
                    normalized = normalized.replace(".", "").replace(',', '.');
                } else {
                    normalized = normalized.replace(",", "");
                }
            } else if (lastComma >= 0) {
                int decimalDigits = normalized.length() - lastComma - 1;
                normalized = decimalDigits == 1 || decimalDigits == 2
                        ? normalized.replace(',', '.')
                        : normalized.replace(",", "");
            }

            if (accountingNegative) {
                normalized = "-" + normalized;
            }
            return normalized;
        }

        private UUID parseUUID(String value) {

            if (value == null) {
                return null;
            }

            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private TransactionType parseTransactionType(String value) {

            if (value == null || value.isBlank()) {
                return null;
            }

            try {
                String normalized = value.trim().toUpperCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');
                return TransactionType.valueOf(normalized);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

            private LocalDateTime parseTimestamp(Cell cell, FormulaEvaluator formulaEvaluator) {

            if (cell == null) {
                return null;
            }

            try {

                if (cell.getCellType() == CellType.NUMERIC &&
                        DateUtil.isCellDateFormatted(cell)) {

                    return cell.getDateCellValue()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                }

                String value = getCellValue(cell, formulaEvaluator);

                if (value == null) {
                    return null;
                }

                return parseDateTime(value);

            } catch (Exception e) {
                return null;
            }
        }

        private boolean isEmptyRow(Row row, FormulaEvaluator formulaEvaluator) {
            for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
                Cell cell = row.getCell(i);
                if (cell != null && cell.getCellType() != CellType.BLANK
                        && getCellValue(cell, formulaEvaluator) != null) {
                    return false;
                }
            }
            return true;
        }

        private void validateHeader(Row headerRow, FormulaEvaluator formulaEvaluator) {
            String[] expected = {"transaction_id", "source_account_id", "destination_account_id", "customer_id",
                    "amount", "currency", "transaction_type", "country_code", "counterparty_country_code",
                    "counterparty_name", "channel", "timestamp"};
            for (int index = 0; index < expected.length; index++) {
                String actual = getCellValue(headerRow.getCell(index), formulaEvaluator);
                if (actual == null || !expected[index].equals(actual.trim().toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Invalid Excel header at column " + (index + 1)
                            + ". Expected '" + expected[index] + "'.");
                }
            }
        }

        private String normalizeCode(String value) {
            return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
        }

        private LocalDateTime parseDateTime(String value) {
            String normalized = value.trim();
            List<DateTimeFormatter> formatters = List.of(
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                    DateTimeFormatter.ofPattern("M/d/uuuu H:mm", Locale.ROOT),
                    DateTimeFormatter.ofPattern("M/d/uuuu h:mm a", Locale.ROOT),
                    DateTimeFormatter.ofPattern("MM/dd/uuuu HH:mm:ss", Locale.ROOT),
                    DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm:ss", Locale.ROOT)
            );
            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDateTime.parse(normalized, formatter.withResolverStyle(ResolverStyle.SMART));
                } catch (DateTimeParseException ignored) { }
            }
            throw new IllegalArgumentException("Unsupported timestamp format: " + value);
        }




}

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
import java.util.ArrayList;
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

                Row headerRow = sheet.getRow(0);

                if(headerRow == null){
                    throw new  IllegalArgumentException("Excel Header is missing");
                }

                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++){

                    Row row = sheet.getRow(rowIndex);

                    if(row==null || isEmptyRow(row)){
                        continue;
                    }


                    int excelRowNumber = rowIndex +1;
                    Transaction transaction = mapRowToTransaction(row);

                    transactionValidator.validate(transaction, excelRowNumber);

                    transactions.add(transaction);

                }
            }
            return  transactions;
        }


        private  Transaction mapRowToTransaction(Row row){
            return  Transaction.builder()
                    .transactionId(parseUUID(getCellValue(row.getCell(0))))
                    .sourceAccountId(getCellValue(row.getCell(1)))
                    .destinationAccountId(getCellValue(row.getCell(2)))
                    .customerID(getCellValue(row.getCell(3)))
                    .amount(parseAmount(row.getCell(4)))
                    .currency(getCellValue(row.getCell(5)))
                    .transactionType(parseTransactionType(
                            getCellValue(row.getCell(6))
                    ))
                    .countryCode(getCellValue(row.getCell(7)))
                    .counterpartyCountryCode(getCellValue(row.getCell(8)))
                    .counterpartyName(getCellValue(row.getCell(9)))
                    .channel(getCellValue(row.getCell(10)))
                    .timestamp(parseTimestamp(row.getCell(11)))
                    .build();
        }


        private String getCellValue(Cell cell) {

            if (cell == null) {
                return null;
            }

            DataFormatter formatter = new DataFormatter();

            String value = formatter.formatCellValue(cell);

            if (value == null || value.isBlank()) {
                return null;
            }

            return value.trim();
        }

        private BigDecimal parseAmount(Cell cell) {

            String value = getCellValue(cell);

            if (value == null) {
                return null;
            }

            try {
                return new BigDecimal(value);
            } catch (NumberFormatException e) {
                return null;
            }
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

            System.out.println("Excel Transaction Type = [" + value + "]");

            if (value == null || value.isBlank()) {
                return null;
            }

            try {
                return TransactionType.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println(
                        "Invalid TransactionType value from Excel: [" + value + "]"
                );
                return null;
            }
        }

        private LocalDateTime parseTimestamp(Cell cell) {

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

                String value = getCellValue(cell);

                if (value == null) {
                    return null;
                }

                return LocalDateTime.parse(value);

            } catch (Exception e) {
                return null;
            }
        }

        private boolean isEmptyRow(Row row) {

            for (int i = row.getFirstCellNum();
                 i < row.getLastCellNum();
                 i++) {

                Cell cell = row.getCell(i);

                if (cell != null &&
                        cell.getCellType() != CellType.BLANK &&
                        !getCellValue(cell).isBlank()) {

                    return false;
                }
            }

            return true;
        }




}

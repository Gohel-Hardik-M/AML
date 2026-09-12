package com.aml.system.Validation;

import com.aml.system.exception.TransactionException;
import com.aml.system.model.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Component
public class TransactionValidator {




        public  void  validate(Transaction transaction, int rowNumber){
            if (transaction.getTransactionId() == null) {
                throw new TransactionException("Invalid transaction at row " + rowNumber + " : Transaction ID must be a valid UUID.");
            }
            if(transaction.getSourceAccountId() == null || transaction.getSourceAccountId().isBlank()){
                throw  new TransactionException("Invalid Transaction at Row :"+rowNumber+" -> Source Account ID is required.");

            }


            if(transaction.getDestinationAccountId() == null || transaction.getDestinationAccountId().isBlank()){
                throw new TransactionException("Invalid Trasaction at ROW :"+rowNumber+" : Destination Account ID is required. ");
            }


            if(transaction.getCustomerID() == null || transaction.getCustomerID().isBlank()){
                throw new TransactionException("Invalid transaction at ROW :"+rowNumber+" : Customer ID is required.");
            }

            if(transaction.getAmount() == null){
                throw new TransactionException("Invalid transaction at row "+ rowNumber+" : Amount is required.");
            }
            if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new TransactionException("Invalid transaction at row " + rowNumber + " : Amount must be greater than zero.");
            }

            if(transaction.getCurrency() == null || transaction.getCurrency().isBlank()){
                throw new TransactionException("Invalid Transaction at row :"+ rowNumber+" : Currency is required. ");
            }
            if (!"INR".equalsIgnoreCase(transaction.getCurrency())) {
                throw new TransactionException("Invalid transaction at row " + rowNumber + " : Only INR currency is supported.");
            }
            if (transaction.getCountryCode() != null && !transaction.getCountryCode().matches("[A-Za-z]{2}")) {
                throw new TransactionException("Invalid transaction at row " + rowNumber + " : Country code must contain 2 letters.");
            }
            if (transaction.getCounterpartyCountryCode() != null && !transaction.getCounterpartyCountryCode().matches("[A-Za-z]{2}")) {
                throw new TransactionException("Invalid transaction at row " + rowNumber + " : Counterparty country code must contain 2 letters.");
            }

            if(transaction.getTransactionType() == null){
                throw new TransactionException("Invalid Transaction at row "+ rowNumber+" : Transaction Type is required.");
            }
            if (transaction.getTransactionType() == com.aml.system.model.TransactionType.CRYPTO_PURCHASE
                    || transaction.getTransactionType() == com.aml.system.model.TransactionType.CRYPTO_DISBURSEMENT) {
                throw new TransactionException("Invalid transaction at row " + rowNumber + " : Cryptocurrency transaction types are not supported.");
            }

            if(transaction.getTimestamp() == null){
                throw new TransactionException("Invalid transaction at row "+ rowNumber+ " : Timestamp is required.");
            }
            if (transaction.getTimestamp().isAfter(LocalDateTime.now())) {
                throw new TransactionException("Invalid transaction at row " + rowNumber + " : Timestamp cannot be in the future.");
            }



        }


}

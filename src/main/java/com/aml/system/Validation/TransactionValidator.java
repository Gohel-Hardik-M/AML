package com.aml.system.Validation;

import com.aml.system.model.Transaction;
import org.hibernate.TransactionException;
import org.springframework.stereotype.Component;


@Component
public class TransactionValidator {




        public  void  validate(Transaction transaction, int rowNumber){
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

            if(transaction.getCurrency() == null || transaction.getCurrency().isBlank()){
                throw new TransactionException("Invalid Transaction at row :"+ rowNumber+" : Currency is required. ");
            }

            if(transaction.getTransactionType() == null){
                throw new TransactionException("Invalid Transaction at row "+ rowNumber+" : Transaction Type is required.");
            }

            if(transaction.getTimestamp() == null){
                throw new TransactionException("Invalid transaction at row "+ rowNumber+ " : Timestamp is required.");
            }



        }


}

package com.aml.system.Validation;

import com.aml.system.exception.TransactionException;
import com.aml.system.model.Transaction;
import com.aml.system.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionValidatorTest {
    private final TransactionValidator validator = new TransactionValidator();

    @Test
    void acceptsCompletePositiveInrTransaction() {
        assertDoesNotThrow(() -> validator.validate(transaction(), 2));
    }

    @Test
    void rejectsMissingRequiredFieldsWithRowNumber() {
        Transaction invalid = transaction();
        invalid.setSourceAccountId(null);
        assertMessage(invalid, "Source Account ID is required");

        invalid = transaction();
        invalid.setDestinationAccountId(null);
        assertMessage(invalid, "Destination Account ID is required");

        invalid = transaction();
        invalid.setCustomerID(null);
        assertMessage(invalid, "Customer ID is required");

        invalid = transaction();
        invalid.setAmount(null);
        assertMessage(invalid, "Amount is required");

        invalid = transaction();
        invalid.setTimestamp(null);
        assertMessage(invalid, "Timestamp is required");
    }

    @Test
    void rejectsZeroAndNegativeAmounts() {
        Transaction zero = transaction();
        zero.setAmount(BigDecimal.ZERO);
        assertMessage(zero, "greater than zero");

        Transaction negative = transaction();
        negative.setAmount(new BigDecimal("-0.01"));
        assertMessage(negative, "greater than zero");
    }

    @Test
    void rejectsUnsupportedCurrenciesCryptoTypesBadCountriesAndFutureDates() {
        Transaction currency = transaction();
        currency.setCurrency("USD");
        assertMessage(currency, "Only INR");

        Transaction crypto = transaction();
        crypto.setTransactionType(TransactionType.CRYPTO_PURCHASE);
        assertMessage(crypto, "Cryptocurrency");

        Transaction country = transaction();
        country.setCountryCode("IND");
        assertMessage(country, "Country code");

        Transaction future = transaction();
        future.setTimestamp(LocalDateTime.now().plusMinutes(2));
        assertMessage(future, "cannot be in the future");
    }

    private Transaction transaction() {
        return Transaction.builder().transactionId(UUID.randomUUID()).sourceAccountId("SRC")
                .destinationAccountId("DST").customerID("CUSTOMER").amount(new BigDecimal("10.00"))
                .currency("INR").transactionType(TransactionType.WIRE_TRANSFER).countryCode("IN")
                .counterpartyCountryCode("IN").timestamp(LocalDateTime.now().minusMinutes(1)).build();
    }

    private void assertMessage(Transaction transaction, String expected) {
        TransactionException error = assertThrows(TransactionException.class, () -> validator.validate(transaction, 7));
        assertTrue(error.getMessage().contains(expected), error.getMessage());
        assertTrue(error.getMessage().contains("7"), error.getMessage());
    }
}

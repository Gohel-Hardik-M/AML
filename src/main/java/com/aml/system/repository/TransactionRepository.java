package com.aml.system.repository;

import com.aml.system.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>{


        @Query("SELECT COUNT(t) FROM Transaction t " +
                "WHERE t.customerID = :customerId AND t.timestamp BETWEEN :from AND :to")
        long countCustomerTransactions(
                @Param("customerId") String customerId,
                @Param("from") LocalDateTime from,
                @Param("to") LocalDateTime to);

        @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
                "WHERE t.customerID = :customerId AND t.timestamp BETWEEN :from AND :to")
        BigDecimal sumCustomerTransactions(
                @Param("customerId") String customerId,
                @Param("from") LocalDateTime from,
                @Param("to") LocalDateTime to);

}

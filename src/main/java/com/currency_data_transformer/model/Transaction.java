package com.currency_data_transformer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a financial transaction with date, ID, amount, and currency information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    
    private LocalDate date;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
}


package com.currency_data_transformer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

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


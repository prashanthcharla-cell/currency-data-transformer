package com.currency_data_transformer.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class Transaction {
    private LocalDate date;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
}


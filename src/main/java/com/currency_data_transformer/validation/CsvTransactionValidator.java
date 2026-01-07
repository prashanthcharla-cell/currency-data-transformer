package com.currency_data_transformer.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.currency_data_transformer.exception.InvalidFileException;
import com.currency_data_transformer.model.Transaction;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Validator for CSV files containing transaction data.
 * Validates file format, headers, and individual transaction records.
 */
@Slf4j
@Component
public class CsvTransactionValidator {
    
    private static final String CSV_CONTENT_TYPE = "text/csv";
    private static final String[] EXPECTED_HEADERS = {"Date", "TransactionID", "Amount", "Currency"};
    private static final String CSV_DELIMITER = ",";
    private static final int EXPECTED_COLUMN_COUNT = 4;
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ISO_LOCAL_DATE
    };
    
    private static final Set<String> VALID_CURRENCY_CODES = Currency.getAvailableCurrencies()
        .stream()
        .map(Currency::getCurrencyCode)
        .collect(Collectors.toSet());
    
    /**
     * Validates a CSV file and returns a validation result with valid transactions
     * and error messages for invalid rows.
     *
     * @param file the multipart file to validate
     * @return ValidationResult containing valid transactions and errors
     * @throws InvalidFileException if the file fails basic validation
     */
    public ValidationResult validate(MultipartFile file) {
        // Basic file validation (includes null check)
        validateFileBasics(file);
        
        log.info("Starting validation for file: {}", file.getOriginalFilename());
        
        List<Transaction> validTransactions = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            // Validate headers
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                throw new InvalidFileException("File is empty");
            }
            
            validateHeaders(headerLine);
            
            // Validate data rows
            String line;
            int lineNumber = 2; // Start from 2 (1 is header)
            
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    lineNumber++;
                    continue; // Skip empty lines
                }
                
                try {
                    Transaction transaction = validateAndParseRow(line, lineNumber);
                    validTransactions.add(transaction);
                } catch (ValidationException e) {
                    errors.add(e.getMessage());
                    log.warn("Validation error at line {}: {}", lineNumber, e.getMessage());
                }
                
                lineNumber++;
            }
            
            // Check if we have any data
            if (validTransactions.isEmpty() && errors.isEmpty()) {
                throw new InvalidFileException("File contains only headers, no data rows found");
            }
            
            log.info("Validation completed. Valid transactions: {}, Errors: {}", 
                     validTransactions.size(), errors.size());
            
            return new ValidationResult(validTransactions, errors);
            
        } catch (IOException e) {
            log.error("Error reading file: {}", file.getOriginalFilename(), e);
            throw new InvalidFileException("Error reading file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validates basic file properties (not null, not empty, correct type).
     */
    private void validateFileBasics(MultipartFile file) {
        if (file == null) {
            throw new InvalidFileException("File is null or empty");
        }
        
        if (file.isEmpty()) {
            throw new InvalidFileException("File is null or empty");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            throw new InvalidFileException("File must be a CSV file with .csv extension");
        }
        
        String contentType = file.getContentType();
        if (contentType != null && !contentType.equals(CSV_CONTENT_TYPE) 
                && !contentType.equals("application/vnd.ms-excel")
                && !contentType.equals("application/csv")) {
            log.warn("Content type is {}, expected text/csv", contentType);
            // Don't throw exception for content type, as browsers may send different types
        }
    }
    
    /**
     * Validates that the CSV headers match the expected format.
     */
    private void validateHeaders(String headerLine) {
        String[] headers = headerLine.split(CSV_DELIMITER, -1);
        
        // Trim whitespace from headers
        headers = Arrays.stream(headers)
            .map(String::trim)
            .toArray(String[]::new);
        
        if (headers.length != EXPECTED_COLUMN_COUNT) {
            throw new InvalidFileException(
                String.format("Invalid number of columns. Expected %d columns: %s, but found %d", 
                    EXPECTED_COLUMN_COUNT, 
                    String.join(", ", EXPECTED_HEADERS),
                    headers.length)
            );
        }
        
        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
            if (!headers[i].equalsIgnoreCase(EXPECTED_HEADERS[i])) {
                throw new InvalidFileException(
                    String.format("Invalid header at position %d. Expected '%s', but found '%s'", 
                        i + 1, EXPECTED_HEADERS[i], headers[i])
                );
            }
        }
    }
    
    /**
     * Validates and parses a single data row into a Transaction object.
     */
    private Transaction validateAndParseRow(String line, int lineNumber) {
        String[] fields = line.split(CSV_DELIMITER, -1);
        
        if (fields.length != EXPECTED_COLUMN_COUNT) {
            throw new ValidationException(
                String.format("Line %d: Invalid number of columns. Expected %d, found %d", 
                    lineNumber, EXPECTED_COLUMN_COUNT, fields.length)
            );
        }
        
        // Trim all fields
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].trim();
        }
        
        // Validate and parse each field
        LocalDate date = validateAndParseDate(fields[0], lineNumber);
        String transactionId = validateTransactionId(fields[1], lineNumber);
        BigDecimal amount = validateAndParseAmount(fields[2], lineNumber);
        String currency = validateCurrency(fields[3], lineNumber);
        
        return Transaction.builder()
            .date(date)
            .transactionId(transactionId)
            .amount(amount)
            .currency(currency)
            .build();
    }
    
    /**
     * Validates and parses the date field.
     */
    private LocalDate validateAndParseDate(String dateStr, int lineNumber) {
        if (dateStr == null || dateStr.isEmpty()) {
            throw new ValidationException(
                String.format("Line %d: Date field is empty", lineNumber)
            );
        }
        
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }
        
        throw new ValidationException(
            String.format("Line %d: Invalid date format '%s'. Expected formats: yyyy-MM-dd, dd/MM/yyyy, or MM/dd/yyyy", 
                lineNumber, dateStr)
        );
    }
    
    /**
     * Validates the transaction ID field.
     */
    private String validateTransactionId(String transactionId, int lineNumber) {
        if (transactionId == null || transactionId.isEmpty()) {
            throw new ValidationException(
                String.format("Line %d: TransactionID field is empty", lineNumber)
            );
        }
        
        if (transactionId.length() > 100) {
            throw new ValidationException(
                String.format("Line %d: TransactionID is too long (max 100 characters)", lineNumber)
            );
        }
        
        // TransactionID should contain alphanumeric characters and common separators
        if (!transactionId.matches("^[a-zA-Z0-9_-]+$")) {
            throw new ValidationException(
                String.format("Line %d: TransactionID '%s' contains invalid characters. Only alphanumeric, underscore, and hyphen are allowed", 
                    lineNumber, transactionId)
            );
        }
        
        return transactionId;
    }
    
    /**
     * Validates and parses the amount field.
     */
    private BigDecimal validateAndParseAmount(String amountStr, int lineNumber) {
        if (amountStr == null || amountStr.isEmpty()) {
            throw new ValidationException(
                String.format("Line %d: Amount field is empty", lineNumber)
            );
        }
        
        try {
            BigDecimal amount = new BigDecimal(amountStr);
            
            // Amount must be positive
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException(
                    String.format("Line %d: Amount must be positive, found: %s", lineNumber, amountStr)
                );
            }
            
            // Check for reasonable precision (max 2 decimal places for currency)
            if (amount.scale() > 2) {
                throw new ValidationException(
                    String.format("Line %d: Amount has too many decimal places (max 2 allowed): %s", 
                        lineNumber, amountStr)
                );
            }
            
            return amount;
            
        } catch (NumberFormatException e) {
            throw new ValidationException(
                String.format("Line %d: Invalid amount format '%s'. Must be a valid number", 
                    lineNumber, amountStr)
            );
        }
    }
    
    /**
     * Validates the currency field.
     */
    private String validateCurrency(String currency, int lineNumber) {
        if (currency == null || currency.isEmpty()) {
            throw new ValidationException(
                String.format("Line %d: Currency field is empty", lineNumber)
            );
        }
        
        // Currency code should be 3 uppercase letters (ISO 4217)
        if (!currency.matches("^[A-Z]{3}$")) {
            throw new ValidationException(
                String.format("Line %d: Currency code '%s' must be 3 uppercase letters (ISO 4217)", 
                    lineNumber, currency)
            );
        }
        
        // Validate against actual currency codes
        if (!VALID_CURRENCY_CODES.contains(currency)) {
            throw new ValidationException(
                String.format("Line %d: Invalid currency code '%s'. Must be a valid ISO 4217 currency code", 
                    lineNumber, currency)
            );
        }
        
        return currency;
    }
    
    /**
     * Internal exception for validation errors during row parsing.
     */
    private static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
    
    /**
     * Result object containing validated transactions and any validation errors.
     */
    @Data
    public static class ValidationResult {
        private final List<Transaction> validTransactions;
        private final List<String> errors;
        
        /**
         * Checks if the validation was completely successful (no errors).
         */
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        /**
         * Checks if there is at least one valid transaction.
         */
        public boolean hasValidTransactions() {
            return !validTransactions.isEmpty();
        }
        
        /**
         * Returns the total number of validated transactions (valid + invalid).
         */
        public int getTotalRowsProcessed() {
            return validTransactions.size() + errors.size();
        }
    }
}


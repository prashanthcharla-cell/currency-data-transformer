package com.currency_data_transformer.validation;

import com.currency_data_transformer.exception.InvalidFileException;
import com.currency_data_transformer.model.Transaction;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class CsvTransactionValidator {
    
    private static final String[] EXPECTED_HEADERS = {"Date", "TransactionID", "Amount", "Currency"};
    private static final Set<String> ALLOWED_CURRENCIES = Set.of("USD", "EUR", "INR");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
    private static final int TRANSACTION_ID_LENGTH = 10;
    
    /**
     * Validates the uploaded CSV file and returns validation results
     * @param file the uploaded MultipartFile
     * @return ValidationResult containing valid transactions and error details
     * @throws InvalidFileException if file is null, empty, or not CSV
     */
    public ValidationResult validate(MultipartFile file) {
        // File-level validations
        validateFile(file);
        
        ValidationResult result = new ValidationResult();
        Set<String> transactionIds = new HashSet<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            validateHeaders(headerLine);
            
            String line;
            int rowNumber = 2; // Starting from row 2 (row 1 is header)
            
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    rowNumber++;
                    continue; // Skip empty lines
                }
                
                validateRow(line, rowNumber, transactionIds, result);
                rowNumber++;
            }
            
            if (result.getValidTransactions().isEmpty() && result.getErrors().isEmpty()) {
                throw new InvalidFileException("File contains no data rows");
            }
            
        } catch (IOException e) {
            throw new InvalidFileException("Error reading file: " + e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * Validates that the file is valid
     */
    private void validateFile(MultipartFile file) {
        if (file == null) {
            throw new InvalidFileException("File is null");
        }
        
        if (file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new InvalidFileException("File must be a CSV file");
        }
    }
    
    /**
     * Validates the CSV headers
     */
    private void validateHeaders(String headerLine) {
        if (headerLine == null || headerLine.trim().isEmpty()) {
            throw new InvalidFileException("File is empty or missing headers");
        }
        
        String[] headers = headerLine.split(",");
        if (headers.length != EXPECTED_HEADERS.length) {
            throw new InvalidFileException(
                String.format("Invalid headers. Expected 4 columns: %s", 
                    String.join(", ", EXPECTED_HEADERS))
            );
        }
        
        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
            if (!headers[i].trim().equals(EXPECTED_HEADERS[i])) {
                throw new InvalidFileException(
                    String.format("Invalid headers. Expected: %s, but found: %s at position %d", 
                        EXPECTED_HEADERS[i], headers[i].trim(), i + 1)
                );
            }
        }
    }
    
    /**
     * Validates a single row of data
     */
    private void validateRow(String line, int rowNumber, Set<String> transactionIds, ValidationResult result) {
        String[] fields = line.split(",", -1); // -1 to preserve empty trailing fields
        
        if (fields.length != EXPECTED_HEADERS.length) {
            result.addError(rowNumber, 
                String.format("Invalid number of columns. Expected 4, found %d", fields.length));
            return;
        }
        
        List<String> rowErrors = new ArrayList<>();
        
        // Validate Date
        LocalDate date = validateDate(fields[0].trim(), rowErrors);
        
        // Validate TransactionID
        String transactionId = validateTransactionId(fields[1].trim(), transactionIds, rowErrors);
        
        // Validate Amount
        BigDecimal amount = validateAmount(fields[2].trim(), rowErrors);
        
        // Validate Currency
        String currency = validateCurrency(fields[3].trim(), rowErrors);
        
        // If there are errors, add them to result
        if (!rowErrors.isEmpty()) {
            result.addError(rowNumber, String.join("; ", rowErrors));
        } else {
            // All validations passed, add to valid transactions
            Transaction transaction = Transaction.builder()
                .date(date)
                .transactionId(transactionId)
                .amount(amount)
                .currency(currency)
                .build();
            result.addValidTransaction(transaction);
        }
    }
    
    /**
     * Validates date field
     */
    private LocalDate validateDate(String dateStr, List<String> errors) {
        if (dateStr.isEmpty()) {
            errors.add("Date is required");
            return null;
        }
        
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            errors.add("Date must be in yyyy-MM-dd format");
            return null;
        }
    }
    
    /**
     * Validates transaction ID field
     */
    private String validateTransactionId(String transactionId, Set<String> existingIds, List<String> errors) {
        if (transactionId.isEmpty()) {
            errors.add("TransactionID is required");
            return null;
        }
        
        if (transactionId.length() != TRANSACTION_ID_LENGTH) {
            errors.add(String.format("TransactionID must be exactly %d characters", TRANSACTION_ID_LENGTH));
            return null;
        }
        
        if (existingIds.contains(transactionId)) {
            errors.add(String.format("Duplicate TransactionID: %s", transactionId));
            return null;
        }
        
        existingIds.add(transactionId);
        return transactionId;
    }
    
    /**
     * Validates amount field
     */
    private BigDecimal validateAmount(String amountStr, List<String> errors) {
        if (amountStr.isEmpty()) {
            errors.add("Amount is required");
            return null;
        }
        
        try {
            BigDecimal amount = new BigDecimal(amountStr);
            
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Amount must be a positive number");
                return null;
            }
            
            // Check decimal places
            if (!DECIMAL_PATTERN.matcher(amountStr).matches()) {
                errors.add("Amount must have maximum 2 decimal places");
                return null;
            }
            
            return amount;
        } catch (NumberFormatException e) {
            errors.add("Amount must be a valid number");
            return null;
        }
    }
    
    /**
     * Validates currency field
     */
    private String validateCurrency(String currency, List<String> errors) {
        if (currency.isEmpty()) {
            errors.add("Currency is required");
            return null;
        }
        
        if (!ALLOWED_CURRENCIES.contains(currency)) {
            errors.add(String.format("Currency must be one of: %s", 
                String.join(", ", ALLOWED_CURRENCIES)));
            return null;
        }
        
        return currency;
    }
    
    /**
     * Result class containing validation results
     */
    @Data
    public static class ValidationResult {
        private List<Transaction> validTransactions = new ArrayList<>();
        private List<ValidationError> errors = new ArrayList<>();
        
        public void addValidTransaction(Transaction transaction) {
            validTransactions.add(transaction);
        }
        
        public void addError(int rowNumber, String message) {
            errors.add(new ValidationError(rowNumber, message));
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
    
    /**
     * Represents a validation error
     */
    @Data
    public static class ValidationError {
        private final int rowNumber;
        private final String message;
        
        public ValidationError(int rowNumber, String message) {
            this.rowNumber = rowNumber;
            this.message = message;
        }
    }
}


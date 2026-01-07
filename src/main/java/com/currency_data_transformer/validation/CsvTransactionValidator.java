package com.currency_data_transformer.validation;

import com.currency_data_transformer.exception.InvalidFileException;
import com.currency_data_transformer.model.Transaction;
import lombok.Builder;
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

    private static final String CSV_CONTENT_TYPE = "text/csv";
    private static final String CSV_EXTENSION = ".csv";
    private static final Set<String> VALID_CURRENCIES = Set.of("USD", "EUR", "INR");
    private static final int TRANSACTION_ID_LENGTH = 10;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
    
    // Expected headers
    private static final String HEADER_DATE = "Date";
    private static final String HEADER_TRANSACTION_ID = "TransactionID";
    private static final String HEADER_AMOUNT = "Amount";
    private static final String HEADER_CURRENCY = "Currency";
    
    /**
     * Validates and parses the uploaded CSV file
     * 
     * @param file the uploaded CSV file
     * @return ValidationResult containing valid transactions and error messages
     * @throws InvalidFileException if the file is invalid
     */
    public ValidationResult validateCsvFile(MultipartFile file) {
        // Step 1: Validate file existence and basic properties
        validateFileBasics(file);
        
        // Step 2: Parse and validate CSV content
        return parseAndValidateCsvContent(file);
    }
    
    /**
     * Validates basic file properties (not null, not empty, correct type)
     */
    private void validateFileBasics(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty or not provided");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(CSV_EXTENSION)) {
            throw new InvalidFileException("File must be a CSV file with .csv extension");
        }
        
        String contentType = file.getContentType();
        if (contentType != null && !contentType.equals(CSV_CONTENT_TYPE) && 
            !contentType.equals("application/vnd.ms-excel") && 
            !contentType.equals("application/octet-stream")) {
            throw new InvalidFileException("Invalid file type. Expected CSV file");
        }
    }
    
    /**
     * Parses and validates the CSV content
     */
    private ValidationResult parseAndValidateCsvContent(MultipartFile file) {
        List<Transaction> validTransactions = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        Set<String> transactionIds = new HashSet<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            // Read and validate headers
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                throw new InvalidFileException("CSV file is empty or missing headers");
            }
            
            Map<String, Integer> headerIndices = validateAndParseHeaders(headerLine);
            
            // Read and validate data rows
            String line;
            int rowNumber = 1; // Start from 1 (row after header)
            
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                String[] fields = parseCsvLine(line);
                
                // Validate row has correct number of fields
                if (fields.length < 4) {
                    errorMessages.add(String.format("Row %d: Insufficient number of columns. Expected 4, found %d", 
                        rowNumber, fields.length));
                    continue;
                }
                
                // Validate and parse each field
                String dateStr = getFieldValue(fields, headerIndices, HEADER_DATE);
                String transactionId = getFieldValue(fields, headerIndices, HEADER_TRANSACTION_ID);
                String amountStr = getFieldValue(fields, headerIndices, HEADER_AMOUNT);
                String currency = getFieldValue(fields, headerIndices, HEADER_CURRENCY);
                
                List<String> rowErrors = validateRow(rowNumber, dateStr, transactionId, amountStr, 
                    currency, transactionIds);
                
                if (!rowErrors.isEmpty()) {
                    errorMessages.addAll(rowErrors);
                    continue;
                }
                
                // If validation passed, create transaction
                try {
                    Transaction transaction = Transaction.builder()
                            .date(LocalDate.parse(dateStr, DATE_FORMATTER))
                            .transactionId(transactionId)
                            .amount(new BigDecimal(amountStr))
                            .currency(currency)
                            .build();
                    
                    validTransactions.add(transaction);
                    transactionIds.add(transactionId);
                } catch (Exception e) {
                    errorMessages.add(String.format("Row %d: Error creating transaction: %s", 
                        rowNumber, e.getMessage()));
                }
            }
            
            // Check if we have any valid data
            if (validTransactions.isEmpty() && errorMessages.isEmpty()) {
                throw new InvalidFileException("CSV file contains no data rows");
            }
            
        } catch (IOException e) {
            throw new InvalidFileException("Error reading CSV file: " + e.getMessage(), e);
        }
        
        return ValidationResult.builder()
                .validTransactions(validTransactions)
                .errorMessages(errorMessages)
                .totalRows(validTransactions.size() + errorMessages.size())
                .validRows(validTransactions.size())
                .invalidRows(errorMessages.size())
                .build();
    }
    
    /**
     * Validates and parses CSV headers, handling duplicates by using first occurrence
     */
    private Map<String, Integer> validateAndParseHeaders(String headerLine) {
        String[] headers = parseCsvLine(headerLine);
        Map<String, Integer> headerIndices = new LinkedHashMap<>();
        
        // Map headers to their indices, keeping only first occurrence
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim();
            if (!headerIndices.containsKey(header)) {
                headerIndices.put(header, i);
            }
        }
        
        // Validate required headers exist
        List<String> missingHeaders = new ArrayList<>();
        if (!headerIndices.containsKey(HEADER_DATE)) {
            missingHeaders.add(HEADER_DATE);
        }
        if (!headerIndices.containsKey(HEADER_TRANSACTION_ID)) {
            missingHeaders.add(HEADER_TRANSACTION_ID);
        }
        if (!headerIndices.containsKey(HEADER_AMOUNT)) {
            missingHeaders.add(HEADER_AMOUNT);
        }
        if (!headerIndices.containsKey(HEADER_CURRENCY)) {
            missingHeaders.add(HEADER_CURRENCY);
        }
        
        if (!missingHeaders.isEmpty()) {
            throw new InvalidFileException(
                String.format("Missing required headers: %s. Expected headers: Date, TransactionID, Amount, Currency", 
                String.join(", ", missingHeaders)));
        }
        
        return headerIndices;
    }
    
    /**
     * Parses a CSV line, handling quoted values
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString().trim());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        
        fields.add(currentField.toString().trim());
        return fields.toArray(new String[0]);
    }
    
    /**
     * Gets field value from the fields array using header index
     */
    private String getFieldValue(String[] fields, Map<String, Integer> headerIndices, String headerName) {
        Integer index = headerIndices.get(headerName);
        if (index != null && index < fields.length) {
            return fields[index].trim();
        }
        return "";
    }
    
    /**
     * Validates a single row and returns list of errors
     */
    private List<String> validateRow(int rowNumber, String dateStr, String transactionId, 
                                     String amountStr, String currency, Set<String> existingTransactionIds) {
        List<String> errors = new ArrayList<>();
        
        // Validate Date
        if (dateStr == null || dateStr.isEmpty()) {
            errors.add(String.format("Row %d: Date is required", rowNumber));
        } else {
            try {
                LocalDate.parse(dateStr, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                errors.add(String.format("Row %d: Invalid date format '%s'. Expected format: YYYY-MM-DD", 
                    rowNumber, dateStr));
            }
        }
        
        // Validate TransactionID
        if (transactionId == null || transactionId.isEmpty()) {
            errors.add(String.format("Row %d: TransactionID is required", rowNumber));
        } else {
            if (transactionId.length() != TRANSACTION_ID_LENGTH) {
                errors.add(String.format("Row %d: TransactionID must be exactly %d characters. Found: '%s' (%d characters)", 
                    rowNumber, TRANSACTION_ID_LENGTH, transactionId, transactionId.length()));
            }
            if (existingTransactionIds.contains(transactionId)) {
                errors.add(String.format("Row %d: Duplicate TransactionID '%s'", rowNumber, transactionId));
            }
        }
        
        // Validate Amount
        if (amountStr == null || amountStr.isEmpty()) {
            errors.add(String.format("Row %d: Amount is required", rowNumber));
        } else {
            if (!AMOUNT_PATTERN.matcher(amountStr).matches()) {
                errors.add(String.format("Row %d: Invalid amount format '%s'. Amount must be a positive number with up to 2 decimal places", 
                    rowNumber, amountStr));
            } else {
                try {
                    BigDecimal amount = new BigDecimal(amountStr);
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add(String.format("Row %d: Amount must be positive. Found: %s", 
                            rowNumber, amountStr));
                    }
                } catch (NumberFormatException e) {
                    errors.add(String.format("Row %d: Invalid amount value '%s'", rowNumber, amountStr));
                }
            }
        }
        
        // Validate Currency
        if (currency == null || currency.isEmpty()) {
            errors.add(String.format("Row %d: Currency is required", rowNumber));
        } else if (!VALID_CURRENCIES.contains(currency.toUpperCase())) {
            errors.add(String.format("Row %d: Invalid currency '%s'. Allowed values: USD, EUR, INR", 
                rowNumber, currency));
        }
        
        return errors;
    }
    
    /**
     * Result object containing validation results
     */
    @Data
    @Builder
    public static class ValidationResult {
        private List<Transaction> validTransactions;
        private List<String> errorMessages;
        private int totalRows;
        private int validRows;
        private int invalidRows;
        
        public boolean hasErrors() {
            return !errorMessages.isEmpty();
        }
        
        public boolean isValid() {
            return !validTransactions.isEmpty();
        }
    }
}


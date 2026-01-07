package com.currency_data_transformer.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.currency_data_transformer.exception.InvalidFileException;
import com.currency_data_transformer.validation.CsvTransactionValidator.ValidationResult;

/**
 * Comprehensive test suite for CsvTransactionValidator.
 */
class CsvTransactionValidatorTest {
    
    private CsvTransactionValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new CsvTransactionValidator();
    }
    
    // ========== File Basic Validation Tests ==========
    
    @Test
    void testValidateNullFile() {
        InvalidFileException exception = assertThrows(InvalidFileException.class, 
            () -> validator.validate(null));
        assertEquals("File is null or empty", exception.getMessage());
    }
    
    @Test
    void testValidateEmptyFile() {
        MultipartFile emptyFile = new MockMultipartFile(
            "file", "test.csv", "text/csv", new byte[0]);
        
        InvalidFileException exception = assertThrows(InvalidFileException.class, 
            () -> validator.validate(emptyFile));
        assertEquals("File is null or empty", exception.getMessage());
    }
    
    @Test
    void testValidateNonCsvFile() {
        MultipartFile txtFile = new MockMultipartFile(
            "file", "test.txt", "text/plain", "some content".getBytes());
        
        InvalidFileException exception = assertThrows(InvalidFileException.class, 
            () -> validator.validate(txtFile));
        assertEquals("File must be a CSV file with .csv extension", exception.getMessage());
    }
    
    @Test
    void testValidateFileWithNullFilename() {
        MultipartFile file = new MockMultipartFile(
            "file", null, "text/csv", "some content".getBytes());
        
        InvalidFileException exception = assertThrows(InvalidFileException.class, 
            () -> validator.validate(file));
        assertEquals("File must be a CSV file with .csv extension", exception.getMessage());
    }
    
    // ========== Header Validation Tests ==========
    
    @Test
    void testValidateFileWithOnlyHeaders() {
        String csvContent = "Date,TransactionID,Amount,Currency";
        MultipartFile file = createCsvFile(csvContent);
        
        InvalidFileException exception = assertThrows(InvalidFileException.class, 
            () -> validator.validate(file));
        assertEquals("File contains only headers, no data rows found", exception.getMessage());
    }
    
    @Test
    void testValidateFileWithIncorrectHeaderCount() {
        String csvContent = "Date,TransactionID,Amount";
        MultipartFile file = createCsvFile(csvContent);
        
        InvalidFileException exception = assertThrows(InvalidFileException.class, 
            () -> validator.validate(file));
        assertTrue(exception.getMessage().contains("Invalid number of columns"));
        assertTrue(exception.getMessage().contains("Expected 4 columns"));
    }
    
    @Test
    void testValidateFileWithIncorrectHeaderNames() {
        String csvContent = "Date,TransactionID,Price,Currency";
        MultipartFile file = createCsvFile(csvContent);
        
        InvalidFileException exception = assertThrows(InvalidFileException.class, 
            () -> validator.validate(file));
        assertTrue(exception.getMessage().contains("Invalid header at position 3"));
        assertTrue(exception.getMessage().contains("Expected 'Amount'"));
    }
    
    @Test
    void testValidateFileWithCaseInsensitiveHeaders() {
        String csvContent = "date,transactionid,amount,currency\n" +
                          "2024-01-15,TXN-001,100.50,USD";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertTrue(result.isValid());
        assertEquals(1, result.getValidTransactions().size());
    }
    
    @Test
    void testValidateFileWithWhitespaceInHeaders() {
        String csvContent = " Date , TransactionID , Amount , Currency \n" +
                          "2024-01-15,TXN-001,100.50,USD";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertTrue(result.isValid());
        assertEquals(1, result.getValidTransactions().size());
    }
    
    // ========== Valid Transaction Tests ==========
    
    @Test
    void testValidateFileWithSingleValidTransaction() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN-001,100.50,USD";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertTrue(result.isValid());
        assertEquals(1, result.getValidTransactions().size());
        assertEquals(0, result.getErrors().size());
        
        var transaction = result.getValidTransactions().get(0);
        assertEquals(LocalDate.of(2024, 1, 15), transaction.getDate());
        assertEquals("TXN-001", transaction.getTransactionId());
        assertEquals(new BigDecimal("100.50"), transaction.getAmount());
        assertEquals("USD", transaction.getCurrency());
    }
    
    @Test
    void testValidateFileWithMultipleValidTransactions() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN-001,100.50,USD\n" +
                          "2024-01-16,TXN-002,250.75,EUR\n" +
                          "2024-01-17,TXN-003,99.99,GBP";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertTrue(result.isValid());
        assertEquals(3, result.getValidTransactions().size());
        assertEquals(0, result.getErrors().size());
    }
    
    @Test
    void testValidateFileWithEmptyLines() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN-001,100.50,USD\n" +
                          "\n" +
                          "2024-01-16,TXN-002,250.75,EUR\n" +
                          "   \n" +
                          "2024-01-17,TXN-003,99.99,GBP";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertTrue(result.isValid());
        assertEquals(3, result.getValidTransactions().size());
    }
    
    // ========== Date Validation Tests ==========
    
    @Test
    void testValidateDateInMultipleFormats() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN-001,100.50,USD\n" +
                          "15/01/2024,TXN-002,250.75,EUR\n" +
                          "01/15/2024,TXN-003,99.99,GBP";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertTrue(result.isValid());
        assertEquals(3, result.getValidTransactions().size());
    }
    
    @Test
    void testValidateEmptyDate() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          ",TXN-001,100.50,USD";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertFalse(result.isValid());
        assertEquals(0, result.getValidTransactions().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Date field is empty"));
    }
    
    @Test
    void testValidateInvalidDateFormat() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "15-Jan-2024,TXN-001,100.50,USD";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertFalse(result.isValid());
        assertEquals(0, result.getValidTransactions().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Invalid date format"));
    }
    
    // ========== Transaction ID Validation Tests ==========
    
    @Test
    void testValidateEmptyTransactionId() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,,100.50,USD";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("TransactionID field is empty"));
    }
    
    @Test
    void testValidateTransactionIdWithValidCharacters() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN_001-A,100.50,USD\n" +
                          "2024-01-16,ABC123,250.75,EUR\n" +
                          "2024-01-17,txn-999,99.99,GBP";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertTrue(result.isValid());
        assertEquals(3, result.getValidTransactions().size());
    }
    
    @Test
    void testValidateTransactionIdWithInvalidCharacters() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN@001,100.50,USD";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("contains invalid characters"));
    }
    
    @Test
    void testValidateTransactionIdTooLong() {
        String tooLongId = "A".repeat(101);
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15," + tooLongId + ",100.50,USD";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("TransactionID is too long"));
    }
    
    // ========== Amount Validation Tests ==========
    
    @Test
    void testValidateEmptyAmount() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN-001,,USD";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("Amount field is empty"));
    }
    
    @Test
    void testValidateNegativeAmount() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN-001,-100.50,USD";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("Amount must be positive"));
    }
    
    @Test
    void testValidateZeroAmount() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN-001,0,USD";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("Amount must be positive"));
    }
    
    @Test
    void testValidateInvalidAmountFormat() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN-001,100.5a,USD";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("Invalid amount format"));
    }
    
    @Test
    void testValidateAmountWithTooManyDecimals() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN-001,100.123,USD";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("too many decimal places"));
    }
    
    @Test
    void testValidateValidAmountFormats() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN-001,100,USD\n" +
                          "2024-01-16,TXN-002,100.5,EUR\n" +
                          "2024-01-17,TXN-003,100.50,GBP";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertTrue(result.isValid());
        assertEquals(3, result.getValidTransactions().size());
    }
    
    // ========== Currency Validation Tests ==========
    
    @Test
    void testValidateEmptyCurrency() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN-001,100.50,";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("Currency field is empty"));
    }
    
    @Test
    void testValidateCurrencyWithInvalidLength() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN-001,100.50,US";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("must be 3 uppercase letters"));
    }
    
    @Test
    void testValidateCurrencyWithLowercase() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN-001,100.50,usd";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("must be 3 uppercase letters"));
    }
    
    @Test
    void testValidateInvalidCurrencyCode() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN-001,100.50,ZZZ";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("Invalid currency code"));
    }
    
    @Test
    void testValidateCommonCurrencies() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN-001,100.50,USD\n" +
                          "2024-01-16,TXN-002,250.75,EUR\n" +
                          "2024-01-17,TXN-003,99.99,GBP\n" +
                          "2024-01-18,TXN-004,150.00,JPY\n" +
                          "2024-01-19,TXN-005,200.25,CAD";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertTrue(result.isValid());
        assertEquals(5, result.getValidTransactions().size());
    }
    
    // ========== Mixed Valid and Invalid Rows Tests ==========
    
    @Test
    void testValidateFileWithMixedValidAndInvalidRows() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN-001,100.50,USD\n" +
                          "2024-01-16,TXN-002,-50.00,EUR\n" +
                          "2024-01-17,TXN-003,75.25,GBP\n" +
                          "invalid-date,TXN-004,100.00,USD\n" +
                          "2024-01-19,TXN-005,200.25,CAD";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertFalse(result.isValid());
        assertEquals(3, result.getValidTransactions().size());
        assertEquals(2, result.getErrors().size());
        assertTrue(result.hasValidTransactions());
    }
    
    @Test
    void testValidateFileWithAllInvalidRows() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN-001,-100.50,USD\n" +
                          "invalid-date,TXN-002,50.00,EUR\n" +
                          "2024-01-17,,75.25,GBP";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertFalse(result.isValid());
        assertEquals(0, result.getValidTransactions().size());
        assertEquals(3, result.getErrors().size());
        assertFalse(result.hasValidTransactions());
    }
    
    @Test
    void testValidateFileWithIncorrectColumnCount() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN-001,100.50\n" +
                          "2024-01-16,TXN-002,250.75,EUR,ExtraColumn";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertFalse(result.isValid());
        assertEquals(0, result.getValidTransactions().size());
        assertEquals(2, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Invalid number of columns"));
    }
    
    // ========== ValidationResult Tests ==========
    
    @Test
    void testValidationResultMethods() {
        String csvContent = "Date,TransactionID,Amount,Currency\n" +
                          "2024-01-15,TXN-001,100.50,USD\n" +
                          "2024-01-16,TXN-002,-50.00,EUR\n" +
                          "2024-01-17,TXN-003,75.25,GBP";
        MultipartFile file = createCsvFile(csvContent);
        
        ValidationResult result = validator.validate(file);
        
        assertFalse(result.isValid());
        assertTrue(result.hasValidTransactions());
        assertEquals(3, result.getTotalRowsProcessed());
        assertEquals(2, result.getValidTransactions().size());
        assertEquals(1, result.getErrors().size());
    }
    
    // ========== Helper Methods ==========
    
    private MultipartFile createCsvFile(String content) {
        return new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            content.getBytes()
        );
    }
}


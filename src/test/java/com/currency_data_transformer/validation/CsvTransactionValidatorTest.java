package com.currency_data_transformer.validation;

import com.currency_data_transformer.exception.InvalidFileException;
import com.currency_data_transformer.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CsvTransactionValidatorTest {

    private CsvTransactionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CsvTransactionValidator();
    }

    @Test
    void testValidCsvFile() {
        String csvContent = """
                Date,TransactionID,Amount,Currency
                2024-01-15,TXN1234567,100.50,USD
                2024-01-16,TXN7654321,200.00,EUR
                2024-01-17,TXN9876543,50.5,INR
                """;

        MultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validateCsvFile(file);

        assertNotNull(result);
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
        assertEquals(3, result.getValidRows());
        assertEquals(0, result.getInvalidRows());
        assertEquals(3, result.getTotalRows());
        assertEquals(3, result.getValidTransactions().size());

        // Verify first transaction
        Transaction firstTransaction = result.getValidTransactions().get(0);
        assertEquals(LocalDate.of(2024, 1, 15), firstTransaction.getDate());
        assertEquals("TXN1234567", firstTransaction.getTransactionId());
        assertEquals(new BigDecimal("100.50"), firstTransaction.getAmount());
        assertEquals("USD", firstTransaction.getCurrency());
    }

    @Test
    void testEmptyFile() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                new byte[0]
        );

        InvalidFileException exception = assertThrows(
                InvalidFileException.class,
                () -> validator.validateCsvFile(file)
        );

        assertTrue(exception.getMessage().contains("empty"));
    }

    @Test
    void testNullFile() {
        InvalidFileException exception = assertThrows(
                InvalidFileException.class,
                () -> validator.validateCsvFile(null)
        );

        assertTrue(exception.getMessage().contains("empty or not provided"));
    }

    @Test
    void testInvalidFileExtension() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "transactions.txt",
                "text/plain",
                "some content".getBytes()
        );

        InvalidFileException exception = assertThrows(
                InvalidFileException.class,
                () -> validator.validateCsvFile(file)
        );

        assertTrue(exception.getMessage().contains("CSV file"));
    }

    @Test
    void testMissingHeaders() {
        String csvContent = """
                Date,Amount,Currency
                2024-01-15,100.50,USD
                """;

        MultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csvContent.getBytes()
        );

        InvalidFileException exception = assertThrows(
                InvalidFileException.class,
                () -> validator.validateCsvFile(file)
        );

        assertTrue(exception.getMessage().contains("Missing required headers"));
        assertTrue(exception.getMessage().contains("TransactionID"));
    }

    @Test
    void testDuplicateHeaders() {
        String csvContent = """
                Date,TransactionID,Amount,Currency,Date,TransactionID
                2024-01-15,TXN1234567,100.50,USD,2024-01-16,TXN9999999
                """;

        MultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csvContent.getBytes()
        );

        // Should use first occurrence of headers and validate successfully
        CsvTransactionValidator.ValidationResult result = validator.validateCsvFile(file);

        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(1, result.getValidRows());
        
        // Should use values from first Date and TransactionID columns
        Transaction transaction = result.getValidTransactions().get(0);
        assertEquals(LocalDate.of(2024, 1, 15), transaction.getDate());
        assertEquals("TXN1234567", transaction.getTransactionId());
    }

    @Test
    void testInvalidDateFormat() {
        String csvContent = """
                Date,TransactionID,Amount,Currency
                01-15-2024,TXN1234567,100.50,USD
                2024/01/16,TXN7654321,200.00,EUR
                """;

        MultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validateCsvFile(file);

        assertTrue(result.hasErrors());
        assertEquals(2, result.getInvalidRows());
        assertTrue(result.getErrorMessages().get(0).contains("Invalid date format"));
        assertTrue(result.getErrorMessages().get(0).contains("YYYY-MM-DD"));
    }

    @Test
    void testInvalidTransactionIdLength() {
        String csvContent = """
                Date,TransactionID,Amount,Currency
                2024-01-15,TXN123,100.50,USD
                2024-01-16,TXN12345678901,200.00,EUR
                """;

        MultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validateCsvFile(file);

        assertTrue(result.hasErrors());
        assertEquals(2, result.getInvalidRows());
        assertTrue(result.getErrorMessages().get(0).contains("exactly 10 characters"));
    }

    @Test
    void testDuplicateTransactionId() {
        String csvContent = """
                Date,TransactionID,Amount,Currency
                2024-01-15,TXN1234567,100.50,USD
                2024-01-16,TXN1234567,200.00,EUR
                """;

        MultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validateCsvFile(file);

        assertTrue(result.hasErrors());
        assertEquals(1, result.getValidRows());
        assertEquals(1, result.getInvalidRows());
        assertTrue(result.getErrorMessages().get(0).contains("Duplicate TransactionID"));
    }

    @Test
    void testInvalidAmountFormat() {
        String csvContent = """
                Date,TransactionID,Amount,Currency
                2024-01-15,TXN1234567,100.555,USD
                2024-01-16,TXN7654321,-50.00,EUR
                2024-01-17,TXN9876543,abc,INR
                2024-01-18,TXN1111111,0,USD
                """;

        MultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validateCsvFile(file);

        assertTrue(result.hasErrors());
        assertEquals(4, result.getInvalidRows());
        
        assertTrue(result.getErrorMessages().stream()
                .anyMatch(msg -> msg.contains("up to 2 decimal places")));
        assertTrue(result.getErrorMessages().stream()
                .anyMatch(msg -> msg.contains("must be positive")));
    }

    @Test
    void testInvalidCurrency() {
        String csvContent = """
                Date,TransactionID,Amount,Currency
                2024-01-15,TXN1234567,100.50,GBP
                2024-01-16,TXN7654321,200.00,JPY
                """;

        MultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validateCsvFile(file);

        assertTrue(result.hasErrors());
        assertEquals(2, result.getInvalidRows());
        assertTrue(result.getErrorMessages().get(0).contains("Invalid currency"));
        assertTrue(result.getErrorMessages().get(0).contains("USD, EUR, INR"));
    }

    @Test
    void testMixedValidAndInvalidRows() {
        String csvContent = """
                Date,TransactionID,Amount,Currency
                2024-01-15,TXN1234567,100.50,USD
                2024/01/16,TXN7654321,200.00,EUR
                2024-01-17,TXN9876543,50.00,INR
                2024-01-18,TXN123,100.00,USD
                """;

        MultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validateCsvFile(file);

        assertTrue(result.isValid());
        assertTrue(result.hasErrors());
        assertEquals(2, result.getValidRows());
        assertEquals(2, result.getInvalidRows());
        assertEquals(4, result.getTotalRows());
    }

    @Test
    void testEmptyFields() {
        String csvContent = """
                Date,TransactionID,Amount,Currency
                ,TXN1234567,100.50,USD
                2024-01-16,,200.00,EUR
                2024-01-17,TXN9876543,,INR
                2024-01-18,TXN1111111,100.00,
                """;

        MultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validateCsvFile(file);

        assertTrue(result.hasErrors());
        assertEquals(4, result.getInvalidRows());
        
        assertTrue(result.getErrorMessages().stream()
                .anyMatch(msg -> msg.contains("Date is required")));
        assertTrue(result.getErrorMessages().stream()
                .anyMatch(msg -> msg.contains("TransactionID is required")));
        assertTrue(result.getErrorMessages().stream()
                .anyMatch(msg -> msg.contains("Amount is required")));
        assertTrue(result.getErrorMessages().stream()
                .anyMatch(msg -> msg.contains("Currency is required")));
    }

    @Test
    void testCsvWithQuotedValues() {
        String csvContent = """
                Date,TransactionID,Amount,Currency
                "2024-01-15","TXN1234567","100.50","USD"
                2024-01-16,TXN7654321,200.00,EUR
                """;

        MultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validateCsvFile(file);

        assertTrue(result.isValid());
        assertEquals(2, result.getValidRows());
        assertEquals(0, result.getInvalidRows());
    }

    @Test
    void testSkipEmptyLines() {
        String csvContent = """
                Date,TransactionID,Amount,Currency
                2024-01-15,TXN1234567,100.50,USD
                
                2024-01-16,TXN7654321,200.00,EUR
                
                """;

        MultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validateCsvFile(file);

        assertTrue(result.isValid());
        assertEquals(2, result.getValidRows());
        assertEquals(0, result.getInvalidRows());
    }

    @Test
    void testAmountWithOneDecimalPlace() {
        String csvContent = """
                Date,TransactionID,Amount,Currency
                2024-01-15,TXN1234567,100.5,USD
                """;

        MultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validateCsvFile(file);

        assertTrue(result.isValid());
        assertEquals(1, result.getValidRows());
        assertEquals(new BigDecimal("100.5"), result.getValidTransactions().get(0).getAmount());
    }

    @Test
    void testAmountWithNoDecimalPlace() {
        String csvContent = """
                Date,TransactionID,Amount,Currency
                2024-01-15,TXN1234567,100,USD
                """;

        MultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validateCsvFile(file);

        assertTrue(result.isValid());
        assertEquals(1, result.getValidRows());
        assertEquals(new BigDecimal("100"), result.getValidTransactions().get(0).getAmount());
    }

    @Test
    void testCaseInsensitiveCurrency() {
        String csvContent = """
                Date,TransactionID,Amount,Currency
                2024-01-15,TXN1234567,100.50,usd
                2024-01-16,TXN7654321,200.00,Eur
                2024-01-17,TXN9876543,50.00,inr
                """;

        MultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validateCsvFile(file);

        assertTrue(result.isValid());
        assertEquals(3, result.getValidRows());
    }

    @Test
    void testOnlyHeadersNoData() {
        String csvContent = """
                Date,TransactionID,Amount,Currency
                """;

        MultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csvContent.getBytes()
        );

        InvalidFileException exception = assertThrows(
                InvalidFileException.class,
                () -> validator.validateCsvFile(file)
        );

        assertTrue(exception.getMessage().contains("no data rows"));
    }

    @Test
    void testInsufficientColumns() {
        String csvContent = """
                Date,TransactionID,Amount,Currency
                2024-01-15,TXN1234567,100.50
                """;

        MultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validateCsvFile(file);

        assertTrue(result.hasErrors());
        assertEquals(1, result.getInvalidRows());
        assertTrue(result.getErrorMessages().get(0).contains("Insufficient number of columns"));
    }
}


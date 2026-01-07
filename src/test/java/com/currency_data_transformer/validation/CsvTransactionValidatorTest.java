package com.currency_data_transformer.validation;

import com.currency_data_transformer.exception.InvalidFileException;
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

    // ==================== File-level Validation Tests ====================

    @Test
    void testValidateFile_WhenFileIsNull_ThrowsException() {
        InvalidFileException exception = assertThrows(
            InvalidFileException.class,
            () -> validator.validate(null)
        );
        assertEquals("File is null", exception.getMessage());
    }

    @Test
    void testValidateFile_WhenFileIsEmpty_ThrowsException() {
        MultipartFile emptyFile = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            new byte[0]
        );

        InvalidFileException exception = assertThrows(
            InvalidFileException.class,
            () -> validator.validate(emptyFile)
        );
        assertEquals("File is empty", exception.getMessage());
    }

    @Test
    void testValidateFile_WhenFileIsNotCsv_ThrowsException() {
        MultipartFile txtFile = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "Some content".getBytes()
        );

        InvalidFileException exception = assertThrows(
            InvalidFileException.class,
            () -> validator.validate(txtFile)
        );
        assertEquals("File must be a CSV file", exception.getMessage());
    }

    @Test
    void testValidateFile_WhenFileHasNoExtension_ThrowsException() {
        MultipartFile noExtFile = new MockMultipartFile(
            "file",
            "test",
            "text/plain",
            "Some content".getBytes()
        );

        InvalidFileException exception = assertThrows(
            InvalidFileException.class,
            () -> validator.validate(noExtFile)
        );
        assertEquals("File must be a CSV file", exception.getMessage());
    }

    // ==================== Header Validation Tests ====================

    @Test
    void testValidateHeaders_WhenHeadersAreCorrect_PassesValidation() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,100.50,USD
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertNotNull(result);
        assertEquals(1, result.getValidTransactions().size());
    }

    @Test
    void testValidateHeaders_WhenHeadersAreIncorrect_ThrowsException() {
        String csvContent = """
            Date,ID,Amount,Currency
            2024-01-15,TXN1234567,100.50,USD
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        InvalidFileException exception = assertThrows(
            InvalidFileException.class,
            () -> validator.validate(file)
        );
        assertTrue(exception.getMessage().contains("Invalid headers"));
    }

    @Test
    void testValidateHeaders_WhenHeadersAreMissing_ThrowsException() {
        String csvContent = "2024-01-15,TXN1234567,100.50,USD";

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        InvalidFileException exception = assertThrows(
            InvalidFileException.class,
            () -> validator.validate(file)
        );
        assertTrue(exception.getMessage().contains("Invalid headers"));
    }

    @Test
    void testValidateHeaders_WhenWrongNumberOfColumns_ThrowsException() {
        String csvContent = """
            Date,TransactionID,Amount
            2024-01-15,TXN1234567,100.50
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        InvalidFileException exception = assertThrows(
            InvalidFileException.class,
            () -> validator.validate(file)
        );
        assertTrue(exception.getMessage().contains("Invalid headers"));
    }

    // ==================== Date Validation Tests ====================

    @Test
    void testValidateDate_WhenDateIsValid_PassesValidation() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,100.50,USD
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(1, result.getValidTransactions().size());
        assertEquals(LocalDate.of(2024, 1, 15), result.getValidTransactions().get(0).getDate());
    }

    @Test
    void testValidateDate_WhenDateIsInWrongFormat_ReturnsError() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            15-01-2024,TXN1234567,100.50,USD
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(0, result.getValidTransactions().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getMessage().contains("yyyy-MM-dd format"));
    }

    @Test
    void testValidateDate_WhenDateIsEmpty_ReturnsError() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            ,TXN1234567,100.50,USD
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(0, result.getValidTransactions().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getMessage().contains("Date is required"));
    }

    @Test
    void testValidateDate_WhenDateIsInvalid_ReturnsError() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-13-45,TXN1234567,100.50,USD
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(0, result.getValidTransactions().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getMessage().contains("yyyy-MM-dd format"));
    }

    // ==================== TransactionID Validation Tests ====================

    @Test
    void testValidateTransactionId_WhenIdIsValid_PassesValidation() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,100.50,USD
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(1, result.getValidTransactions().size());
        assertEquals("TXN1234567", result.getValidTransactions().get(0).getTransactionId());
    }

    @Test
    void testValidateTransactionId_WhenIdIsTooShort_ReturnsError() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN123,100.50,USD
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(0, result.getValidTransactions().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getMessage().contains("exactly 10 characters"));
    }

    @Test
    void testValidateTransactionId_WhenIdIsTooLong_ReturnsError() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN12345678901,100.50,USD
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(0, result.getValidTransactions().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getMessage().contains("exactly 10 characters"));
    }

    @Test
    void testValidateTransactionId_WhenIdIsEmpty_ReturnsError() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,,100.50,USD
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(0, result.getValidTransactions().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getMessage().contains("TransactionID is required"));
    }

    @Test
    void testValidateTransactionId_WhenIdIsDuplicate_ReturnsError() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,100.50,USD
            2024-01-16,TXN1234567,200.75,EUR
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(1, result.getValidTransactions().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getMessage().contains("Duplicate TransactionID"));
        assertEquals(3, result.getErrors().get(0).getRowNumber());
    }

    // ==================== Amount Validation Tests ====================

    @Test
    void testValidateAmount_WhenAmountIsValid_PassesValidation() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,100.50,USD
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(1, result.getValidTransactions().size());
        assertEquals(new BigDecimal("100.50"), result.getValidTransactions().get(0).getAmount());
    }

    @Test
    void testValidateAmount_WhenAmountHasNoDecimal_PassesValidation() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,100,USD
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(1, result.getValidTransactions().size());
        assertEquals(new BigDecimal("100"), result.getValidTransactions().get(0).getAmount());
    }

    @Test
    void testValidateAmount_WhenAmountHasOneDecimal_PassesValidation() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,100.5,USD
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(1, result.getValidTransactions().size());
        assertEquals(new BigDecimal("100.5"), result.getValidTransactions().get(0).getAmount());
    }

    @Test
    void testValidateAmount_WhenAmountIsZero_ReturnsError() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,0,USD
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(0, result.getValidTransactions().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getMessage().contains("positive number"));
    }

    @Test
    void testValidateAmount_WhenAmountIsNegative_ReturnsError() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,-100.50,USD
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(0, result.getValidTransactions().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getMessage().contains("positive number"));
    }

    @Test
    void testValidateAmount_WhenAmountHasMoreThanTwoDecimals_ReturnsError() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,100.505,USD
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(0, result.getValidTransactions().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getMessage().contains("maximum 2 decimal places"));
    }

    @Test
    void testValidateAmount_WhenAmountIsEmpty_ReturnsError() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,,USD
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(0, result.getValidTransactions().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getMessage().contains("Amount is required"));
    }

    @Test
    void testValidateAmount_WhenAmountIsNotNumeric_ReturnsError() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,abc,USD
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(0, result.getValidTransactions().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getMessage().contains("valid number"));
    }

    // ==================== Currency Validation Tests ====================

    @Test
    void testValidateCurrency_WhenCurrencyIsUSD_PassesValidation() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,100.50,USD
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(1, result.getValidTransactions().size());
        assertEquals("USD", result.getValidTransactions().get(0).getCurrency());
    }

    @Test
    void testValidateCurrency_WhenCurrencyIsEUR_PassesValidation() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,100.50,EUR
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(1, result.getValidTransactions().size());
        assertEquals("EUR", result.getValidTransactions().get(0).getCurrency());
    }

    @Test
    void testValidateCurrency_WhenCurrencyIsINR_PassesValidation() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,100.50,INR
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(1, result.getValidTransactions().size());
        assertEquals("INR", result.getValidTransactions().get(0).getCurrency());
    }

    @Test
    void testValidateCurrency_WhenCurrencyIsInvalid_ReturnsError() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,100.50,GBP
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(0, result.getValidTransactions().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getMessage().contains("Currency must be one of"));
    }

    @Test
    void testValidateCurrency_WhenCurrencyIsEmpty_ReturnsError() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,100.50,
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(0, result.getValidTransactions().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getMessage().contains("Currency is required"));
    }

    // ==================== Integration Tests ====================

    @Test
    void testValidate_WhenMultipleValidRows_ReturnsAllTransactions() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,100.50,USD
            2024-01-16,TXN9876543,250.75,EUR
            2024-01-17,TXN5555555,99.99,INR
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(3, result.getValidTransactions().size());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void testValidate_WhenMixedValidAndInvalidRows_ReturnsBothValidAndErrors() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,100.50,USD
            2024-13-99,TXN9876543,250.75,EUR
            2024-01-17,TXN5555555,-99.99,INR
            2024-01-18,TXN0000000,50.00,GBP
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(1, result.getValidTransactions().size());
        assertEquals(3, result.getErrors().size());
        
        // Verify row numbers are correct
        assertEquals(3, result.getErrors().get(0).getRowNumber());
        assertEquals(4, result.getErrors().get(1).getRowNumber());
        assertEquals(5, result.getErrors().get(2).getRowNumber());
    }

    @Test
    void testValidate_WhenRowHasMultipleErrors_CombinesAllErrors() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-13-99,TXN123,-100.505,GBP
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(0, result.getValidTransactions().size());
        assertEquals(1, result.getErrors().size());
        
        String errorMessage = result.getErrors().get(0).getMessage();
        assertTrue(errorMessage.contains("Date"));
        assertTrue(errorMessage.contains("Amount"));
        assertTrue(errorMessage.contains("Currency"));
    }

    @Test
    void testValidate_WhenFileHasEmptyLines_SkipsEmptyLines() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,100.50,USD
            
            2024-01-16,TXN9876543,250.75,EUR
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(2, result.getValidTransactions().size());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void testValidate_WhenFileHasOnlyHeaders_ThrowsException() {
        String csvContent = "Date,TransactionID,Amount,Currency\n";

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        InvalidFileException exception = assertThrows(
            InvalidFileException.class,
            () -> validator.validate(file)
        );
        assertTrue(exception.getMessage().contains("no data rows"));
    }

    @Test
    void testValidate_WhenRowHasWrongNumberOfColumns_ReturnsError() {
        String csvContent = """
            Date,TransactionID,Amount,Currency
            2024-01-15,TXN1234567,100.50
            """;

        MultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        CsvTransactionValidator.ValidationResult result = validator.validate(file);
        assertEquals(0, result.getValidTransactions().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getMessage().contains("Invalid number of columns"));
    }

    @Test
    void testValidationResult_HasErrorsMethod() {
        CsvTransactionValidator.ValidationResult result = new CsvTransactionValidator.ValidationResult();
        assertFalse(result.hasErrors());
        
        result.addError(2, "Some error");
        assertTrue(result.hasErrors());
    }

    @Test
    void testValidationError_Constructor() {
        CsvTransactionValidator.ValidationError error = 
            new CsvTransactionValidator.ValidationError(5, "Test error message");
        
        assertEquals(5, error.getRowNumber());
        assertEquals("Test error message", error.getMessage());
    }
}


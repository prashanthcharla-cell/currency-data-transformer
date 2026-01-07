package com.currency_data_transformer.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.currency_data_transformer.exception.InvalidFileException;
import com.currency_data_transformer.model.Transaction;
import com.currency_data_transformer.model.request.UploadFileRequest;
import com.currency_data_transformer.model.response.UploadFileResponse;
import com.currency_data_transformer.validation.CsvTransactionValidator;
import com.currency_data_transformer.validation.CsvTransactionValidator.ValidationResult;

/**
 * Test suite for CurrencyDataTransformerService.
 */
@ExtendWith(MockitoExtension.class)
class CurrencyDataTransformerServiceTest {
    
    @Mock
    private CsvTransactionValidator validator;
    
    @InjectMocks
    private CurrencyDataTransformerService service;
    
    private MultipartFile mockFile;
    
    @BeforeEach
    void setUp() {
        mockFile = new MockMultipartFile(
            "file", "test.csv", "text/csv", "test content".getBytes());
    }
    
    @Test
    void testTransformCurrencyDataWithValidFile() {
        // Arrange
        List<Transaction> transactions = createValidTransactions();
        ValidationResult validationResult = new ValidationResult(transactions, new ArrayList<>());
        
        when(validator.validate(any(MultipartFile.class))).thenReturn(validationResult);
        
        UploadFileRequest request = new UploadFileRequest(mockFile);
        
        // Act
        UploadFileResponse response = service.transformCurrencyData(request);
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.jobId());
        assertEquals("COMPLETED", response.status());
        assertTrue(response.message().contains("File validated successfully"));
        assertTrue(response.message().contains("3 valid transactions"));
        
        verify(validator, times(1)).validate(mockFile);
    }
    
    @Test
    void testTransformCurrencyDataWithPartiallyValidFile() {
        // Arrange
        List<Transaction> transactions = createValidTransactions();
        List<String> errors = List.of(
            "Line 2: Invalid date format",
            "Line 4: Amount must be positive"
        );
        ValidationResult validationResult = new ValidationResult(transactions, errors);
        
        when(validator.validate(any(MultipartFile.class))).thenReturn(validationResult);
        
        UploadFileRequest request = new UploadFileRequest(mockFile);
        
        // Act
        UploadFileResponse response = service.transformCurrencyData(request);
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.jobId());
        assertEquals("COMPLETED_WITH_WARNINGS", response.status());
        assertTrue(response.message().contains("File processed with warnings"));
        assertTrue(response.message().contains("3 valid transactions"));
        assertTrue(response.message().contains("2 invalid rows"));
        
        verify(validator, times(1)).validate(mockFile);
    }
    
    @Test
    void testTransformCurrencyDataWithNoValidTransactions() {
        // Arrange
        List<String> errors = List.of(
            "Line 2: Invalid date format",
            "Line 3: Amount must be positive",
            "Line 4: Invalid currency code"
        );
        ValidationResult validationResult = new ValidationResult(new ArrayList<>(), errors);
        
        when(validator.validate(any(MultipartFile.class))).thenReturn(validationResult);
        
        UploadFileRequest request = new UploadFileRequest(mockFile);
        
        // Act
        UploadFileResponse response = service.transformCurrencyData(request);
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.jobId());
        assertEquals("FAILED", response.status());
        assertTrue(response.message().contains("File validation failed"));
        assertTrue(response.message().contains("No valid transactions found"));
        
        verify(validator, times(1)).validate(mockFile);
    }
    
    @Test
    void testTransformCurrencyDataWithValidationException() {
        // Arrange
        when(validator.validate(any(MultipartFile.class)))
            .thenThrow(new InvalidFileException("File is empty"));
        
        UploadFileRequest request = new UploadFileRequest(mockFile);
        
        // Act
        UploadFileResponse response = service.transformCurrencyData(request);
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.jobId());
        assertEquals("FAILED", response.status());
        assertTrue(response.message().contains("Error processing file"));
        assertTrue(response.message().contains("File is empty"));
        
        verify(validator, times(1)).validate(mockFile);
    }
    
    @Test
    void testTransformCurrencyDataWithMultipleErrors() {
        // Arrange
        List<Transaction> transactions = List.of(createTransaction());
        List<String> errors = List.of(
            "Line 2: Invalid date format",
            "Line 3: Amount must be positive",
            "Line 4: Invalid currency code",
            "Line 5: TransactionID is empty"
        );
        ValidationResult validationResult = new ValidationResult(transactions, errors);
        
        when(validator.validate(any(MultipartFile.class))).thenReturn(validationResult);
        
        UploadFileRequest request = new UploadFileRequest(mockFile);
        
        // Act
        UploadFileResponse response = service.transformCurrencyData(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("COMPLETED_WITH_WARNINGS", response.status());
        assertTrue(response.message().contains("1 valid transactions"));
        assertTrue(response.message().contains("4 invalid rows"));
        assertTrue(response.message().contains("and 1 more errors"));
        
        verify(validator, times(1)).validate(mockFile);
    }
    
    @Test
    void testTransformCurrencyDataGeneratesUniqueJobIds() {
        // Arrange
        List<Transaction> transactions = createValidTransactions();
        ValidationResult validationResult = new ValidationResult(transactions, new ArrayList<>());
        
        when(validator.validate(any(MultipartFile.class))).thenReturn(validationResult);
        
        UploadFileRequest request = new UploadFileRequest(mockFile);
        
        // Act
        UploadFileResponse response1 = service.transformCurrencyData(request);
        UploadFileResponse response2 = service.transformCurrencyData(request);
        
        // Assert
        assertNotNull(response1.jobId());
        assertNotNull(response2.jobId());
        assertNotEquals(response1.jobId(), response2.jobId());
    }
    
    // Helper methods
    
    private List<Transaction> createValidTransactions() {
        return List.of(
            createTransaction(),
            Transaction.builder()
                .date(LocalDate.of(2024, 1, 16))
                .transactionId("TXN-002")
                .amount(new BigDecimal("250.75"))
                .currency("EUR")
                .build(),
            Transaction.builder()
                .date(LocalDate.of(2024, 1, 17))
                .transactionId("TXN-003")
                .amount(new BigDecimal("99.99"))
                .currency("GBP")
                .build()
        );
    }
    
    private Transaction createTransaction() {
        return Transaction.builder()
            .date(LocalDate.of(2024, 1, 15))
            .transactionId("TXN-001")
            .amount(new BigDecimal("100.50"))
            .currency("USD")
            .build();
    }
}


package com.currency_data_transformer.service;

import com.currency_data_transformer.exception.InvalidFileException;
import com.currency_data_transformer.model.Transaction;
import com.currency_data_transformer.model.request.UploadFileRequest;
import com.currency_data_transformer.model.response.UploadFileResponse;
import com.currency_data_transformer.validation.CsvTransactionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrencyDataTransformerServiceTest {

    @Mock
    private CsvTransactionValidator csvTransactionValidator;

    @InjectMocks
    private CurrencyDataTransformerService service;

    private MockMultipartFile validFile;

    @BeforeEach
    void setUp() {
        validFile = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                "Date,TransactionID,Amount,Currency\n2024-01-15,TXN1234567,100.50,USD".getBytes()
        );
    }

    @Test
    void testSuccessfulValidation() {
        // Prepare mock data
        List<Transaction> transactions = List.of(
                Transaction.builder()
                        .date(LocalDate.of(2024, 1, 15))
                        .transactionId("TXN1234567")
                        .amount(new BigDecimal("100.50"))
                        .currency("USD")
                        .build()
        );

        CsvTransactionValidator.ValidationResult validationResult = CsvTransactionValidator.ValidationResult.builder()
                .validTransactions(transactions)
                .errorMessages(new ArrayList<>())
                .totalRows(1)
                .validRows(1)
                .invalidRows(0)
                .build();

        when(csvTransactionValidator.validateCsvFile(any())).thenReturn(validationResult);

        // Execute
        UploadFileRequest request = new UploadFileRequest(validFile);
        UploadFileResponse response = service.transformCurrencyData(request);

        // Verify
        assertNotNull(response);
        assertEquals("COMPLETED", response.status());
        assertTrue(response.message().contains("Valid: 1"));
        assertTrue(response.message().contains("Invalid: 0"));
        assertNotNull(response.jobId());

        verify(csvTransactionValidator, times(1)).validateCsvFile(validFile);
    }

    @Test
    void testValidationWithErrors() {
        // Prepare mock data
        List<String> errors = List.of(
                "Row 2: Invalid date format",
                "Row 3: TransactionID must be exactly 10 characters"
        );

        List<Transaction> transactions = List.of(
                Transaction.builder()
                        .date(LocalDate.of(2024, 1, 15))
                        .transactionId("TXN1234567")
                        .amount(new BigDecimal("100.50"))
                        .currency("USD")
                        .build()
        );

        CsvTransactionValidator.ValidationResult validationResult = CsvTransactionValidator.ValidationResult.builder()
                .validTransactions(transactions)
                .errorMessages(errors)
                .totalRows(3)
                .validRows(1)
                .invalidRows(2)
                .build();

        when(csvTransactionValidator.validateCsvFile(any())).thenReturn(validationResult);

        // Execute
        UploadFileRequest request = new UploadFileRequest(validFile);
        UploadFileResponse response = service.transformCurrencyData(request);

        // Verify
        assertNotNull(response);
        assertEquals("COMPLETED", response.status());
        assertTrue(response.message().contains("Valid: 1"));
        assertTrue(response.message().contains("Invalid: 2"));
        assertTrue(response.message().contains("Validation Errors"));
        assertTrue(response.message().contains("Invalid date format"));

        verify(csvTransactionValidator, times(1)).validateCsvFile(validFile);
    }

    @Test
    void testValidationFailure() {
        // Prepare mock data - no valid transactions
        List<String> errors = List.of(
                "Row 2: Invalid date format",
                "Row 3: TransactionID must be exactly 10 characters"
        );

        CsvTransactionValidator.ValidationResult validationResult = CsvTransactionValidator.ValidationResult.builder()
                .validTransactions(new ArrayList<>())
                .errorMessages(errors)
                .totalRows(2)
                .validRows(0)
                .invalidRows(2)
                .build();

        when(csvTransactionValidator.validateCsvFile(any())).thenReturn(validationResult);

        // Execute
        UploadFileRequest request = new UploadFileRequest(validFile);
        UploadFileResponse response = service.transformCurrencyData(request);

        // Verify
        assertNotNull(response);
        assertEquals("FAILED", response.status());
        assertTrue(response.message().contains("Valid: 0"));
        assertTrue(response.message().contains("Invalid: 2"));

        verify(csvTransactionValidator, times(1)).validateCsvFile(validFile);
    }

    @Test
    void testInvalidFileException() {
        // Mock validator throwing exception
        when(csvTransactionValidator.validateCsvFile(any()))
                .thenThrow(new InvalidFileException("File is empty or not provided"));

        // Execute and verify
        UploadFileRequest request = new UploadFileRequest(validFile);
        
        assertThrows(InvalidFileException.class, () -> service.transformCurrencyData(request));

        verify(csvTransactionValidator, times(1)).validateCsvFile(validFile);
    }
}


package com.currency_data_transformer.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.currency_data_transformer.model.request.UploadFileRequest;
import com.currency_data_transformer.model.response.UploadFileResponse;
import com.currency_data_transformer.validation.CsvTransactionValidator;
import com.currency_data_transformer.validation.CsvTransactionValidator.ValidationResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for transforming currency data from uploaded CSV files.
 * Validates and processes transaction files.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyDataTransformerService {
    
    private final CsvTransactionValidator validator;
    
    /**
     * Transforms currency data from an uploaded CSV file.
     * Validates the file and returns processing results.
     *
     * @param request the upload file request containing the CSV file
     * @return response with job ID, status, and processing message
     */
    public UploadFileResponse transformCurrencyData(UploadFileRequest request) {
        String jobId = UUID.randomUUID().toString();
        log.info("Processing file upload with job ID: {}", jobId);
        
        try {
            // Validate the CSV file
            ValidationResult validationResult = validator.validate(request.file());
            
            // Build response message based on validation results
            String message = buildResponseMessage(validationResult);
            String status = determineStatus(validationResult);
            
            log.info("Job {}: {} valid transactions, {} errors", 
                     jobId, validationResult.getValidTransactions().size(), validationResult.getErrors().size());
            
            return new UploadFileResponse(jobId, status, message);
            
        } catch (Exception e) {
            log.error("Error processing file for job {}: {}", jobId, e.getMessage(), e);
            return new UploadFileResponse(jobId, "FAILED", "Error processing file: " + e.getMessage());
        }
    }
    
    /**
     * Builds a detailed response message based on validation results.
     */
    private String buildResponseMessage(ValidationResult result) {
        StringBuilder message = new StringBuilder();
        
        if (result.isValid()) {
            message.append(String.format("File validated successfully. Processed %d valid transactions.", 
                result.getValidTransactions().size()));
        } else if (result.hasValidTransactions()) {
            message.append(String.format("File processed with warnings. %d valid transactions, %d invalid rows. ", 
                result.getValidTransactions().size(), result.getErrors().size()));
            
            // Include first few errors as examples
            message.append("Errors: ");
            int errorLimit = Math.min(3, result.getErrors().size());
            for (int i = 0; i < errorLimit; i++) {
                message.append(result.getErrors().get(i));
                if (i < errorLimit - 1) {
                    message.append("; ");
                }
            }
            
            if (result.getErrors().size() > 3) {
                message.append(String.format("; ... and %d more errors.", result.getErrors().size() - 3));
            }
        } else {
            message.append("File validation failed. No valid transactions found. Errors: ");
            message.append(String.join("; ", result.getErrors()));
        }
        
        return message.toString();
    }
    
    /**
     * Determines the processing status based on validation results.
     */
    private String determineStatus(ValidationResult result) {
        if (result.isValid()) {
            return "COMPLETED";
        } else if (result.hasValidTransactions()) {
            return "COMPLETED_WITH_WARNINGS";
        } else {
            return "FAILED";
        }
    }
}

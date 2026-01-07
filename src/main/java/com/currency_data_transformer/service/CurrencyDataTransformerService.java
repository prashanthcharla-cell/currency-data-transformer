package com.currency_data_transformer.service;

import org.springframework.stereotype.Service;

import com.currency_data_transformer.model.request.UploadFileRequest;
import com.currency_data_transformer.model.response.UploadFileResponse;
import com.currency_data_transformer.validation.CsvTransactionValidator;
import com.currency_data_transformer.validation.CsvTransactionValidator.ValidationResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyDataTransformerService {
    
    private final CsvTransactionValidator csvTransactionValidator;
    
    public UploadFileResponse transformCurrencyData(UploadFileRequest request) {
        log.info("Starting CSV file validation for file: {}", request.file().getOriginalFilename());
        
        // Validate the CSV file
        ValidationResult validationResult = csvTransactionValidator.validateCsvFile(request.file());
        
        String jobId = UUID.randomUUID().toString();
        
        log.info("Validation completed. Valid rows: {}, Invalid rows: {}", 
            validationResult.getValidRows(), validationResult.getInvalidRows());
        
        if (validationResult.hasErrors()) {
            log.warn("Validation errors found: {}", validationResult.getErrorMessages());
        }
        
        // Build response message
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(String.format("File validation completed. Total rows: %d, Valid: %d, Invalid: %d", 
            validationResult.getTotalRows(), 
            validationResult.getValidRows(), 
            validationResult.getInvalidRows()));
        
        if (validationResult.hasErrors()) {
            messageBuilder.append("\nValidation Errors:\n");
            validationResult.getErrorMessages().forEach(error -> 
                messageBuilder.append("- ").append(error).append("\n"));
        }
        
        String status = validationResult.isValid() ? "COMPLETED" : "FAILED";
        
        return new UploadFileResponse(jobId, status, messageBuilder.toString().trim());
    }
}

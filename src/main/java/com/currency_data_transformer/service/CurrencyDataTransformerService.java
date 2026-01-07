package com.currency_data_transformer.service;

import org.springframework.stereotype.Service;

import com.currency_data_transformer.model.request.UploadFileRequest;
import com.currency_data_transformer.model.response.UploadFileResponse;
import com.currency_data_transformer.validation.CsvTransactionValidator;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrencyDataTransformerService {
    
    private final CsvTransactionValidator validator;
    
    public UploadFileResponse transformCurrencyData(UploadFileRequest request) {
        // Validate the uploaded file
        CsvTransactionValidator.ValidationResult validationResult = validator.validate(request.file());
        
        String jobId = UUID.randomUUID().toString();
        
        if (validationResult.hasErrors()) {
            // File has validation errors
            StringBuilder errorMessage = new StringBuilder("Validation failed with errors:\n");
            validationResult.getErrors().forEach(error -> 
                errorMessage.append(String.format("Row %d: %s\n", error.getRowNumber(), error.getMessage()))
            );
            
            return new UploadFileResponse(
                jobId, 
                "FAILED", 
                errorMessage.toString().trim()
            );
        }
        
        // All rows are valid, proceed with transformation
        int validCount = validationResult.getValidTransactions().size();
        return new UploadFileResponse(
            jobId, 
            "COMPLETED", 
            String.format("File uploaded successfully. %d transaction(s) validated and processing started.", validCount)
        );
    }
}

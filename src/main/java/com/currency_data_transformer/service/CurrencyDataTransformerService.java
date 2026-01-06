package com.currency_data_transformer.service;

import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.currency_data_transformer.exception.InvalidFileException;
import com.currency_data_transformer.model.request.UploadFileRequest;
import com.currency_data_transformer.model.response.UploadFileResponse;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CurrencyDataTransformerService {
    
    public UploadFileResponse transformCurrencyData(UploadFileRequest request) {
        MultipartFile file = request.file();
        
        // Additional business logic validation
        validateFileContent(file);
        
        log.info("Processing file: {} (size: {} bytes, type: {})", 
            file.getOriginalFilename(), file.getSize(), file.getContentType());
        
        // Process the file here
        String jobId = generateJobId();
        
        return new UploadFileResponse(jobId, "COMPLETED", 
            String.format("File '%s' uploaded successfully. Processing started.", file.getOriginalFilename()));
    }
    
    private void validateFileContent(MultipartFile file) {
        try {
            // Check if file content is readable
            if (file.getBytes().length == 0) {
                throw new InvalidFileException("File content is empty");
            }
            
            // Add additional content validation here
            // For example, validate CSV structure, JSON format, etc.
            
        } catch (IOException e) {
            log.error("Error reading file content: {}", e.getMessage());
            throw new InvalidFileException("Unable to read file content", e);
        }
    }
    
    private String generateJobId() {
        return String.format("JOB-%d", System.currentTimeMillis());
    }
}

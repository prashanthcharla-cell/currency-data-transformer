package com.currency_data_transformer.validation;

import java.util.Arrays;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FileValidator implements ConstraintValidator<ValidFile, MultipartFile> {
    
    private long maxSize;
    private String[] allowedExtensions;
    private boolean allowEmpty;
    
    @Override
    public void initialize(ValidFile constraintAnnotation) {
        this.maxSize = constraintAnnotation.maxSize();
        this.allowedExtensions = constraintAnnotation.allowedExtensions();
        this.allowEmpty = constraintAnnotation.allowEmpty();
    }
    
    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null) {
            return true; // @NotNull handles null check
        }
        
        // Disable default constraint violation
        context.disableDefaultConstraintViolation();
        
        // Check if file is empty
        if (file.isEmpty() && !allowEmpty) {
            context.buildConstraintViolationWithTemplate("File cannot be empty")
                   .addConstraintViolation();
            return false;
        }
        
        // Check file size
        if (file.getSize() > maxSize) {
            context.buildConstraintViolationWithTemplate(
                String.format("File size must not exceed %d bytes (%.2f MB)", 
                    maxSize, maxSize / (1024.0 * 1024.0)))
                   .addConstraintViolation();
            return false;
        }
        
        // Check filename
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            context.buildConstraintViolationWithTemplate("Filename cannot be empty")
                   .addConstraintViolation();
            return false;
        }
        
        // Check file extension
        String fileExtension = getFileExtension(filename);
        if (fileExtension == null || fileExtension.isEmpty()) {
            context.buildConstraintViolationWithTemplate("File must have a valid extension")
                   .addConstraintViolation();
            return false;
        }
        
        boolean isValidExtension = Arrays.stream(allowedExtensions)
            .anyMatch(ext -> ext.equalsIgnoreCase(fileExtension));
        
        if (!isValidExtension) {
            context.buildConstraintViolationWithTemplate(
                String.format("File extension must be one of: %s", 
                    String.join(", ", allowedExtensions)))
                   .addConstraintViolation();
            return false;
        }
        
        return true;
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return null;
        }
        
        return filename.substring(lastDotIndex + 1);
    }
}


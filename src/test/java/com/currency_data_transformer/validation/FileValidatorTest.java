package com.currency_data_transformer.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.ConstraintValidatorContext;

class FileValidatorTest {
    
    private FileValidator validator;
    private ConstraintValidatorContext context;
    private ValidFile validFile;
    
    @BeforeEach
    void setUp() {
        validator = new FileValidator();
        context = mock(ConstraintValidatorContext.class);
        validFile = mock(ValidFile.class);
        
        when(validFile.maxSize()).thenReturn(10 * 1024 * 1024L); // 10MB
        when(validFile.allowedExtensions()).thenReturn(new String[]{"csv", "json", "xml", "txt"});
        when(validFile.allowEmpty()).thenReturn(false);
        
        when(context.buildConstraintViolationWithTemplate(org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(mock(ConstraintValidatorContext.ConstraintViolationBuilder.class));
        
        validator.initialize(validFile);
    }
    
    @Test
    void testValidFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("data.csv");
        
        assertTrue(validator.isValid(file, context));
    }
    
    @Test
    void testNullFile() {
        assertTrue(validator.isValid(null, context));
    }
    
    @Test
    void testEmptyFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);
        when(file.getOriginalFilename()).thenReturn("data.csv");
        
        assertFalse(validator.isValid(file, context));
    }
    
    @Test
    void testFileTooLarge() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(11 * 1024 * 1024L); // 11MB
        when(file.getOriginalFilename()).thenReturn("data.csv");
        
        assertFalse(validator.isValid(file, context));
    }
    
    @Test
    void testInvalidExtension() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("data.pdf");
        
        assertFalse(validator.isValid(file, context));
    }
    
    @Test
    void testNoExtension() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("data");
        
        assertFalse(validator.isValid(file, context));
    }
    
    @Test
    void testEmptyFilename() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("");
        
        assertFalse(validator.isValid(file, context));
    }
    
    @Test
    void testNullFilename() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn(null);
        
        assertFalse(validator.isValid(file, context));
    }
    
    @Test
    void testValidJsonFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(2048L);
        when(file.getOriginalFilename()).thenReturn("currencies.json");
        
        assertTrue(validator.isValid(file, context));
    }
    
    @Test
    void testValidXmlFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(2048L);
        when(file.getOriginalFilename()).thenReturn("currencies.xml");
        
        assertTrue(validator.isValid(file, context));
    }
}


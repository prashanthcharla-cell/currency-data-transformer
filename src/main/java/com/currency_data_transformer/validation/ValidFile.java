package com.currency_data_transformer.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = FileValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFile {
    
    String message() default "Invalid file";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    // Maximum file size in bytes (default 10MB)
    long maxSize() default 10 * 1024 * 1024;
    
    // Allowed file extensions
    String[] allowedExtensions() default {"csv", "json", "xml", "txt"};
    
    // Whether empty files are allowed
    boolean allowEmpty() default false;
}


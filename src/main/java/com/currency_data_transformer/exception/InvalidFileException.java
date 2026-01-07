package com.currency_data_transformer.exception;

/**
 * Exception thrown when a file fails validation checks.
 * This can occur due to invalid format, missing headers, or invalid data.
 */
public class InvalidFileException extends RuntimeException {
    
    public InvalidFileException(String message) {
        super(message);
    }
    
    public InvalidFileException(String message, Throwable cause) {
        super(message, cause);
    }
}


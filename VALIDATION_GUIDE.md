# File Upload Validation Guide

## Overview

This document describes the comprehensive file upload validation implemented in the Currency Data Transformer application.

## Validation Features

### 1. Bean Validation
The application uses Jakarta Bean Validation to ensure uploaded files meet required criteria.

### 2. File Size Limits
- **Maximum file size**: 10MB (configurable)
- **Maximum request size**: 10MB (configurable)
- Configured in `application.yaml`

### 3. File Extension Validation
Allowed file extensions:
- `.csv` - Comma-separated values
- `.json` - JSON format
- `.xml` - XML format
- `.txt` - Plain text

### 4. File Content Validation
- Files cannot be empty
- File must have valid content
- Filename cannot be null or empty
- File extension is required

## Configuration

### application.yaml
```yaml
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB
      max-request-size: 10MB
      file-size-threshold: 2KB
```

### Custom Validation Annotation

The `@ValidFile` annotation can be customized:

```java
@ValidFile(
    maxSize = 10 * 1024 * 1024,  // 10MB in bytes
    allowedExtensions = {"csv", "json", "xml", "txt"},
    allowEmpty = false
)
```

## Validation Rules

### 1. Required Field Validation
- File field is required (`@NotNull`)
- Returns error if file is not provided

### 2. Empty File Check
- Files cannot be empty
- Error message: "File cannot be empty"

### 3. File Size Check
- Maximum size: 10MB (configurable)
- Error message: "File size must not exceed X bytes (Y MB)"

### 4. Filename Validation
- Filename cannot be null or empty
- Error message: "Filename cannot be empty"

### 5. Extension Validation
- File must have a valid extension
- Must match one of the allowed extensions
- Case-insensitive comparison
- Error message: "File extension must be one of: csv, json, xml, txt"

## Error Responses

### Validation Error Response Format
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "file": "File cannot be empty"
  },
  "timestamp": "2026-01-06T21:00:00"
}
```

### File Too Large Response (HTTP 413)
```json
{
  "status": 413,
  "message": "File too large",
  "errors": {
    "file": "File size exceeds the maximum allowed limit"
  },
  "timestamp": "2026-01-06T21:00:00"
}
```

### Invalid File Extension Response
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "file": "File extension must be one of: csv, json, xml, txt"
  },
  "timestamp": "2026-01-06T21:00:00"
}
```

## Exception Handling

The application includes a global exception handler that catches:

1. **MethodArgumentNotValidException** - Bean validation errors
2. **ConstraintViolationException** - Constraint validation errors
3. **MaxUploadSizeExceededException** - File size exceeds limit
4. **InvalidFileException** - Custom file validation errors
5. **Exception** - Generic server errors

## Testing

The validation logic is thoroughly tested in `FileValidatorTest.java`:

- ✅ Valid file upload
- ✅ Empty file rejection
- ✅ File too large rejection
- ✅ Invalid extension rejection
- ✅ Missing extension rejection
- ✅ Empty filename rejection
- ✅ Various valid file types (CSV, JSON, XML)

## API Usage Example

### Valid Request
```bash
curl -X POST http://localhost:8080/api/v1/upload \
  -H "Content-Type: application/json" \
  -d '{
    "file": {
      "originalFilename": "currency-data.csv",
      "size": 2048,
      "contentType": "text/csv"
    }
  }'
```

### Successful Response
```json
{
  "jobId": "JOB-1704562800000",
  "status": "COMPLETED",
  "message": "File 'currency-data.csv' uploaded successfully. Processing started."
}
```

## Implementation Details

### Key Components

1. **ValidFile.java** - Custom validation annotation
2. **FileValidator.java** - Validation logic implementation
3. **GlobalExceptionHandler.java** - Centralized error handling
4. **UploadFileRequest.java** - Request model with validation annotations
5. **CurrencyDataTransformerService.java** - Business logic with additional validation

### Validation Flow

1. Request received at controller
2. `@Valid` annotation triggers validation
3. Bean validation checks (`@NotNull`)
4. Custom `@ValidFile` validation runs
5. File validator checks all constraints
6. If validation fails, exception handler returns error response
7. If validation passes, service processes the file

## Customization

To modify validation rules, update:

1. **Maximum file size**: Edit `application.yaml` or `@ValidFile` annotation
2. **Allowed extensions**: Modify `allowedExtensions` in `@ValidFile`
3. **Empty file handling**: Change `allowEmpty` in `@ValidFile`
4. **Custom error messages**: Update messages in `FileValidator.java`

## Benefits

✅ **Security**: Prevents malicious file uploads
✅ **Reliability**: Ensures data quality
✅ **User Experience**: Clear error messages
✅ **Maintainability**: Centralized validation logic
✅ **Testability**: Comprehensive test coverage
✅ **Configurability**: Easy to adjust limits and rules


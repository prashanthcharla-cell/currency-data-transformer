# CSV Transaction Validation Rules

## Overview
This document describes the comprehensive validation rules implemented for CSV transaction file uploads in the Currency Data Transformer application.

## File-Level Validation

### 1. File Type Validation
- **Rule**: File must be a CSV file
- **Validation**: 
  - File extension must be `.csv`
  - Content type should be `text/csv`, `application/vnd.ms-excel`, or `application/octet-stream`
- **Error Message**: "File must be a CSV file with .csv extension" or "Invalid file type. Expected CSV file"

### 2. File Empty Check
- **Rule**: File must not be empty
- **Validation**: File size must be greater than 0 bytes
- **Error Message**: "File is empty or not provided"

### 3. Header Validation
- **Rule**: CSV must contain all required headers
- **Required Headers**: 
  - `Date`
  - `TransactionID`
  - `Amount`
  - `Currency`
- **Case Sensitivity**: Headers are case-sensitive
- **Duplicate Headers**: If duplicate header names exist, the system uses the **first occurrence only**
- **Error Message**: "Missing required headers: [list of missing headers]. Expected headers: Date, TransactionID, Amount, Currency"

### 4. Data Presence
- **Rule**: CSV must contain at least one data row after headers
- **Validation**: At least one non-empty row must exist after the header row
- **Error Message**: "CSV file contains no data rows"

## Row-Level Validation

### 1. Date Field Validation

#### Format
- **Rule**: Date must be in `YYYY-MM-DD` format
- **Examples**: 
  - ✅ Valid: `2024-01-15`, `2023-12-31`, `2024-02-29` (leap year)
  - ❌ Invalid: `01-15-2024`, `2024/01/15`, `15-01-2024`, `2024-1-5`
- **Error Message**: "Row X: Invalid date format 'value'. Expected format: YYYY-MM-DD"

#### Required
- **Rule**: Date field cannot be empty
- **Error Message**: "Row X: Date is required"

### 2. TransactionID Field Validation

#### Length
- **Rule**: TransactionID must be exactly 10 characters
- **Examples**:
  - ✅ Valid: `TXN1234567`, `ABC1234567`, `1234567890`
  - ❌ Invalid: `TXN123` (6 chars), `TXN12345678901` (13 chars)
- **Error Message**: "Row X: TransactionID must be exactly 10 characters. Found: 'value' (Y characters)"

#### Uniqueness
- **Rule**: TransactionID must be unique across all rows in the file
- **Validation**: No two rows can have the same TransactionID
- **Error Message**: "Row X: Duplicate TransactionID 'value'"

#### Required
- **Rule**: TransactionID field cannot be empty
- **Error Message**: "Row X: TransactionID is required"

### 3. Amount Field Validation

#### Format
- **Rule**: Amount must be a positive number with up to 2 decimal places
- **Pattern**: `^\d+(\.\d{1,2})?$`
- **Examples**:
  - ✅ Valid: `100`, `100.5`, `100.50`, `0.01`, `1000000`
  - ❌ Invalid: `100.555` (3 decimals), `-50.00` (negative), `abc`, `100.`, `.50`
- **Error Message**: "Row X: Invalid amount format 'value'. Amount must be a positive number with up to 2 decimal places"

#### Positive Value
- **Rule**: Amount must be greater than zero
- **Examples**:
  - ✅ Valid: `0.01`, `100`, `100.50`
  - ❌ Invalid: `0`, `-50.00`, `-100`
- **Error Message**: "Row X: Amount must be positive. Found: value"

#### Required
- **Rule**: Amount field cannot be empty
- **Error Message**: "Row X: Amount is required"

### 4. Currency Field Validation

#### Allowed Values
- **Rule**: Currency must be one of the allowed values
- **Allowed Values**: `USD`, `EUR`, `INR`
- **Case Handling**: Case-insensitive (e.g., `usd`, `USD`, `Usd` are all valid)
- **Examples**:
  - ✅ Valid: `USD`, `EUR`, `INR`, `usd`, `eur`, `inr`
  - ❌ Invalid: `GBP`, `JPY`, `CAD`, `AUD`
- **Error Message**: "Row X: Invalid currency 'value'. Allowed values: USD, EUR, INR"

#### Required
- **Rule**: Currency field cannot be empty
- **Error Message**: "Row X: Currency is required"

## Special Cases

### 1. Empty Lines
- **Handling**: Empty lines in the CSV are skipped and not counted as errors
- **Example**:
```csv
Date,TransactionID,Amount,Currency
2024-01-15,TXN1234567,100.50,USD

2024-01-16,TXN7654321,200.00,EUR
```
This is valid and will process 2 transactions.

### 2. Quoted Values
- **Handling**: CSV values enclosed in quotes are properly parsed
- **Example**:
```csv
Date,TransactionID,Amount,Currency
"2024-01-15","TXN1234567","100.50","USD"
```
This is valid and quotes are removed during parsing.

### 3. Insufficient Columns
- **Rule**: Each data row must have at least 4 columns
- **Error Message**: "Row X: Insufficient number of columns. Expected 4, found Y"

### 4. Extra Columns
- **Handling**: Extra columns beyond the required 4 are ignored
- **Example**: A row with 6 columns where only the first 4 match the headers will be processed using only those 4 columns.

## Validation Response

### Success Response
When validation is successful (all rows valid):
```json
{
  "jobId": "uuid",
  "status": "COMPLETED",
  "message": "File validation completed. Total rows: X, Valid: X, Invalid: 0"
}
```

### Partial Success Response
When some rows are valid and some are invalid:
```json
{
  "jobId": "uuid",
  "status": "COMPLETED",
  "message": "File validation completed. Total rows: X, Valid: Y, Invalid: Z\nValidation Errors:\n- Row 2: Invalid date format...\n- Row 3: TransactionID must be exactly 10 characters..."
}
```

### Failure Response
When all rows are invalid:
```json
{
  "jobId": "uuid",
  "status": "FAILED",
  "message": "File validation completed. Total rows: X, Valid: 0, Invalid: X\nValidation Errors:\n- Row 2: ..."
}
```

### Error Response
When file-level validation fails:
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Invalid File",
  "message": "File is empty or not provided",
  "details": []
}
```

## Implementation Details

### Technology Stack
- **Language**: Java 25
- **Framework**: Spring Boot 4.0.2
- **Build Tool**: Gradle
- **Testing**: JUnit 5, Mockito

### Key Classes

#### 1. CsvTransactionValidator
- **Location**: `com.currency_data_transformer.validation.CsvTransactionValidator`
- **Purpose**: Main validation logic for CSV files
- **Methods**:
  - `validateCsvFile(MultipartFile file)`: Main entry point for validation
  - `validateFileBasics(MultipartFile file)`: File-level validation
  - `parseAndValidateCsvContent(MultipartFile file)`: Row-level validation
  - `validateAndParseHeaders(String headerLine)`: Header validation
  - `validateRow(...)`: Individual row validation

#### 2. Transaction
- **Location**: `com.currency_data_transformer.model.Transaction`
- **Purpose**: Domain model representing a valid transaction
- **Fields**: date, transactionId, amount, currency

#### 3. ValidationResult
- **Location**: `com.currency_data_transformer.validation.CsvTransactionValidator.ValidationResult`
- **Purpose**: Encapsulates validation results
- **Fields**:
  - `validTransactions`: List of successfully validated transactions
  - `errorMessages`: List of validation error messages
  - `totalRows`: Total number of data rows processed
  - `validRows`: Number of valid rows
  - `invalidRows`: Number of invalid rows

#### 4. InvalidFileException
- **Location**: `com.currency_data_transformer.exception.InvalidFileException`
- **Purpose**: Custom exception for file validation errors
- **HTTP Status**: 400 Bad Request

### Testing

The implementation includes comprehensive unit tests covering:
- Valid CSV files
- Invalid file types and empty files
- Missing and duplicate headers
- Invalid date formats
- Invalid TransactionID lengths and duplicates
- Invalid amount formats and negative values
- Invalid currencies
- Mixed valid and invalid rows
- Empty fields
- Quoted values
- Empty lines
- Edge cases

**Test Location**: `src/test/java/com/currency_data_transformer/validation/CsvTransactionValidatorTest.java`

**Run Tests**: 
```bash
./gradlew test
```

## Sample Test Files

Sample CSV files are provided in the `test-files/` directory:

1. **valid_transactions.csv**: Contains only valid transactions
2. **invalid_transactions.csv**: Contains various validation errors
3. **duplicate_headers.csv**: Demonstrates duplicate header handling
4. **mixed_valid_invalid.csv**: Mix of valid and invalid rows

## API Usage

### Endpoint
```
POST /api/v1/upload
Content-Type: multipart/form-data
```

### Request
```bash
curl -X POST http://localhost:8080/api/v1/upload \
  -F "file=@transactions.csv"
```

### Response Examples

**Success (200 OK)**:
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "message": "File validation completed. Total rows: 5, Valid: 5, Invalid: 0"
}
```

**Partial Success (200 OK)**:
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "message": "File validation completed. Total rows: 5, Valid: 3, Invalid: 2\nValidation Errors:\n- Row 2: Invalid date format '01-15-2024'. Expected format: YYYY-MM-DD\n- Row 4: TransactionID must be exactly 10 characters. Found: 'TXN123' (6 characters)"
}
```

**File Error (400 Bad Request)**:
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Invalid File",
  "message": "File must be a CSV file with .csv extension",
  "details": []
}
```

## Performance Considerations

1. **Streaming**: The validator uses `BufferedReader` to read the file line by line, minimizing memory usage for large files
2. **Early Validation**: File-level checks are performed before row-level validation to fail fast
3. **Efficient Parsing**: Custom CSV parser handles quoted values without external dependencies
4. **Transaction ID Tracking**: Uses `HashSet` for O(1) duplicate detection

## Best Practices

1. **Clean Data**: Ensure your CSV files follow the specified format before upload
2. **Unique IDs**: Generate unique 10-character TransactionIDs
3. **Date Format**: Always use ISO 8601 date format (YYYY-MM-DD)
4. **Amount Precision**: Limit amounts to 2 decimal places
5. **Currency Codes**: Use only supported currency codes (USD, EUR, INR)
6. **File Size**: While there's no hard limit, keep files reasonably sized for optimal performance
7. **Error Handling**: Review validation error messages to correct invalid rows

## Future Enhancements

Potential improvements for future versions:
1. Support for additional currency codes
2. Configurable validation rules via properties
3. Async processing for large files
4. Detailed validation reports in multiple formats (JSON, PDF)
5. Support for additional date formats
6. Batch processing of multiple files
7. Database persistence of validation results

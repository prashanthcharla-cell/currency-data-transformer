This README is designed for your **Spring Boot 4.0.2** project using **Java 25**. It incorporates the business requirements, the INR conversion logic, and the REST API specifications we discussed.

---

# 💸 CSV to INR Currency Converter

A Spring Boot 4.0.2 application designed to process foreign currency transactions and convert them into Indian Rupees (INR) using a JSON format.

## 🚀 Tech Stack

* **Java:** 25
* **Framework:** Spring Boot 4.0.2 (SNAPSHOT)
* **Database:** H2 (In-Memory)
* **Dependencies:** Spring Web, Spring Data JPA, H2 Database

## 🛠 Features

* **Comprehensive CSV Validation:** Robust validation of uploaded CSV files with detailed error reporting
* **Asynchronous Processing:** Upload large CSV files and track conversion status via a Job ID
* **Currency Conversion:** Automated math to convert USD, EUR, and INR currencies
* **Data Persistence:** Stores job metadata and processing status in H2
* **JSON Transformation:** Generates a structured financial report from raw CSV data
* **Production-Ready Code:** Clean, well-tested implementation with 30+ unit tests

---

## 📈 Exchange Rates (Base: INR)

The application currently uses the following conversion rates (Fixed for Jan 2026):

| Source | Target | Rate |
| --- | --- | --- |
| **USD** | INR | 90.25 |
| **EUR** | INR | 98.10 |
| **GBP** | INR | 114.40 |

---

## 🚦 API Endpoints

### 1. Upload Transactions

`POST /api/v1/upload`

Uploads a `.csv` file for processing.

* **Form Data:** `file` (multipart/form-data)
* **Success Response:** `202 Accepted`

```json
{
  "jobId": "7bca-4921-9d12",
  "status": "PENDING",
  "message": "File uploaded successfully. Processing started."
}

```

### 2. Check Job Status

`GET /api/v1/status/{jobId}`

Retrieves the current status and the converted data if finished.

* **Success Response:** `200 OK`

```json
{
  "jobId": "7bca-4921-9d12",
  "status": "COMPLETED",
  "targetCurrency": "INR",
  "data": [
    {
      "transactionId": "TXN001",
      "sourceAmount": 100.00,
      "sourceCurrency": "USD",
      "convertedAmount": 9025.00,
      "currencySymbol": "₹"
    }
  ],
  "summary": {
    "totalCount": 1,
    "totalInrValue": 9025.00
  }
}

```

---

## 📂 Sample Input Format (`transactions.csv`)

The input file must contain the following headers:

```csv
Date,TransactionID,Amount,Currency
2026-01-01,TXN001,100.00,USD
2026-01-02,TXN002,50.50,EUR

```

### Validation Rules

The CSV file must follow these business rules:

1. **File Type**: Must be a `.csv` file
2. **Headers**: Must contain `Date`, `TransactionID`, `Amount`, and `Currency` (case-sensitive)
3. **Date**: Must be in `YYYY-MM-DD` format (e.g., `2024-01-15`)
4. **TransactionID**: Must be exactly 10 characters and unique across all rows
5. **Amount**: Must be a positive number with up to 2 decimal places
6. **Currency**: Must be one of `USD`, `EUR`, or `INR` (case-insensitive)

**Note**: If duplicate header names exist in the CSV, the system uses the first occurrence only.

For detailed validation rules and error messages, see [VALIDATION_RULES.md](VALIDATION_RULES.md).

### Sample Test Files

Sample CSV files are available in the `test-files/` directory:
- `valid_transactions.csv` - Valid transactions
- `invalid_transactions.csv` - Various validation errors
- `duplicate_headers.csv` - Duplicate header handling
- `mixed_valid_invalid.csv` - Mix of valid and invalid rows

---

## ⚙️ Development Setup

1. **Clone the repository:**
```bash
git clone https://github.com/your-username/currency-converter.git

```


2. **Build the project:**
```bash
./gradlew clean build

```


3. **Run the application:**
```bash
./gradlew bootRun

```


4. **H2 Console:**
Access the database at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:testdb`).

5. **Run Tests:**
```bash
./gradlew test

```

---

## 🧪 Testing

The project includes comprehensive unit tests covering:
- CSV file validation (30+ test cases)
- Service layer integration
- Exception handling
- Edge cases and error scenarios

**Test Coverage:**
- `CsvTransactionValidatorTest`: 25+ validation test cases
- `CurrencyDataTransformerServiceTest`: Service integration tests

**Run Tests:**
```bash
./gradlew test

```

**View Test Report:**
After running tests, open `build/reports/tests/test/index.html` in your browser.

---

## 📋 Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/currency_data_transformer/
│   │       ├── controller/          # REST endpoints
│   │       ├── service/              # Business logic
│   │       ├── validation/           # CSV validation
│   │       ├── model/                # Domain models
│   │       └── exception/            # Custom exceptions
│   └── resources/
│       └── application.yaml          # Configuration
└── test/
    └── java/
        └── com/currency_data_transformer/
            ├── validation/           # Validator tests
            └── service/              # Service tests

test-files/                           # Sample CSV files
VALIDATION_RULES.md                   # Detailed validation documentation
```

---

## 🔍 Validation Response Examples

### Success Response
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "message": "File validation completed. Total rows: 5, Valid: 5, Invalid: 0"
}
```

### Partial Success (with errors)
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "message": "File validation completed. Total rows: 5, Valid: 3, Invalid: 2\nValidation Errors:\n- Row 2: Invalid date format '01-15-2024'. Expected format: YYYY-MM-DD\n- Row 4: TransactionID must be exactly 10 characters. Found: 'TXN123' (6 characters)"
}
```

### File-Level Error (400 Bad Request)
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Invalid File",
  "message": "File must be a CSV file with .csv extension",
  "details": []
}
```

---

## 🎯 Example Usage

### Using cURL

**Upload a valid CSV file:**
```bash
curl -X POST http://localhost:8080/api/v1/upload \
  -F "file=@test-files/valid_transactions.csv"
```

**Upload an invalid CSV file:**
```bash
curl -X POST http://localhost:8080/api/v1/upload \
  -F "file=@test-files/invalid_transactions.csv"
```

### Using Postman

1. Set method to `POST`
2. URL: `http://localhost:8080/api/v1/upload`
3. Go to Body → form-data
4. Add key `file` (type: File)
5. Select your CSV file
6. Click Send

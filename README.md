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

* **Asynchronous Processing:** Upload large CSV files and track conversion status via a Job ID.
* **CSV Validation:** Comprehensive validation of uploaded CSV files with detailed error reporting.
* **Currency Conversion:** Automated math to convert USD, EUR, and GBP to INR.
* **Data Persistence:** Stores job metadata and processing status in H2.
* **JSON Transformation:** Generates a structured financial report from raw CSV data.

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

### CSV Validation Rules

The application performs comprehensive validation on uploaded CSV files:

#### File Validation
- File must be a CSV with `.csv` extension
- File must not be empty
- File must contain valid CSV headers: `Date`, `TransactionID`, `Amount`, `Currency`

#### Data Validation

**Date Field:**
- Must not be empty
- Supports multiple formats: `yyyy-MM-dd`, `dd/MM/yyyy`, `MM/dd/yyyy`
- Example: `2024-01-15`, `15/01/2024`, or `01/15/2024`

**TransactionID Field:**
- Must not be empty
- Maximum 100 characters
- Only alphanumeric characters, underscores, and hyphens allowed
- Example: `TXN-001`, `TXN_123`, `ABC123`

**Amount Field:**
- Must not be empty
- Must be a valid positive number
- Maximum 2 decimal places
- Example: `100`, `100.5`, `100.50`

**Currency Field:**
- Must not be empty
- Must be a valid 3-letter ISO 4217 currency code (uppercase)
- Example: `USD`, `EUR`, `GBP`, `JPY`, `CAD`

#### Validation Response

**Successful Validation:**
```json
{
  "jobId": "uuid-here",
  "status": "COMPLETED",
  "message": "File validated successfully. Processed 10 valid transactions."
}
```

**Partial Validation (Some Invalid Rows):**
```json
{
  "jobId": "uuid-here",
  "status": "COMPLETED_WITH_WARNINGS",
  "message": "File processed with warnings. 8 valid transactions, 2 invalid rows. Errors: Line 3: Amount must be positive; Line 5: Invalid date format..."
}
```

**Failed Validation:**
```json
{
  "jobId": "uuid-here",
  "status": "FAILED",
  "message": "File validation failed. No valid transactions found. Errors: ..."
}
```

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

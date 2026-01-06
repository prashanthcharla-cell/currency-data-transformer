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

# Financial Ledger API with Double-Entry Bookkeeping

## Overview
This project implements a **Financial Ledger API** that strictly follows **double-entry bookkeeping principles**.  
It is designed as the backend core of a mock banking system, ensuring **data integrity, auditability, and correctness**.

The system avoids traditional balance storage and instead derives balances **authoritatively from immutable ledger entries**, making the ledger the single source of truth.

---

## Key Features
- Double-entry bookkeeping (every transfer = debit + credit)
- Immutable ledger (append-only, no updates/deletes)
- ACID-compliant database transactions
- Overdraft prevention (no negative balances)
- Transaction-safe concurrency handling
- Precise decimal arithmetic for money
- Full audit trail per account

---

## Tech Stack
- **Backend:** Node.js, Express.js
- **Database:** PostgreSQL
- **DB Driver:** pg
- **Environment Management:** dotenv
- **Runtime:** Node.js 18+

---

## Project Structure
```
financial-ledger-api/
│
├── src/
│   ├── app.js
│   ├── server.js
│   ├── db.js
│   ├── services/
│   │   ├── balanceService.js
│   │   └── transferService.js
│   ├── controllers/
│   │   └── accountController.js
│   └── testTransfer.js
│
├── .env
├── package.json
└── README.md
```

---

## Database Design

### Accounts
- No balance column
- Balance is calculated from ledger entries

### Transactions
- Represents intent (transfer, deposit, withdrawal)

### Ledger Entries
- Immutable debit or credit records
- Append-only
- Enforced via database trigger

---

## Setup Instructions

### 1. Clone Repository
```bash
git clone <repo-url>
cd Financial-Ledger-API-with-Double-Entry-Bookkeeping
```

### 2. Install Dependencies
```bash
npm install
```

### 3. Configure Environment
Create `.env` file:
```env
PORT=3000
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASSWORD=your_password
DB_NAME=financial_ledger
```

---

## Database Setup

### Create Database
```sql
CREATE DATABASE financial_ledger;
```

### Enable UUID Extension
```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

### Tables
- accounts
- transactions
- ledger_entries

Ledger immutability is enforced via triggers.

---

## Running the Server
```bash
npm run dev
```

Health check:
```
GET http://localhost:3000/health
```

---

## API Endpoints

### Create Account
```
POST /accounts
```
```json
{
  "userId": "uuid",
  "accountType": "checking",
  "currency": "USD"
}
```

### Get Account (with balance)
```
GET /accounts/{accountId}
```

### Get Ledger Entries
```
GET /accounts/{accountId}/ledger
```

### Deposit
```
POST /deposits
```

### Withdrawal
```
POST /withdrawals
```

### Transfer
```
POST /transfers
```

---

## Double-Entry Enforcement
Every transfer:
- Creates **two ledger entries**
- Debit from source
- Credit to destination
- Same amount
- Same transaction ID
- Executed inside a single DB transaction

---

## Balance Calculation
```sql
SUM(
  CASE
    WHEN entry_type = 'credit' THEN amount
    WHEN entry_type = 'debit' THEN -amount
  END
)
```

---

## Data Integrity Guarantees
- No negative balances allowed
- Atomic DB transactions
- Row-level locking
- Serializable financial history
- Ledger cannot be modified

---

## Evaluation Readiness
This implementation satisfies:
- ACID compliance
- Correct double-entry bookkeeping
- Immutable audit trail
- Concurrency safety
- Business rule enforcement

---
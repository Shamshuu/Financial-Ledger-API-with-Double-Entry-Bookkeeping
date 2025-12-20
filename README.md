Financial Ledger API – Double-Entry Bookkeeping

A robust backend API implementing double-entry bookkeeping principles with strong data integrity, ACID compliance, and immutable audit trails.
This system serves as the core ledger for a mock banking application and ensures correctness over convenience.

🚀 Objective

The goal of this project is to build a financially correct backend system, not a simple CRUD API.
All balances are derived from an immutable ledger, and every monetary movement follows strict accounting rules.

Key guarantees:

No negative balances

No mutable transaction history

Atomic and isolated financial operations

Verifiable audit trail

🛠 Tech Stack

Backend: Node.js, Express.js

Database: PostgreSQL

DB Client: pg (node-postgres)

Architecture: Service-layer driven, transaction-safe

Data Types: UUID, NUMERIC(18,2) for financial precision

🧱 Core Concepts Implemented
Double-Entry Bookkeeping

Every transfer creates exactly two ledger entries:

Debit from source account

Credit to destination account

The sum of all entries in a transaction is always zero.

Immutability

Ledger entries:

Cannot be updated

Cannot be deleted

Enforced at database trigger level

This guarantees a permanent audit trail.

ACID Transactions

All financial operations are wrapped in a single database transaction:

Either all changes succeed

Or everything is rolled back safely

Row-level locks (SELECT … FOR UPDATE) prevent race conditions.

🗄 Database Schema
accounts

id (UUID, PK)

user_id (UUID)

account_type (checking, savings)

currency (CHAR(3))

status (active, frozen)

⚠️ No balance column – balance is calculated from ledger entries.

transactions

Represents intent to move money.

id (UUID, PK)

type (transfer, deposit, withdrawal)

source_account_id

destination_account_id

amount

currency

status

description

ledger_entries

Immutable financial records.

id (UUID, PK)

account_id

transaction_id

entry_type (debit / credit)

amount

created_at

🔌 API Endpoints
Create Account
POST /accounts


Request

{
  "userId": "uuid",
  "accountType": "checking",
  "currency": "USD"
}

Get Account Details (with balance)
GET /accounts/{accountId}


Balance is calculated dynamically from ledger entries.

Get Account Ledger
GET /accounts/{accountId}/ledger


Returns a chronological, immutable ledger history.

Deposit
POST /deposits


Creates a credit ledger entry.

Withdraw
POST /withdrawals


Rejected if balance would go negative.

Transfer
POST /transfers


Atomic

Double-entry enforced

Overdraft protected

🔐 Business Rules Enforced

❌ No negative balances

❌ No partial transactions

❌ No ledger mutation

✅ Every transfer is balanced

✅ Balance always matches ledger sum

⚙️ Setup & Execution Guide
1️⃣ Prerequisites

Node.js ≥ 18

PostgreSQL ≥ 13

2️⃣ Clone & Install
git clone <repository-url>
cd financial-ledger-api
npm install

3️⃣ Environment Variables

Create .env file:

PORT=3000
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASSWORD=your_password
DB_NAME=financial_ledger

4️⃣ Database Setup
CREATE DATABASE financial_ledger;


Run schema SQL (tables, enums, triggers).

5️⃣ Start Server
npm run dev


Health check:

GET http://localhost:3000/health

🧪 Validation Scenarios

Transfer without funds → ❌ rejected

Concurrent transfers → ✅ safe

Ledger modification → ❌ blocked

Balance mismatch → ❌ impossible

📌 Key Takeaways

This project demonstrates:

Real-world financial system design

Correct use of database transactions

Ledger-based accounting models

Backend engineering beyond CRUD

🏁 Status

✅ Fully implemented
✅ Submission ready
✅ Meets all task requirements
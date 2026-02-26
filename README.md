# Financial Ledger API with Double-Entry Bookkeeping

A robust financial ledger REST API built with **Java 21** and **Spring Boot 3.2**, implementing the principles of **double-entry bookkeeping**. The system serves as the backbone for a mock banking application with absolute data integrity, auditability, and ACID-compliant transaction processing.

---

## Table of Contents

1. [Features](#features)
2. [Tech Stack](#tech-stack)
3. [Architecture](#architecture)
4. [Database Schema (ERD)](#database-schema-erd)
5. [Setup & Run](#setup--run)
6. [API Endpoints](#api-endpoints)
7. [Design Decisions](#design-decisions)
8. [Transfer Transaction Flow](#transfer-transaction-flow)
9. [Testing with Postman](#testing-with-postman)

---

## Features

- **Double-Entry Bookkeeping** — Every financial movement creates exactly two balanced ledger entries (debit + credit) that sum to zero.
- **ACID Transactions** — All database operations for a single financial transaction are atomic; they either all succeed or all fail.
- **Immutable Ledger** — Ledger entries are append-only. A PostgreSQL trigger prevents UPDATE/DELETE at the database level.
- **Overdraft Prevention** — The system strictly prevents negative account balances with pre- and post-entry verification.
- **Concurrency Safety** — Pessimistic locking (`SELECT ... FOR UPDATE`) with ordered lock acquisition prevents race conditions and deadlocks.
- **Calculated Balances** — Account balances are never stored; they are always computed on-demand from ledger entries.
- **High-Precision Arithmetic** — All monetary amounts use `DECIMAL(19,4)` to eliminate floating-point errors.

---

## Tech Stack

| Component      | Technology              |
|----------------|-------------------------|
| Language       | Java 21                 |
| Framework      | Spring Boot 3.2.5       |
| ORM            | Spring Data JPA / Hibernate |
| Database       | PostgreSQL 16           |
| Build Tool     | Maven                   |
| Containerization | Docker & Docker Compose |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         REST Clients                            │
│                    (Postman / Frontend / cURL)                  │
└──────────────────────────┬──────────────────────────────────────┘
                           │  HTTP (JSON)
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Controller Layer                           │
│  ┌──────────────────┐  ┌────────────────────────────────────┐   │
│  │ AccountController│  │      TransactionController         │   │
│  │  POST /accounts  │  │  POST /transfers                   │   │
│  │  GET  /accounts/*│  │  POST /deposits                    │   │
│  │                  │  │  POST /withdrawals                 │   │
│  └────────┬─────────┘  └──────────────┬─────────────────────┘   │
└───────────┼───────────────────────────┼─────────────────────────┘
            │                           │
            ▼                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Service Layer                             │
│  ┌────────────────┐ ┌────────────────┐ ┌─────────────────────┐  │
│  │ AccountService │ │ LedgerService  │ │ TransactionService  │  │
│  │                │ │                │ │                     │  │
│  │ • createAccount│ │ • calcBalance  │ │ • executeTransfer   │  │
│  │ • getAccount   │ │ • getEntries   │ │ • executeDeposit    │  │
│  │                │ │                │ │ • executeWithdrawal │  │
│  └───────┬────────┘ └───────┬────────┘ └──────────┬──────────┘  │
└──────────┼──────────────────┼─────────────────────┼─────────────┘
           │                  │                     │
           ▼                  ▼                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Repository Layer (JPA)                      │
│  ┌──────────────────┐ ┌───────────────────┐ ┌───────────────┐   │
│  │AccountRepository │ │LedgerEntryRepo    │ │TransactionRepo│   │
│  │                  │ │                   │ │               │   │
│  │• findByIdWithLock│ │• calculateBalance │ │• save / find  │   │
│  │  (FOR UPDATE)    │ │• findByAccountId  │ │               │   │
│  └──────────────────┘ └───────────────────┘ └───────────────┘   │
└──────────────────────────────┬──────────────────────────────────┘
                               │  JDBC / Hibernate
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                      PostgreSQL 16                              │
│  ┌──────────┐  ┌──────────────┐  ┌───────────────────────────┐  │
│  │ accounts │  │ transactions │  │ledger_entries             │  │
│  │          │  │              │  │(immutable — trigger guard)│  │
│  └──────────┘  └──────────────┘  └───────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Database Schema (ERD)

```
┌──────────────────────────┐       ┌─────────────────────────────┐
│        accounts          │       │        transactions         │
├──────────────────────────┤       ├─────────────────────────────┤
│ PK  id          UUID     │       │ PK  id            UUID      │
│     user_id     VARCHAR  │       │     type          VARCHAR   │
│     account_type VARCHAR │       │ FK  source_account_id UUID  │──┐
│     currency    CHAR(3)  │       │ FK  dest_account_id   UUID  │──┤
│     status      VARCHAR  │       │     amount        DECIMAL   │  │
│     created_at  TIMESTAMP│       │     currency      CHAR(3)   │  │
│     updated_at  TIMESTAMP│       │     status        VARCHAR   │  │
└─────────┬────────────────┘       │     description   VARCHAR   │  │
          │                        │     created_at    TIMESTAMP │  │
          │  1                     │     updated_at    TIMESTAMP │  │
          │  ┆                     └──────────┬──────────────────┘  │
          │  ┆                                │                     │
          │  ┆ N                              │ 1                   │
          │  ┆                                │ ┆                   │
┌─────────▼────────────────┐                  │ ┆ N                 │
│     ledger_entries       │                  │ ┆                   │
│     (IMMUTABLE)          │◄─────────────────┘ │                   │
├──────────────────────────┤                    │                   │
│ PK  id            UUID   │                    │                   │
│ FK  account_id    UUID   │────────────────────┘                   │
│ FK  transaction_id UUID  │────────────────────────────────────────┘
│     entry_type    VARCHAR│   (DEBIT or CREDIT)
│     amount        DECIMAL│   (always positive, precision 19,4)
│     created_at  TIMESTAMP│
└──────────────────────────┘
   ⚠ UPDATE / DELETE blocked
     by PostgreSQL trigger
```

### Key Constraints

| Constraint | Implementation |
|---|---|
| Balance not stored | `accounts` has no balance column; computed via `SUM()` over `ledger_entries` |
| Immutability | Hibernate `@Immutable` + PostgreSQL `BEFORE UPDATE OR DELETE` trigger |
| Referential integrity | Foreign keys from `ledger_entries` → `accounts` and `transactions` |
| Precision | `DECIMAL(19,4)` for all monetary amounts |

---

## Setup & Run

### Prerequisites

- **Docker** and **Docker Compose** installed

### Quick Start (Docker — Recommended)

```bash
# Clone the repository
git clone https://github.com/Shamshuu/Financial-Ledger-API-with-Double-Entry-Bookkeeping
cd Financial-Ledger-API-with-Double-Entry-Bookkeeping

# Build and start all services
docker-compose up --build

# The API will be available at http://localhost:8080
```

### Local Development (Without Docker)

1. **Install prerequisites:** Java 21, Maven 3.9+, PostgreSQL 16

2. **Create the database:**
   ```sql
   CREATE DATABASE ledger_db;
   CREATE USER ledger_user WITH PASSWORD 'ledger_password';
   GRANT ALL PRIVILEGES ON DATABASE ledger_db TO ledger_user;
   ```

3. **Build and run:**
   ```bash
   mvn clean package -DskipTests
   java -jar target/financial-ledger-api-1.0.0.jar
   ```

### Stopping

```bash
docker-compose down          # stop containers
docker-compose down -v       # stop and remove database volume
```

---

## API Endpoints

### Accounts

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/accounts` | Create a new account |
| `GET`  | `/accounts/{accountId}` | Get account details with calculated balance |
| `GET`  | `/accounts/{accountId}/ledger` | Get chronological ledger entries |

### Financial Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/transfers` | Transfer between two internal accounts |
| `POST` | `/deposits` | Deposit into an account |
| `POST` | `/withdrawals` | Withdraw from an account |

### Request / Response Examples

<details>
<summary><b>POST /accounts</b></summary>

**Request:**
```json
{
  "userId": "user-alice",
  "accountType": "CHECKING",
  "currency": "USD"
}
```

**Response (201 Created):**
```json
{
  "id": "a1b2c3d4-...",
  "userId": "user-alice",
  "accountType": "CHECKING",
  "currency": "USD",
  "status": "ACTIVE",
  "balance": 0,
  "createdAt": "2026-02-25T10:00:00",
  "updatedAt": "2026-02-25T10:00:00"
}
```
</details>

<details>
<summary><b>POST /deposits</b></summary>

**Request:**
```json
{
  "accountId": "a1b2c3d4-...",
  "amount": 1000.00,
  "description": "Initial deposit"
}
```

**Response (201 Created):**
```json
{
  "id": "tx-uuid-...",
  "type": "DEPOSIT",
  "sourceAccountId": "00000000-0000-0000-0000-000000000000",
  "destinationAccountId": "a1b2c3d4-...",
  "amount": 1000.00,
  "currency": "USD",
  "status": "COMPLETED",
  "description": "Initial deposit",
  "createdAt": "2026-02-25T10:01:00",
  "updatedAt": "2026-02-25T10:01:00"
}
```
</details>

<details>
<summary><b>POST /transfers</b></summary>

**Request:**
```json
{
  "sourceAccountId": "a1b2c3d4-...",
  "destinationAccountId": "e5f6g7h8-...",
  "amount": 250.00,
  "description": "Payment for services"
}
```

**Response (201 Created):**
```json
{
  "id": "tx-uuid-...",
  "type": "TRANSFER",
  "sourceAccountId": "a1b2c3d4-...",
  "destinationAccountId": "e5f6g7h8-...",
  "amount": 250.00,
  "currency": "USD",
  "status": "COMPLETED",
  "description": "Payment for services",
  "createdAt": "2026-02-25T10:02:00",
  "updatedAt": "2026-02-25T10:02:00"
}
```
</details>

<details>
<summary><b>POST /withdrawals</b></summary>

**Request:**
```json
{
  "accountId": "a1b2c3d4-...",
  "amount": 100.00,
  "description": "ATM withdrawal"
}
```
</details>

### Error Responses

| Status | Meaning | Example Trigger |
|--------|---------|-----------------|
| `400` | Bad Request | Missing required field, negative amount, self-transfer |
| `404` | Not Found | Account ID does not exist |
| `422` | Unprocessable Entity | Insufficient funds, frozen account |
| `500` | Internal Server Error | Unexpected system failure |

---

## Design Decisions

### 1. Double-Entry Bookkeeping Model

Every financial operation creates **exactly two ledger entries** that are mirror images:

| Operation   | Entry 1 (Source)  | Entry 2 (Destination) |
|-------------|-------------------|-----------------------|
| Transfer    | DEBIT (source)    | CREDIT (destination)  |
| Deposit     | DEBIT (system)    | CREDIT (target)       |
| Withdrawal  | DEBIT (source)    | CREDIT (system)       |

A **system account** (`00000000-0000-0000-0000-000000000000`) acts as the external counterparty for deposits and withdrawals, ensuring every operation has both a debit and credit side. The sum of all debit and credit amounts across the entire system always equals zero.

### 2. ACID Transaction Strategy

All financial operations are wrapped in a Spring `@Transactional` boundary:

- **Atomicity:** If any step fails (balance check, entry creation, status update), the entire database transaction is rolled back. No partial state is ever committed.
- **Consistency:** Pre- and post-entry balance checks ensure the account never goes negative. Foreign key constraints maintain referential integrity.
- **Isolation:** `READ_COMMITTED` isolation level (see below).
- **Durability:** PostgreSQL's WAL (Write-Ahead Logging) guarantees committed transactions survive crashes.

The transaction boundary covers:
1. Acquiring pessimistic locks on involved accounts
2. Checking the source balance
3. Creating the DEBIT ledger entry
4. Creating the CREDIT ledger entry
5. Verifying the post-entry balance ≥ 0
6. Updating the transaction status to COMPLETED

### 3. Transaction Isolation Level: READ_COMMITTED

**Choice:** `READ_COMMITTED` combined with pessimistic locking (`SELECT ... FOR UPDATE`).

**Rationale:**

| Option | Pros | Cons |
|--------|------|------|
| `READ_COMMITTED` + pessimistic lock | Sees latest committed data after acquiring lock; no serialization failures | Requires explicit lock management |
| `REPEATABLE_READ` | Snapshot consistency | Stale reads after lock acquisition (PostgreSQL snapshots are taken at first query, not after lock wait); causes unnecessary serialization failures |
| `SERIALIZABLE` | Highest safety | Significant performance overhead; frequent aborts under contention |

With `READ_COMMITTED`, after a transaction acquires the `FOR UPDATE` lock and another transaction releases it, the waiting transaction will see the **latest committed state**, including any new ledger entries. This ensures the balance check is always accurate.

**Deadlock prevention:** When a transfer involves two accounts, locks are always acquired in **ascending UUID order**, eliminating circular-wait deadlocks.

### 4. Balance Calculation and Overdraft Prevention

**Balance = SUM(CREDIT amounts) − SUM(DEBIT amounts)**

```sql
SELECT COALESCE(
  SUM(CASE WHEN entry_type = 'CREDIT' THEN amount
           WHEN entry_type = 'DEBIT'  THEN -amount
           ELSE 0 END), 0)
FROM ledger_entries
WHERE account_id = :accountId
```

- Balance is **never stored** as a column — it is always derived from the ledger.
- **Two-phase validation:** The balance is checked both **before** creating entries (fast rejection) and **after** creating entries (safety net against race conditions).
- If the post-entry balance is negative, the exception propagates and Spring rolls back the entire transaction, undoing both ledger entries.

### 5. Immutability Enforcement

Ledger immutability is enforced at **two levels**:

1. **Application level:** Hibernate's `@Immutable` annotation prevents the ORM from generating `UPDATE` SQL for `LedgerEntry` entities. No update/delete endpoints are exposed.
2. **Database level:** A PostgreSQL trigger (`prevent_ledger_update`) fires `BEFORE UPDATE OR DELETE` on the `ledger_entries` table and raises an exception, blocking the operation even from direct SQL access.

---

## Transfer Transaction Flow

```
Client                  Controller              TransactionService            DB (PostgreSQL)
  │                        │                          │                           │
  │  POST /transfers       │                          │                           │
  │───────────────────────>│                          │                           │
  │                        │  executeTransfer()       │                           │
  │                        │─────────────────────────>│                           │
  │                        │                          │                           │
  │                        │                          │  BEGIN TRANSACTION        │
  │                        │                          │──────────────────────────>│
  │                        │                          │                           │
  │                        │                          │  SELECT ... FOR UPDATE    │
  │                        │                          │  (lock source & dest,     │
  │                        │                          │   ordered by UUID)        │
  │                        │                          │──────────────────────────>│
  │                        │                          │  ◄── accounts locked ─────│
  │                        │                          │                           │
  │                        │                          │  SUM(ledger_entries)      │
  │                        │                          │  → check balance ≥ amt    │
  │                        │                          │──────────────────────────>│
  │                        │                          │  ◄── balance OK ──────────│
  │                        │                          │                           │
  │                        │                          │  INSERT transaction       │
  │                        │                          │  (status = PENDING)       │
  │                        │                          │──────────────────────────>│
  │                        │                          │                           │
  │                        │                          │  INSERT ledger_entry      │
  │                        │                          │  (DEBIT, source)          │
  │                        │                          │──────────────────────────>│
  │                        │                          │                           │
  │                        │                          │  INSERT ledger_entry      │
  │                        │                          │  (CREDIT, destination)    │
  │                        │                          │──────────────────────────>│
  │                        │                          │                           │
  │                        │                          │  SUM(ledger_entries)      │
  │                        │                          │  → verify balance ≥ 0     │
  │                        │                          │──────────────────────────>│
  │                        │                          │  ◄── verified ────────────│
  │                        │                          │                           │
  │                        │                          │  UPDATE transaction       │
  │                        │                          │  (status = COMPLETED)     │
  │                        │                          │──────────────────────────>│
  │                        │                          │                           │
  │                        │                          │  COMMIT                   │
  │                        │                          │──────────────────────────>│
  │                        │                          │                           │
  │  ◄── 201 Created ─────│◄──────────────────────────│                           │
  │      (TransactionResp) │                          │                           │
```

---

## Testing with Postman

A complete Postman collection is included at [`postman/Financial_Ledger_API.postman_collection.json`](postman/Financial_Ledger_API.postman_collection.json).

### Import & Run

1. Open Postman → **Import** → select the collection file.
2. The collection uses variables `{{accountId1}}` and `{{accountId2}}` that are auto-populated by the "Create Account" test scripts.
3. Run the requests **in order** from top to bottom (the collection is organized as a sequential workflow).

### Test Scenarios Covered

| # | Scenario | Expected |
|---|----------|----------|
| 1 | Create two accounts | 201, balance = 0 |
| 2 | Deposit $1000 into Account A | 201, COMPLETED |
| 3 | Deposit $500 into Account B | 201, COMPLETED |
| 4 | Verify Account A balance | 200, balance = 1000 |
| 5 | Transfer $250 from A → B | 201, COMPLETED |
| 6 | Verify A balance = $750 | 200 |
| 7 | Verify B balance = $750 | 200 |
| 8 | Transfer exceeding balance | 422, Insufficient Funds |
| 9 | Self-transfer | 400, Bad Request |
| 10 | Withdraw $100 from A | 201, COMPLETED |
| 11 | Verify A balance = $650 | 200 |
| 12 | Overdraft withdrawal | 422, Insufficient Funds |
| 13 | View ledger entries for A | 200, array of entries |
| 14 | Account not found | 404 |
| 15 | Missing required fields | 400, validation errors |

---

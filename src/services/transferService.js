const pool = require("../db");
const { getAccountBalance } = require("./balanceService");

async function transferFunds({
  sourceAccountId,
  destinationAccountId,
  amount,
  currency,
  description,
}) {
  const client = await pool.connect();

  try {
    await client.query("BEGIN");

    // Lock source account row
    const sourceAccount = await client.query(
      `
      SELECT id, status
      FROM accounts
      WHERE id = $1
      FOR UPDATE
      `,
      [sourceAccountId]
    );

    if (sourceAccount.rowCount === 0) {
      throw new Error("Source account not found");
    }

    if (sourceAccount.rows[0].status !== "active") {
      throw new Error("Source account is not active");
    }

    // Check balance
    const currentBalance = await getAccountBalance(
      sourceAccountId,
      client
    );

    if (Number(currentBalance) < Number(amount)) {
      throw new Error("Insufficient funds");
    }

    // Create transaction record
    const transactionResult = await client.query(
      `
      INSERT INTO transactions
      (type, source_account_id, destination_account_id, amount, currency, status, description)
      VALUES ('transfer', $1, $2, $3, $4, 'pending', $5)
      RETURNING id
      `,
      [
        sourceAccountId,
        destinationAccountId,
        amount,
        currency,
        description,
      ]
    );

    const transactionId = transactionResult.rows[0].id;

    // Debit source
    await client.query(
      `
      INSERT INTO ledger_entries
      (account_id, transaction_id, entry_type, amount)
      VALUES ($1, $2, 'debit', $3)
      `,
      [sourceAccountId, transactionId, amount]
    );

    // Credit destination
    await client.query(
      `
      INSERT INTO ledger_entries
      (account_id, transaction_id, entry_type, amount)
      VALUES ($1, $2, 'credit', $3)
      `,
      [destinationAccountId, transactionId, amount]
    );

    // Mark transaction complete
    await client.query(
      `
      UPDATE transactions
      SET status = 'completed'
      WHERE id = $1
      `,
      [transactionId]
    );

    await client.query("COMMIT");

    return { transactionId };
  } catch (err) {
    await client.query("ROLLBACK");
    throw err;
  } finally {
    client.release();
  }
}

module.exports = { transferFunds };
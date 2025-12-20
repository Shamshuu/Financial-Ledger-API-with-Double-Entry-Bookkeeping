const pool = require("../db");

async function depositFunds({ accountId, amount, currency, description }) {
  const client = await pool.connect();

  try {
    await client.query("BEGIN");

    const account = await client.query(
      `SELECT id, status FROM accounts WHERE id = $1 FOR UPDATE`,
      [accountId]
    );

    if (account.rowCount === 0) {
      throw new Error("Account not found");
    }

    if (account.rows[0].status !== "active") {
      throw new Error("Account not active");
    }

    const tx = await client.query(
      `
      INSERT INTO transactions
      (type, destination_account_id, amount, currency, status, description)
      VALUES ('deposit', $1, $2, $3, 'pending', $4)
      RETURNING id
      `,
      [accountId, amount, currency, description]
    );

    const transactionId = tx.rows[0].id;

    await client.query(
      `
      INSERT INTO ledger_entries
      (account_id, transaction_id, entry_type, amount)
      VALUES ($1, $2, 'credit', $3)
      `,
      [accountId, transactionId, amount]
    );

    await client.query(
      `UPDATE transactions SET status = 'completed' WHERE id = $1`,
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

module.exports = { depositFunds };

const pool = require("../db");
const { getAccountBalance } = require("../services/balanceService");

async function createAccount(req, res) {
  const { userId, accountType, currency } = req.body;

  if (!userId || !accountType || !currency) {
    return res.status(400).json({ error: "Missing required fields" });
  }

  const result = await pool.query(
    `
    INSERT INTO accounts (user_id, account_type, currency)
    VALUES ($1, $2, $3)
    RETURNING id
    `,
    [userId, accountType, currency]
  );

  res.status(201).json({ accountId: result.rows[0].id });
}

async function getAccount(req, res) {
  const { accountId } = req.params;

  const accountResult = await pool.query(
    `
    SELECT id, user_id, account_type, currency, status, created_at
    FROM accounts
    WHERE id = $1
    `,
    [accountId]
  );

  if (accountResult.rowCount === 0) {
    return res.status(404).json({ error: "Account not found" });
  }

  const balance = await getAccountBalance(accountId);

  res.json({
    ...accountResult.rows[0],
    balance,
  });
}

async function getLedger(req, res) {
  const { accountId } = req.params;

  const ledgerResult = await pool.query(
    `
    SELECT id, transaction_id, entry_type, amount, created_at
    FROM ledger_entries
    WHERE account_id = $1
    ORDER BY created_at
    `,
    [accountId]
  );

  res.json(ledgerResult.rows);
}

module.exports = {
  createAccount,
  getAccount,
  getLedger,
};

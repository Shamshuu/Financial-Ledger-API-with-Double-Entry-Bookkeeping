const pool = require("../db");

async function getAccountBalance(accountId, client = pool) {
  const result = await client.query(
    `
    SELECT
      COALESCE(
        SUM(
          CASE
            WHEN entry_type = 'credit' THEN amount
            WHEN entry_type = 'debit' THEN -amount
          END
        ),
        0
      ) AS balance
    FROM ledger_entries
    WHERE account_id = $1
    `,
    [accountId]
  );

  return result.rows[0].balance;
}

module.exports = { getAccountBalance };

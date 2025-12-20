require("dotenv").config();
const { transferFunds } = require("./services/transferService");
const pool = require("./db");

(async () => {
  const result = await pool.query(
    `
    SELECT id FROM accounts LIMIT 2
    `
  );

  if (result.rowCount < 2) {
    console.log("Create at least 2 accounts first");
    process.exit(1);
  }

  const [source, destination] = result.rows;

  try {
    const tx = await transferFunds({
      sourceAccountId: source.id,
      destinationAccountId: destination.id,
      amount: 50,
      currency: "USD",
      description: "Test transfer",
    });

    console.log("Transfer successful:", tx);
  } catch (e) {
    console.error("Transfer failed:", e.message);
  } finally {
    process.exit(0);
  }
})();

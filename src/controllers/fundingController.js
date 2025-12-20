const { depositFunds } = require("../services/depositService");
const { withdrawFunds } = require("../services/withdrawalService");

async function deposit(req, res) {
  try {
    const result = await depositFunds(req.body);
    res.status(201).json(result);
  } catch (e) {
    res.status(422).json({ error: e.message });
  }
}

async function withdraw(req, res) {
  try {
    const result = await withdrawFunds(req.body);
    res.status(201).json(result);
  } catch (e) {
    res.status(422).json({ error: e.message });
  }
}

module.exports = { deposit, withdraw };

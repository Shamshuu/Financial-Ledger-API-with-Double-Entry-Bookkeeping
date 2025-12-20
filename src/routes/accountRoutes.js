const express = require("express");
const router = express.Router();
const {
  createAccount,
  getAccount,
  getLedger,
} = require("../controllers/accountController");

router.post("/accounts", createAccount);
router.get("/accounts/:accountId", getAccount);
router.get("/accounts/:accountId/ledger", getLedger);

module.exports = router;
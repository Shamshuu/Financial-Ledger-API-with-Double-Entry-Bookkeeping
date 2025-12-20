const express = require("express");
const app = express();

app.use(express.json());

const accountRoutes = require("./routes/accountRoutes");
app.use(accountRoutes);

const { deposit, withdraw } = require("./controllers/fundingController");

app.post("/deposits", deposit);
app.post("/withdrawals", withdraw);


app.get("/health", (req, res) => {
  res.json({ status: "OK" });
});

module.exports = app;

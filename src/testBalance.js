require("dotenv").config();
const { getAccountBalance } = require("./services/balanceService");

(async () => {
  const balance = await getAccountBalance("49b4b1d5-89a2-4855-962e-2a882cd278b1");
  console.log("Balance:", balance);
  process.exit(0);
})();

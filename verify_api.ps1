$ErrorActionPreference = "Stop"

function Test-Api($method, $uri, $body) {
    echo "=================================================="
    echo "Using: $method $uri $body"
    echo "=================================================="
    try {
        if ($body) {
            $response = Invoke-RestMethod -Uri "http://localhost:8080$uri" -Method $method -ContentType "application/json" -Body $body
        } else {
            $response = Invoke-RestMethod -Uri "http://localhost:8080$uri" -Method $method
        }
        return $response
    } catch {
        Write-Host "Error calling $method $uri" -ForegroundColor Red
        Write-Host $_.Exception.Response.StatusCode.value__
        Write-Host $_.ErrorDetails.Message
        return $null
    }
}

Write-Host "1. Creating UserA..."
$respA = Invoke-RestMethod -Uri "http://localhost:8080/accounts" -Method POST -ContentType "application/json" -Body '{"userId":"UserA","accountType":"CHECKING","currency":"USD"}'
$idA = $respA.id
Write-Host "Created UserA: $idA"

Write-Host "`n2. Creating UserB..."
$respB = Invoke-RestMethod -Uri "http://localhost:8080/accounts" -Method POST -ContentType "application/json" -Body '{"userId":"UserB","accountType":"SAVINGS","currency":"USD"}'
$idB = $respB.id
Write-Host "Created UserB: $idB"

Write-Host "`n3. Depositing 1000 to UserA..."
$dep = Invoke-RestMethod -Uri "http://localhost:8080/deposits" -Method POST -ContentType "application/json" -Body "{`"accountId`":`"$idA`",`"amount`":1000}"
Write-Host "Deposited: $($dep.amount)"

Write-Host "`n4. Transferring 200 from UserA to UserB..."
$trans = Invoke-RestMethod -Uri "http://localhost:8080/transfers" -Method POST -ContentType "application/json" -Body "{`"sourceAccountId`":`"$idA`",`"destinationAccountId`":`"$idB`",`"amount`":200}"
Write-Host "Transferred: $($trans.amount)"

Write-Host "`n5. Checking UserA Balance (Expect 800)..."
$balA = Invoke-RestMethod -Uri "http://localhost:8080/accounts/$idA"
Write-Host "UserA Balance: $($balA.balance)"

Write-Host "`n6. Checking UserB Balance (Expect 200)..."
$balB = Invoke-RestMethod -Uri "http://localhost:8080/accounts/$idB"
Write-Host "UserB Balance: $($balB.balance)"

Write-Host "`n7. Checking Ledger UserA..."
$ledA = Invoke-RestMethod -Uri "http://localhost:8080/accounts/$idA/ledger"
$ledA | Format-Table -Property entryType,amount

Write-Host "`n8. Attempting Overdraft (Withdraw 1000 from UserA - Expect Fail)..."
try {
    Invoke-RestMethod -Uri "http://localhost:8080/withdrawals" -Method POST -ContentType "application/json" -Body "{`"accountId`":`"$idA`",`"amount`":1000}"
} catch {
    Write-Host "Caught Expected Error: $($_.Exception.Message)"
}

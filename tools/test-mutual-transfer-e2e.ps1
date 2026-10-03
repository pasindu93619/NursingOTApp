param (
    [string]$BaseUrl = "https://nursing-transfer-worker.pasindu93pavithra.workers.dev",
    [string]$IdTokenA = "",
    [string]$IdTokenB = ""
)

$ErrorActionPreference = "Continue"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " NURSING SUPER APP - MUTUAL TRANSFER BACKEND E2E TEST " -ForegroundColor Cyan
Write-Host " Target URL: $BaseUrl" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$testResults = [System.Collections.Generic.List[PSObject]]::new()

function Add-TestResult {
    param([string]$Name, [bool]$Passed, [string]$Details)
    $testResults.Add([PSCustomObject]@{
        Name = $Name
        Passed = $Passed
        Details = $Details
    })
    $color = if ($Passed) { "Green" } else { "Red" }
    $mark = if ($Passed) { "[PASS]" } else { "[FAIL]" }
    Write-Host "$mark $Name - $Details" -ForegroundColor $color
}

function Invoke-SafeRequest {
    param(
        [string]$Uri,
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        [string]$Body = ""
    )
    try {
        $req = [System.Net.HttpWebRequest]::Create($Uri)
        $req.Method = $Method
        $req.Timeout = 10000
        foreach ($k in $Headers.Keys) {
            if ($k -eq "Authorization") {
                $req.Headers["Authorization"] = $Headers[$k]
            } elseif ($k -eq "Content-Type") {
                $req.ContentType = $Headers[$k]
            } else {
                $req.Headers[$k] = $Headers[$k]
            }
        }
        if ($Body -ne "" -and ($Method -eq "POST" -or $Method -eq "PUT")) {
            $req.ContentType = "application/json"
            $bytes = [System.Text.Encoding]::UTF8.GetBytes($Body)
            $req.ContentLength = $bytes.Length
            $stream = $req.GetRequestStream()
            $stream.Write($bytes, 0, $bytes.Length)
            $stream.Close()
        }
        $resp = $req.GetResponse()
        $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
        $respBody = $reader.ReadToEnd()
        $reader.Close()
        $statusCode = [int]$resp.StatusCode
        $resp.Close()
        return @{ StatusCode = $statusCode; Body = $respBody }
    } catch [System.Net.WebException] {
        $ex = $_.Exception
        if ($ex.Response -ne $null) {
            $statusCode = [int]$ex.Response.StatusCode
            $reader = New-Object System.IO.StreamReader($ex.Response.GetResponseStream())
            $respBody = $reader.ReadToEnd()
            $reader.Close()
            $ex.Response.Close()
            return @{ StatusCode = $statusCode; Body = $respBody }
        }
        return @{ StatusCode = 0; Body = $ex.Message }
    } catch {
        return @{ StatusCode = 0; Body = $_.Exception.Message }
    }
}

# TEST 1: Health check endpoint
Write-Host "`n--> Testing GET /health..." -ForegroundColor Yellow
$h = Invoke-SafeRequest -Uri "$BaseUrl/health" -Method "GET"
if ($h.StatusCode -eq 200 -and ($h.Body -match "nursing-transfer-worker")) {
    Add-TestResult "GET /health" $true "Worker responded healthy (HTTP 200, nursing-transfer-worker)"
} else {
    Add-TestResult "GET /health" $false "HTTP $($h.StatusCode): $($h.Body)"
}

# TEST 2: Unauthenticated POST /api/matching/find-and-lock rejection
Write-Host "`n--> Testing unauthenticated POST /api/matching/find-and-lock..." -ForegroundColor Yellow
$r2 = Invoke-SafeRequest -Uri "$BaseUrl/api/matching/find-and-lock" -Method "POST"
if ($r2.StatusCode -eq 401) {
    Add-TestResult "POST /api/matching/find-and-lock unauth rejection" $true "Correctly rejected with HTTP 401 Unauthorized"
} else {
    Add-TestResult "POST /api/matching/find-and-lock unauth rejection" $false "HTTP $($r2.StatusCode): $($r2.Body)"
}

# TEST 3: Unauthenticated POST /api/matching/respond rejection
Write-Host "`n--> Testing unauthenticated POST /api/matching/respond..." -ForegroundColor Yellow
$body = '{"matchId":"test-match","decision":"ACCEPT"}'
$r3 = Invoke-SafeRequest -Uri "$BaseUrl/api/matching/respond" -Method "POST" -Body $body
if ($r3.StatusCode -eq 401) {
    Add-TestResult "POST /api/matching/respond unauth rejection" $true "Correctly rejected with HTTP 401 Unauthorized"
} else {
    Add-TestResult "POST /api/matching/respond unauth rejection" $false "HTTP $($r3.StatusCode): $($r3.Body)"
}

# TEST 4: Invalid Token POST /api/matching/respond rejection
Write-Host "`n--> Testing invalid token POST /api/matching/respond..." -ForegroundColor Yellow
$r4 = Invoke-SafeRequest -Uri "$BaseUrl/api/matching/respond" -Method "POST" -Headers @{ Authorization = "Bearer invalid-token-xyz" } -Body $body
if ($r4.StatusCode -eq 401) {
    Add-TestResult "POST /api/matching/respond invalid token" $true "Correctly rejected invalid token with HTTP 401 Unauthorized"
} else {
    Add-TestResult "POST /api/matching/respond invalid token" $false "HTTP $($r4.StatusCode): $($r4.Body)"
}

# TEST 5: Method Not Allowed checks
Write-Host "`n--> Testing method validation..." -ForegroundColor Yellow
$r5 = Invoke-SafeRequest -Uri "$BaseUrl/api/matching/respond" -Method "GET"
if ($r5.StatusCode -eq 405) {
    Add-TestResult "GET /api/matching/respond method check" $true "Correctly rejected GET with HTTP 405 Method Not Allowed"
} else {
    Add-TestResult "GET /api/matching/respond method check" $false "HTTP $($r5.StatusCode): $($r5.Body)"
}

# TEST 6: Authenticated End-to-End Match Flow (if ID tokens provided)
if ($IdTokenA -ne "" -and $IdTokenB -ne "") {
    Write-Host "`n--> Testing authenticated reciprocal match flow..." -ForegroundColor Yellow
    $matchResp = Invoke-SafeRequest -Uri "$BaseUrl/api/matching/find-and-lock" -Method "POST" -Headers @{ Authorization = "Bearer $IdTokenA" }
    Write-Host "Nurse A find-and-lock response: $($matchResp.Body)"
    
    if ($matchResp.StatusCode -eq 200 -and ($matchResp.Body -match '"matched":true')) {
        $json = $matchResp.Body | ConvertFrom-Json
        $matchId = $json.matchId
        Add-TestResult "Find-and-Lock Reciprocal Match" $true "Found and locked match ID: $matchId"

        # Nurse A accepts
        $respA = Invoke-SafeRequest -Uri "$BaseUrl/api/matching/respond" -Method "POST" -Headers @{ Authorization = "Bearer $IdTokenA" } -Body "{`"matchId`":`"$matchId`",`"decision`":`"ACCEPT`"}"
        Write-Host "Nurse A accept response: $($respA.Body)"
        if ($respA.StatusCode -eq 200 -and ($respA.Body -match 'PENDING_CONFIRMATION')) {
            Add-TestResult "Nurse A Accept Decision" $true "Status transitioned to PENDING_CONFIRMATION"
        } else {
            Add-TestResult "Nurse A Accept Decision" $false "HTTP $($respA.StatusCode): $($respA.Body)"
        }

        # Duplicate decision protection
        $dupResp = Invoke-SafeRequest -Uri "$BaseUrl/api/matching/respond" -Method "POST" -Headers @{ Authorization = "Bearer $IdTokenA" } -Body "{`"matchId`":`"$matchId`",`"decision`":`"ACCEPT`"}"
        if ($dupResp.StatusCode -eq 409) {
            Add-TestResult "Nurse A Duplicate Decision Protection" $true "Correctly blocked duplicate decision with HTTP 409"
        } else {
            Add-TestResult "Nurse A Duplicate Decision Protection" $false "HTTP $($dupResp.StatusCode): $($dupResp.Body)"
        }

        # Nurse B accepts
        $respB = Invoke-SafeRequest -Uri "$BaseUrl/api/matching/respond" -Method "POST" -Headers @{ Authorization = "Bearer $IdTokenB" } -Body "{`"matchId`":`"$matchId`",`"decision`":`"ACCEPT`"}"
        Write-Host "Nurse B accept response: $($respB.Body)"
        if ($respB.StatusCode -eq 200 -and ($respB.Body -match 'CONFIRMED')) {
            Add-TestResult "Nurse B Accept Decision (CONFIRMED)" $true "Both accepted -> Status transitioned to CONFIRMED!"
        } else {
            Add-TestResult "Nurse B Accept Decision (CONFIRMED)" $false "HTTP $($respB.StatusCode): $($respB.Body)"
        }
    } else {
        Add-TestResult "Find-and-Lock Reciprocal Match" $false "Response: $($matchResp.Body)"
    }
} else {
    Write-Host "`n[INFO] Authenticated E2E tokens not passed via CLI parameters. Authentication/route boundaries verified." -ForegroundColor Cyan
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host " SUMMARY: $($testResults.Where({$_.Passed}).Count)/$($testResults.Count) Tests Passed" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

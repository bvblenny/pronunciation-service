# PowerShell script to test the Pronunciation Scoring API

# Configuration
$baseUrl = "http://localhost:8080/api/pronunciation"
$audioFile = $null
$referenceText = "Hello world"
$languageCode = "en-US"

# Display banner
Write-Host "====================================================="
Write-Host "  Pronunciation Scoring API Test Script"
Write-Host "====================================================="
Write-Host ""

# Function to test health endpoint
function Test-Health {
    Write-Host "Testing health endpoint..."
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/health" -Method Get
        Write-Host "Service status: $($response.status)" -ForegroundColor Green
        Write-Host "Service name: $($response.service)" -ForegroundColor Green
        Write-Host "Health check successful!" -ForegroundColor Green
        return $true
    }
    catch {
        Write-Host "Error connecting to service: $_" -ForegroundColor Red
        Write-Host "Make sure the service is running on http://localhost:8080" -ForegroundColor Yellow
        return $false
    }
}

# Function to test pronunciation scoring
function Test-PronunciationScoring {
    param (
        [string]$audioFilePath,
        [string]$text,
        [string]$language
    )

    if (-not (Test-Path $audioFilePath)) {
        Write-Host "Error: Audio file not found at path: $audioFilePath" -ForegroundColor Red
        return
    }

    Write-Host "Testing pronunciation scoring with:"
    Write-Host "  - Audio file: $audioFilePath"
    Write-Host "  - Reference text: $text"
    Write-Host "  - Language code: $language"
    Write-Host ""

    try {
        $fileBytes = [System.IO.File]::ReadAllBytes($audioFilePath)
        $fileName = Split-Path $audioFilePath -Leaf

        $form = New-Object System.Net.Http.MultipartFormDataContent
        $fileContent = New-Object System.Net.Http.ByteArrayContent -ArgumentList @(,$fileBytes)
        $form.Add($fileContent, "audio", $fileName)

        $textContent = New-Object System.Net.Http.StringContent -ArgumentList $text
        $form.Add($textContent, "referenceText")

        $langContent = New-Object System.Net.Http.StringContent -ArgumentList $language
        $form.Add($langContent, "languageCode")

        $client = New-Object System.Net.Http.HttpClient
        $response = $client.PostAsync("$baseUrl/score", $form).Result

        if ($response.IsSuccessStatusCode) {
            $result = $response.Content.ReadAsStringAsync().Result | ConvertFrom-Json

            # Display results
            Write-Host "Pronunciation Score: $([math]::Round($result.score * 100, 1))%" -ForegroundColor Green
            Write-Host "Transcribed Text: $($result.transcribedText)"
            Write-Host ""
            Write-Host "Word Details:"

            foreach ($word in $result.wordDetails) {
                $confidence = [math]::Round($word.confidence * 100, 1)
                $color = if ($word.isCorrect) { "Green" } else { "Red" }
                $expected = if (-not $word.isCorrect -and $word.expectedWord) { " (Expected: '$($word.expectedWord)')" } else { "" }

                Write-Host "  - $($word.word) ($confidence% confidence)$expected" -ForegroundColor $color
            }
        }
        else {
            $errorMsg = $response.Content.ReadAsStringAsync().Result
            Write-Host "Error: $($response.StatusCode) - $errorMsg" -ForegroundColor Red
        }
    }
    catch {
        Write-Host "Error sending request: $_" -ForegroundColor Red
    }
}

# Main script execution
$healthOk = Test-Health
Write-Host ""

if ($healthOk) {
    # Ask for audio file
    Write-Host "Please enter the path to your audio file (WAV format recommended):"
    $audioFile = Read-Host

    # Ask for reference text
    Write-Host "Enter reference text (press Enter to use default '$referenceText'):"
    $input = Read-Host
    if ($input) {
        $referenceText = $input
    }

    # Ask for language code
    Write-Host "Enter language code (press Enter to use default '$languageCode'):"
    $input = Read-Host
    if ($input) {
        $languageCode = $input
    }

    Write-Host ""
    Test-PronunciationScoring -audioFilePath $audioFile -text $referenceText -language $languageCode
}

Write-Host ""
Write-Host "Script execution completed."

# PowerShell script to create a sample audio file using Windows Speech Synthesis

# Configuration
$outputFile = "sample-audio-2.wav"
$textToSpeak = "The quick brown fox jumps over the lazy dog"

# Display banner
Write-Host "====================================================="
Write-Host "  Sample Audio Generator for Pronunciation Testing"
Write-Host "====================================================="
Write-Host ""

# Ask for text to speak
Write-Host "Enter text to convert to speech (press Enter to use default '$textToSpeak'):"
$input = Read-Host
if ($input) {
    $textToSpeak = $input
}

# Ask for output file
Write-Host "Enter output file name (press Enter to use default '$outputFile'):"
$input = Read-Host
if ($input) {
    $outputFile = $input
}

Write-Host ""
Write-Host "Generating audio file with text: '$textToSpeak'"
Write-Host "Output will be saved to: $outputFile"
Write-Host ""

try {
    # Create a speech synthesizer
    Add-Type -AssemblyName System.Speech
    $synthesizer = New-Object System.Speech.Synthesis.SpeechSynthesizer

    # Get available voices
    $voices = $synthesizer.GetInstalledVoices()

    Write-Host "Available voices:"
    $voiceOptions = @()
    $index = 0

    foreach ($voice in $voices) {
        $info = $voice.VoiceInfo
        Write-Host "[$index] $($info.Name) ($($info.Gender), $($info.Culture))"
        $voiceOptions += $info.Name
        $index++
    }

    # Ask user to select a voice
    Write-Host ""
    Write-Host "Select a voice by number (press Enter to use default):"
    $voiceSelection = Read-Host

    if ($voiceSelection -and $voiceSelection -match '^\d+$' -and [int]$voiceSelection -lt $voiceOptions.Count) {
        $synthesizer.SelectVoice($voiceOptions[[int]$voiceSelection])
        Write-Host "Selected voice: $($voiceOptions[[int]$voiceSelection])"
    } else {
        Write-Host "Using default voice: $($synthesizer.Voice.Name)"
    }

    # Set output to file
    $synthesizer.SetOutputToWaveFile($outputFile)

    # Speak text to file
    $synthesizer.Speak($textToSpeak)

    # Clean up
    $synthesizer.Dispose()

    Write-Host ""
    Write-Host "Audio file created successfully!" -ForegroundColor Green
    Write-Host "File location: $((Get-Item $outputFile).FullName)"
    Write-Host ""
    Write-Host "You can now use this file with the test-api.ps1 script to test the pronunciation scoring API."
}
catch {
    Write-Host "Error creating audio file: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "Script execution completed."

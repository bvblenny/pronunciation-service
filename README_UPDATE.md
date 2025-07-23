# How to Try the Pronunciation Scoring Feature

This guide will help you test the newly created pronunciation scoring feature using the included web interface or command-line script.

## Running the Application

1. Make sure you have set up your Google Cloud credentials as described in the main README.md
2. Start the application:

```bash
# For Windows
.\gradlew.bat bootRun

# For Linux/macOS
./gradlew bootRun
```

3. The service will start on port 8080 by default

## Using the Web Interface

Once the application is running, you can access the web interface:

1. Open your browser and navigate to: http://localhost:8080
2. You'll see the Pronunciation Scoring Demo interface

## Testing the Feature

### Step 1: Check Service Health

1. Click the "Check Service Health" button to verify the service is running properly
2. You should see a response with "Status: UP"

### Step 2: Test Pronunciation Scoring

1. Enter a reference text in the text area (default is "Hello world")
2. Select the appropriate language code (default is "en-US")

#### Option 1: Record Audio
3. Click "Start Recording" and speak the reference text
4. Click "Stop Recording" when finished
5. Review your recording using the audio player
6. Click "Submit Recording" to analyze your pronunciation

#### Option 2: Upload Audio File
3. Under "Option 2: Upload Audio File", click "Browse" or "Choose File"
4. Select an audio file from your computer (WAV format recommended)
5. Click "Submit Audio File" to analyze the uploaded audio

### Step 3: Review Results

The results will show:
- Your overall pronunciation score (0-100%)
- The text that was transcribed from your audio
- Word-level details showing which words were pronounced correctly
- Confidence scores for each word

## Troubleshooting

If you encounter issues:

1. Make sure your Google Cloud credentials are properly configured
2. Check that you have enabled the Speech-to-Text API in your Google Cloud project
3. Ensure your microphone is working and you've granted permission in the browser
4. Try using a different browser if you experience recording issues
5. Check the application logs for more detailed error information

## Using the Command-Line Scripts

### Testing with Pre-recorded Audio

If you prefer using the command line or want to test with pre-recorded audio files, you can use the included PowerShell script:

1. Open PowerShell
2. Navigate to the project directory
3. Run the script:

```powershell
.\test-api.ps1
```

4. The script will:
   - Check if the service is running by testing the health endpoint
   - Prompt you for the path to an audio file (WAV format recommended)
   - Ask for a reference text (default is "Hello world")
   - Ask for a language code (default is "en-US")
   - Send the audio to the API and display the results

### Example Output

```
=====================================================
  Pronunciation Scoring API Test Script
=====================================================

Testing health endpoint...
Service status: UP
Service name: pronunciation-scoring
Health check successful!

Please enter the path to your audio file (WAV format recommended):
C:\path\to\your\audio.wav

Enter reference text (press Enter to use default 'Hello world'):

Enter language code (press Enter to use default 'en-US'):

Testing pronunciation scoring with:
  - Audio file: C:\path\to\your\audio.wav
  - Reference text: Hello world
  - Language code: en-US

Pronunciation Score: 85.0%
Transcribed Text: hello world

Word Details:
  - hello (92.0% confidence)
  - world (88.0% confidence)

Script execution completed.
```

### Generating Sample Audio

If you don't have an audio file for testing, you can use the included script to generate one:

1. Open PowerShell
2. Navigate to the project directory
3. Run the script:

```powershell
.\create-sample-audio.ps1
```

4. The script will:
   - Prompt you for the text you want to convert to speech
   - Let you choose from available voices on your system
   - Generate a WAV file that you can use for testing

5. Once the audio file is created, you can use it with the test-api.ps1 script

### Example Output

```
=====================================================
  Sample Audio Generator for Pronunciation Testing
=====================================================

Enter text to convert to speech (press Enter to use default 'Hello world'):
How are you today

Enter output file name (press Enter to use default 'sample-audio.wav'):

Generating audio file with text: 'How are you today'
Output will be saved to: sample-audio.wav

Available voices:
[0] Microsoft David Desktop (Male, en-US)
[1] Microsoft Zira Desktop (Female, en-US)

Select a voice by number (press Enter to use default):
1
Selected voice: Microsoft Zira Desktop

Audio file created successfully!
File location: D:\Dokumente\repos\pronunciation-service\sample-audio.wav

You can now use this file with the test-api.ps1 script to test the pronunciation scoring API.

Script execution completed.
```

## Summary of Available Tools

To help you try the newly created pronunciation scoring feature, this project provides:

1. **Web Interface** - A user-friendly browser-based interface for:
   - Testing the service health
   - Recording audio directly in your browser
   - Uploading audio files from your computer
   - Submitting audio for pronunciation scoring
   - Viewing detailed results with visual feedback

2. **Command-Line Testing Script** - A PowerShell script for:
   - Testing the service health
   - Submitting pre-recorded audio files
   - Viewing detailed results in the terminal

3. **Sample Audio Generator** - A PowerShell script for:
   - Creating test audio files using Windows Speech Synthesis
   - Customizing the text and voice
   - Generating properly formatted WAV files for testing

## Next Steps

After trying the basic functionality, you can:
- Experiment with different languages
- Try more complex phrases
- Integrate the API into your own applications using the REST endpoints
- Modify the scripts or web interface to suit your specific needs

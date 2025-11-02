# ASR Provider Integration Guide

## Overview

This service supports three ASR (Automatic Speech Recognition) providers for transcription and subtitle generation, implemented using the **Strategy design pattern**:

1. **CMU Sphinx** - Offline, fast, pre-bundled (default)
2. **Vosk** - Offline, high accuracy, requires model download
3. **Google Cloud Speech-to-Text** - Cloud-based, highest accuracy, requires API credentials

### Strategy Pattern Architecture

Each ASR provider is implemented as a concrete strategy implementing the `TranscriptionStrategy` interface:
- `SphinxTranscriptionStrategy`
- `VoskTranscriptionStrategy`
- `GoogleCloudTranscriptionStrategy`

The `TranscriptionStrategyResolver` dynamically selects and executes the appropriate strategy at runtime based on the provider name. This design makes the system highly extensible - new providers can be added by simply implementing the strategy interface and registering as a Spring bean.

## Quick Comparison

| Feature | Sphinx | Vosk | Google Cloud |
|---------|--------|------|--------------|
| **Accuracy** | Moderate | High | Highest |
| **Speed** | Fast | Fast | Fast (network dependent) |
| **Cost** | Free | Free | ~$0.006 per 15 seconds |
| **Setup** | None (pre-bundled) | Model download | API credentials |
| **Languages** | English | 20+ | 125+ |
| **Internet** | Not required | Not required | Required |
| **Best For** | Development/testing | Production offline | Production with internet |

## Provider Setup Guides

### 1. CMU Sphinx (Default)

**No setup required!** Sphinx is pre-configured and works out of the box.

**Usage:**
```bash
# Default provider - no parameters needed
curl -X POST http://localhost:8080/api/transcription/transcribe-with-subtitles \
  -F "file=@audio.mp3"

# Explicit provider selection
curl -X POST http://localhost:8080/api/transcription/transcribe-with-subtitles \
  -F "file=@audio.mp3" \
  -F "provider=sphinx"
```

**When to use:**
- Quick prototyping and testing
- Offline development environments
- When setup time is critical
- Budget-constrained projects

---

### 2. Vosk (Offline, High Accuracy)

**Setup Steps:**

1. **Download a Vosk model** from https://alphacephei.com/vosk/models

   Recommended models:
   - Small English (40 MB): `vosk-model-small-en-us-0.15` - Good balance
   - Full English (1.8 GB): `vosk-model-en-us-0.22` - Best offline accuracy
   - Other languages: Check models page for 20+ languages

   ```bash
   # Example: Download small English model
   wget https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
   unzip vosk-model-small-en-us-0.15.zip
   ```

2. **Configure the model path**

   Option A - Environment variable:
   ```bash
   export VOSK_MODEL_PATH=/path/to/vosk-model-small-en-us-0.15
   ```

   Option B - application.properties:
   ```properties
   vosk.model-path=/path/to/vosk-model-small-en-us-0.15
   ```

3. **Optional: Set as default provider**
   ```bash
   export TRANSCRIPTION_PROVIDER=vosk
   ```

**Usage:**
```bash
curl -X POST http://localhost:8080/api/transcription/transcribe-with-subtitles \
  -F "file=@audio.mp3" \
  -F "provider=vosk"
```

**When to use:**
- Production environments without internet
- When high accuracy is needed offline
- Privacy-sensitive applications (data stays local)
- Multi-language support needed

**Health Check:**
```bash
curl http://localhost:8080/actuator/health | jq .components.vosk
```

---

### 3. Google Cloud Speech-to-Text (Cloud, Highest Accuracy)

**Setup Steps:**

1. **Create a Google Cloud project**
   - Go to https://console.cloud.google.com
   - Create a new project or select existing one
   - Note your project ID

2. **Enable the Speech-to-Text API**
   - Navigate to "APIs & Services" → "Library"
   - Search for "Cloud Speech-to-Text API"
   - Click "Enable"

3. **Create service account credentials**
   - Go to "APIs & Services" → "Credentials"
   - Click "Create Credentials" → "Service Account"
   - Grant "Cloud Speech Client" role
   - Create and download JSON key file

4. **Configure credentials**

   Option A - Environment variable (recommended):
   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
   ```

   Option B - application.properties:
   ```properties
   spring.cloud.gcp.credentials.location=file:/path/to/service-account-key.json
   spring.cloud.gcp.project-id=your-project-id
   ```

5. **Optional: Set as default provider**
   ```bash
   export TRANSCRIPTION_PROVIDER=google
   ```

**Usage:**
```bash
# Using 'google' alias
curl -X POST http://localhost:8080/api/transcription/transcribe-with-subtitles \
  -F "file=@audio.mp3" \
  -F "provider=google"

# Alternative aliases: 'google-cloud', 'gcp'
curl -X POST http://localhost:8080/api/transcription/transcribe-with-subtitles \
  -F "file=@audio.mp3" \
  -F "provider=gcp"
```

**When to use:**
- Production with internet connectivity
- When highest accuracy is critical
- Multi-language applications (125+ languages)
- When budget allows pay-per-use pricing

**Cost Estimation:**
- $0.006 per 15 seconds of audio
- Example: 1 hour of audio = ~$1.44
- Free tier: 60 minutes/month

**Health Check:**
```bash
curl http://localhost:8080/actuator/health | jq .components.googleCloudSpeech
```

---

## Configuration Reference

### Application Properties

```properties
# Default provider (sphinx, vosk, or google)
transcription.default-provider=sphinx

# Sphinx (pre-configured)
sphinx.acoustic-model=resource:/models/cmusphinx-en-us-5.2
sphinx.dictionary=resource:/models/cmudict-en-us.dict
sphinx.language-model=resource:/models/en-us.lm.bin

# Vosk
vosk.model-path=/path/to/vosk-model
vosk.sample-rate=16000

# Google Cloud Speech
google.speech.language-code=en-US
google.speech.enable-word-time-offsets=true
spring.cloud.gcp.credentials.location=file:/path/to/credentials.json
spring.cloud.gcp.project-id=your-project-id
```

### Environment Variables

```bash
# Provider selection
export TRANSCRIPTION_PROVIDER=sphinx  # or vosk, or google

# Vosk
export VOSK_MODEL_PATH=/path/to/vosk-model

# Google Cloud
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json
export GCP_PROJECT_ID=your-project-id
export GOOGLE_SPEECH_LANGUAGE=en-US
```

## API Usage Examples

### Basic Transcription

```bash
# Default provider
curl -X POST http://localhost:8080/api/transcription/transcribe \
  -F "file=@audio.mp3" \
  -F "languageCode=en-US"

# Specific provider
curl -X POST http://localhost:8080/api/transcription/transcribe \
  -F "file=@audio.mp3" \
  -F "languageCode=en-US" \
  -F "provider=vosk"
```

### Subtitle Generation

```bash
# Generate SRT subtitles
curl -X POST http://localhost:8080/api/transcription/transcribe-with-subtitles \
  -F "file=@video.mp4" \
  -F "languageCode=en-US" \
  -F "provider=google" \
  -o response.json

# Extract subtitle content
jq -r '.subtitleContent' response.json > subtitles.srt
```

### Supported Languages

```bash
# Vosk with German
curl -X POST http://localhost:8080/api/transcription/transcribe-with-subtitles \
  -F "file=@audio.mp3" \
  -F "languageCode=de-DE" \
  -F "provider=vosk"

# Google Cloud with French
curl -X POST http://localhost:8080/api/transcription/transcribe-with-subtitles \
  -F "file=@audio.mp3" \
  -F "languageCode=fr-FR" \
  -F "provider=google"
```

## Troubleshooting

### Vosk Issues

**Problem:** `Vosk provider is not available`
```bash
# Check health status
curl http://localhost:8080/actuator/health | jq .components.vosk

# Verify model path
ls -la $VOSK_MODEL_PATH

# Check logs
tail -f logs/application.log | grep Vosk
```

**Solution:**
- Ensure model directory exists and contains model files
- Verify `vosk.model-path` is correctly configured
- Check file permissions

### Google Cloud Issues

**Problem:** `Google Cloud Speech client not initialized`
```bash
# Verify credentials file exists
ls -la $GOOGLE_APPLICATION_CREDENTIALS

# Check health status
curl http://localhost:8080/actuator/health | jq .components.googleCloudSpeech

# Test credentials
gcloud auth application-default login
```

**Solution:**
- Verify credentials file path is correct
- Ensure Speech-to-Text API is enabled
- Check service account has proper roles
- Verify network connectivity to Google Cloud

### General Issues

**Problem:** `Unknown transcription provider`

**Solution:** Use one of: `sphinx`, `vosk`, `google`, `google-cloud`, or `gcp`

## Performance Tips

### Vosk
- Use small models for faster transcription
- Large models provide better accuracy but slower processing
- Consider memory requirements (100MB - 2GB RAM)

### Google Cloud
- Batch multiple requests to optimize cost
- Use appropriate language codes for best accuracy
- Consider network latency in response time
- Monitor API quotas and limits

### General
- Audio is automatically normalized to mono 16kHz WAV
- Longer audio files take proportionally longer to process
- Consider async processing for large files (future feature)

## Migration Between Providers

### From Sphinx to Vosk
```bash
# 1. Download and configure Vosk model
export VOSK_MODEL_PATH=/path/to/vosk-model

# 2. Update default provider (optional)
export TRANSCRIPTION_PROVIDER=vosk

# 3. Restart application
./gradlew bootRun
```

### From Offline to Google Cloud
```bash
# 1. Set up Google Cloud credentials
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json

# 2. Update default provider (optional)
export TRANSCRIPTION_PROVIDER=google

# 3. Restart application
./gradlew bootRun
```

### Testing Multiple Providers
```bash
# Test all providers with same audio
for provider in sphinx vosk google; do
  echo "Testing $provider..."
  curl -X POST http://localhost:8080/api/transcription/transcribe \
    -F "file=@test.mp3" \
    -F "provider=$provider" \
    -o "${provider}_result.json"
done
```

## Security Considerations

### Google Cloud Credentials
- Never commit credentials to version control
- Use environment variables or secure secrets management
- Rotate service account keys regularly
- Apply principle of least privilege to service accounts

### API Access
- Implement rate limiting for production
- Use authentication/authorization (e.g., JWT)
- Monitor API usage and costs
- Set up alerts for unusual activity

## Cost Management (Google Cloud)

### Optimization Strategies
1. **Use appropriate provider:** Use Sphinx/Vosk for development
2. **Batch processing:** Process multiple files in batches
3. **Monitor usage:** Set up billing alerts
4. **Cache results:** Store transcriptions to avoid re-processing
5. **Language optimization:** Use correct language codes (improves accuracy = fewer retries)

### Cost Examples
```
1 minute audio   = $0.024
1 hour audio     = $1.44
10 hours/day     = $14.40/day = $432/month
Free tier        = 60 minutes/month free
```

## Support and Resources

- **Vosk:** https://alphacephei.com/vosk/
- **Google Cloud Speech:** https://cloud.google.com/speech-to-text
- **CMU Sphinx:** https://cmusphinx.github.io/

## License

This integration maintains the MIT license of the parent project.

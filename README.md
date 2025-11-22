# Pronunciation Service

Kotlin / Spring Boot service that normalizes user audio (ffmpeg), transcribes it via pluggable engines (Google Cloud Speech‑to‑Text and offline CMU Sphinx), performs forced alignment against a reference passage, and returns an overall pronunciation score plus word‑level timing & feedback.

## Features
- Forced alignment for scoring: reference vs hypothesis comparison, word correctness, confidence proxy, timings.
- **Prosody (suprasegmental) scoring**: Explainable, extensible scoring for rhythm, intonation, stress, pacing, and fluency with diagnostic metrics and learner feedback.
- Media normalization: converts mixed input formats to mono 16 kHz WAV via ffmpeg.
- Clean architecture for maintainability, extensibility, and testability
- Extensible: drop in new STT / ASR providers

## Quick Start

Prereqs: Kotlin/JVM (Java 21), Gradle, ffmpeg on PATH (or set media.ffmpeg.path), optional Google Cloud credentials.

```bash
./gradlew bootRun

# All /api/** endpoints require API key authentication
# Use the X-API-Key header with your API key
curl -H "X-API-Key: IJAE8hy/yqNcJEsWhgRS54vN39A/9kWBYB4lMb0QPc4=" \
  http://localhost:8080/api/transcription/languages
```

Docs: http://localhost:8080/swagger-ui.html  
OpenAPI: /v3/api-docs (JSON) | /v3/api-docs.yaml

## Security & Authentication

The API uses API key authentication to protect all `/api/**` endpoints. Health endpoints (`/actuator/health/**`) remain public for monitoring.

### Configuration

Configure API keys in `application.properties` or via environment variables:

```properties
# Enable/disable API key authentication (default: true)
app.security.enabled=true

# Comma-separated list of valid API keys
app.security.api-keys=your-api-key-1,your-api-key-2
```

**Environment variables:**
- `API_SECURITY_ENABLED`: Set to `true` or `false` to enable/disable authentication
- `API_KEYS`: Comma-separated list of valid API keys

### Default API Key (Development Only)

For development/testing, a default API key is provided:
```
IJAE8hy/yqNcJEsWhgRS54vN39A/9kWBYB4lMb0QPc4=
```

**⚠️ WARNING**: Change this key in production! Never commit real API keys to source control.

### Generating New API Keys

Generate secure API keys using OpenSSL:

```bash
# Generate a 32-character base64-encoded key
openssl rand -base64 32

# Generate a 64-character base64-encoded key
openssl rand -base64 48
```

### Usage Examples

**With API Key (valid request):**
```bash
curl -X POST http://localhost:8080/api/pronunciation/evaluate-stt \
  -H "X-API-Key: your-api-key-here" \
  -F "audio=@sample.wav" \
  -F "referenceText=Hello world" \
  -F "languageCode=en-US"
```

**Without API Key (will fail with 401):**
```bash
curl -X POST http://localhost:8080/api/pronunciation/evaluate-stt \
  -F "audio=@sample.wav" \
  -F "referenceText=Hello world"
# Response: {"timestamp":"...","status":401,"error":"Unauthorized","message":"Missing API key..."}
```

**Invalid API Key (will fail with 403):**
```bash
curl -X POST http://localhost:8080/api/pronunciation/evaluate-stt \
  -H "X-API-Key: invalid-key" \
  -F "audio=@sample.wav"
# Response: {"timestamp":"...","status":403,"error":"Forbidden","message":"Invalid API key..."}
```

### Security Best Practices

1. **Store API keys securely**: Use environment variables or secret management systems (e.g., AWS Secrets Manager, HashiCorp Vault, Kubernetes Secrets)
2. **Rotate keys regularly**: Generate new keys periodically and update clients
3. **Use strong keys**: Minimum 32 characters, cryptographically random (use `openssl rand -base64 32`)
4. **Never commit keys**: Keep keys out of source control using `.gitignore`
5. **Use HTTPS**: Always use HTTPS in production to encrypt API key transmission
6. **Monitor access**: Check logs for authentication failures
7. **Limit key distribution**: Only share keys with authorized clients
8. **Disable in development**: Set `app.security.enabled=false` for local development if needed

### Disabling Authentication (Development Only)

For local development or testing, you can disable authentication:

```properties
app.security.enabled=false
```

Or via environment variable:
```bash
export API_SECURITY_ENABLED=false
./gradlew bootRun
```

## Endpoints

- POST /api/pronunciation/evaluate-stt  
  Multipart: audio, referenceText, languageCode (default en-US)  
  → { score, transcribedText, wordDetails[] }

- POST /api/pronunciation/evaluate-sphinx-recognition  
  Quick Sphinx transcription → { transcript, words[] }

- POST /api/pronunciation/evaluate-sphinx-alignment  
  Forced alignment vs reference → detailed timings (and phoneme estimates)

- POST /api/transcription/transcribe  
  Raw Sphinx transcription with segments → { transcript, segments[] }

- POST /api/transcription/transcribe-with-subtitles  
  Transcription with SRT subtitle generation → { transcript, segments[], subtitleContent }

- POST /api/prosody/evaluate
  **Prosody scoring**: Multipart: audio, referenceText (optional), languageCode  
  → { overallScore, subScores{rhythm, intonation, stress, pacing, fluency}, diagnostics, feedback[], features, metadata }  
  Provides explainable scoring with numeric sub-scores, detailed metrics explaining the scores, and actionable learner hints.

- POST /api/prosody/features
  Extract raw prosody features (pitch, energy, timing) without scoring  
  → { duration, pitchContour[], energyContour[], wordTimings[], pauseRegions[] }

- GET /api/prosody/health  
  Prosody service status with capabilities.

- GET /api/pronunciation/health  
  Simple service status.

## Scoring

1. Normalize audio (ffmpeg) → mono 16 kHz WAV.
2. Transcribe via STT.
3. Normalize text (case/punctuation cleanup).
4. Align reference vs hypothesis (word sequence).
5. Compute word match ratio + insertion/deletion adjustments + confidence aggregation.
6. Return aggregate score + per-word details.

## Configuration (application.properties)

- **Security:**
  - `app.security.enabled` - Enable/disable API key authentication (default: true)
  - `app.security.api-keys` - Comma-separated list of valid API keys
- **Media:**
  - `media.ffmpeg.path` - Path to ffmpeg executable
  - `spring.servlet.multipart.max-file-size` / `max-request-size` - Upload size limits
- **Speech Recognition:**
  - `sphinx.acoustic-model` / `sphinx.dictionary` / `sphinx.language-model` - Sphinx models
  - `GOOGLE_APPLICATION_CREDENTIALS` or `spring.cloud.gcp.credentials.location` - GCP credentials
- **CORS:**
  - `app.cors.*` (CORS domains, methods, headers)

## Architecture

- Thin controllers: delegate to services (TranscriptionService, PronunciationService, SphinxService, SubtitleService).
- Strategy abstraction for transcription providers keeps evaluation logic agnostic.
- Temporary WAV handling ensures consistent input for engines.
- DTO layer isolates internal scoring model from API payloads for future versioning.
- SubtitleService generates SRT-formatted subtitles from transcription segments with timing information.

## Extending a New STT Provider

1. Implement a provider (e.g., WhisperTranscriptionProvider) with a transcribe(audioBytes) method returning a uniform internal transcript model.
2. Register it as a Spring bean.
3. Add selection logic (query param or config) or a new endpoint.

## Roadmap Ideas

- Whisper provider
- Persistence & analytics dashboard
- JWT auth (upgrade from API keys)

## Tech Stack

Kotlin, Spring Boot, Google Speech-to-Text, CMU Sphinx, ffmpeg, OpenAPI/Swagger, Gradle.

## License

MIT

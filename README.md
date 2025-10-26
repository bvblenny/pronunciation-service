# Pronunciation Service

Kotlin / Spring Boot service that normalizes user audio (ffmpeg), transcribes it via pluggable engines (Google Cloud Speech‑to‑Text and offline CMU Sphinx), performs forced alignment against a reference passage, and returns an overall pronunciation score plus word‑level timing & feedback. Designed to showcase clean architecture, extensibility, and production readiness.

## Highlights

- Multiple transcription engines: cloud accuracy (Google) + offline fallback (Sphinx) behind a common provider interface.
- Forced alignment & scoring: reference vs hypothesis comparison, word correctness, confidence proxy, timings.
- **Prosody (suprasegmental) scoring**: Explainable, extensible scoring for rhythm, intonation, stress, pacing, and fluency with diagnostic metrics and learner feedback.
- Media normalization: converts mixed input formats to mono 16 kHz WAV via ffmpeg for consistent STT quality.
- Clean layering: Controller → Media/Transcription → Alignment & Scoring → DTOs (OpenAPI documented).
- Extensible: drop in new STT (e.g., Whisper, Vosk, Azure) with minimal touch points.
- Production touches: health endpoint, OpenAPI/Swagger UI, environment/property configuration, CORS & upload limits.

## Quick Start

Prereqs: Kotlin/JVM (Java 21), Gradle, ffmpeg on PATH (or set media.ffmpeg.path), optional Google Cloud credentials.

```bash
./gradlew bootRun
curl http://localhost:8080/api/pronunciation/health
```

Docs: http://localhost:8080/swagger-ui.html  
OpenAPI: /v3/api-docs (JSON) | /v3/api-docs.yaml

## Key Endpoints

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
2. Transcribe (selected engine).  
3. Normalize text (case/punctuation cleanup).  
4. Align reference vs hypothesis (word sequence).  
5. Compute word match ratio + insertion/deletion adjustments + confidence aggregation.  
6. Return aggregate score + per-word details.

## Configuration (application.properties)

- media.ffmpeg.path
- sphinx.acoustic-model / sphinx.dictionary / sphinx.language-model
- GOOGLE_APPLICATION_CREDENTIALS or spring.cloud.gcp.credentials.location
- spring.servlet.multipart.max-file-size / max-request-size
- app.cors.* (CORS domains, methods, headers)

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
4. (Optional) Enhance scoring with engine-specific confidence or phoneme data.

## Roadmap Ideas

- Whisper or Vosk provider
- Persistence & analytics dashboard (e.g., Postgres + aggregated learning KPIs)
- JWT auth + rate limiting
- Phoneme/prosody scoring & CEFR heuristic tagging
- Docker image & Helm chart for one‑command deployment

## Tech Stack

Kotlin, Spring Boot, Google Speech-to-Text, CMU Sphinx, ffmpeg, OpenAPI/Swagger, Gradle.

## License

MIT

# Pronunciation Service

A Spring Boot service for evaluating pronunciation. It transcribes audio, compares it to a reference text, and returns a score with word-level feedback. It supports Google Cloud STT (primary) and CMU Sphinx (local) for quick recognition/alignment.

## Quick start

Prereqs: Java 21, Gradle, ffmpeg available on PATH (or set media.ffmpeg.path), optional Google Cloud credentials for STT.

```bash
./gradlew bootRun
# Health check
curl http://localhost:8080/api/pronunciation/health
```

## Configuration (application.properties)

- sphinx.acoustic-model, sphinx.dictionary, sphinx.language-model
- media.ffmpeg.path (e.g., C:/tools/ffmpeg/bin/ffmpeg.exe)
- spring.servlet.multipart.max-file-size, spring.servlet.multipart.max-request-size
- app.cors.*
- Google STT: set GOOGLE_APPLICATION_CREDENTIALS or spring.cloud.gcp.credentials.location

## API

- POST /api/pronunciation/evaluate-stt — Evaluate pronunciation (Google STT)
  - Inputs: audio (file), referenceText (string), languageCode (optional, default: en-US)
  - Behavior: converts media to mono 16 kHz WAV, transcribes with Google STT, compares to reference
  - Output: JSON { score, transcribedText, wordDetails[] }

- POST /api/transcription/transcribe — Transcribe media (Sphinx)
  - Inputs: file (audio/video), languageCode (optional)
  - Behavior: converts media to WAV, runs Sphinx recognition
  - Output: JSON { transcript, segments[] { text, startMs, endMs } }

- POST /api/pronunciation/evaluate-sphinx-recognition — Quick recognition (Sphinx)
  - Inputs: audio (file)
  - Output: JSON { transcript, words[] with timings }

- POST /api/pronunciation/evaluate-sphinx-alignment — Forced alignment (Sphinx)
  - Inputs: audio (file), referenceText (string)
  - Output: JSON with word timings and phoneme estimates

## License

MIT

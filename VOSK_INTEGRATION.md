# Vosk ASR Integration Guide

## Overview

This document describes the Vosk ASR integration added to the pronunciation service for enhanced subtitle generation capabilities.

## Why Vosk?

After researching ASR provider options for subtitle generation, **Vosk** was selected as the optimal solution because it:

- ✅ **Cost-Free**: Open-source with no API costs
- ✅ **Fast**: Optimized for real-time transcription
- ✅ **Accurate**: Superior accuracy compared to CMU Sphinx
- ✅ **Easy Integration**: Official Java bindings, minimal code changes
- ✅ **Offline**: No internet connection required
- ✅ **Production Ready**: Battle-tested in production environments
- ✅ **Multi-language**: Supports 20+ languages
- ✅ **Word Timestamps**: Essential for subtitle generation

## Architecture

The integration uses the **Strategy design pattern** for pluggable ASR providers:

```
┌─────────────────────────────────┐
│   TranscriptionController       │
│   - /transcribe                 │
│   - /transcribe-with-subtitles  │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│   TranscriptionService          │
│   - Audio normalization         │
│   - Strategy coordination       │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│  TranscriptionStrategyResolver  │
│  - Strategy discovery           │
│  - Runtime strategy selection   │
└────────────┬────────────────────┘
             │
      ┌──────┴──────────┬──────────────┐
      ▼                 ▼              ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐
│   Sphinx     │  │    Vosk      │  │   Google Cloud       │
│  Strategy    │  │   Strategy   │  │     Strategy         │
└──────┬───────┘  └──────┬───────┘  └──────┬───────────────┘
       │                 │                 │
       ▼                 ▼                 ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐
│SphinxService │  │ VoskService  │  │GoogleCloudSpeech     │
│              │  │              │  │     Service          │
└──────────────┘  └──────────────┘  └──────────────────────┘
       │                 │                 │
       └─────────────────┴─────────────────┘
                         │
                         ▼
                  ┌──────────────┐
                  │   Subtitle   │
                  │   Service    │
                  └──────────────┘
```

## Setup Instructions

### 1. Download Vosk Model

Choose a model from https://alphacephei.com/vosk/models:

**For English:**
- Small (40 MB): `vosk-model-small-en-us-0.15` - Good balance of speed and accuracy
- Full (1.8 GB): `vosk-model-en-us-0.22` - Best accuracy

**For Other Languages:**
- Check the models page for 20+ supported languages

```bash
# Example: Download small English model
wget https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
unzip vosk-model-small-en-us-0.15.zip
```

### 2. Configure Application

**Option A: Using application.properties**
```properties
# Path to Vosk model directory
vosk.model-path=/path/to/vosk-model-small-en-us-0.15

# Optional: Set as default provider
transcription.default-provider=vosk
```

**Option B: Using Environment Variables**
```bash
export VOSK_MODEL_PATH=/path/to/vosk-model-small-en-us-0.15
export TRANSCRIPTION_PROVIDER=vosk
```

### 3. Start Application

```bash
./gradlew bootRun
```

Check health endpoint to verify Vosk is loaded:
```bash
curl http://localhost:8080/actuator/health | jq .components.vosk
```

## API Usage

### Transcription with Provider Selection

**Using Sphinx (default, no model download needed):**
```bash
curl -X POST http://localhost:8080/api/transcription/transcribe \
  -F "file=@audio.mp3" \
  -F "languageCode=en-US" \
  -F "provider=sphinx"
```

**Using Vosk (requires model configuration):**
```bash
curl -X POST http://localhost:8080/api/transcription/transcribe \
  -F "file=@audio.mp3" \
  -F "languageCode=en-US" \
  -F "provider=vosk"
```

### Subtitle Generation

Generate SRT format subtitles from any audio/video file:

```bash
curl -X POST http://localhost:8080/api/transcription/transcribe-with-subtitles \
  -F "file=@video.mp4" \
  -F "languageCode=en-US" \
  -F "provider=vosk" \
  -o response.json
```

Extract subtitle content from response:
```bash
jq -r '.subtitleContent' response.json > subtitles.srt
```

### Response Format

```json
{
  "transcript": "Hello world this is a test",
  "segments": [
    {
      "text": "Hello",
      "startMs": 0,
      "endMs": 500
    },
    {
      "text": "world",
      "startMs": 500,
      "endMs": 1000
    }
  ],
  "subtitleContent": "1\n00:00:00,000 --> 00:00:00,500\nHello\n\n2\n00:00:00,500 --> 00:00:01,000\nworld\n\n..."
}
```

## Provider Comparison

| Feature | Sphinx | Vosk |
|---------|--------|------|
| **Accuracy** | Moderate (older models) | High (modern deep learning) |
| **Speed** | Fast | Fast |
| **Model Size** | ~100 MB (bundled) | 40 MB - 2 GB |
| **Setup** | Pre-configured | Model download required |
| **Cost** | Free | Free |
| **Languages** | English (bundled) | 20+ languages available |
| **Word Timestamps** | ✓ | ✓ |
| **Confidence Scores** | ✓ | ✓ |
| **Maintenance** | Limited updates | Actively maintained |

## Implementation Details

### Key Components Added

1. **VoskService** (`VoskService.kt`)
   - Manages Vosk model lifecycle
   - Handles audio recognition
   - Provides word-level timestamps
   - Graceful fallback if model not configured

2. **VoskHealthIndicator** (`VoskHealthIndicator.kt`)
   - Reports Vosk initialization status
   - Available via `/actuator/health`

3. **Configuration Properties**
   - `vosk.model-path`: Path to Vosk model directory
   - `vosk.sample-rate`: Audio sample rate (default: 16000)
   - `transcription.default-provider`: Default provider to use

### Code Changes

- **TranscriptionService**: Enhanced to support multiple providers
- **TranscriptionController**: Added `provider` parameter to endpoints
- **Tests**: Added unit tests, updated existing tests
- **Dependencies**: Added `com.alphacephei:vosk:0.3.45`

## Testing

### Unit Tests

```bash
./gradlew test
```

All tests pass without requiring Vosk model (graceful degradation).

### Manual Testing with Vosk

1. Configure Vosk model path
2. Start application
3. Test with sample audio:

```bash
curl -X POST http://localhost:8080/api/transcription/transcribe-with-subtitles \
  -F "file=@src/test/resources/sample-audio.wav" \
  -F "provider=vosk" \
  | jq '.subtitleContent'
```

## Troubleshooting

### Vosk Model Not Found
```
ERROR: Failed to initialize Vosk model at path: /invalid/path
```
**Solution**: Verify model path is correct and model files exist

### Provider Not Available
```
ERROR: Vosk provider is not available
```
**Solution**: Configure `vosk.model-path` in application.properties

### Memory Issues with Large Models
**Solution**: Use smaller model or increase JVM heap: `-Xmx2g`

## Performance Tips

1. **Choose Right Model Size**:
   - Small models (40-100 MB): Good for most use cases
   - Large models (1-2 GB): Use when maximum accuracy needed

2. **Model Loading**: 
   - Model loads once at startup (lazy initialization)
   - Subsequent requests are fast

3. **Audio Processing**:
   - Audio is automatically normalized to mono 16kHz WAV
   - ffmpeg handles format conversion

## Migration Guide

### From Sphinx-only to Vosk

**Before:**
```bash
curl -X POST /api/transcription/transcribe -F "file=@audio.mp3"
```

**After (using Vosk):**
```bash
# Option 1: Set default provider in config
transcription.default-provider=vosk

# Option 2: Specify provider per request
curl -X POST /api/transcription/transcribe \
  -F "file=@audio.mp3" \
  -F "provider=vosk"
```

### Backward Compatibility

✅ **100% backward compatible**
- Default provider remains Sphinx
- No changes needed for existing integrations
- Vosk is optional enhancement

## Future Enhancements

Potential improvements for future versions:

1. **Whisper Integration**: Add OpenAI Whisper as another provider option
2. **Model Auto-download**: Automatically download models on first use
3. **Multiple Languages**: Support language-specific model selection
4. **Streaming**: Add support for streaming/real-time transcription
5. **Custom Vocabularies**: Allow domain-specific vocabulary tuning

## References

- [Vosk Official Website](https://alphacephei.com/vosk/)
- [Vosk Models](https://alphacephei.com/vosk/models)
- [Vosk GitHub](https://github.com/alphacep/vosk-api)
- [SRT Subtitle Format](https://en.wikipedia.org/wiki/SubRip)

## License

This integration maintains the MIT license of the parent project.

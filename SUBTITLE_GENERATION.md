# Subtitle Generation Feature

## Overview

The pronunciation service now supports automatic subtitle generation from audio and video files. This feature extends the existing transcription capabilities to produce SRT (SubRip Subtitle) format files that can be used with media players and video editing software.

## How It Works

1. **Upload Media**: Send an audio or video file to the `/api/transcription/transcribe-with-subtitles` endpoint
2. **Transcription**: The service transcribes the media using CMU Sphinx with word-level timing information
3. **Subtitle Generation**: The timing data is converted to SRT format with proper sequencing and timestamps
4. **Response**: Returns the full transcript, timing segments, and ready-to-use SRT subtitle content

## API Endpoint

### POST /api/transcription/transcribe-with-subtitles

**Request:**
- Method: `POST`
- Content-Type: `multipart/form-data`
- Parameters:
  - `file` (required): Audio or video file
  - `languageCode` (optional, default: "en-US"): Language code for transcription

**Supported File Formats:**
- Audio: MP3, WAV, M4A, AAC, FLAC, OGG
- Video: MP4, MOV, WebM

**Response:**
```json
{
  "transcript": "Full transcribed text of the audio or video",
  "segments": [
    {
      "text": "Individual word or phrase",
      "startMs": 0,
      "endMs": 1500
    },
    {
      "text": "Another word or phrase",
      "startMs": 1500,
      "endMs": 3200
    }
  ],
  "subtitleContent": "1\n00:00:00,000 --> 00:00:01,500\nIndividual word or phrase\n\n2\n00:00:01,500 --> 00:00:03,200\nAnother word or phrase\n\n"
}
```

## SRT Format

The generated subtitles follow the standard SRT format:

```
1
00:00:00,000 --> 00:00:02,500
First subtitle line

2
00:00:02,500 --> 00:00:05,000
Second subtitle line

3
00:00:05,000 --> 00:00:08,500
Third subtitle line
```

Each subtitle entry contains:
1. Sequence number (starting from 1)
2. Timestamp range in format `HH:MM:SS,mmm --> HH:MM:SS,mmm`
3. Subtitle text
4. Blank line separator

## Usage Examples

### Using cURL

```bash
curl -X POST http://localhost:8080/api/transcription/transcribe-with-subtitles \
  -F "file=@/path/to/audio.mp3" \
  -F "languageCode=en-US" \
  -H "Accept: application/json"
```

### Saving Subtitle Content

To save the subtitle content as an SRT file:

```bash
curl -X POST http://localhost:8080/api/transcription/transcribe-with-subtitles \
  -F "file=@/path/to/video.mp4" \
  | jq -r '.subtitleContent' > subtitles.srt
```

### Using with Media Players

Once you have the SRT file, you can:
1. Save it with the same name as your video file (e.g., `video.mp4` and `video.srt`)
2. Most media players (VLC, MPV, etc.) will automatically load the subtitles
3. Or manually load the subtitle file in your player's subtitle menu

## Implementation Details

### Components

1. **SubtitleService** - Core service for generating SRT format
   - Converts transcription segments to SRT format
   - Handles timestamp formatting (HH:MM:SS,mmm)
   - Ensures proper sequence numbering

2. **SubtitleResponseDto** - Response data transfer object
   - Includes transcript, segments, and subtitle content
   - Extends existing transcription response

3. **TranscriptionController** - REST endpoint
   - Validates audio/video file uploads
   - Coordinates transcription and subtitle generation
   - Returns unified response

### Timing Accuracy

The timing accuracy of the subtitles depends on:
- Word-level timing from CMU Sphinx speech recognition
- Audio quality and clarity
- Speaking rate and pauses in the audio

For best results:
- Use clear, high-quality audio
- Avoid background noise
- Use standard speaking rates

## Future Enhancements

Potential improvements for the subtitle generation feature:

1. **Multiple Formats**: Support for additional subtitle formats (WebVTT, ASS, SSA)
2. **Real-time Streaming**: Live subtitle generation for streaming media
3. **Customization**: Configurable subtitle length, line breaks, and positioning
4. **Translation**: Multi-language subtitle generation
5. **Styling**: Support for formatted subtitles with colors and styles
6. **Direct File Download**: Endpoint to download subtitles directly as .srt file

## Testing

The SubtitleService includes comprehensive unit tests covering:
- Empty segment handling
- Single and multiple segment formatting
- Timestamp formatting with hours, minutes, seconds, and milliseconds
- Edge cases and precision handling

Run tests with:
```bash
./gradlew test --tests SubtitleServiceTest
```

## Troubleshooting

### Common Issues

1. **File too large**: Check `spring.servlet.multipart.max-file-size` setting (default: 25MB)
2. **Unsupported format**: Ensure file format is in the supported list
3. **FFmpeg not found**: Ensure FFmpeg is installed and in PATH or configure `media.ffmpeg.path`
4. **Poor subtitle quality**: Check audio quality and clarity; consider audio preprocessing

### Debug Mode

Enable debug logging to troubleshoot issues:
```properties
logging.level.de.demo.pronunciationservice=DEBUG
```

## License

This feature is part of the pronunciation-service project and follows the same MIT license.

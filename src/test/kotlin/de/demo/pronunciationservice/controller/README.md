# Large File Upload E2E Tests

## Overview

This directory contains end-to-end tests for the large file upload functionality of the pronunciation service. The tests verify that the service can handle audio file uploads of various sizes up to the configured limit.

## Test Coverage

### LargeFileUploadE2ETest (Integration Tests)

Full end-to-end integration tests that require the complete Spring Boot context:

1. **Small File Upload (1MB)** - Baseline test for basic functionality
2. **Medium File Upload (50MB)** - Tests mid-range file handling
3. **Large File Upload (200MB)** - Tests large file processing
4. **Near-Limit Upload (400MB)** - Tests files approaching the 500MB limit
5. **Multiple Endpoints** - Tests all file upload endpoints:
   - `/api/pronunciation/evaluate-stt`
   - `/api/transcription/transcribe`
   - `/api/pronunciation/analyze-detailed`
   - `/api/pronunciation/evaluate-sphinx-recognition`
   - `/api/pronunciation/evaluate-sphinx-alignment`
6. **Edge Cases**:
   - Empty file rejection
   - Different file formats (WAV, MP3)

### LargeFileUploadControllerTest (Unit Tests)

Controller-level unit tests with mocked services (does not require Sphinx dependencies):

1. **Small File Upload (1MB)** - Validates controller accepts and processes small files
2. **Medium File Upload (50MB)** - Validates medium file handling
3. **Large File Upload (200MB)** - Validates large file processing
4. **Near-Limit Upload (400MB)** - Validates files near 500MB limit
5. **Empty File Rejection** - Verifies proper error handling
6. **Default Parameters** - Tests default language code handling
7. **Sequential Uploads** - Tests multiple file uploads in sequence

## File Size Limits

Current configuration (from `application.properties`):
- `spring.servlet.multipart.max-file-size=500MB`
- `spring.servlet.multipart.max-request-size=500MB`

## Test Implementation

### Integration Tests (LargeFileUploadE2ETest)
- **SpringBootTest** with **AutoConfigureMockMvc** for full integration testing
- **MockMultipartFile** to simulate file uploads
- Java Sound API to generate valid WAV files programmatically
- Various file sizes to test upload capacity
- Requires all dependencies including Sphinx

### Unit Tests (LargeFileUploadControllerTest)
- **WebMvcTest** for lightweight controller testing
- **MockBean** to mock service dependencies
- **Mockito** for service behavior stubbing
- Same WAV file generation utility
- Does not require Sphinx dependencies

### WAV File Generation

The helper method `generateWavFile(targetSizeMB: Int)` creates valid WAV audio files:
- Format: Mono, 16kHz sample rate, 16-bit PCM
- Content: Silent audio (zeros)
- Size: Configurable via parameter

## Running the Tests

```bash
# Run all tests
./gradlew test

# Run only controller unit tests (works without Sphinx dependencies)
./gradlew test --tests LargeFileUploadControllerTest

# Run only e2e integration tests (requires Sphinx dependencies)
./gradlew test --tests LargeFileUploadE2ETest

# Run a specific test
./gradlew test --tests LargeFileUploadControllerTest.testLargeFileUpload
```

**Note:** The controller unit tests (`LargeFileUploadControllerTest`) will run successfully even without the Sphinx dependencies, as they mock all service dependencies. The integration tests (`LargeFileUploadE2ETest`) require the full application context and all dependencies.

## Performance Considerations

- Large file tests (200MB+) may take several minutes to run due to:
  - File generation in memory
  - Network transfer simulation
  - Audio processing by the service
- Consider running these tests separately from fast unit tests
- Memory usage can be significant for 400MB+ files

## Known Limitations

1. The tests currently depend on the availability of:
   - CMU Sphinx dependencies (5prealpha-SNAPSHOT)
   - FFmpeg for audio processing
   - Google Cloud Speech-to-Text (optional)

2. If Sphinx dependencies are unavailable, tests will fail during Spring context initialization.

3. The MP3 test uses a WAV file with MP3 extension as a simplified test. For production-grade testing, use properly encoded MP3 files.

## Future Improvements

1. Add tests for files exceeding the limit (expected to fail with 413 Payload Too Large)
2. Add performance benchmarks for upload and processing times
3. Add tests for concurrent large file uploads
4. Add tests for different video formats (MP4, WebM, MOV)
5. Add cleanup verification to ensure temporary files are deleted
6. Consider using test profiles to skip resource-intensive tests in CI

## Troubleshooting

### Build Failures

If you encounter "Could not find edu.cmu.sphinx:sphinx4-core:5prealpha-SNAPSHOT":
- This is a known issue with the Sphinx SNAPSHOT dependency
- The dependency may not be available in public repositories
- Contact the repository maintainer for resolution

### Memory Issues

If tests fail with OutOfMemoryError:
- Increase JVM heap size: `./gradlew test -Xmx2g`
- Reduce file sizes in tests
- Run tests individually rather than all at once

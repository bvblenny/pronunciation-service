# Pronunciation Scoring Service

A Spring Boot service that uses Google Cloud Speech-to-Text API to analyze audio and provide a pronunciation score by comparing it to a reference text.

## Features

- Audio pronunciation analysis using Google Cloud Speech-to-Text API
- Scoring based on text similarity and word confidence
- Detailed word-level feedback
- REST API for easy integration

## Prerequisites

- Java 21 or higher
- Gradle
- Google Cloud account with Speech-to-Text API enabled
- Google Cloud service account with appropriate permissions

## Setup

### 1. Clone the repository

```bash
git clone https://github.com/yourusername/pronunciation-service.git
cd pronunciation-service
```

### 2. Configure Google Cloud credentials

There are two ways to configure Google Cloud credentials:

#### Option 1: Using a service account key file

1. Create a service account in the Google Cloud Console
2. Download the service account key file (JSON)
3. Uncomment and update the following line in `application.properties`:

```properties
spring.cloud.gcp.credentials.location=file:/path/to/service-account-key.json
```

#### Option 2: Using environment variables (recommended for production)

Set the `GOOGLE_APPLICATION_CREDENTIALS` environment variable to the path of your service account key file:

```bash
# Linux/macOS
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json

# Windows
set GOOGLE_APPLICATION_CREDENTIALS=C:\path\to\service-account-key.json
```

### 3. Configure your Google Cloud project ID

Update the project ID in `application.properties` or set the `GCP_PROJECT_ID` environment variable:

```properties
spring.cloud.gcp.project-id=${GCP_PROJECT_ID:your-project-id}
```

### 4. Build and run the application

```bash
./gradlew bootRun
```

The service will start on port 8080 by default.

## API Usage

### Score Pronunciation

**Endpoint:** `POST /api/pronunciation/score`

**Content-Type:** `multipart/form-data`

**Parameters:**
- `audio` (file): Audio file containing speech to be analyzed (WAV format, 16kHz sample rate recommended)
- `referenceText` (string): The expected text that should be pronounced in the audio
- `languageCode` (string, optional): The language code (e.g., "en-US", "de-DE"). Defaults to "en-US"

**Example Request:**

```bash
curl -X POST http://localhost:8080/api/pronunciation/score \
  -F "audio=@/path/to/audio.wav" \
  -F "referenceText=Hello world" \
  -F "languageCode=en-US"
```

**Example Response:**

```json
{
  "score": 0.85,
  "transcribedText": "hello world",
  "wordDetails": [
    {
      "word": "hello",
      "confidence": 0.92,
      "isCorrect": true,
      "expectedWord": "hello"
    },
    {
      "word": "world",
      "confidence": 0.88,
      "isCorrect": true,
      "expectedWord": "world"
    }
  ]
}
```

### Health Check

**Endpoint:** `GET /api/pronunciation/health`

**Example Request:**

```bash
curl http://localhost:8080/api/pronunciation/health
```

**Example Response:**

```json
{
  "status": "UP",
  "service": "pronunciation-scoring"
}
```

## Audio Format Recommendations

For best results with the Google Cloud Speech-to-Text API:

- Use WAV or FLAC format
- 16-bit PCM encoding
- 16kHz sample rate
- Mono channel
- Clear audio with minimal background noise

## Scoring Methodology

The pronunciation score is calculated using:

1. **Text Similarity (70%)**: How closely the transcribed text matches the reference text, using Levenshtein distance
2. **Word Confidence (30%)**: The average confidence score from the Speech-to-Text API for each word

The final score ranges from 0.0 (poor) to 1.0 (excellent).

## License

[MIT License](LICENSE)

# Pronunciation Service

## Running the Backend (Spring Boot)

```bash
./mvnw spring-boot:run
```

## Running the Frontend (Angular)

```bash
cd frontend
npm install
ng serve
```

The Angular app will be available at [http://localhost:4200](http://localhost:4200) and will communicate with the backend at [http://localhost:8080](http://localhost:8080).

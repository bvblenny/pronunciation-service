package de.demo.pronunciationservice.service

import de.demo.pronunciationservice.model.TranscriptSegmentDto
import de.demo.pronunciationservice.model.TranscriptionResponseDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.util.*

@Service
class TranscriptionService(
    private val sphinxService: SphinxService,
    private val voskService: VoskService,
    private val googleCloudSpeechService: GoogleCloudSpeechService,
    @Value("\${media.ffmpeg.path:ffmpeg}") private val ffmpegPath: String,
    @Value("\${transcription.default-provider:sphinx}") private val defaultProvider: String
) {

    companion object {
        private const val SECONDS_TO_MILLIS = 1000
    }

    private val supportedVideoTypes = setOf(
        MediaType.valueOf("video/mp4"),
        MediaType.valueOf("video/quicktime"),
        MediaType.valueOf("video/webm")
    )

    /**
     * Transcribes audio using the default provider configured in application.properties.
     */
    fun transcribe(file: MultipartFile, languageCode: String = "en-US"): TranscriptionResponseDto {
        return transcribe(file, languageCode, defaultProvider)
    }

    /**
     * Transcribes audio using the specified provider.
     * 
     * @param provider The ASR provider to use ("sphinx", "vosk", or "google")
     */
    fun transcribe(file: MultipartFile, languageCode: String = "en-US", provider: String): TranscriptionResponseDto {
        if (file.isEmpty) throw IllegalArgumentException("File is required")

        val wavBytes = toWavBytes(file)

        val recognized = when (provider.lowercase()) {
            "vosk" -> {
                if (!voskService.isAvailable()) {
                    throw IllegalStateException("Vosk provider is not available. Please configure vosk.model-path.")
                }
                voskService.recognize(wavBytes)
            }
            "google", "google-cloud", "gcp" -> {
                if (!googleCloudSpeechService.isAvailable()) {
                    throw IllegalStateException("Google Cloud Speech provider is not available. Please configure Google Cloud credentials.")
                }
                googleCloudSpeechService.recognize(wavBytes, languageCode)
            }
            "sphinx" -> sphinxService.recognize(wavBytes)
            else -> throw IllegalArgumentException("Unknown transcription provider: $provider. Use 'sphinx', 'vosk', or 'google'.")
        }

        val segments = recognized.words.map {
            TranscriptSegmentDto(
                text = it.word,
                startMs = (it.startTime * SECONDS_TO_MILLIS).toLong(),
                endMs = (it.endTime * SECONDS_TO_MILLIS).toLong()
            )
        }
        return TranscriptionResponseDto(
            transcript = recognized.transcript.trim(),
            segments = segments
        )
    }

    /**
     * Convert an uploaded media file (audio/video) to mono 16k WAV bytes if needed; if already WAV, returns original bytes.
     */
    fun toWavBytes(file: MultipartFile): ByteArray {
        val contentType = file.contentType?.let { MediaType.parseMediaType(it) }
        val originalName = file.originalFilename ?: "upload"

        val needsExtraction = when {
            contentType == null -> shouldExtractByExtension(originalName)
            contentType.type == "audio" && !contentType.subtype.equals("wav", ignoreCase = true) -> true
            contentType.type == "video" -> true
            supportedVideoTypes.contains(contentType) -> true
            else -> false
        }

        return if (needsExtraction) {
            val inFile = toTempFile(file)
            try {
                extractWavWithFfmpeg(inFile)
            } finally {
                inFile.delete()
            }
        } else {
            file.bytes
        }
    }

    private fun toTempFile(file: MultipartFile): File {
        val suffix = file.originalFilename?.let { name ->
            val dot = name.lastIndexOf('.')
            if (dot != -1) name.substring(dot) else ""
        } ?: ""
        val temp = Files.createTempFile("upload_", suffix).toFile()
        temp.deleteOnExit()
        file.inputStream.use { input ->
            temp.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return temp
    }

    private fun extractWavWithFfmpeg(inputFile: File): ByteArray {
        val outFile = Files.createTempFile("audio_", ".wav").toFile()
        outFile.deleteOnExit()
        try {
            val cmd = arrayOf(
                ffmpegPath,
                "-y",
                "-i", inputFile.absolutePath,
                "-ac", "1",
                "-ar", "16000",
                "-f", "wav",
                outFile.absolutePath
            )
            val proc = ProcessBuilder(*cmd)
                .redirectErrorStream(true)
                .start()
            proc.inputStream.bufferedReader().use { it.readText() }
            val exit = proc.waitFor()
            if (exit != 0 || !outFile.exists()) {
                throw IllegalStateException("Failed to convert media to WAV")
            }
            return outFile.readBytes()
        } finally {
            outFile.delete()
        }
    }

    private fun shouldExtractByExtension(name: String): Boolean {
        val ext = name.lowercase(Locale.getDefault()).substringAfterLast('.', "")
        return when (ext) {
            "wav" -> false
            "mp3", "m4a", "ogg", "webm", "mp4", "mov", "aac", "flac" -> true
            else -> true
        }
    }
}

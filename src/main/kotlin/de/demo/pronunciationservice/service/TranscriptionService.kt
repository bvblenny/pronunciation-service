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
    @Value("\${media.ffmpeg.path:ffmpeg}") private val ffmpegPath: String
) {

    private val supportedVideoTypes = setOf(
        MediaType.valueOf("video/mp4"),
        MediaType.valueOf("video/quicktime"),
        MediaType.valueOf("video/webm")
    )

    fun transcribe(file: MultipartFile, languageCode: String = "en-US"): TranscriptionResponseDto {
        if (file.isEmpty) throw IllegalArgumentException("File is required")

        val wavBytes = toWavBytes(file)

        val recognized = sphinxService.recognize(wavBytes)
        val segments = recognized.words.map {
            TranscriptSegmentDto(
                text = it.word,
                startMs = (it.startTime * 1000).toLong(),
                endMs = (it.endTime * 1000).toLong()
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
        // Mark for deletion on JVM exit as a safety net
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
        // Mark for deletion on JVM exit as a safety net
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
            // Always clean up the output file
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

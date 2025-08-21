package de.demo.pronunciationservice.health

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class FfmpegHealthIndicator(
    @Value("\${media.ffmpeg.path:ffmpeg}") private val ffmpegPath: String
) : HealthIndicator {

    override fun health(): Health {
        return try {
            val process = ProcessBuilder(ffmpegPath, "-version")
                .redirectErrorStream(true)
                .start()
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                Health.down()
                    .withDetail("path", ffmpegPath)
                    .withDetail("error", "ffmpeg not responding")
                    .build()
            } else {
                val exit = process.exitValue()
                if (exit == 0) {
                    Health.up().withDetail("path", ffmpegPath).build()
                } else {
                    Health.down()
                        .withDetail("path", ffmpegPath)
                        .withDetail("exitCode", exit)
                        .build()
                }
            }
        } catch (ex: Exception) {
            Health.down(ex).withDetail("path", ffmpegPath).build()
        }
    }
}


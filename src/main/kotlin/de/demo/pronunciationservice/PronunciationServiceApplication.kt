package de.demo.pronunciationservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PronunciationServiceApplication

fun main(args: Array<String>) {
    runApplication<PronunciationServiceApplication>(*args)
}

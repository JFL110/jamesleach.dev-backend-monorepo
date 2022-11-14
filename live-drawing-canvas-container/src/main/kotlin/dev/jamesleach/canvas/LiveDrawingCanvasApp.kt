package dev.jamesleach.canvas

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["dev.jamesleach"])
@EnableScheduling
class LiveDrawingCanvasApp

fun main(args: Array<String>) {
    runApplication<LiveDrawingCanvasApp>(*args)
}
package dev.jamesleach

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["dev.jamesleach"])
class CommonLibTestApp

fun main(args: Array<String>) {
    runApplication<CommonLibTestApp>(*args)
}
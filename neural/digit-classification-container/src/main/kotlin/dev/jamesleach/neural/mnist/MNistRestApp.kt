package dev.jamesleach.neural.mnist

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["dev.jamesleach"])
class MNistRestApp

fun main(args: Array<String>) {
    runApplication<MNistRestApp>(*args)
}
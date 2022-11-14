package dev.jamesleach.location

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["dev.jamesleach"])
class LocationContainerApp

fun main(args: Array<String>) {
    runApplication<LocationContainerApp>(*args)
}
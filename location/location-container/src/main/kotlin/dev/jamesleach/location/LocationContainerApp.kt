package dev.jamesleach.location

import dev.jamesleach.location.map.MapLocationDigester
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import kotlin.system.exitProcess

@SpringBootApplication(scanBasePackages = ["dev.jamesleach"])
class LocationContainerApp(
    private val mapLocationDigester: MapLocationDigester,
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        mapLocationDigester.digestLocations()
    }
}


fun main(args: Array<String>) {
    SpringApplication.run(LocationContainerApp::class.java, *args)
    exitProcess(0)
}
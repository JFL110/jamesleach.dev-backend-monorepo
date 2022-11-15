package dev.jamesleach.neural.mnist

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Profile
import java.nio.file.Paths
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.io.path.absolutePathString

/**
 * Run [MNistNetworkFactory] as a command line application.
 */
@Profile("mnist-cli")
@SpringBootApplication(scanBasePackages = ["dev.jamesleach"])
class MNistNetworkFactoryCli(private val factory: MNistNetworkFactory) : CommandLineRunner {
    override fun run(vararg args: String) {
        factory.createNetwork(
            "feedforward-current",
            Duration.of(20, ChronoUnit.MINUTES),
            Paths.get("./neural/digit-classification-trainer-cli/src/main/resources/mnist_train.csv"),
            Paths.get("./neural/digit-classification-trainer-cli/src/main/resources/mnist_test.csv")
        )
    }
}

fun main(args: Array<String>) {
    SpringApplicationBuilder(MNistNetworkFactoryCli::class.java)
        .profiles("mnist-cli")
        .web(WebApplicationType.NONE)
        .run(*args)
}
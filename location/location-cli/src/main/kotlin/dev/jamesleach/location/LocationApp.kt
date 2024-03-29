package dev.jamesleach.location

import dev.jamesleach.location.googletakeout.TakeoutExtractService
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.net.URL
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.inputStream
import kotlin.system.exitProcess

@SpringBootApplication(scanBasePackages = ["dev.jamesleach"])
class LocationApp(
    private val takeoutExtractService: TakeoutExtractService
) : CommandLineRunner {

    companion object {
        private val log = LoggerFactory.getLogger(LocationApp::class.java)
    }

    override fun run(vararg args: String) {
        log.info("Starting CLI")

        val options = Options()
            .addOption("help", "Display help options")
            .addOption("name", true, "Name of the extract")
            .addOption("path", true, "Path to the local Google Takeout zip")
            .addOption("url", true, "URL to the Google Takeout zip")

        val commandLineArguments = DefaultParser().parse(options, args)

        if (commandLineArguments.hasOption("help")) {
            HelpFormatter().printHelp("ant", options)
            return
        }

        if (commandLineArguments.hasOption("url")) {
            takeoutExtractService.extract(
                getName(commandLineArguments),
                URL(commandLineArguments.getOptionValue("url")).openConnection().getInputStream()
            )
        }

        if (commandLineArguments.hasOption("path")) {
            println(Paths.get(commandLineArguments.getOptionValue("path")).absolutePathString())
            takeoutExtractService.extract(
                getName(commandLineArguments),
                Paths.get(commandLineArguments.getOptionValue("path")).inputStream()
            )
        }
    }

    private fun getName(commandLineArguments: CommandLine): String {
        val extractName = commandLineArguments.getOptionValue("name", "")
        if (extractName.isNotEmpty()) {
            return extractName
        }
        println("Argument 'name' is required")
        exitProcess(-1)
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(LocationApp::class.java, *args)
    exitProcess(0)
}
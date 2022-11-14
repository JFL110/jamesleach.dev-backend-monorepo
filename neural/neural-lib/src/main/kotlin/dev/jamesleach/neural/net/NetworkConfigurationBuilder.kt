package dev.jamesleach.neural.net

import dev.jamesleach.neural.data.DataShape
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration

interface NetworkConfigurationBuilder {
    fun build(
        commonNetConfig: CommonNetSpecification,
        dataShape: DataShape
    ): ComputationGraphConfiguration
}
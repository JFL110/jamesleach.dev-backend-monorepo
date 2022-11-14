package dev.jamesleach.s3

import com.amazonaws.services.s3.AmazonS3
import org.mockito.kotlin.mock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MockS3Configuration {
    @Bean
    fun mockS3Client() = mock<AmazonS3>()
}
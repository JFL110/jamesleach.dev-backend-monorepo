package dev.jamesleach.s3

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile

@Configuration
@ConfigurationProperties
@Profile("!test")
class S3Configuration(
    @Value("\${s3.accessKey:}") val accessKey: String?,
    @Value("\${s3.secretKey:}") val secretKey: String?,
    @Value("\${s3.region}") val region: String,
) {
    @Bean
    @Lazy
    fun s3Client(): AmazonS3 = AmazonS3ClientBuilder.standard()
        .withCredentials(
            if (accessKey.isNullOrBlank() && secretKey.isNullOrBlank())
                DefaultAWSCredentialsProviderChain.getInstance()
            else
                AWSStaticCredentialsProvider(BasicAWSCredentials(accessKey, secretKey))
        )
        .withRegion(region)
        .build()
}
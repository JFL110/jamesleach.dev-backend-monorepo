package dev.jamesleach.web

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.client.HttpClientErrorException

private val logger = LoggerFactory.getLogger(ExceptionMapper::class.java)

/**
 * Exception -> JsonErrorResponse
 */
@ControllerAdvice
class ExceptionMapper {
    @ExceptionHandler(Exception::class)
    @ResponseBody
    fun global(e: Exception): ResponseEntity<*> {
        logger.error(e.localizedMessage)
        logger.error(e.stackTraceToString())

        return ResponseEntity(
            JsonErrorResponse(e.message),
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }

    @ExceptionHandler(HttpClientErrorException::class)
    @ResponseBody
    fun handleException(e: HttpClientErrorException): ResponseEntity<*> {
        logger.error(e.localizedMessage)
        return ResponseEntity(
            JsonErrorResponse(e.message),
            e.statusCode
        )
    }

    @ExceptionHandler(value = [HttpMessageNotReadableException::class])
    @ResponseBody
    fun onException(exception: HttpMessageNotReadableException): ResponseEntity<*> {
        logger.error(exception.localizedMessage)
        val parameterName = (exception.rootCause as MissingKotlinParameterException).parameter.name // id
        val parameterType = (exception.rootCause as MissingKotlinParameterException).parameter.type // ObjectId
        val fieldName = (exception.rootCause as MissingKotlinParameterException).path[0].fieldName // in User part
        return ResponseEntity(
            // TODO standard validation format
            "there is a missing parameter in your request, check your request body." +
                    " detail : missing $parameterName ($parameterType) type in $fieldName",
            HttpStatus.BAD_REQUEST
        )
    }
}
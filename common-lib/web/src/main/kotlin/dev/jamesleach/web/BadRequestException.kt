package dev.jamesleach.web

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

/**
 * Exception for a 400.
 */
class BadRequestException(message: String) : HttpClientErrorException(HttpStatus.BAD_REQUEST, message)
package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@ControllerAdvice
class RestResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {

    @ResponseBody
    @ExceptionHandler(value = [InvalidUrlException::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun invalidUrls(ex: InvalidUrlException) = ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)


    @ResponseBody
    @ExceptionHandler(value = [InformationNotFound::class ,RedirectionNotFound::class])
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun informationNotFound(ex: InformationNotFound) = ErrorMessage(HttpStatus.NOT_FOUND.value(), ex.message)


    @ExceptionHandler(value = [CalculandoException::class])
    fun calculandoException(ex: CalculandoException) = ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .header("Retry-After", "10")
        .body(ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message))

    @ResponseBody
    @ExceptionHandler(value = [InvalidExist::class])
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun invalidUrls(ex: InvalidExist) = ErrorMessage(HttpStatus.FORBIDDEN.value(), ex.message)

}

data class ErrorMessage(
    val statusCode: Int,
    val message: String?,
    val timestamp: String = DateTimeFormatter.ISO_DATE_TIME.format(OffsetDateTime.now())
)

package com.coinexchange.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ExceptionControllerAdvice {

    @ExceptionHandler(BaseException.class)
    ResponseEntity<ExceptionResponse> handleBaseException(HttpServletRequest request, BaseException e) {
        BaseExceptionType type = e.exceptionType();
        return buildResponse(request, type.httpStatus(), type.errorMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ExceptionResponse> handleMessageNotReadable(HttpServletRequest request, HttpMessageNotReadableException e) {
        log.info("JSON 파싱 에러. URI: {}, 내용: {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ExceptionResponse("BAD_REQUEST", "요청 형식이 올바르지 않습니다. : \n" + e.getMessage()));
    }

    @ExceptionHandler(HttpClientErrorException.BadRequest.class)
    ResponseEntity<ExceptionResponse> handleBadRequest(HttpServletRequest request, HttpClientErrorException.BadRequest e) {
        return buildResponse(request, HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(HttpClientErrorException.NotFound.class)
    ResponseEntity<ExceptionResponse> handleNotFound(HttpServletRequest request, HttpClientErrorException.NotFound e) {
        return buildResponse(request, HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationExceptionResponse> handleValidationException(MethodArgumentNotValidException e) {

        List<String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());

        var errorResponse = new ValidationExceptionResponse(
                BAD_REQUEST.value(),
                "검증 오류",
                errors
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    private String formatFieldError(FieldError fieldError) {
        return String.format("[%s] %s", fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ResponseEntity<ExceptionResponse> buildResponse(HttpServletRequest request, HttpStatus status, String message) {
        log.info("잘못된 요청이 들어왔습니다. URI: {}, 내용: {}", request.getRequestURI(), message);
        return ResponseEntity.status(status)
                .body(new ExceptionResponse(status.toString(), message));
    }

}


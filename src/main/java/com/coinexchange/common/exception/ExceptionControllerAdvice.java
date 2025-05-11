package com.coinexchange.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ExceptionControllerAdvice {

    @ExceptionHandler(BaseException.class)
    ResponseEntity<ExceptionResponse> handleException(HttpServletRequest request, BaseException e) {
        BaseExceptionType type = e.exceptionType();
        log.info("잘못된 요청이 들어왔습니다. URI: {},  내용:  {}",
                request.getRequestURI(), type.errorMessage());
        return ResponseEntity.status(type.httpStatus())
                .body(new ExceptionResponse(type.httpStatus().toString(), type.errorMessage()));
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

}


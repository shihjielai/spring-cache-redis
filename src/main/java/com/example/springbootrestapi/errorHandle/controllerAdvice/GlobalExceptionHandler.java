package com.example.springbootrestapi.errorHandle.controllerAdvice;

import com.example.springbootrestapi.errorHandle.exception.ErrorCodeException;
import com.example.springbootrestapi.errorHandle.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ErrorCodeException.class)
    protected ResponseEntity<Object> runtimeExceptionHandler(ErrorCodeException ex) {
        ErrorResponse response = new ErrorResponse();
        response.setTimestamp(LocalDateTime.now());
        response.setStatus(ex.getError().getHttpStatusCode());
        response.setStatusCode(ex.getError().getHttpStatusCode().value());
        response.setErrorCode(ex.getError().getErrorCode());
        response.setMessage(ex.getError().getMessage());
        return new ResponseEntity<>(response, response.getStatus());
    }
}

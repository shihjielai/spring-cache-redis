package com.example.springbootrestapi.errorHandle.exception;

import com.example.springbootrestapi.errorHandle.enums.ErrorCodeEnum;
import lombok.Getter;

public class ErrorCodeException extends RuntimeException {

    @Getter
    private ErrorCodeEnum error;

    public ErrorCodeException(String message, ErrorCodeEnum error) {
        super(message);
        this.error = error;
    }
}

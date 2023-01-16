package com.example.springbootrestapi.errorHandle.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public enum ErrorCodeEnum {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, -10000, "查無使用者");

    @Getter
    private final HttpStatus httpStatusCode;

    @Getter
    private final Integer errorCode;

    @Getter
    private final String message;

    ErrorCodeEnum(HttpStatus httpStatusCode, Integer errorCode, String message) {
        this.httpStatusCode = httpStatusCode;
        this.errorCode = errorCode;
        this.message = message;
    }
}

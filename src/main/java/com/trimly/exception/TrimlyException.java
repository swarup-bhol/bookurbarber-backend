package com.trimly.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class TrimlyException extends RuntimeException {
    private final HttpStatus status;

    public TrimlyException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public static TrimlyException notFound(String msg)   { return new TrimlyException(msg, HttpStatus.NOT_FOUND); }
    public static TrimlyException badRequest(String msg) { return new TrimlyException(msg, HttpStatus.BAD_REQUEST); }
    public static TrimlyException conflict(String msg)   { return new TrimlyException(msg, HttpStatus.CONFLICT); }
    public static TrimlyException forbidden(String msg)  { return new TrimlyException(msg, HttpStatus.FORBIDDEN); }
    public static TrimlyException unauth(String msg)     { return new TrimlyException(msg, HttpStatus.UNAUTHORIZED); }
    public static TrimlyException rateLimit(String msg)  { return new TrimlyException(msg, HttpStatus.TOO_MANY_REQUESTS); }
}

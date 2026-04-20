package com.smartcampus.exception;

/**
 * Part 5.1b — Thrown when a POST /sensors references a roomId that doesn't exist.
 * Mapped to HTTP 422 Unprocessable Entity by LinkedResourceNotFoundExceptionMapper.
 */
public class LinkedResourceNotFoundException extends RuntimeException {
    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}

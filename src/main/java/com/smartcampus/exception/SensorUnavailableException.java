package com.smartcampus.exception;

/**
 * Part 5.1c — Thrown when a POST /readings is attempted on a MAINTENANCE sensor.
 * Mapped to HTTP 403 Forbidden by SensorUnavailableExceptionMapper.
 */
public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException(String message) {
        super(message);
    }
}

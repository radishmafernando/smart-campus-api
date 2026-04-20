package com.smartcampus.mapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Part 5.2 — Global Safety Net.
 *
 * Catches ALL uncaught Throwables. For JAX-RS built-in exceptions (404, 405,
 * 415 etc.) it returns a clean JSON body with the correct status code.
 * For truly unexpected errors it logs the stack trace server-side and returns
 * a generic 500 — the client NEVER sees a Java stack trace.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {

        // JAX-RS built-in exceptions (NotFoundException=404, NotAllowedException=405, etc.)
        // These have a correct HTTP status already — return it as clean JSON instead of 500.
        if (ex instanceof WebApplicationException) {
            int status = ((WebApplicationException) ex).getResponse().getStatus();
            Response.Status rs = Response.Status.fromStatusCode(status);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status",  "error");
            body.put("code",    status);
            body.put("error",   rs != null ? rs.getReasonPhrase() : "HTTP Error");
            body.put("message", "The requested resource or method was not found.");

            return Response.status(status)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(body)
                    .build();
        }

        // Truly unexpected errors — log full trace server-side, send nothing sensitive to client
        LOGGER.log(Level.SEVERE,
                "Unhandled exception: " + ex.getClass().getName() + " — " + ex.getMessage(), ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",  "error");
        body.put("code",    500);
        body.put("error",   "Internal Server Error");
        body.put("message", "An unexpected error occurred. Please contact the system administrator.");

        return Response.status(500)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}

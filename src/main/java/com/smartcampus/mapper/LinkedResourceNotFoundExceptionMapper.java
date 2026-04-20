package com.smartcampus.mapper;

import com.smartcampus.exception.LinkedResourceNotFoundException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Part 5.1b — Maps LinkedResourceNotFoundException to HTTP 422 Unprocessable Entity.
 *
 * 422 is used (not 404) because the request itself is syntactically valid JSON,
 * but the semantic content (the roomId reference) cannot be processed because
 * the linked resource does not exist. The problem is inside the payload, not
 * a missing URL — hence 422 over 404.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",  "error");
        body.put("code",    422);
        body.put("error",   "Unprocessable Entity");
        body.put("message", ex.getMessage());
        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}

package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Part 5.3 — API Request & Response Logging Filter.
 *
 * Implements BOTH ContainerRequestFilter and ContainerResponseFilter in one class,
 * acting as a cross-cutting interceptor for every request/response cycle.
 *
 * This approach (filters vs. manual Logger.info() in each method) is superior
 * because:
 *  - It's DRY: one place to update logging format for all endpoints
 *  - It's consistent: no endpoint can accidentally skip logging
 *  - It separates concerns: resource classes focus on business logic only
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Fired BEFORE the request reaches a resource method.
     * Logs the HTTP method and full request URI.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format(
                "--> Incoming Request  | Method: %-6s | URI: %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()
        ));
    }

    /**
     * Fired AFTER the resource method returns a response.
     * Logs the HTTP status code alongside the original request info.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format(
                "<-- Outgoing Response | Status: %-3d | URI: %s",
                responseContext.getStatus(),
                requestContext.getUriInfo().getRequestUri()
        ));
    }
}

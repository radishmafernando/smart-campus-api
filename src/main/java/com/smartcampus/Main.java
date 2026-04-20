package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;

public class Main {

    // Grizzly uses this URI as the application mount point.
    // @ApplicationPath("/api/v1") in SmartCampusApplication satisfies the spec requirement;
    // with Grizzly standalone the BASE_URI here is what actually controls the path.
    public static final String BASE_URI = "http://localhost:8080/api/v1/";

    public static void main(String[] args) throws IOException {
        ResourceConfig config = ResourceConfig.forApplication(new SmartCampusApplication());

        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(
                URI.create(BASE_URI),
                config
        );

        System.out.println("=====================================================");
        System.out.println("  Smart Campus API is running!");
        System.out.println("  Base URL  : http://localhost:8080/api/v1");
        System.out.println("  Discovery : http://localhost:8080/api/v1");
        System.out.println("  Rooms     : http://localhost:8080/api/v1/rooms");
        System.out.println("  Sensors   : http://localhost:8080/api/v1/sensors");
        System.out.println("  Press ENTER to stop the server...");
        System.out.println("=====================================================");

        System.in.read();
        server.shutdownNow();
        System.out.println("Server stopped.");
    }
}
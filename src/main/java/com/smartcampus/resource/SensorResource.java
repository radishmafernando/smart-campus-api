package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Part 3 — Sensor Operations
 *
 * GET  /api/v1/sensors            -> list all sensors (optional ?type= filter)
 * POST /api/v1/sensors            -> create sensor (validates roomId exists)
 * GET  /api/v1/sensors/{id}       -> get single sensor
 *
 * Part 4 — Sub-Resource Locator
 * ANY  /api/v1/sensors/{id}/readings -> delegates to SensorReadingResource
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    // ---------------------------------------------------------------
    // GET /api/v1/sensors  (optional ?type=CO2)
    // ---------------------------------------------------------------
    /**
     * Part 3.2 — Filtered retrieval using @QueryParam.
     *
     * If 'type' param is absent or null, all sensors are returned.
     * If 'type' is provided, only sensors matching that type are returned.
     */
    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> result = new ArrayList<>(store.getSensors().values());

        if (type != null && !type.trim().isEmpty()) {
            result = result.stream()
                    .filter(s -> s.getType() != null && s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        return Response.ok(result).build();
    }

    // ---------------------------------------------------------------
    // POST /api/v1/sensors
    // ---------------------------------------------------------------
    /**
     * Part 3.1 — Create sensor with roomId integrity check.
     *
     * If the roomId provided in the body doesn't match a real room,
     * a LinkedResourceNotFoundException is thrown → HTTP 422.
     */
    @POST
    public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
        if (sensor == null || sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            return Response.status(400)
                    .entity(error("Sensor 'id' field is required."))
                    .build();
        }
        if (sensor.getRoomId() == null || sensor.getRoomId().trim().isEmpty()) {
            return Response.status(400)
                    .entity(error("Sensor 'roomId' field is required."))
                    .build();
        }

        // Part 3.1 — Validate that the referenced room actually exists
        Room room = store.getRooms().get(sensor.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException(
                    "Cannot register sensor: room with ID '" + sensor.getRoomId()
                    + "' does not exist in the system."
            );
        }

        if (store.getSensors().containsKey(sensor.getId())) {
            return Response.status(409)
                    .entity(error("A sensor with ID '" + sensor.getId() + "' already exists."))
                    .build();
        }

        // Default status to ACTIVE if not supplied
        if (sensor.getStatus() == null || sensor.getStatus().trim().isEmpty()) {
            sensor.setStatus("ACTIVE");
        }

        store.getSensors().put(sensor.getId(), sensor);

        // Bi-directional link: add sensor ID to the room's sensorIds list
        room.getSensorIds().add(sensor.getId());

        URI location = uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build();
        return Response.created(location).entity(sensor).build();
    }

    // ---------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}
    // ---------------------------------------------------------------
    @GET
    @Path("{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(404)
                    .entity(error("No sensor found with ID: " + sensorId))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    // ---------------------------------------------------------------
    // Part 4.1 — Sub-Resource Locator (NO HTTP verb annotation!)
    // ---------------------------------------------------------------
    /**
     * This method is a Sub-Resource Locator. It has NO @GET/@POST annotation,
     * so JAX-RS does not handle the request here — it delegates to the
     * returned SensorReadingResource instance which handles GET and POST itself.
     *
     * URL pattern: /api/v1/sensors/{sensorId}/readings[/{readingId}]
     */
    @Path("{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------
    private Map<String, Object> error(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",  "error");
        body.put("message", message);
        return body;
    }
}

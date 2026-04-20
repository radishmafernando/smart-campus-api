package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Part 4.2 — Historical Reading Management (Sub-Resource)
 *
 * This class is NOT registered directly with JAX-RS.
 * It is returned by SensorResource's sub-resource locator method.
 * JAX-RS then inspects THIS class for @GET / @POST annotations.
 *
 * Effective URL patterns (relative to the locator path):
 *   GET  /api/v1/sensors/{sensorId}/readings   -> reading history
 *   POST /api/v1/sensors/{sensorId}/readings   -> add new reading
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String    sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // ---------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}/readings
    // ---------------------------------------------------------------
    @GET
    public Response getReadings() {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(404)
                    .entity(error("No sensor found with ID: " + sensorId))
                    .build();
        }

        List<SensorReading> history = store.getReadingsForSensor(sensorId);
        return Response.ok(history).build();
    }

    // ---------------------------------------------------------------
    // POST /api/v1/sensors/{sensorId}/readings
    // ---------------------------------------------------------------
    /**
     * Appends a new reading to this sensor's history.
     *
     * Part 4.2 Side Effect: After storing the reading, this method updates
     * the parent Sensor's currentValue to keep the API data consistent.
     *
     * Part 5.1c: If the sensor's status is "MAINTENANCE", a
     * SensorUnavailableException is thrown → HTTP 403 Forbidden.
     */
    @POST
    public Response addReading(SensorReading reading, @Context UriInfo uriInfo) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(404)
                    .entity(error("No sensor found with ID: " + sensorId))
                    .build();
        }

        // Part 5.1c — Block readings if sensor is under maintenance
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is currently under MAINTENANCE. "
                    + "It cannot accept new readings until it is back ACTIVE."
            );
        }

        if (reading == null) {
            return Response.status(400)
                    .entity(error("Request body is required."))
                    .build();
        }

        // Auto-generate ID and timestamp — client doesn't need to supply these
        reading.setId(UUID.randomUUID().toString());
        reading.setTimestamp(System.currentTimeMillis());

        // Store the reading
        store.getReadingsForSensor(sensorId).add(reading);

        // *** SIDE EFFECT: update the parent sensor's currentValue ***
        sensor.setCurrentValue(reading.getValue());

        URI location = uriInfo.getAbsolutePathBuilder().path(reading.getId()).build();
        return Response.created(location).entity(reading).build();
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

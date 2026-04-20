package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Part 2 — Room Management
 *
 * GET    /api/v1/rooms          -> list all rooms
 * POST   /api/v1/rooms          -> create a new room
 * GET    /api/v1/rooms/{roomId} -> get one room by ID
 * DELETE /api/v1/rooms/{roomId} -> delete room (blocked if it has sensors)
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    // ---------------------------------------------------------------
    // GET /api/v1/rooms
    // ---------------------------------------------------------------
    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(store.getRooms().values());
        return Response.ok(roomList).build();
    }

    // ---------------------------------------------------------------
    // POST /api/v1/rooms
    // ---------------------------------------------------------------
    @POST
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        // Validate required fields
        if (room == null || room.getId() == null || room.getId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("Room 'id' field is required."))
                    .build();
        }
        if (room.getName() == null || room.getName().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("Room 'name' field is required."))
                    .build();
        }

        // Prevent duplicate IDs
        if (store.getRooms().containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(error("A room with ID '" + room.getId() + "' already exists."))
                    .build();
        }

        store.getRooms().put(room.getId(), room);

        // Return 201 Created with Location header pointing to the new resource
        URI location = uriInfo.getAbsolutePathBuilder().path(room.getId()).build();
        return Response.created(location).entity(room).build();
    }

    // ---------------------------------------------------------------
    // GET /api/v1/rooms/{roomId}
    // ---------------------------------------------------------------
    @GET
    @Path("{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(error("No room found with ID: " + roomId))
                    .build();
        }
        return Response.ok(room).build();
    }

    // ---------------------------------------------------------------
    // DELETE /api/v1/rooms/{roomId}
    // ---------------------------------------------------------------
    /**
     * Part 2.2 — Safety-constrained deletion.
     *
     * A room with active sensors cannot be deleted to prevent data orphans.
     * This throws RoomNotEmptyException which is mapped to HTTP 409 Conflict.
     *
     * Idempotency: DELETE on an already-deleted room returns 404, not 204.
     * This means our DELETE is NOT strictly idempotent — repeated calls have
     * different outcomes (first succeeds, subsequent return 404).
     */
    @DELETE
    @Path("{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);

        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(error("No room found with ID: " + roomId))
                    .build();
        }

        // Business rule: cannot delete a room that still has sensors
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                    "Room '" + roomId + "' cannot be deleted. It still has "
                    + room.getSensorIds().size() + " sensor(s) assigned to it. "
                    + "Please remove all sensors before decommissioning this room."
            );
        }

        store.getRooms().remove(roomId);
        // 204 No Content — successful deletion, no body needed
        return Response.noContent().build();
    }

    // ---------------------------------------------------------------
    // Helper — builds a consistent JSON error body
    // ---------------------------------------------------------------
    private Map<String, Object> error(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",  "error");
        body.put("message", message);
        return body;
    }
}

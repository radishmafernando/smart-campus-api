# Smart Campus Sensor & Room Management API

**Module:** 5COSC022W — Client-Server Architectures  
**Student:** Shenali Radishma Fernando | w2120071  
**Technology:** Java 11, JAX-RS (Jersey 2.41), Grizzly HTTP Server  
**Base URL:** `http://localhost:8080/api/v1`

---

## API Overview

This is a RESTful API built with JAX-RS (Jersey) for the University of Westminster's "Smart Campus" initiative. It provides a complete interface to manage campus **Rooms** and their associated **Sensors**, including a full historical **Readings** log per sensor.

### Resource Hierarchy

```
/api/v1
├── /                          Discovery endpoint
├── /rooms                     Room management
│   └── /{roomId}              Single room operations
└── /sensors                   Sensor management
    └── /{sensorId}
        └── /readings          Sensor reading history (sub-resource)
```

---

## How to Build & Run

### Prerequisites
- Java 11 or higher (`java -version`)
- Apache Maven 3.6+ (`mvn -version`)

### Step 1 — Clone the repository
```bash
git clone https://github.com/YOUR_USERNAME/smart-campus-api.git
cd smart-campus-api
```

### Step 2 — Build the project
```bash
mvn clean package
```
This produces a single fat JAR at `target/smart-campus-api-1.0.0.jar`.

### Step 3 — Run the server
```bash
java -jar target/smart-campus-api-1.0.0.jar
```
You should see:
```
=====================================================
  Smart Campus API is running!
  Base URL  : http://localhost:8080/api/v1
  Discovery : http://localhost:8080/api/v1
  Rooms     : http://localhost:8080/api/v1/rooms
  Sensors   : http://localhost:8080/api/v1/sensors
  Press ENTER to stop the server...
=====================================================
```

### Step 4 — Test with curl or Postman
Open a new terminal and try the curl commands below.

---

## Sample curl Commands

### 1. Discover the API
```bash
curl -X GET http://localhost:8080/api/v1
```
**Expected response (200 OK):**
```json
{
  "api": "Smart Campus Sensor & Room Management API",
  "version": "v1",
  "contact": "admin@smartcampus.ac.uk",
  "resources": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

---

### 2. Create a Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":40}'
```
**Expected response (201 Created):**
```json
{
  "id": "LIB-301",
  "name": "Library Quiet Study",
  "capacity": 40,
  "sensorIds": []
}
```

---

### 3. Create a Sensor (valid roomId)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-001","type":"CO2","status":"ACTIVE","currentValue":0.0,"roomId":"LIB-301"}'
```
**Expected response (201 Created):**
```json
{
  "id": "CO2-001",
  "type": "CO2",
  "status": "ACTIVE",
  "currentValue": 0.0,
  "roomId": "LIB-301"
}
```

---

### 4. Filter Sensors by Type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
```
**Expected response (200 OK):** Array containing only CO2 sensors.

---

### 5. Post a Sensor Reading
```bash
curl -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 412.5}'
```
**Expected response (201 Created):**
```json
{
  "id": "a3f2c1...",
  "timestamp": 1713870000000,
  "value": 412.5
}
```

---

### 6. Try to Delete a Room with Sensors (409 Conflict)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```
**Expected response (409 Conflict):**
```json
{
  "status": "error",
  "code": 409,
  "error": "Room Conflict",
  "message": "Room 'LIB-301' cannot be deleted. It still has 1 sensor(s) assigned to it."
}
```

---

### 7. Create a Sensor with invalid roomId (422 Unprocessable Entity)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-999","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"FAKE-999"}'
```
**Expected response (422 Unprocessable Entity):**
```json
{
  "status": "error",
  "code": 422,
  "error": "Unprocessable Entity",
  "message": "Cannot register sensor: room with ID 'FAKE-999' does not exist in the system."
}
```

---

### 8. Post a Reading to a MAINTENANCE Sensor (403 Forbidden)
```bash
# First set a sensor to MAINTENANCE by creating one with that status
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-002","type":"Temperature","status":"MAINTENANCE","currentValue":0.0,"roomId":"LIB-301"}'

# Then try to post a reading — this will be blocked
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-002/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 22.5}'
```
**Expected response (403 Forbidden):**
```json
{
  "status": "error",
  "code": 403,
  "error": "Sensor Unavailable",
  "message": "Sensor 'TEMP-002' is currently under MAINTENANCE and cannot accept new readings."
}
```

---

## Report — Answers to Coursework Questions

---

### Part 1.1 — JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new instance of each resource class for every incoming HTTP request** (request-scoped lifecycle). This means all instance variables in a resource class are reset on each request, so they cannot be used to store shared state.

This has a direct impact on in-memory data management. Since data stored inside a resource class instance would be lost after each request, all shared state — the rooms, sensors, and readings — must live in a **singleton outside the resource classes**. In this project, the `DataStore` class is a static singleton holding `ConcurrentHashMap` instances. `ConcurrentHashMap` is used instead of a regular `HashMap` because multiple requests can arrive simultaneously (multi-threading), and `ConcurrentHashMap` provides thread-safe reads and writes without requiring explicit `synchronized` blocks, preventing race conditions and data corruption.

---

### Part 1.2 — HATEOAS and Hypermedia in REST

HATEOAS (Hypermedia as the Engine of Application State) is considered a hallmark of mature RESTful design because it makes an API **self-documenting and navigable**. Rather than requiring developers to study external documentation to know which URLs exist, each API response includes links to related resources and available actions. For example, the discovery endpoint at `GET /api/v1` returns links to `/api/v1/rooms` and `/api/v1/sensors`, allowing a client to explore the entire API starting from a single known URL.

This benefits client developers significantly: when the server-side URL structure changes, only the links in the response need updating — the client code that follows those links does not need to be rewritten. In contrast, static documentation quickly becomes outdated and forces clients to hard-code URLs that may break silently.

---

### Part 2.1 — ID-Only vs Full Object Lists

Returning **only IDs** in a list response minimises payload size — bandwidth usage is low and responses arrive faster, which matters on a campus network serving thousands of concurrent sensor queries. However, the client must then make a separate `GET /{id}` request for each room it needs details about, which causes an "N+1 request problem" and increases total latency.

Returning **full objects** in the list eliminates the need for follow-up requests and is simpler for clients to consume, at the cost of larger payloads. For this API, full room objects are returned in the list because rooms are lightweight (only a few fields) and the benefit of a single round-trip outweighs the minor extra bandwidth.

---

### Part 2.2 — Idempotency of DELETE

The DELETE operation in this implementation is **not strictly idempotent**. The HTTP specification defines idempotency as: calling the same request multiple times produces the same server state. While the first successful DELETE removes the room and produces `204 No Content`, a second identical DELETE request finds no room and returns `404 Not Found`. The server state after both calls is the same (the room is gone), but the **HTTP response codes differ**. This design is intentional — returning 404 on subsequent deletes clearly communicates to the client that the resource no longer exists, which is more informative than silently returning 204 again.

---

### Part 3.1 — @Consumes and Media Type Mismatches

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells JAX-RS that the POST endpoint only accepts requests with a `Content-Type: application/json` header. If a client sends data as `text/plain` or `application/xml`, JAX-RS will reject the request **before it even reaches the resource method**. It returns an **HTTP 415 Unsupported Media Type** response automatically. This is handled at the framework level — no application code is needed to enforce it — which keeps resource methods clean and ensures only properly formatted JSON payloads are processed.

---

### Part 3.2 — @QueryParam vs Path Parameter for Filtering

Using `@QueryParam` (e.g., `GET /api/v1/sensors?type=CO2`) is considered superior to embedding the filter in the path (e.g., `/api/v1/sensors/type/CO2`) for several reasons:

1. **Semantic clarity**: A path segment implies a distinct resource identity. `/sensors/CO2` looks like it refers to a sensor whose ID is "CO2", which is misleading. Query parameters clearly signal "this is a filter on a collection", not a unique resource.
2. **Optional filters**: `@QueryParam` values are naturally optional — if absent, the full list is returned. Path parameters cannot be made optional without defining a separate route.
3. **Multiple filters**: Query parameters compose cleanly (e.g., `?type=CO2&status=ACTIVE`). Embedding multiple filters in the path creates deeply nested, awkward URLs.
4. **Caching and REST conventions**: REST conventions treat the path as identifying a resource and query strings as modifying the representation, which aligns with how HTTP caches and proxies operate.

---

### Part 4.1 — Sub-Resource Locator Pattern

The Sub-Resource Locator pattern allows a resource method to **delegate routing to a separate class** rather than handle all nested paths itself. In this project, `SensorResource` does not contain `@GET` or `@POST` methods for readings. Instead, its `getReadingsResource()` method — which has no HTTP verb annotation — returns a `SensorReadingResource` instance. JAX-RS then inspects that returned object for the actual HTTP method handlers.

The architectural benefits are significant. In large APIs, defining every nested path in one controller class creates a monolithic, hard-to-maintain file. By delegating to `SensorReadingResource`, each class has a single responsibility: `SensorResource` manages sensors; `SensorReadingResource` manages readings. This improves readability, allows the reading logic to be tested independently, and makes the codebase easier to extend — for example, adding pagination or filtering to readings would only require changes to `SensorReadingResource`.

---

### Part 5.1b — HTTP 422 vs 404 for Missing References

When a client POSTs a sensor with a `roomId` that does not exist, **HTTP 422 Unprocessable Entity** is more semantically accurate than 404 for the following reason: 404 means "the URL you requested was not found", implying the *endpoint itself* does not exist. In this case, `POST /api/v1/sensors` is a perfectly valid endpoint that was found and processed. The problem is not the URL — it is that the *content* of the JSON body references a resource (`roomId`) that the server cannot locate. HTTP 422 means "the request is syntactically well-formed, but the server cannot process it due to semantic errors in the payload", which precisely describes this situation.

---

### Part 5.2 — Cybersecurity Risks of Exposing Stack Traces

Exposing Java stack traces in API responses is a significant security vulnerability known as **Information Disclosure**. An attacker examining a stack trace can gather:

1. **Internal file paths**: Stack frames reveal the exact directory structure and class names on the server (e.g., `com.smartcampus.resource.SensorResource.createSensor(SensorResource.java:67)`), helping map the application architecture.
2. **Library versions**: Exception messages often include dependency names and versions (e.g., `jersey-server-2.41`), allowing attackers to look up known CVEs for those exact versions.
3. **Application logic**: The call stack reveals the sequence of method calls leading to the error, exposing business logic flow and potentially pointing to unvalidated code paths.
4. **Database details**: If a DB-related exception slips through (even though this project uses in-memory storage), connection strings, table names, and SQL queries can appear in traces.

The `GlobalExceptionMapper` in this project prevents all of this by logging the full trace server-side using `java.util.logging.Logger` while returning only a generic message to the client.

---

### Part 5.3 — JAX-RS Filters vs Manual Logging

Using JAX-RS filters for cross-cutting concerns like logging is far superior to manually calling `Logger.info()` inside every resource method for several reasons:

1. **DRY principle**: With a filter, logging logic is written once and applied to every request automatically. Manual logging requires adding identical boilerplate to every method — easy to forget and hard to keep consistent.
2. **Separation of concerns**: Resource methods should focus on business logic. A filter keeps infrastructure concerns (logging, security, timing) cleanly separated.
3. **Consistency**: A filter guarantees that every request is logged, even if a new endpoint is added later. Manual logging requires the developer to remember to add it every time.
4. **Maintainability**: Changing the log format (e.g., adding a request ID) requires updating one file — the filter — rather than modifying every resource class in the project.

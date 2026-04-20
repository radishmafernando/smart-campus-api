package com.smartcampus;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton in-memory data store.
 *
 * Because JAX-RS Resource classes are instantiated per-request by default,
 * shared state must live outside them. This singleton holds all data in
 * thread-safe ConcurrentHashMaps, preventing race conditions when multiple
 * requests arrive simultaneously.
 */
public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    // Core data maps — ConcurrentHashMap ensures thread-safe reads/writes
    private final Map<String, Room>         rooms    = new ConcurrentHashMap<>();
    private final Map<String, Sensor>       sensors  = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    private DataStore() {}

    public static DataStore getInstance() {
        return INSTANCE;
    }

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    /**
     * Returns the reading history for a given sensor.
     * Creates a new synchronized list if one doesn't exist yet.
     */
    public List<SensorReading> getReadingsForSensor(String sensorId) {
        return readings.computeIfAbsent(
                sensorId,
                k -> Collections.synchronizedList(new ArrayList<>())
        );
    }
}

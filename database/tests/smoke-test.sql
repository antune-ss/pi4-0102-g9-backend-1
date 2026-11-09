-- Run after database/init/01-schema.sql has been applied.
-- All test data is rolled back at the end.
BEGIN;

INSERT INTO sensor_device (device_id, display_name, location)
VALUES ('TEST-ESP32', 'Sensor de teste', 'Sala de teste')
ON CONFLICT (device_id) DO NOTHING;

INSERT INTO sensor_reading (device_id, co2, temperature, humidity, measured_at)
VALUES
    ('TEST-ESP32', 820, 24.50, 55.00, TIMESTAMPTZ '2026-10-26 12:00:00-03'),
    ('TEST-ESP32', 940, 25.10, 58.00, TIMESTAMPTZ '2026-10-26 12:00:05-03');

-- Expected: 2 rows; averages: CO2=880 (rounded to integer), temperature=24.80, humidity=56.50.
SELECT COUNT(*) AS reading_count,
       ROUND(AVG(co2))::BIGINT AS co2_average,
       AVG(temperature) AS temperature_average,
       AVG(humidity) AS humidity_average
FROM sensor_reading
WHERE device_id = 'TEST-ESP32'
  AND measured_at BETWEEN TIMESTAMPTZ '2026-10-26 11:59:00-03'
                      AND TIMESTAMPTZ '2026-10-26 12:01:00-03';

-- Expected: newest item first; device_id FK and units should be visible.
SELECT id, device_id, co2, temperature, humidity, measured_at, received_at
FROM sensor_reading
WHERE device_id = 'TEST-ESP32'
ORDER BY measured_at DESC, id DESC;

ROLLBACK;

-- CO2NNECT - schema PostgreSQL (H01.2 / CT2)
-- Roda automaticamente só quando o volume de dados do PostgreSQL é criado pela primeira vez.
-- Também pode ser aplicado à mão num banco vazio: psql -d co2nect_db -f db/init/01-schema.sql
BEGIN;

CREATE TABLE IF NOT EXISTS sensor_device (
    device_id VARCHAR(100) PRIMARY KEY,
    display_name VARCHAR(120),
    location VARCHAR(200),
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    last_seen_at TIMESTAMPTZ,
    last_status_at  TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT sensor_device_id_not_blank CHECK (length(btrim(device_id)) > 0)
);

CREATE TABLE IF NOT EXISTS sensor_reading (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    co2 INTEGER NOT NULL,
    temperature NUMERIC(6, 2) NOT NULL,
    humidity NUMERIC(5, 2) NOT NULL,
    measured_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT sensor_reading_device_fk
        FOREIGN KEY (device_id)
        REFERENCES sensor_device(device_id)
        ON DELETE RESTRICT,
    CONSTRAINT sensor_reading_co2_nonnegative
        CHECK (co2 >= 0),
    CONSTRAINT sensor_reading_temperature_range
        CHECK (temperature BETWEEN -40 AND 100),
    CONSTRAINT sensor_reading_humidity_range
        CHECK (humidity >= 0 AND humidity <= 100)
);

CREATE INDEX IF NOT EXISTS idx_sensor_reading_device_measured_at
    ON sensor_reading (device_id, measured_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_sensor_reading_received_at
    ON sensor_reading (received_at DESC);

COMMENT ON TABLE sensor_device IS
    'Registry and latest known connectivity state for each CO2NNECT sensor device.';
COMMENT ON TABLE sensor_reading IS
    'Historical samples received from the sensor, one row per accepted reading.';
COMMENT ON COLUMN sensor_reading.co2 IS
    'CO2 concentration in parts per million (ppm).';
COMMENT ON COLUMN sensor_reading.temperature IS
    'Temperature in degrees Celsius.';
COMMENT ON COLUMN sensor_reading.humidity IS
    'Relative humidity in percent (0..100).';
COMMENT ON COLUMN sensor_reading.measured_at IS
    'Timestamp reported by the device, normalized to an instant.';
COMMENT ON COLUMN sensor_reading.received_at IS
    'Timestamp when the backend stored the reading.';

COMMIT;
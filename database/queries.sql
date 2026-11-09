-- Últimas 100 leituras de um dispositivo
SELECT id, device_id, co2, temperature, humidity, measured_at, received_at
FROM sensor_reading
WHERE device_id = 'ESP32-SALA-01'
ORDER BY measured_at DESC, id DESC
LIMIT 100;

-- Histórico em um intervalo (substitua parâmetros pelos valores da API)
SELECT id, device_id, co2, temperature, humidity, measured_at, received_at
FROM sensor_reading
WHERE device_id = 'ESP32-SALA-01'
  AND measured_at >= TIMESTAMPTZ '2026-10-26 00:00:00-03'
  AND measured_at <= TIMESTAMPTZ '2026-10-27 00:00:00-03'
ORDER BY measured_at DESC, id DESC;

-- Resumo do intervalo. A média de CO2 é arredondada para inteiro para corresponder ao contrato CT6.
SELECT COUNT(*) AS reading_count,
       CASE WHEN COUNT(*) = 0 THEN NULL ELSE ROUND(AVG(co2))::BIGINT END AS co2_average,
       AVG(temperature) AS temperature_average,
       AVG(humidity) AS humidity_average
FROM sensor_reading
WHERE device_id = 'ESP32-SALA-01'
  AND measured_at >= TIMESTAMPTZ '2026-10-26 00:00:00-03'
  AND measured_at <= TIMESTAMPTZ '2026-10-27 00:00:00-03';

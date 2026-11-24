# Modelo entidade-relacionamento — CO₂NNECT

Fonte: `database/init/01-schema.sql` (schema versionado e montado pelo Compose local).

```mermaid
erDiagram
    SENSOR_DEVICE ||--o{ SENSOR_READING : produz
    SENSOR_DEVICE {
        varchar device_id PK
        varchar display_name
        varchar location
        boolean is_online
        timestamptz last_seen_at
        timestamptz last_status_at
        timestamptz created_at
        timestamptz updated_at
    }
    SENSOR_READING {
        bigint id PK
        varchar device_id FK
        integer co2
        numeric temperature
        numeric humidity
        timestamptz measured_at
        timestamptz received_at
    }
```

- `sensor_device`: cadastro de cada dispositivo e seu estado de conectividade mais recente.
- `sensor_reading`: histórico de amostras ambientais. Cada linha referencia exatamente um dispositivo.
- Cardinalidade: `sensor_device` 1:N `sensor_reading` (um dispositivo pode ter zero ou muitas leituras).
- Chave estrangeira: `sensor_reading.device_id` → `sensor_device.device_id`, `ON DELETE RESTRICT`.
- O diagrama representa o SQL versionado; a equivalência com o schema remoto do Supabase deve ser verificada separadamente.

# CT2: sensor_reading

| Linha | Valor |
| :--- | :--- |
| Fronteira | Banco de dados PostgreSQL|
| Quem grava | Backend Java (Backend 1), por JDBC (Java Database Connectivity) |
| Quem consulta |	Backend Java, pela consulta listReadings (CT5); Frontend, pelo Supabase REST|
| Quem responde | PostgreSQL configurado para o Backend Java e acessado pelo Supabase REST no Frontend |
| Atende | E2, E3, E6, E7 |
| Como chamar | Tabela `sensor_reading`, chave primária `id`; FK `device_id` → `sensor_device.device_id` |
| Schema de referência	| `database/init/01-schema.sql` |

> **Responsabilidades na implementação atual:** O Backend Java grava as medições no PostgreSQL por JDBC. O Frontend consulta `sensor_reading` pelo Supabase REST usando a chave publicável e as políticas de acesso (RLS) configuradas no Supabase. O Backend Java também possui a API `GET /api/readings` documentada no CT5, mas o Histórico web atual não utiliza esse endpoint. Para que os dados gravados apareçam no Histórico, `DB_URL` do Java e `NEXT_PUBLIC_SUPABASE_URL` do Frontend precisam apontar para a mesma base lógica. O schema remoto deve ser conferido separadamente em relação ao SQL versionado.

## Tabelas envolvidas

A tabela de leituras contratada é `sensor_reading`. Cada leitura referencia um dispositivo cadastrado em `sensor_device`, por meio da coluna `device_id` (chave estrangeira). Ao persistir uma medição, o Backend Java realiza primeiro o cadastro ou a atualização do dispositivo em `sensor_device` e, em seguida, insere a leitura em `sensor_reading`. As duas operações são executadas na mesma transação.

### Entrada e saída de `sensor_reading`
Os campos persistidos são:

| Campo | Tipo PostgreSQL | Obrigatório | Regra / exemplo |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT IDENTITY | gerado pelo banco | PK; `GENERATED ALWAYS AS IDENTITY` |
| `device_id` | VARCHAR(100) | sim | FK para `sensor_device.device_id`; `ESP32-SALA-01` |
| `co2` | INTEGER | sim | ppm; deve ser maior ou igual a zero; `850` |
| `temperature` | NUMERIC(6,2) | sim | °C; de -40 a 100; `24.50` |
| `humidity` | NUMERIC(5,2) | sim | %; de 0 a 100; `45.20` |
| `measured_at` | TIMESTAMPTZ | sim | Instante informado pelo dispositivo em ISO-8601, normalizado para um instante |
| `received_at` | TIMESTAMPTZ | preenchido pelo banco | `CURRENT_TIMESTAMP` por padrão, correspondente ao recebimento/gravação |

A chave estrangeira `sensor_reading_device_fk` referencia `sensor_device(device_id)` com `ON DELETE RESTRICT`. O banco também valida `co2 >= 0`, `temperature BETWEEN -40 AND 100` e `humidity BETWEEN 0 AND 100`.

## Fluxo de gravação

1. O Backend Java valida `deviceId`, CO₂, temperatura, umidade e `measuredAt`.
2. Na mesma transação, insere ou atualiza o registro de `sensor_device` (`is_online = TRUE`, atualizando `last_seen_at` e `updated_at`).
3. Insere uma nova linha em `sensor_reading` e recupera o `id` gerado pelo PostgreSQL.
4. Confirma a transação. Se uma operação SQL falhar, realiza rollback e propaga a falha; a leitura não deve ser descrita como persistida.

O histórico é append-only: uma nova amostra gera uma nova linha, sem sobrescrever leituras anteriores.

## Consulta

- **Backend Java:** `DatabaseManager.listReadings` consulta por `device_id` e intervalo de `measured_at`, ordena por `measured_at DESC, id DESC` e aplica o limite solicitado. O endpoint HTTP que disponibiliza essa consulta é o `GET /api/readings` (CT5).
- **Frontend:** `fetchReadings`, em `lib/services/supabase.ts`, consulta `sensor_reading` pelo Supabase REST. O Histórico aplica filtros de dispositivo, data e leituras críticas, ordena por `measured_at DESC` e limita a 100 registros. As médias da CT6 são calculadas no Frontend sobre os registros retornados.

## Erros e limites

| Quando | Comportamento |
| :--- | :--- |
| Identificador, data ou valor fora das regras | A validação do Backend Java rejeita a leitura antes da confirmação da transação. |
| Falha SQL durante o cadastro do dispositivo ou inserção da leitura | A transação é desfeita (rollback); a operação falha e não há confirmação de persistência. |
| Falha de consulta pelo Supabase REST | O Frontend recebe erro de consulta; o Histórico exibe mensagem e permite nova tentativa. |
| Banco indisponível | A conexão JDBC da operação falha. Cada operação abre sua própria conexão; não há garantia de reprocessamento automático da leitura perdida. |

## Índices

O schema cria `idx_sensor_reading_device_measured_at` em `(device_id, measured_at DESC, id DESC)` para consultas de histórico por dispositivo e horário e `idx_sensor_reading_received_at` em `(received_at DESC)` para análise de ingestão.

# Criação de tabelas

## 1. Tabela de sensor_reading

```sql
CREATE TABLE sensor_reading (
    device_id VARCHAR(50) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    co2 DOUBLE PRECISION,
    temperature DOUBLE PRECISION,
    humidity DOUBLE PRECISION
);
```

A tabela `sensor_reading` armazenará os dados coletados pelo sensor, incluindo a data e o horário da leitura, a concentração de CO₂, a temperatura e a umidade.

- `BIGSERIAL`: cria um identificador numérico com incremento automático.
- `TIMESTAMPTZ`: armazena a data e o horário com informação de fuso horário.
- `DOUBLE PRECISION`: permite armazenar valores numéricos com casas decimais.

É possível realizar consultas por intervalos de tempo diretamente utilizando a coluna `measuredAt`.

Exemplo de consulta para buscar todas as leituras realizadas entre 14h e 15h do dia 30 de setembro de 2026:

```sql
SELECT *
FROM sensor_reading
WHERE measuredAt >= '2026-09-30 14:00:00'
  AND measuredAt <  '2026-09-30 15:00:00';
```

Esse tipo de filtro pode ser utilizado no dashboard para permitir que o próprio usuário selecione um período e visualize apenas as leituras correspondentes.

## 2. Tabela de log de alertas

Além das leituras, podemos criar uma tabela para registrar os alertas identificados pelo sistema, como valores de CO₂, temperatura ou umidade fora dos limites definidos.

```sql
CREATE TABLE log_alerta (
    id BIGSERIAL PRIMARY KEY,
    measuredAt TIMESTAMPTZ NOT NULL,
    co2 DOUBLE PRECISION,
    temperaturae DOUBLE PRECISION,
    humidity DOUBLE PRECISION,
    cause TEXT
);
```

A tabela `log_alerta` armazenará o momento em que o alerta foi registrado, os valores medidos e uma descrição do problema identificado.

- `TIMESTAMPTZ`: registra a data e o horário do alerta.
- `TEXT`: permite armazenar mensagens de tamanho variável, como "Concentração de CO₂ acima do limite".

O PostgreSQL não identifica automaticamente os valores fora do normal. Essa verificação deverá ser realizada pela aplicação Java ou por uma rotina configurada no próprio banco de dados, que poderá inserir os alertas nessa tabela.

## 3. Exclusão automática de dados antigos

No PostgreSQL puro, não existe limpeza automatica de dados para uma tabela configurada para isso, sendo necessário implementar uma rotina de limpeza.

Por exemplo, para excluir todas as leituras com mais de 30 dias:

```sql
DELETE FROM sensor_reading
WHERE measuredAt < NOW() - INTERVAL '30 days';
```

Essa instrução remove os registros que ultrapassam o período de retenção definido.

Para automatizar a execução, pode-se utilizar um agendador externo, como o cron em sistemas Linux, ou o Agendador de Tarefas do Windows, executando periodicamente um script SQL.

**Atenção:** a exclusão é permanente, a menos que exista uma cópia de segurança. Antes de configurar a limpeza automática, é importante confirmar se os dados antigos realmente não precisam ser mantidos para análises históricas.

## 4. Exclusão de sensor_reading por intervalo de tempo

Também é possível excluir leituras de um intervalo específico, sem precisar consultar os identificadores individualmente.

Exemplo: excluir as leituras realizadas entre 10h e 11h do dia 30 de setembro de 2026.

```sql
DELETE FROM sensor_reading
WHERE measuredAt >= '2026-09-30 10:00:00'
  AND measuredAt <  '2026-09-30 11:00:00';
```

A utilização de `>=` para o início e `<` para o fim do intervalo evita sobreposição entre períodos consecutivos.

**Importante:** o comando `DELETE` remove os dados permanentemente. Para apenas visualizar as leituras de determinado período, utilize `SELECT` com as mesmas condições.

## 5. Agrupamento de sensor_reading para o dashboard

No PostgreSQL, podemos obter um resultado de média de medidas por um determinado tempo utilizando funções de data e hora, como `date_trunc()`.

Por exemplo, para calcular a média de CO₂ por minuto:

```sql
SELECT
    date_trunc('minute', measuredAt) AS minuto,
    AVG(co2) AS media_co2
FROM sensor_reading
GROUP BY minuto
ORDER BY minuto;
```

Nesse exemplo:

- `date_trunc('minute', measuredAt)`: agrupa as leituras pelo minuto em que foram registradas.
- `AVG(co2)`: calcula a média de CO₂ de cada grupo.
- `GROUP BY minuto`: reúne as leituras que pertencem ao mesmo minuto.
- `ORDER BY minuto`: apresenta os resultados em ordem cronológica.

Esse agrupamento é útil para gerar gráficos no dashboard, reduzindo a quantidade de pontos exibidos quando existem muitas leituras armazenadas.

A função `date_trunc()` agrupa por unidades de calendário, como minuto, hora ou dia. Para intervalos fixos de duração arbitrária, como blocos de cinco minutos alinhados a uma referência específica, será necessário utilizar uma expressão de agrupamento apropriada.

## 6. Observações gerais

- O PostgreSQL permite filtrar, excluir e agrupar registros por data e horário sem a necessidade de extensões.
- O uso de `TIMESTAMPTZ` é recomendado para registros de sensores, especialmente quando a aplicação precisa lidar com fusos horários.
- Índices podem melhorar o desempenho das consultas por período. Como a coluna `measuredAt` será utilizada frequentemente nos filtros, podemos criar um índice:

```sql
CREATE INDEX idx_sensor_reading_measuredAt
ON sensor_reading (measuredAt);
```
# CT2: sensor_reading

| Linha | Valor |
| :--- | :--- |
| Fronteira | Banco |
| Quem chama | Backend Java (único módulo que lê e grava nesta tabela) |
| Quem responde | Banco PostgreSQL |
| Atende | E2, E6, E7 |
| Como chamar | Tabela `sensor_reading`, chave `id` |

## Entrada e saída
Os campos gravados são os mesmos campos lidos.

| Campo | Tipo | Obrigatório | Exemplo |
| :--- | :--- | :--- | :--- |
| id | inteiro | sim | 1 |
| device_id | texto | sim | ESP32-SALA-01 |
| co2 | inteiro | sim | 850 |
| temperature | decimal | sim | 24.5 |
| humidity | decimal | sim | 45.2 |
| measured_at | data e hora | sim | 2026-10-13T19:20:00 |

## Erros
| Quando | O que volta |
| :--- | :--- |
| Falha de conexão ou timeout com o PostgreSQL | O banco recusa a gravação, a transação falha e o Java tenta reconectar |
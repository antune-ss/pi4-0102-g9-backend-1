# CT1: publishReading

| Linha | Valor |
| :--- | :--- |
| Fronteira | MQTT |
| Quem chama | ESP32-S3 (publica) e Broker (repassa a mensagem) |
| Quem responde | Broker (recebe) e Backend Java (assina o tópico e lê) |
| Atende | E1 |
| Como chamar | Tópico MQTT `/sensor/data` |

## Entrada
| Campo | Tipo | Obrigatório | Exemplo |
| :--- | :--- | :--- | :--- |
| deviceId | texto | sim | ESP32-SALA-01 |
| co2 | inteiro | sim | 850 |
| temperature | decimal | sim | 24.5 |
| humidity | decimal | sim | 45.2 |
| measuredAt | data e hora | não | 2026-10-13T19:20:00 |

## Saída
Não tem.

## Erros
| Quando | O que volta |
| :--- | :--- |
| O formato do JSON está quebrado ou ilegível | O backend Java descarta a mensagem e registra a falha no log |
| O sensor envia valores negativos ou nulos | O backend Java recusa a leitura e não grava no banco (Erro INVALID_READING) |
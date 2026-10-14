# CT6: getAverage

| Linha | Valor |
| :--- | :--- |
| Fronteira | HTTP |
| Quem chama | Frontend |
| Quem responde | Backend Java |
| Atende | E7 |
| Como chamar | GET `/api/readings/average` |

## Entrada
| Campo | Tipo | Obrigatório | Exemplo |
| :--- | :--- | :--- | :--- |
| deviceId | texto | sim | ESP32-SALA-01 |
| startDate | data e hora | sim | 2026-10-10T00:00:00 |
| endDate | data e hora | sim | 2026-10-13T23:59:59 |

## Saída
| Campo | Tipo | Obrigatório | Exemplo |
| :--- | :--- | :--- | :--- |
| co2Average | inteiro | sim | 920 |
| temperatureAverage | decimal | sim | 25.1 |
| humidityAverage | decimal | sim | 46.8 |

## Erros
| Quando | O que volta |
| :--- | :--- |
| O formato da data é inválido | HTTP 400 (Bad Request) |
| Não há medições no período informado | HTTP 200 (OK) com os valores de média retornando 0 (zero) ou nulos |
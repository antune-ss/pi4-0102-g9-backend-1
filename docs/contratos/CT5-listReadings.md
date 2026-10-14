# CT5: listReadings

| Linha | Valor |
| :--- | :--- |
| Fronteira | HTTP |
| Quem chama | Frontend |
| Quem responde | Backend Java |
| Atende | E6 |
| Como chamar | GET `/api/readings` |

## Entrada
| Campo | Tipo | Obrigatório | Exemplo |
| :--- | :--- | :--- | :--- |
| deviceId | texto | sim | ESP32-SALA-01 |
| startDate | data e hora | sim | 2026-10-10T00:00:00 |
| endDate | data e hora | sim | 2026-10-13T23:59:59 |

## Saída
| Campo | Tipo | Obrigatório | Exemplo |
| :--- | :--- | :--- | :--- |
| readings | lista de (objeto Reading) | sim | [lista abaixo] |

**objeto Reading**
| Campo | Tipo | Obrigatório | Exemplo |
| :--- | :--- | :--- | :--- |
| id | inteiro | sim | 1042 |
| co2 | inteiro | sim | 850 |
| temperature | decimal | sim | 24.5 |
| humidity | decimal | sim | 45.2 |
| measuredAt | data e hora | sim | 2026-10-13T19:20:00 |

## Erros
| Quando | O que volta |
| :--- | :--- |
| O formato da data é inválido ou startDate é maior que endDate | HTTP 400 (Bad Request) |
| O deviceId não existe no sistema | HTTP 404 (Not Found) |
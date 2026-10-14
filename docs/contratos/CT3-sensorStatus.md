# CT3: sensorStatus

| Linha | Valor |
| :--- | :--- |
| Fronteira | MQTT |
| Quem chama | Broker (envia automaticamente via Last Will and Testament quando o ESP32 cai, ou o próprio ESP32 quando conecta) |
| Quem responde | Backend Java (assina o tópico para monitorar a conexão) |
| Atende | E1, E5 |
| Como chamar | Tópico MQTT `co2nect/{deviceId}/status` |

## Entrada
| Campo | Tipo | Obrigatório | Exemplo |
| :--- | :--- | :--- | :--- |
| status | 0 ou 1 | sim | 0 |

## Saída
Não tem.

## Erros
| Quando | O que volta |
| :--- | :--- |
| O payload não for reconhecido | O Java ignora a mensagem e mantém o último estado conhecido do sensor |
# CT4: deviceCommand

| Linha | Valor |
| :--- | :--- |
| Fronteira | MQTT |
| Quem chama | Backend Java (publica o comando quando uma regra de negócio é violada) |
| Quem responde | ESP32-S3 (assina o tópico de comandos para ligar o alarme) |
| Atende | E5 |
| Como chamar | Tópico MQTT `co2nect/{deviceId}/command` |

## Entrada
| Campo | Tipo | Obrigatório | Exemplo |
| :--- | :--- | :--- | :--- |
| commandType | um de: turnOnAlarm, turnOffAlarm | sim | turnOnAlarm |

## Saída
Não tem.

## Erros
| Quando | O que volta |
| :--- | :--- |
| O dispositivo está offline (conforme status do LWT) | O comando não é entregue e o Java registra o erro de comunicação |
| O comando enviado não existe na lista | O ESP32 ignora o payload |
# ADR 0001: usar WebSocket para transmitir as medições do dispositivo

**Status:** proposto

**Contexto:** O dispositivo baseado em ESP32-S3 coleta, por meio do sensor SCD30, medições de concentração de CO₂, temperatura e umidade relativa. Essas leituras precisam ser encaminhadas ao backend para processamento e posterior visualização no dashboard. O sistema deve atualizar os dados durante o funcionamento do protótipo, sem depender de consultas manuais do usuário.

**Decisão:** Utilizar WebSocket como mecanismo de comunicação entre o dispositivo e o backend, mantendo um canal persistente para transmitir as medições coletadas.

**Alternativas consideradas:**
- Requisições HTTP independentes para cada leitura: descartadas porque exigiriam abrir e processar uma requisição para cada envio, em vez de reutilizar uma conexão persistente.

**Consequências:**
- Positivas: permite comunicação bidirecional por uma conexão persistente e facilita a atualização contínua das medições recebidas pelo backend.
- Negativas: a aplicação precisa tratar desconexões, reconexão e erros de transmissão; o WebSocket, por si só, não substitui a validação das mensagens nem garante que leituras perdidas durante uma interrupção sejam recuperadas.
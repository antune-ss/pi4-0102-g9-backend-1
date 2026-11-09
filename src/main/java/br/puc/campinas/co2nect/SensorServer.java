package br.puc.campinas.co2nect;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Instant;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.Gson;

/** Servidor WebSocket mantido para o fluxo de comunicação já existente no projeto. */
public class SensorServer extends WebSocketServer {
    private final DatabaseManager database;
    private final Gson gson = new Gson();

    public SensorServer(int port, DatabaseManager database) throws UnknownHostException {
        super(new InetSocketAddress(port));
        if (database == null) {
            throw new IllegalArgumentException("DatabaseManager é obrigatório.");
        }
        this.database = database;
    }

    @Override
    public void onOpen(WebSocket connection, ClientHandshake handshake) {
        System.out.println("[WS] Cliente conectado: " + connection.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket connection, int code, String reason, boolean remote) {
        System.out.println("[WS] Cliente desconectado: " + reason);
    }

    @Override
    public void onMessage(WebSocket connection, String message) {
        try {
            SensorData data = gson.fromJson(message, SensorData.class);
            if (data == null) {
                throw new IllegalArgumentException("Payload vazio ou inválido.");
            }
            if (data.measuredAt == null || data.measuredAt.isBlank()) {
                data.measuredAt = Instant.now().toString();
            }
            long id = database.saveRead(data);
            System.out.println("[WS] Medição persistida: id=" + id
                    + ", dispositivo=" + data.deviceId);
        } catch (Exception exception) {
            System.err.println("[WS] Mensagem não persistida: " + exception.getMessage());
        }
    }

    @Override
    public void onError(WebSocket connection, Exception exception) {
        System.err.println("[WS] Erro no servidor WebSocket: " + exception.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("[WS] Servidor iniciado na porta " + getPort());
        setConnectionLostTimeout(100);
    }
}

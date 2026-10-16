package br.puc.campinas.co2nect;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.Gson;

import org.java_websocket.WebSocket;

import br.puc.campinas.co2nect.model.SensorData;
import br.puc.campinas.co2nect.repository.DatabaseManager;

public class SensorServer extends WebSocketServer {
    private DatabaseManager db;

    SensorServer(int port, DatabaseManager db) throws UnknownHostException {
        super(new InetSocketAddress(port));
        this.db = db;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // O que o servidor deve fazer quando o ESP32 conseguir conectar com sucesso?
        System.out.println(conn.getRemoteSocketAddress().getAddress().getHostName() + " connected with the server.");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // O que fazer quando a conexão cair ou o ESP32 for desligado?
        System.out.println(conn + ": " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Toda vez que o ESP32 enviar uma leitura do sensor (ex: um texto JSON com o CO2), esse método será chamado automaticamente e o texto estará dentro da variável message.
        System.out.println(conn + ": " + message);

        Gson gson = new Gson();
        SensorData data = gson.fromJson(message, SensorData.class);

        db.saveRead(data);
        System.out.println("");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        // O que fazer se der algum erro na conexão?
        ex.printStackTrace();
        if (conn == null) {
            // TODO: Nn sei ainda
        }
    }

    @Override
    public void onStart() {
        // O que fazer quando o servidor ligar com sucesso na porta definida?
        System.out.println("Server started on port: " + getPort());
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }
}

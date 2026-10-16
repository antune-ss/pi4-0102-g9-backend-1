package br.puc.campinas.co2nect.repository;
import br.puc.campinas.co2nect.model.SensorData;
import java.sql.*;

public class DatabaseManager {
    private String url;
    private String user;
    private String password;

    public DatabaseManager(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public void saveRead(SensorData data) {
        // Quando se passa um valor para as ?, o Java trata esse dado puramente como texto ou número, neutralizando qualquer tentativa de SQL Injection
        String sql = "INSERT INTO reads (co2, temperature, humidity) VALUES (?, ?, ?)";

        // Try-with-Resources
        // Qualquer objeto criado dentro dos parênteses que implemente a interface AutoCloseable será fechado automaticamente pelo Java assim que o bloco try terminar
        // Antigamente, era preciso fechar manualmente o stmt e o conn em um bloco finally. Se esquecesse, causava um memory leak.
        try (Connection conn = DriverManager.getConnection(url, user, password);
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Substitui as ? do sql informando o índice (começando em 1) e o valor
            stmt.setDouble(1, data.co2);
            stmt.setDouble(2, data.temperature);
            stmt.setDouble(3, data.humidity);

            stmt.executeUpdate();
            System.out.println();
        } catch (SQLException e) {
            System.out.println("");
            e.printStackTrace();
        }


    }
}

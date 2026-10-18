package br.puc.campinas.co2nect.repository;

import java.sql.*;
import br.puc.campinas.co2nect.model.SensorReading;

/**
 * Camada de Acesso a Dados (Data Access Object / Repository).
 * 
 * Responsável exclusivamente por manipular as relações com o banco
 * relacional PostgreSQL via JDBC nativo.
 */
public class ReadingRepository {
    private String url;
    private String user;
    private String password;

    /**
     * Construtor que inicializa as credenciais de acesso ao banco de dados.
     * As credenciais são injetadas pelo orquestrador (App.java), que resolve variáveis de ambiente ou fallbacks locais.
     * 
     * @param url      URL de conexão JDBC (ex: "jdbc:postgresql://localhost:5432/co2nect_db")
     * @param user     usuário do PostgreSQL (ex: "co2nect_user")
     * @param password senha do PostgreSQL (ex: "co2nect_pass")
     */
    public ReadingRepository(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Insere um novo registro de telemetria na tabela 'sensor_reading'.
     * 
     * Detalhes técnicos da operação:
     * - O campo 'id' não é enviado no INSERT porque é autoincrementado (SERIAL PRIMARY KEY) pelo PostgreSQL.
     * - Usa PreparedStatement com placeholders (?) para neutralizar SQL Injection e evitar inconsistências de formatação decimal.
     * - Utiliza Try-with-Resources para assegurar o fechamento imediato da Conexão e do Statement após a execução,
     *   prevenindo vazamento de conexões (connection leaks) e consumo desnecessário de memória.
     * 
     * @param reading objeto contendo os dados validados da medição física
     * @throws SQLException lançada caso ocorra falha de rede, autenticação ou execução SQL,
     *                      permitindo que a camada de negócio (Service) capture o erro e registre o log.
     */
    public void save(SensorReading reading) throws SQLException {
        String sql = "INSERT INTO sensor_reading (device_id, co2, temperature, humidity, measured_at) VALUES (?, ?, ?, ?, ?)";

        // Se quiser entender o try-with-resources: https://pt.stackoverflow.com/questions/172909/como-funciona-o-try-with-resources
        try (
            Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, reading.getDeviceId());
            stmt.setInt(2, reading.getCo2());
            stmt.setDouble(3, reading.getTemperature());
            stmt.setDouble(4, reading.getHumidity());
            stmt.setObject(5, reading.getMeasuredAt());

            stmt.executeUpdate();
        } // Se der erro, a conexão fecha sozinha e a SQLException sobe para o Service tratar.
    }
}

package br.puc.campinas.co2nect;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

public class AppTest {
    @Test
    public void testDatabaseConnection() throws Exception {
        App.loadEnv();
        String dbUrl = App.getEnv("DB_URL", "jdbc:postgresql://localhost:5432/co2nect_db");
        String dbUser = App.getEnv("DB_USER", "co2nect_user");
        String dbPass = App.getEnv("DB_PASSWORD", "co2nect_pass");

        System.out.println("[Test] Testing connection to: " + dbUrl + " as " + dbUser);
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT count(*) FROM sensor_reading")) {
            assertTrue(rs.next());
            int count = rs.getInt(1);
            System.out.println("[Test] Connected successfully! Total rows in sensor_reading: " + count);
        }
    }
}

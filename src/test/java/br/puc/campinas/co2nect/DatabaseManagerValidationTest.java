package br.puc.campinas.co2nect;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.time.Instant;

import org.junit.Before;
import org.junit.Test;

/**
 * Testes que NÃO precisam de banco: a validação acontece antes de abrir conexão.
 * Rodam sempre, inclusive no `mvn test` de um PR.
 */
public class DatabaseManagerValidationTest {
    // Porta 1 não tem nada escutando: qualquer tentativa de conexão falha.
    private static final String DEAD_URL = "jdbc:postgresql://localhost:1/co2nect";
    private DatabaseManager db;

    @Before
    public void setUp() {
        db = new DatabaseManager(DEAD_URL, "u", "p");
    }

    private static SensorData valid() {
        SensorData d = new SensorData();
        d.deviceId = "ESP32-SALA-01";
        d.co2 = 850;
        d.temperature = 24.5;
        d.humidity = 45.2;
        d.measuredAt = "2026-10-13T19:20:00Z";
        return d;
    }

    private void assertRejected(String motivo, SensorData data) throws SQLException {
        try {
            db.saveRead(data);
            fail("Deveria rejeitar: " + motivo);
        } catch (IllegalArgumentException esperado) {
            // ok: rejeitado antes de tocar no banco
        }
    }

    @Test
    public void construtor_exigeUrlUsuarioESenha() {
        String[][] casos = { {null, "u", "p"}, {"", "u", "p"}, {DEAD_URL, " ", "p"}, {DEAD_URL, "u", null} };
        for (String[] c : casos) {
            try {
                new DatabaseManager(c[0], c[1], c[2]);
                fail("Deveria exigir URL, usuário e senha");
            } catch (IllegalArgumentException esperado) {
                // ok
            }
        }
    }

    @Test
    public void saveRead_rejeitaPayloadNulo() throws Exception {
        assertRejected("payload nulo", null);
    }

    @Test
    public void saveRead_rejeitaDeviceIdInvalido() throws Exception {
        SensorData d = valid(); d.deviceId = null;  assertRejected("deviceId nulo", d);
        d = valid(); d.deviceId = "   ";            assertRejected("deviceId em branco", d);
        d = valid(); d.deviceId = "X".repeat(101);  assertRejected("deviceId > 100 caracteres", d);
    }

    @Test
    public void saveRead_rejeitaCo2Invalido() throws Exception {
        SensorData d = valid(); d.co2 = null; assertRejected("co2 nulo", d);
        d = valid(); d.co2 = -1;              assertRejected("co2 negativo", d);
    }

    @Test
    public void saveRead_rejeitaTemperaturaInvalida() throws Exception {
        SensorData d = valid(); d.temperature = null;                    assertRejected("temperatura nula", d);
        d = valid(); d.temperature = Double.NaN;                         assertRejected("temperatura NaN", d);
        d = valid(); d.temperature = Double.POSITIVE_INFINITY;           assertRejected("temperatura infinita", d);
        d = valid(); d.temperature = -40.1;                              assertRejected("abaixo de -40", d);
        d = valid(); d.temperature = 100.1;                              assertRejected("acima de 100", d);
    }

    @Test
    public void saveRead_rejeitaUmidadeInvalida() throws Exception {
        SensorData d = valid(); d.humidity = null;   assertRejected("umidade nula", d);
        d = valid(); d.humidity = -0.1;              assertRejected("umidade < 0", d);
        d = valid(); d.humidity = 100.1;             assertRejected("umidade > 100", d);
        d = valid(); d.humidity = Double.NaN;        assertRejected("umidade NaN", d);
    }

    @Test
    public void saveRead_rejeitaDataInvalida() throws Exception {
        SensorData d = valid(); d.measuredAt = null;          assertRejected("data nula", d);
        d = valid(); d.measuredAt = "  ";                      assertRejected("data em branco", d);
        d = valid(); d.measuredAt = "ontem à tarde";           assertRejected("data fora do ISO 8601", d);
        d = valid(); d.measuredAt = "13/10/2026 19:20";        assertRejected("data no formato brasileiro", d);
    }

    @Test
    public void saveRead_aceitaTemperaturaNegativaDentroDaFaixaDoSensor() throws Exception {
        SensorData d = valid(); d.temperature = -5.0;
        try {
            db.saveRead(d);
            fail("Sem banco, deveria falhar com SQLException (e não com IllegalArgumentException)");
        } catch (SQLException esperado) {
            // passou da validação e só falhou ao conectar
        }
    }

    @Test
    public void listReadingsEGetAverage_validamParametros() throws Exception {
        Instant a = Instant.parse("2026-10-13T00:00:00Z");
        Instant b = Instant.parse("2026-10-14T00:00:00Z");
        try { db.listReadings("X", b, a, 10); fail("início depois do fim"); } catch (IllegalArgumentException ok) { }
        try { db.listReadings("X", null, b, 10); fail("início nulo"); } catch (IllegalArgumentException ok) { }
        try { db.listReadings("X", a, b, 0); fail("limit 0"); } catch (IllegalArgumentException ok) { }
        try { db.listReadings("X", a, b, 1001); fail("limit 1001"); } catch (IllegalArgumentException ok) { }
        try { db.listReadings(" ", a, b, 10); fail("deviceId em branco"); } catch (IllegalArgumentException ok) { }
        try { db.getAverage("X", b, a); fail("início depois do fim"); } catch (IllegalArgumentException ok) { }
        try { db.getAverage(null, a, b); fail("deviceId nulo"); } catch (IllegalArgumentException ok) { }
    }

    @Test(timeout = 20000)
    public void bancoForaDoAr_falhaRapidoComSQLException() throws Exception {
        long inicio = System.nanoTime();
        try {
            db.checkConnection();
            fail("Não deveria conectar");
        } catch (SQLException esperado) {
            long segundos = (System.nanoTime() - inicio) / 1_000_000_000L;
            assertTrue("deveria falhar em poucos segundos, levou " + segundos, segundos < 10);
        }
    }
}
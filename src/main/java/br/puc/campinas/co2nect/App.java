package br.puc.campinas.co2nect;
import br.puc.campinas.co2nect.repository.DatabaseManager;

public class App {
  public static void main(String[] args) {
    String dbUrl = System.getenv("DB_URL");
    String dbUser = System.getenv("DB_USER");
    String dbPass = System.getenv("DB_PASSWORD");

    DatabaseManager db = new DatabaseManager(dbUrl, dbUser, dbPass);

    try {
      SensorServer ws = new SensorServer(8807, db);
      ws.start();
    } catch (Exception e) {
      // TODO: handle exception
    }
  }
}

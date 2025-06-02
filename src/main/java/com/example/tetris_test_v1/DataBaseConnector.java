package com.example.tetris_test_v1;

//klasa tworząca określoną bazę danych mysql

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DataBaseConnector {
        private static final String serverUrl = "jdbc:mysql://localhost:3306/?serverTimezone=UTC";
        private static final String dbName = "baza_tetris";
        private static final String user = "root";
        private static final String password = "";

    public static void init() {

        try (Connection conn = DriverManager.getConnection(serverUrl, user, password);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            System.out.println("Baza danych '" + dbName + "' została utworzona lub już istnieje");

        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        String dbUrl = "jdbc:mysql://localhost:3306/" + dbName + "?serverTimezone=UTC";

        try (Connection conn = DriverManager.getConnection(dbUrl, user, password);
             Statement stmt = conn.createStatement()) {

            // przykładowo stworzona tabela, potem dostosujemy do naszych potrzeb,
            // łatwo dodawac nowe tabele wystarczy zedytowac poniższy wzór

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    highscore_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    highscore INT DEFAULT 0
                ) ENGINE=InnoDB
            """);

            System.out.println("Tabele zostały utworzone");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateHighscore(String username, int score) {
        String dbUrl = "jdbc:mysql://localhost:3306/" + dbName + "?serverTimezone=UTC";

        try (Connection conn = DriverManager.getConnection(dbUrl, user, password);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("INSERT INTO users (username, highscore) VALUES ('" + username + "', " + score + ") " +
                    "ON DUPLICATE KEY UPDATE highscore = GREATEST(highscore, " + score + "), highscore_date = CURRENT_TIMESTAMP");

            System.out.println("Wynik gracza '" + username + "' został zaktualizowany");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static int getHighscore(String username) {
        String dbUrl = "jdbc:mysql://localhost:3306/" + dbName + "?serverTimezone=UTC";
        int highscore = -1; // -1 indicates no score found

        try (Connection conn = DriverManager.getConnection(dbUrl, user, password);
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT highscore FROM users WHERE username = '" + username + "'")) {

            if (rs.next()) {
                highscore = rs.getInt("highscore");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return highscore;
    }
}
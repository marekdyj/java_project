package com.example.tetris_test_v1;

//klasa tworząca określoną bazę danych mysql

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

        // Wypełnienie przykładowymi danymi
        fillWithSampleData();
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

    public static List<LeaderboardEntry> getTop10() {
        String dbUrl = "jdbc:mysql://localhost:3306/" + dbName + "?serverTimezone=UTC";
        List<LeaderboardEntry> top10 = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT username, highscore FROM users ORDER BY highscore DESC LIMIT 10")) {
            int place = 1;
            while (rs.next()) {
                top10.add(new LeaderboardEntry(rs.getString("username"), rs.getInt("highscore"), place++));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return top10;
    }

    public static LeaderboardEntry getUserRanking(String username) {
        String dbUrl = "jdbc:mysql://localhost:3306/" + dbName + "?serverTimezone=UTC";
        int userScore = getHighscore(username);
        int place = 1;
        try (Connection conn = DriverManager.getConnection(dbUrl, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS rank FROM users WHERE highscore > " + userScore)) {
            if (rs.next()) {
                place += rs.getInt("rank");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new LeaderboardEntry(username, userScore, place);
    }

    public static void fillWithSampleData() {
        String dbUrl = "jdbc:mysql://localhost:3306/" + dbName + "?serverTimezone=UTC";
        String[] usernames = {
                "Anna", "Bartek", "Cezary", "Dorota", "Ewa", "Filip", "Grzegorz", "Hania", "Iga", "Jan",
                "Kamil", "Lena", "Marek", "Natalia", "Oskar", "Patryk", "Rafał", "Sandra", "Tomasz", "Ula"
        };
        int[] scores = {
                12000, 11500, 11000, 10800, 10500, 10200, 10000, 9800, 9500, 9400,
                9000, 8800, 8600, 8500, 8300, 8200, 8000, 7800, 7600, 7500
        };

        try (Connection conn = DriverManager.getConnection(dbUrl, user, password);
             Statement stmt = conn.createStatement()) {
            for (int i = 0; i < usernames.length; i++) {
                stmt.executeUpdate("INSERT INTO users (username, highscore) VALUES ('" + usernames[i] + "', " + scores[i] + ") " +
                        "ON DUPLICATE KEY UPDATE highscore = GREATEST(highscore, " + scores[i] + "), highscore_date = CURRENT_TIMESTAMP");
            }
            System.out.println("Baza została uzupełniona przykładowymi wynikami.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
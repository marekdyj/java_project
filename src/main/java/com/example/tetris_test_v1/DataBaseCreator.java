package com.example.tetris_test_v1;

//klasa tworząca określoną bazę danych mysql

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DataBaseCreator {

    public static void init() {
        String serverUrl = "jdbc:mysql://localhost:3306/?serverTimezone=UTC";
        String dbName = "baza_tetris";
        String user = "root";
        String password = "";

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
                    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB
            """);

            System.out.println("Tabele zostały utworzone");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
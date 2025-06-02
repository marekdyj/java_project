package com.example.tetris_test_v1;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileLogger {

    private String logFilePath;
    private DateTimeFormatter dateTimeFormatter;

    public FileLogger(String logFilePath) {
        this.logFilePath = logFilePath;
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }

    public void log(String message) {
        String timestamp = LocalDateTime.now().format(dateTimeFormatter);
        String logEntry = String.format("[%s] %s", timestamp, message);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Nie udało się zapisać do logów: " + e.getMessage());
        }
    }

}
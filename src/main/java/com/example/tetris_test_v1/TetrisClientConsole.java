package com.example.tetris_test_v1;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

// TODO: to jest do wywalenia czy nie ?
public class TetrisClientConsole {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 5000);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to Tetris server.");

            while (true) {
                String response = (String) in.readObject();
                System.out.println("[Server] " + response);

                if (response.contains("Choose mode")) {
                    System.out.print("Your choice: ");
                    String input = scanner.nextLine();
                    out.writeObject(input);
                    out.flush();
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
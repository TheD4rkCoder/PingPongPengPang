package com.example.pingpongpengpang;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client extends Application {
    private Socket socket;
    private Scanner in;
    private PrintWriter out;
    @Override
    public void start(Stage stage) throws IOException {
        try {
            socket = new Socket("10.10.30.66", 50000);
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Group root = new Group();
        Scene scene = new Scene(root, 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }
    private void sendPosToServer() {


            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            try {
                out.println(br.readLine());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String text = in.nextLine();
    }

    public static void main(String[] args) {
        launch();
    }
}
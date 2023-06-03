package com.example.pingpongpengpang;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

public class Client extends Application {
    private Socket socket;
    private Scanner in;
    private PrintWriter out;
    private Rectangle[] rectangles = new Rectangle[4];
    private Rectangle[] walls = new Rectangle[3];
    private Ellipse ball = new Ellipse(400, 400, 20, 20);
    private int playerID;
    private boolean[] keysPressed = new boolean[3];
    private Scene scene;
    private double oldSceneWidth = 800, oldSceneHeight = 800;

    @Override
    public void start(Stage stage) throws IOException {
        Group root = new Group(ball);
        walls[0] = new Rectangle(0, 0, 50, 800);
        walls[1] = new Rectangle(0, 0, 800, 50);
        walls[2] = new Rectangle(750, 0, 50, 800);

        for (int i = 0; i < 4; i++) {
            if (i < 3) {
                walls[i].setFill(Color.LIGHTGRAY);
                walls[i].setStroke(Color.BLACK);
                walls[i].setStrokeWidth(5);
                walls[i].setArcWidth(25);
                walls[i].setArcHeight(25);
                walls[i].setVisible(false);
                root.getChildren().add(walls[i]);
            }
            rectangles[i] = new Rectangle();
            rectangles[i].setFill(Color.LIGHTGRAY);
            rectangles[i].setStroke(Color.BLACK);
            rectangles[i].setStrokeWidth(5);
            rectangles[i].setArcWidth(25);
            rectangles[i].setArcHeight(25);
            if (i == 0 || i == 2) {
                rectangles[i].setX(300);
                rectangles[i].setHeight(25);
                rectangles[i].setWidth(200);
            } else {
                rectangles[i].setY(300);
                rectangles[i].setWidth(25);
                rectangles[i].setHeight(200);
            }
            root.getChildren().add(rectangles[i]);
        }
        rectangles[0].setY(750);
        rectangles[1].setX(25);
        rectangles[2].setY(25);
        rectangles[3].setX(750);
        scene = new Scene(root, 800, 800);
        scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.A) {
                    keysPressed[0] = true;
                } else if (keyEvent.getCode() == KeyCode.D) {
                    keysPressed[1] = true;
                } else if (keyEvent.getCode() == KeyCode.Q) {
                    keysPressed[2] = true;
                }
            }
        });
        scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.A) {
                    keysPressed[0] = false;
                } else if (keyEvent.getCode() == KeyCode.D) {
                    keysPressed[1] = false;
                }
            }
        });
        stage.setTitle("4 player Ping Pong");
        stage.setScene(scene);
        stage.show();
        ChangeListener<Number> cl = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                ball.setCenterX(ball.getCenterX()*scene.getWidth()/oldSceneWidth);
                ball.setCenterY(ball.getCenterY()*scene.getHeight()/oldSceneHeight);
                ball.setRadiusX(ball.getRadiusX()*scene.getWidth()/oldSceneWidth);
                ball.setRadiusY(ball.getRadiusY()*scene.getHeight()/oldSceneHeight);
                walls[2].setX(walls[2].getX()*scene.getWidth()/oldSceneWidth);
                for (int i = 0; i < 4; i++) {
                    walls[i].setWidth(walls[i].getWidth()*scene.getWidth()/oldSceneWidth);
                    walls[i].setHeight(walls[i].getHeight()*scene.getHeight()/oldSceneHeight);
                    rectangles[i].setX(rectangles[i].getX()*scene.getWidth()/oldSceneWidth);
                    rectangles[i].setY(rectangles[i].getY()*scene.getHeight()/oldSceneHeight);
                    rectangles[i].setWidth(rectangles[i].getWidth()*scene.getWidth()/oldSceneWidth);
                    rectangles[i].setHeight(rectangles[i].getHeight()*scene.getHeight()/oldSceneHeight);
                }

                oldSceneWidth = scene.getWidth();
                oldSceneHeight = scene.getHeight();
            }
        };
        scene.widthProperty().addListener(cl);
        scene.heightProperty().addListener(cl);
        stage.setOnCloseRequest(event -> {
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        try {
            // connect to server
            System.out.println("trying to connect to server");
            socket = new Socket("serafim.link", 3403);
            //socket = new Socket("127.0.0.1", 40000);
            System.out.println("connected to server");
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // server gives you your playerID (between 1 and 4). Also waits for the server to answer (the server waits until 4 players are ready)
        playerID = Integer.parseInt(in.nextLine());
        System.out.println("received ID: " + playerID);

        KeyFrame keyFrame = new KeyFrame(new Duration(40), event -> {
            boolean sendToServer = false;
            if (keysPressed[0] && rectangles[0].getX() - 0.03 * scene.getWidth() > 0) {
                rectangles[0].setX(rectangles[0].getX() - 0.03 * scene.getWidth());
                sendToServer = true;
            }
            if (keysPressed[1] && rectangles[0].getX() + 0.03 * scene.getWidth() < scene.getWidth() - rectangles[0].getWidth()) {
                rectangles[0].setX(rectangles[0].getX() + 0.03 * scene.getWidth());
                sendToServer = !sendToServer;
            }
            if (sendToServer) {
                sendPositionToServer();
            }
        });
        Timeline timeline = new Timeline(keyFrame);
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        Thread receiveFromServerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!keysPressed[2]) {
                    receivePositionsFromServer();
                }
                stage.close();
            }
        });
        receiveFromServerThread.start();
    }

    private void receivePositionsFromServer() {
        //System.out.println("trying to read from server");
        String[] inputs = in.nextLine().split(" ");
        //System.out.println("read from server");
        double[] values = new double[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            values[i] = Double.parseDouble(inputs[i]);
        }
        rectangles[1].setY(values[0] * scene.getHeight() - rectangles[1].getHeight() * 0.5);
        if (values[0] == -1) {
            walls[0].setVisible(true);
        } else {
            walls[0].setVisible(false);
        }
        rectangles[2].setX(scene.getWidth() - values[1] * scene.getWidth() - rectangles[2].getWidth() * 0.5);
        if (values[1] == -1) {
            walls[1].setVisible(true);
        }else {
            walls[1].setVisible(false);
        }
        rectangles[3].setY(scene.getHeight() - values[2] * scene.getHeight() - rectangles[3].getHeight() * 0.5);
        if (values[2] == -1) {
            walls[2].setVisible(true);
        }else {
            walls[2].setVisible(false);
        }

        ball.setCenterX(values[3] * scene.getWidth());
        ball.setCenterY(values[4] * scene.getHeight());
    }

    private void sendPositionToServer() {
        //System.out.println("sending position to server");
        out.println((rectangles[0].getX() + rectangles[0].getWidth() * 0.5) / scene.getWidth()); // gives x-Value of the middle point of the paddle (between 0 and 1) to server
        // System.out.println("sent position to server");
    }

    public static void main(String[] args) {
        launch();
    }
}
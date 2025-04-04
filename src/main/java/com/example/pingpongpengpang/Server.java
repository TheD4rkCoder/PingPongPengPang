package com.example.pingpongpengpang;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Server {
    private static final double PADDLE_WIDTH = 0.25, PADDLE_HEIGHT = 0.03125, BALL_RADIUS = 0.025;
    private static boolean quit = false;
    private static Socket[] clients = new Socket[4];
    private static ServerSocket serverSocket;
    private static Scanner[] in = new Scanner[4];
    private static PrintWriter[] out = new PrintWriter[4];

    private static double[] objectValues = {-1, -1, -1, -1, 0.5, 0.5, 0.5, 0.02}; // player1x, player2y, player3x, player4y, ballx, bally, ballAngle, ballSpeed
    private static int[] score = new int[4];
    private static boolean active;

    public static void main(String[] args) {
        openServerSocket();

        Thread[] receiveFromClientThreads = new Thread[4];
        for (int i = 0; i < 4; i++) {
            int finalI = i;
            receiveFromClientThreads[i] = new Thread(new Runnable() {
                final int client = finalI;

                @Override
                public void run() {
                    while (true) {
                        if (quit) {
                            break;
                        }
                        synchronized (receiveFromClientThreads) {
                            try {
                                System.out.println("waiting for player " + client);
                                clients[client] = serverSocket.accept();
                                System.out.println("connected with player " + client);
                                objectValues[client] = 0.5;
                                active = true;
                                in[client] = new Scanner(clients[client].getInputStream());
                                out[client] = new PrintWriter(clients[client].getOutputStream(), true);
                                out[client].println(client);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        while (true) {
                            try {
                                if (quit) {
                                    try {
                                        clients[client].close();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    break;
                                }
                                double value = Double.parseDouble(in[client].nextLine());
                                //System.out.println("waiting for client " + client + "to write something");
                                objectValues[client] = value;
                            } catch (NoSuchElementException e) {
                                try {
                                    clients[client].close();
                                    System.out.println("disconnected with player " + client);
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                                objectValues[client] = -1;
                                break;
                            }
                        }

                    }
                }
            });
            receiveFromClientThreads[i].start();
        }
        //System.out.println("started threads");
        Thread timeline = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    while (!active) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    moveBall();
                    double tempBallX = objectValues[4], tempBallY = objectValues[5];
                    active = false;
                    for (int i = 0; i < 4; i++) {
                        if (objectValues[i] != -1) {
                            active = true;
                            //System.out.println("sending to player " + i);
                            out[i].printf("%f %f %f %f %f %d %d %d %d\n", objectValues[(i + 1) % 4], objectValues[(i + 2) % 4], objectValues[(i + 3) % 4], tempBallX, tempBallY, score[i], score[(i + 1) % 4], score[(i + 2) % 4], score[(i + 3) % 4]);
                        }
                        double temp = tempBallX;
                        tempBallX = tempBallY;
                        tempBallY = 1 - temp;
                    }

                }
            }
        });
        timeline.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.println("Write \"quit\" to close the server");
            try {
                if (br.readLine().equals("quit")) {
                    // close everything
                    quit = true;
                    break;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void openServerSocket() {
        try {
            System.out.println("Trying to open socket");
            serverSocket = new ServerSocket(30000);
            //serverSocket = new ServerSocket(40000);
            System.out.println("opened socket");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void moveBall() {
        // collisions:
        if (objectValues[4] < 0) {
            score[1]++;
            printScore();
            objectValues[4] = 0.5; // reset ball
            objectValues[5] = 0.5;
            objectValues[7] = 0.02;
        } else if (objectValues[4] > 1) {
            score[3]++;
            printScore();
            objectValues[4] = 0.5;
            objectValues[5] = 0.5;
            objectValues[7] = 0.02;
        } else if (objectValues[5] < 0) {
            score[2]++;
            printScore();
            objectValues[4] = 0.5;
            objectValues[5] = 0.5;
            objectValues[7] = 0.02;
        } else if (objectValues[5] > 1) {
            score[0]++;
            printScore();
            objectValues[4] = 0.5;
            objectValues[5] = 0.5;
            objectValues[7] = 0.02;
        }
        if (objectValues[0] != -1) {
            if (ballIntersectsWithRect(objectValues[0], 1 - PADDLE_HEIGHT * 1.5, PADDLE_WIDTH, PADDLE_HEIGHT)) {
                // should be correct:
                objectValues[5] = 1 - PADDLE_HEIGHT * 2 - BALL_RADIUS - 0.001;
                double percentage = (objectValues[0] - objectValues[4]) / (PADDLE_WIDTH / 2 + BALL_RADIUS);
                double angle = 2 * Math.PI - objectValues[6];
                objectValues[6] = (angle + (Math.PI * 0.5 + percentage * 0.4 * Math.PI)) / 2;
                objectValues[7] *= 1.001;
            }
        } else {
            if (objectValues[5] > 1 - 2 * PADDLE_HEIGHT - BALL_RADIUS) {
                objectValues[5] = 1 - PADDLE_HEIGHT * 2 - BALL_RADIUS - 0.001;
                objectValues[6] = 2 * Math.PI - objectValues[6];
            }
        }
        if (objectValues[1] != -1) {
            if (ballIntersectsWithRect(PADDLE_HEIGHT * 1.5, objectValues[1], PADDLE_HEIGHT, PADDLE_WIDTH)) {
                objectValues[4] = PADDLE_HEIGHT * 2 + BALL_RADIUS + 0.001;
                double percentage = (objectValues[1] - objectValues[5]) / (PADDLE_WIDTH / 2 + BALL_RADIUS);
                double angle = Math.PI - objectValues[6];
                // should work:
                objectValues[6] = (angle + (percentage * 0.4 * Math.PI)) / 2;
                objectValues[6] = angleMod2PI(objectValues[6]);
                objectValues[7] *= 1.001;
            }
        } else {
            if (objectValues[4] < 2 * PADDLE_HEIGHT + BALL_RADIUS) {
                objectValues[4] = PADDLE_HEIGHT * 2 + BALL_RADIUS + 0.001;
                objectValues[6] = angleMod2PI(Math.PI - objectValues[6]);
            }
        }
        if (objectValues[2] != -1) {
            if (ballIntersectsWithRect(1 - objectValues[2], PADDLE_HEIGHT * 1.5, PADDLE_WIDTH, PADDLE_HEIGHT)) {
                objectValues[5] = PADDLE_HEIGHT * 2 + BALL_RADIUS + 0.001;
                double percentage = (objectValues[2] - 1 + objectValues[4]) / (PADDLE_WIDTH / 2 + BALL_RADIUS);
                double angle = 2 * Math.PI - objectValues[6];
                objectValues[6] = (angle + (Math.PI * 1.5 + percentage * 0.4 * Math.PI)) / 2;
                objectValues[7] *= 1.001;
            }
        } else {
            if (objectValues[5] < 2 * PADDLE_HEIGHT + BALL_RADIUS) {
                objectValues[5] = PADDLE_HEIGHT * 2 + BALL_RADIUS + 0.001;
                objectValues[6] = 2 * Math.PI - objectValues[6];
            }
        }
        if (objectValues[3] != -1) {
            if (ballIntersectsWithRect(1 - PADDLE_HEIGHT * 1.5, 1 - objectValues[3], PADDLE_HEIGHT, PADDLE_WIDTH)) {
                objectValues[4] = 1 - PADDLE_HEIGHT * 2 - BALL_RADIUS - 0.001;
                double percentage = (objectValues[3] - 1 + objectValues[5]) / (PADDLE_WIDTH / 2 + BALL_RADIUS);
                double angle = Math.PI - objectValues[6];
                angle = angleMod2PI(angle);
                objectValues[6] = (angle + (Math.PI + percentage * 0.4 * Math.PI)) / 2;
                objectValues[7] *= 1.001;
            }
        } else {
            if (objectValues[4] > 1 - 2 * PADDLE_HEIGHT - BALL_RADIUS) {
                objectValues[6] = angleMod2PI(Math.PI - objectValues[6]);
                objectValues[4] = 1 - 2 * PADDLE_HEIGHT - BALL_RADIUS - 0.001;
            }
        }
        // movement:
        objectValues[4] += Math.cos(objectValues[6]) * objectValues[7];
        objectValues[5] -= Math.sin(objectValues[6]) * objectValues[7];
    }

    private static double angleMod2PI(double angle) {
        while (angle < 0) {
            angle += 2 * Math.PI;
        }
        while (angle >= 2 * Math.PI) {
            angle -= 2 * Math.PI;
        }
        return angle;
    }

    static private boolean ballIntersectsWithRect(double x, double y, double width, double height) // x and y: middle point of the Rectangle
    {
        double circleDistanceX = Math.abs(objectValues[4] - x);
        double circleDistanceY = Math.abs(objectValues[5] - y);

        if (circleDistanceX > (width * 0.5 + 0.025)) {
            return false;
        }
        if (circleDistanceY > (height * 0.5 + 0.025)) {
            return false;
        }

        if (circleDistanceX <= width * 0.5 || circleDistanceY <= height * 0.5) {
            return true;
        }
        double cornerDistance_sq = (circleDistanceX - width / 2 - 0.125) * (circleDistanceX - width / 2 - 0.125) + (circleDistanceY - height / 2 - 0.125) * (circleDistanceY - height / 2 - 0.125);
        return (cornerDistance_sq <= (0.15 * 0.15));
        //return (cornerDistance_sq <= (0.025 * 0.025));
    }

    static private void printScore() {
        System.out.println(score[0] + " " + score[1] + " " + score[2] + " " + score[3]);
    }
}

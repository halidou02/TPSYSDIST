package org.emp.gl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class POP3Server implements Runnable {
    private int port;

    public POP3Server(int port) {
        this.port = port;
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new POP3ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

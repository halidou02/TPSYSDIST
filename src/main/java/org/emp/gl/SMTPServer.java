package org.emp.gl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SMTPServer implements Runnable {
    private int port;

    public SMTPServer(int port) {
        this.port = port;
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("SMTP Server started on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new SMTPClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

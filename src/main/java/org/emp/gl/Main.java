package org.emp.gl;
import java.io.*;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        // Lancer le serveur SMTP sur le port 2525
        Thread smtpThread = new Thread(new SMTPServer(2525));
        smtpThread.start();
        System.out.println("SMTP Server lancé sur le port 2525");

        // Lancer le serveur POP3 sur le port 1110
        Thread pop3Thread = new Thread(new POP3Server(1110));
        pop3Thread.start();
        System.out.println("POP3 Server lancé sur le port 1110");

        System.out.println("Les serveurs sont en écoute. Vous pouvez vous connecter via Telnet.");

    }
}
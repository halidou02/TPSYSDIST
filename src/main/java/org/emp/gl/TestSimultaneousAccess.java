package org.emp.gl;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class TestSimultaneousAccess {

    public static void main(String[] args) {
        // Création des répertoires utilisateurs nécessaires
        createUserDir("bob");
        createUserDir("charlie");

        // Supposons que les serveurs SMTP et POP3 sont déjà lancés (par exemple via MainServer)
        int smtpClientCount = 10;
        int pop3ClientCount = 5;

        ExecutorService executor = Executors.newFixedThreadPool(smtpClientCount + pop3ClientCount);

        // Lancer plusieurs clients SMTP pour envoyer des emails en parallèle
        for (int i = 0; i < smtpClientCount; i++) {
            executor.submit(new SMTPClientSimulator(i));
        }

        // Lancer plusieurs clients POP3 pour récupérer des emails en parallèle
        for (int i = 0; i < pop3ClientCount; i++) {
            executor.submit(new POP3ClientSimulator(i));
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Test d'accès simultané terminé.");
    }

    private static void createUserDir(String user) {
        File dir = new File("mailserver/" + user);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("Répertoire créé pour l'utilisateur : " + dir.getPath());
        }
    }

    // Simulateur de client SMTP
    static class SMTPClientSimulator implements Runnable {
        private int id;
        public SMTPClientSimulator(int id) {
            this.id = id;
        }
        public void run() {
            try (Socket socket = new Socket("localhost", 2525);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                System.out.println("SMTP Client " + id + " connecté.");

                String response;
                // Lecture du message de bienvenue
                response = in.readLine();
                System.out.println("SMTP Client " + id + " Réponse: " + response);

                // HELO
                String cmd = "HELO localhost";
                System.out.println("SMTP Client " + id + " Envoi: " + cmd);
                out.println(cmd);
                response = in.readLine();
                System.out.println("SMTP Client " + id + " Réponse: " + response);

                // MAIL FROM
                cmd = "MAIL FROM: sender" + id + "@example.com";
                System.out.println("SMTP Client " + id + " Envoi: " + cmd);
                out.println(cmd);
                response = in.readLine();
                System.out.println("SMTP Client " + id + " Réponse: " + response);

                // RCPT TO
                String recipient = (id % 2 == 0) ? "bob@example.com" : "charlie@example.com";
                cmd = "RCPT TO: " + recipient;
                System.out.println("SMTP Client " + id + " Envoi: " + cmd);
                out.println(cmd);
                response = in.readLine();
                System.out.println("SMTP Client " + id + " Réponse: " + response);

                // DATA
                cmd = "DATA";
                System.out.println("SMTP Client " + id + " Envoi: " + cmd);
                out.println(cmd);
                response = in.readLine();
                System.out.println("SMTP Client " + id + " Réponse: " + response);

                // Contenu du message
                cmd = "Subject: Test email " + id;
                System.out.println("SMTP Client " + id + " Envoi: " + cmd);
                out.println(cmd);
                cmd = "Ceci est un email envoyé par le client SMTP " + id + ".";
                System.out.println("SMTP Client " + id + " Envoi: " + cmd);
                out.println(cmd);
                // Fin du message
                cmd = ".";
                System.out.println("SMTP Client " + id + " Envoi: " + cmd);
                out.println(cmd);
                response = in.readLine();
                System.out.println("SMTP Client " + id + " Réponse: " + response);

                // QUIT
                cmd = "QUIT";
                System.out.println("SMTP Client " + id + " Envoi: " + cmd);
                out.println(cmd);
                response = in.readLine();
                System.out.println("SMTP Client " + id + " Réponse: " + response);

                System.out.println("SMTP Client " + id + " terminé.");
            } catch (IOException e) {
                System.err.println("Erreur SMTP Client " + id + " : " + e.getMessage());
            }
        }
    }

    // Simulateur de client POP3
    static class POP3ClientSimulator implements Runnable {
        private int id;
        public POP3ClientSimulator(int id) {
            this.id = id;
        }
        public void run() {
            try (Socket socket = new Socket("localhost", 1110);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                System.out.println("POP3 Client " + id + " connecté.");
                String response;
                // Lecture du message de bienvenue
                response = in.readLine();
                System.out.println("POP3 Client " + id + " Réponse: " + response);

                // USER
                String user = (id % 2 == 0) ? "bob" : "charlie";
                String cmd = "USER " + user;
                System.out.println("POP3 Client " + id + " Envoi: " + cmd);
                out.println(cmd);
                response = in.readLine();
                System.out.println("POP3 Client " + id + " Réponse: " + response);

                // PASS
                String password = (user.equals("bob")) ? "secret" : "pass123";
                cmd = "PASS " + password;
                System.out.println("POP3 Client " + id + " Envoi: " + cmd);
                out.println(cmd);
                response = in.readLine();
                System.out.println("POP3 Client " + id + " Réponse: " + response);

                // STAT
                cmd = "STAT";
                System.out.println("POP3 Client " + id + " Envoi: " + cmd);
                out.println(cmd);
                response = in.readLine();
                System.out.println("POP3 Client " + id + " STAT : " + response);

                // QUIT
                cmd = "QUIT";
                System.out.println("POP3 Client " + id + " Envoi: " + cmd);
                out.println(cmd);
                response = in.readLine();
                System.out.println("POP3 Client " + id + " Réponse: " + response);

                System.out.println("POP3 Client " + id + " terminé.");
            } catch (IOException e) {
                System.err.println("Erreur POP3 Client " + id + " : " + e.getMessage());
            }
        }
    }
}

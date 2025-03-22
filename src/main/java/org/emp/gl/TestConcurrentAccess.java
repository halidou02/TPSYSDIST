package org.emp.gl;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;

public class TestConcurrentAccess {
    public static void main(String[] args) {
        // Création des répertoires pour les utilisateurs
        createUserDir("bob");
        createUserDir("charlie");

        // Démarrage des serveurs SMTP et POP3
        Thread smtpServerThread = new Thread(new SMTPServer(2525));
        smtpServerThread.start();
        System.out.println("==> Démarrage du serveur SMTP sur le port 2525.");

        Thread pop3ServerThread = new Thread(new POP3Server(1110));
        pop3ServerThread.start();
        System.out.println("==> Démarrage du serveur POP3 sur le port 1110.");

        sleep(2000);

        // SCÉNARIO 1 : Charge simultanée
        System.out.println("\n--- Scénario 1 : Charge simultanée ---");
        System.out.println("Lancement de 10 clients SMTP et 10 clients POP3 en parallèle.");
        scenarioSimultaneousAccess();
        sleep(2000);

        // SCÉNARIO 2 : Traitement parallèle des messages
        System.out.println("\n--- Scénario 2 : Traitement parallèle des messages ---");
        System.out.println("Envoi simultané d'un email (SMTP) et récupération des emails (POP3) pour des utilisateurs différents.");
        scenarioParallelProcessing();
        sleep(2000);

        // SCÉNARIO 3 : Saturation de la file d'attente
        System.out.println("\n--- Scénario 3 : Saturation du pool de threads ---");
        System.out.println("Soumission de 30 tâches dans un pool de 5 threads pour tester la gestion des connexions en attente.");
        scenarioThreadPoolSaturation();
        sleep(2000);

        System.out.println("\nTous les scénarios de test sont terminés.");
    }

    private static void createUserDir(String user) {
        File dir = new File("mailserver/" + user);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("Répertoire créé pour " + user + " : " + dir.getPath());
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // SCÉNARIO 1 : Charge simultanée
    private static void scenarioSimultaneousAccess() {
        int smtpClients = 10;
        int pop3Clients = 10;
        ExecutorService executor = Executors.newFixedThreadPool(smtpClients + pop3Clients);

        for (int i = 0; i < smtpClients; i++) {
            final int id = i;
            executor.submit(() -> {
                System.out.println("[SMTP " + id + "] Envoi d'email à bob@example.com");
                SMTPClientSimulator.sendEmail(id, "bob@example.com");
            });
        }
        for (int i = 0; i < pop3Clients; i++) {
            final int id = i;
            String user = (id % 2 == 0) ? "bob" : "charlie";
            executor.submit(() -> {
                System.out.println("[POP3 " + id + "] Récupération des emails pour " + user);
                POP3ClientSimulator.retrieveEmails(id, user);
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // SCÉNARIO 2 : Traitement parallèle des messages
    private static void scenarioParallelProcessing() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> {
            System.out.println("[SMTP Parallel] Envoi d'email à bob@example.com");
            SMTPClientSimulator.sendEmail(100, "bob@example.com");
        });
        executor.submit(() -> {
            System.out.println("[POP3 Parallel] Récupération des emails pour charlie");
            POP3ClientSimulator.retrieveEmails(100, "charlie");
        });
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // SCÉNARIO 3 : Saturation du pool de threads
    private static void scenarioThreadPoolSaturation() {
        int totalTasks = 30; // Tâches supérieures à la taille du pool
        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (int i = 0; i < totalTasks; i++) {
            final int id = i;
            if (i % 2 == 0) {
                executor.submit(() -> {
                    System.out.println("[SMTP Saturation " + id + "] Envoi d'email à bob@example.com");
                    SMTPClientSimulator.sendEmail(id, "bob@example.com");
                });
            } else {
                executor.submit(() -> {
                    System.out.println("[POP3 Saturation " + id + "] Récupération des emails pour bob");
                    POP3ClientSimulator.retrieveEmails(id, "bob");
                });
            }
        }
        executor.shutdown();
        try {
            executor.awaitTermination(3, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Simulateur de client SMTP
    public static class SMTPClientSimulator {
        public static void sendEmail(int id, String recipient) {
            try (Socket socket = new Socket("localhost", 2525);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                // Lire le message de bienvenue
                in.readLine();
                out.println("HELO localhost");
                in.readLine();
                out.println("MAIL FROM: sender" + id + "@example.com");
                in.readLine();
                out.println("RCPT TO: " + recipient);
                in.readLine();
                out.println("DATA");
                in.readLine();
                out.println("Subject: Test Email - " + id);
                out.println("This is a test email from SMTP client " + id + ".");
                out.println(".");
                in.readLine();
                out.println("QUIT");
                in.readLine();
                System.out.println("[SMTP " + id + "] Email envoyé avec succès.");
            } catch (IOException e) {
                System.err.println("[SMTP " + id + "] Erreur : " + e.getMessage());
            }
        }
    }

    // Simulateur de client POP3
    public static class POP3ClientSimulator {
        public static void retrieveEmails(int id, String user) {
            try (Socket socket = new Socket("localhost", 1110);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                // Lire le message de bienvenue
                in.readLine();
                out.println("USER " + user);
                in.readLine();
                String password = user.equals("bob") ? "secret" : "pass123";
                out.println("PASS " + password);
                in.readLine();
                out.println("STAT");
                in.readLine();
                out.println("QUIT");
                in.readLine();
                System.out.println("[POP3 " + id + "] Emails récupérés pour " + user + ".");
            } catch (IOException e) {
                System.err.println("[POP3 " + id + "] Erreur : " + e.getMessage());
            }
        }
    }
}

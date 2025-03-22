package org.emp.gl;

import java.io.*;
import java.net.Socket;

public class TestSMTP {
    public static void main(String[] args) {
        // Création des répertoires nécessaires pour les utilisateurs
        createUserDir("bob");
        createUserDir("charlie");

        // Démarrage du serveur SMTP sur le port 2525 dans un thread séparé
        Thread smtpThread = new Thread(new SMTPServer(2525));
        smtpThread.start();

        // Attendre l'initialisation du serveur
        sleep(2000);

        // Exécution des tests
        System.out.println("Test 1 : Scénario de base");
        testBasicScenario();
        sleep(1000);

        System.out.println("\nTest 2 : Scénario avec plusieurs destinataires");
        testMultipleRecipientsScenario();
        sleep(1000);

        System.out.println("\nTest 3 : Scénario avec commandes dans le mauvais ordre");
        testWrongOrderScenario();
        sleep(1000);

        System.out.println("\nTest 4 : Scénario avec interruption de la connexion");
        testInterruptionScenario();
        sleep(1000);

        System.out.println("\nTest 5 : Scénario avec email volumineux");
        testLargeEmailScenario();
        sleep(1000);

        System.out.println("\nTest 6 : Scénario avec erreurs de syntaxe");
        testSyntaxErrorScenario();
        sleep(1000);

        System.out.println("\nTous les tests sont terminés.");
    }

    private static void createUserDir(String user) {
        File userDir = new File("mailserver/" + user);
        if (!userDir.exists()) {
            userDir.mkdirs();
            System.out.println("Création du répertoire : " + userDir.getPath());
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Test 1 : Scénario de base (envoi d'un email simple)
    private static void testBasicScenario() {
        try (Socket socket = new Socket("localhost", 2525);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // Afficher le message de bienvenue
            System.out.println(in.readLine());

            String cmd = "HELO localhost";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "MAIL FROM: alice@example.com";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "RCPT TO: bob@example.com";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "DATA";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "Subject: Test Email - Base";
            System.out.println(cmd);
            out.println(cmd);
            cmd = "Ceci est un email de test - scénario de base.";
            System.out.println(cmd);
            out.println(cmd);
            cmd = ".";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "QUIT";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Test 2 : Scénario avec plusieurs destinataires
    private static void testMultipleRecipientsScenario() {
        try (Socket socket = new Socket("localhost", 2525);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println(in.readLine());

            String cmd = "HELO localhost";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "MAIL FROM: alice@example.com";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "RCPT TO: bob@example.com";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "RCPT TO: charlie@example.com";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "DATA";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "Subject: Test Email - Multiple Recipients";
            System.out.println(cmd);
            out.println(cmd);
            cmd = "Ceci est un email destiné à plusieurs destinataires.";
            System.out.println(cmd);
            out.println(cmd);
            cmd = ".";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "QUIT";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Test 3 : Scénario avec commandes dans le mauvais ordre (DATA avant MAIL FROM)
    private static void testWrongOrderScenario() {
        try (Socket socket = new Socket("localhost", 2525);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println(in.readLine());

            String cmd = "HELO localhost";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "DATA";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "Ceci est un email qui ne devrait pas être accepté.";
            System.out.println(cmd);
            out.println(cmd);
            cmd = ".";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "QUIT";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Test 4 : Scénario avec interruption de la connexion pendant DATA
    private static void testInterruptionScenario() {
        try {
            Socket socket = new Socket("localhost", 2525);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println(in.readLine());

            String cmd = "HELO localhost";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "MAIL FROM: alice@example.com";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "RCPT TO: bob@example.com";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "DATA";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "Subject: Test Email - Interruption";
            System.out.println(cmd);
            out.println(cmd);
            cmd = "Ceci est un email partiellement envoyé.";
            System.out.println(cmd);
            out.println(cmd);
            // Simulation d'interruption : on ferme la connexion sans envoyer "."
            socket.close();
            System.out.println("Connexion interrompue avant la fin du message.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Test 5 : Scénario avec email volumineux
    private static void testLargeEmailScenario() {
        try (Socket socket = new Socket("localhost", 2525);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println(in.readLine());

            String cmd = "HELO localhost";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "MAIL FROM: alice@example.com";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "RCPT TO: bob@example.com";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "DATA";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "Subject: Test Email - Large";
            System.out.println(cmd);
            out.println(cmd);
            // Envoi d'un email volumineux (affichage périodique des commandes)
            for (int i = 0; i < 1000; i++) {
                cmd = "Ligne " + i + " de l'email volumineux.";
                if (i % 100 == 0) {
                    System.out.println(cmd);
                }
                out.println(cmd);
            }
            cmd = ".";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "QUIT";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Test 6 : Scénario avec erreur de syntaxe (MAIL FROM invalide)
    private static void testSyntaxErrorScenario() {
        try (Socket socket = new Socket("localhost", 2525);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println(in.readLine());

            String cmd = "HELO localhost";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "MAIL FROM: aliceexample.com";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

            cmd = "QUIT";
            System.out.println(cmd);
            out.println(cmd);
            System.out.println(in.readLine());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

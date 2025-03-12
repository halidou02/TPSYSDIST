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

            System.out.println("Greeting: " + in.readLine());
            out.println("HELO localhost");
            System.out.println("Response: " + in.readLine());
            out.println("MAIL FROM: alice@example.com");
            System.out.println("Response: " + in.readLine());
            out.println("RCPT TO: bob@example.com");
            System.out.println("Response: " + in.readLine());
            out.println("DATA");
            System.out.println("Response: " + in.readLine());
            out.println("Subject: Test Email - Base");
            out.println("Ceci est un email de test - scénario de base.");
            out.println(".");
            System.out.println("Response: " + in.readLine());
            out.println("QUIT");
            System.out.println("Response: " + in.readLine());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Test 2 : Scénario avec plusieurs destinataires
    private static void testMultipleRecipientsScenario() {
        try (Socket socket = new Socket("localhost", 2525);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Greeting: " + in.readLine());
            out.println("HELO localhost");
            System.out.println("Response: " + in.readLine());
            out.println("MAIL FROM: alice@example.com");
            System.out.println("Response: " + in.readLine());
            out.println("RCPT TO: bob@example.com");
            System.out.println("Response: " + in.readLine());
            out.println("RCPT TO: charlie@example.com");
            System.out.println("Response: " + in.readLine());
            out.println("DATA");
            System.out.println("Response: " + in.readLine());
            out.println("Subject: Test Email - Multiple Recipients");
            out.println("Ceci est un email destiné à plusieurs destinataires.");
            out.println(".");
            System.out.println("Response: " + in.readLine());
            out.println("QUIT");
            System.out.println("Response: " + in.readLine());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Test 3 : Scénario avec commandes dans le mauvais ordre (DATA avant MAIL FROM)
    private static void testWrongOrderScenario() {
        try (Socket socket = new Socket("localhost", 2525);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Greeting: " + in.readLine());
            out.println("HELO localhost");
            System.out.println("Response: " + in.readLine());
            // Envoi de DATA sans MAIL FROM et RCPT TO
            out.println("DATA");
            System.out.println("Response: " + in.readLine());
            out.println("Ceci est un email qui ne devrait pas être accepté.");
            out.println(".");
            System.out.println("Response: " + in.readLine());
            out.println("QUIT");
            System.out.println("Response: " + in.readLine());

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

            System.out.println("Greeting: " + in.readLine());
            out.println("HELO localhost");
            System.out.println("Response: " + in.readLine());
            out.println("MAIL FROM: alice@example.com");
            System.out.println("Response: " + in.readLine());
            out.println("RCPT TO: bob@example.com");
            System.out.println("Response: " + in.readLine());
            out.println("DATA");
            System.out.println("Response: " + in.readLine());
            out.println("Subject: Test Email - Interruption");
            out.println("Ceci est un email partiellement envoyé.");
            // Simulation d'une interruption : fermeture de la connexion sans envoyer "."
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

            System.out.println("Greeting: " + in.readLine());
            out.println("HELO localhost");
            System.out.println("Response: " + in.readLine());
            out.println("MAIL FROM: alice@example.com");
            System.out.println("Response: " + in.readLine());
            out.println("RCPT TO: bob@example.com");
            System.out.println("Response: " + in.readLine());
            out.println("DATA");
            System.out.println("Response: " + in.readLine());
            out.println("Subject: Test Email - Large");
            // Génération d'un email volumineux (ici 1000 lignes)
            for (int i = 0; i < 1000; i++) {
                out.println("Ligne " + i + " de l'email volumineux.");
            }
            out.println(".");
            System.out.println("Response: " + in.readLine());
            out.println("QUIT");
            System.out.println("Response: " + in.readLine());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Test 6 : Scénario avec erreur de syntaxe (MAIL FROM invalide)
    private static void testSyntaxErrorScenario() {
        try (Socket socket = new Socket("localhost", 2525);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Greeting: " + in.readLine());
            out.println("HELO localhost");
            System.out.println("Response: " + in.readLine());
            // Envoi d'un MAIL FROM avec une adresse invalide
            out.println("MAIL FROM: aliceexample.com");
            System.out.println("Response: " + in.readLine());
            out.println("QUIT");
            System.out.println("Response: " + in.readLine());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

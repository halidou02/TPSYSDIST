package org.emp.gl;

import java.io.*;
import java.net.Socket;

public class TestPOP3 {
    public static void main(String[] args) {
        // Création du répertoire pour l'utilisateur "bob"
        createUserDir("bob");

        // Pré-populer la boîte aux lettres avec quelques emails de test
        createEmailFile("bob", "Email 1: Test email for basic scenario.");
        createEmailFile("bob", "Email 2: Second test email.");

        // Démarrage du serveur POP3 sur le port 1110 dans un thread séparé
        Thread pop3Thread = new Thread(new POP3Server(1110));
        pop3Thread.start();

        // Attendre l'initialisation du serveur
        sleep(2000);

        // Exécution des tests POP3
        System.out.println("Test 1 : Scénario de base");
        testBasicScenario();
        sleep(1000);

        System.out.println("\nTest 2 : Scénario avec suppression de messages");
        testDeletionScenario();
        sleep(1000);

        System.out.println("\nTest 3 : Scénario avec annulation des suppressions (RSET)");
        testRsetScenario();
        sleep(1000);

        System.out.println("\nTest 4 : Scénario avec accès à un message inexistant");
        testNonExistentMessageScenario();
        sleep(1000);

        System.out.println("\nTest 5 : Scénario avec connexion interrompue");
        testInterruptionScenario();
        sleep(1000);

        System.out.println("\nTest 6 : Scénario avec une boîte aux lettres volumineuse");
        testLargeMailboxScenario();
        sleep(1000);

        System.out.println("\nTous les tests POP3 sont terminés.");
    }

    private static void createUserDir(String user) {
        File dir = new File("mailserver/" + user);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("Création du répertoire utilisateur : " + dir.getPath());
        }
    }

    private static void createEmailFile(String user, String content) {
        createUserDir(user);
        String timestamp = String.valueOf(System.currentTimeMillis());
        File emailFile = new File("mailserver/" + user, timestamp + ".txt");
        try (FileWriter fw = new FileWriter(emailFile)) {
            fw.write(content);
            System.out.println("Création d'un email pour " + user + " : " + emailFile.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Test 1 : Scénario de base
    private static void testBasicScenario() {
        try (Socket socket = new Socket("localhost", 1110);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // Affichage du message de bienvenue
            System.out.println(in.readLine());

            System.out.println("USER bob");
            out.println("USER bob");
            System.out.println(in.readLine());

            System.out.println("PASS secret");
            out.println("PASS secret");
            System.out.println(in.readLine());

            System.out.println("STAT");
            out.println("STAT");
            System.out.println(in.readLine());

            System.out.println("LIST");
            out.println("LIST");
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
                if (line.equals(".")) break;
            }

            System.out.println("RETR 1");
            out.println("RETR 1");
            while ((line = in.readLine()) != null) {
                System.out.println(line);
                if (line.equals(".")) break;
            }

            System.out.println("QUIT");
            out.println("QUIT");
            System.out.println(in.readLine());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Test 2 : Scénario avec suppression de messages
    private static void testDeletionScenario() {
        // Première partie : suppression
        try (Socket socket = new Socket("localhost", 1110);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println(in.readLine());

            System.out.println("USER bob");
            out.println("USER bob");
            System.out.println(in.readLine());

            System.out.println("PASS secret");
            out.println("PASS secret");
            System.out.println(in.readLine());

            System.out.println("DELE 1");
            out.println("DELE 1");
            System.out.println(in.readLine());

            System.out.println("QUIT");
            out.println("QUIT");
            System.out.println(in.readLine());

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Deuxième partie : vérification de la suppression
        try (Socket socket = new Socket("localhost", 1110);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println(in.readLine());

            System.out.println("USER bob");
            out.println("USER bob");
            System.out.println(in.readLine());

            System.out.println("PASS secret");
            out.println("PASS secret");
            System.out.println(in.readLine());

            System.out.println("STAT");
            out.println("STAT");
            System.out.println(in.readLine());

            System.out.println("QUIT");
            out.println("QUIT");
            System.out.println(in.readLine());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Test 3 : Scénario avec annulation des suppressions (RSET)
    private static void testRsetScenario() {
        // S'assurer qu'il y a au moins un email
        File userDir = new File("mailserver/bob");
        if (userDir.listFiles().length == 0) {
            createEmailFile("bob", "Email pour test RSET.");
        }

        try (Socket socket = new Socket("localhost", 1110);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println(in.readLine());

            System.out.println("USER bob");
            out.println("USER bob");
            System.out.println(in.readLine());

            System.out.println("PASS secret");
            out.println("PASS secret");
            System.out.println(in.readLine());

            System.out.println("DELE 1");
            out.println("DELE 1");
            System.out.println(in.readLine());

            System.out.println("RSET");
            out.println("RSET");
            System.out.println(in.readLine());

            System.out.println("STAT");
            out.println("STAT");
            System.out.println(in.readLine());

            System.out.println("QUIT");
            out.println("QUIT");
            System.out.println(in.readLine());

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    // Test 4 : Scénario avec accès à un message inexistant
    private static void testNonExistentMessageScenario() {
        try (Socket socket = new Socket("localhost", 1110);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println(in.readLine());

            System.out.println("USER bob");
            out.println("USER bob");
            System.out.println(in.readLine());

            System.out.println("PASS secret");
            out.println("PASS secret");
            System.out.println(in.readLine());

            System.out.println("RETR 999");
            out.println("RETR 999");
            System.out.println(in.readLine());

            System.out.println("QUIT");
            out.println("QUIT");
            System.out.println(in.readLine());

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    // Test 5 : Scénario avec connexion interrompue
    private static void testInterruptionScenario() {
        try {
            Socket socket = new Socket("localhost", 1110);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println(in.readLine());

            System.out.println("USER bob");
            out.println("USER bob");
            System.out.println(in.readLine());

            System.out.println("PASS secret");
            out.println("PASS secret");
            System.out.println(in.readLine());

            System.out.println("DELE 1");
            out.println("DELE 1");
            System.out.println(in.readLine());

            socket.close();
            System.out.println("Connexion interrompue avant QUIT.");

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    // Test 6 : Scénario avec une boîte aux lettres volumineuse
    private static void testLargeMailboxScenario() {
        File userDir = new File("mailserver/bob");
        File[] files = userDir.listFiles();
        if (files == null || files.length < 20) {
            for (int i = 0; i < 20; i++) {
                createEmailFile("bob", "Email volumineux numéro " + i);
            }
        }

        try (Socket socket = new Socket("localhost", 1110);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println(in.readLine());

            System.out.println("USER bob");
            out.println("USER bob");
            System.out.println(in.readLine());

            System.out.println("PASS secret");
            out.println("PASS secret");
            System.out.println(in.readLine());

            System.out.println("STAT");
            out.println("STAT");
            System.out.println(in.readLine());

            System.out.println("LIST");
            out.println("LIST");
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
                if (line.equals(".")) break;
            }

            System.out.println("RETR 1");
            out.println("RETR 1");
            while ((line = in.readLine()) != null) {
                System.out.println(line);
                if (line.equals(".")) break;
            }

            System.out.println("QUIT");
            out.println("QUIT");
            System.out.println(in.readLine());

        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}

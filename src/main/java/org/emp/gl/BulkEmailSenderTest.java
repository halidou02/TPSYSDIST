package org.emp.gl;

import java.io.*;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;

import org.emp.auth.AuthService;
import org.emp.auth.Email; // Assurez-vous que cette classe existe et est accessible

public class BulkEmailSenderTest { // Renommé implicitement en FunctionalIntegrationTest

    // --- Configuration ---
    private static final String RMI_HOST = "localhost";
    private static final int RMI_PORT = 1099;
    private static final String RMI_SERVICE_NAME = "AuthService";

    private static final String SMTP_HOST = "localhost";
    private static final int SMTP_PORT = 2525;

    private static final String POP3_HOST = "localhost";
    private static final int POP3_PORT = 1110;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/maildb?serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    // --- Point d'Entrée Principal des Tests ---
    public static void main(String[] args) {
        System.out.println("=== Démarrage des Tests d'Intégration Fonctionnels ===");
        System.out.println("Assurez-vous que les serveurs RMI, SMTP et POP3 sont démarrés.");
        System.out.println("Assurez-vous que les utilisateurs 'bob' et 'charlie' existent en DB.");

        boolean allTestsPassed = true;

        // Test 1: Service d'Authentification
        boolean authTestResult = testAuthenticationService();
        printTestResult("Authentification (RMI & DB)", authTestResult);
        if (!authTestResult) allTestsPassed = false;

        // Test 2: Envoi SMTP et Vérification DB
        boolean smtpTestResult = testSmtpSendAndVerify();
        printTestResult("Envoi SMTP & Vérification DB", smtpTestResult);
        if (!smtpTestResult) allTestsPassed = false;

        // Test 3: Récupération/Suppression POP3 et Vérification DB
        boolean pop3TestResult = testPop3RetrieveAndDelete();
        printTestResult("Récupération/Suppression POP3 & Vérification DB", pop3TestResult);
        if (!pop3TestResult) allTestsPassed = false;


        System.out.println("\n=== Fin des Tests d'Intégration Fonctionnels ===");
        System.out.println("Résultat Global: " + (allTestsPassed ? "TOUS LES TESTS ONT RÉUSSI" : "AU MOINS UN TEST A ÉCHOUÉ"));
    }

    // --- Méthodes de Test ---

    private static boolean testAuthenticationService() {
        System.out.println("\n--- Test du Service d'Authentification ---");
        AuthService authService = getAuthService();
        if (authService == null) return false;

        String testUser = "testuser_auth_" + System.currentTimeMillis(); // Unique user for test
        String testPass = "password123";
        String badPass = "wrongpassword";
        boolean success = true;

        // Cleanup initial (au cas où un test précédent aurait échoué)
        System.out.println("Nettoyage initial pour " + testUser + "...");
        try {
            deleteUserFromDb(testUser); // Suppression directe en DB pour s'assurer
        } catch (SQLException e) {
            System.err.println("Erreur lors du nettoyage initial en DB: " + e.getMessage());
            // On continue quand même, l'étape de création devrait échouer si l'utilisateur existe
        }

        try {
            // 1. Création Utilisateur (via RMI)
            System.out.print("Test Création Utilisateur (" + testUser + ")... ");
            if (!authService.createUser(testUser, testPass)) {
                System.out.println("ÉCHEC (RMI a retourné false)");
                success = false;
            } else {
                 // 2. Vérification Création (via DB)
                if (!checkUserExistsInDb(testUser)) {
                    System.out.println("ÉCHEC (Utilisateur non trouvé en DB après création RMI)");
                    success = false;
                } else {
                    System.out.println("SUCCÈS");
                }
            }

            // 3. Authentification (Bon Mot de Passe - via RMI)
            System.out.print("Test Authentification (Bon MDP)... ");
            if (success && !authService.authenticate(testUser, testPass)) {
                System.out.println("ÉCHEC");
                success = false;
            } else if (success) {
                System.out.println("SUCCÈS");
            }

            // 4. Authentification (Mauvais Mot de Passe - via RMI)
            System.out.print("Test Authentification (Mauvais MDP)... ");
            if (success && authService.authenticate(testUser, badPass)) {
                System.out.println("ÉCHEC (Authentification réussie avec mauvais MDP)");
                success = false;
            } else if (success) {
                System.out.println("SUCCÈS");
            }

            // 5. Authentification (Utilisateur Inexistant - via RMI)
            String nonExistentUser = "nouser_" + System.currentTimeMillis();
            System.out.print("Test Authentification (Utilisateur Inexistant)... ");
             if (success && authService.authenticate(nonExistentUser, testPass)) {
                System.out.println("ÉCHEC (Authentification réussie pour utilisateur inexistant)");
                success = false;
            } else if (success) {
                System.out.println("SUCCÈS");
            }

            // 6. Suppression Utilisateur (via RMI)
            System.out.print("Test Suppression Utilisateur (" + testUser + ")... ");
            if (success && !authService.deleteUser(testUser)) {
                 System.out.println("ÉCHEC (RMI a retourné false)");
                 success = false;
            } else if (success) {
                 // 7. Vérification Suppression (via DB)
                if (checkUserExistsInDb(testUser)) {
                    System.out.println("ÉCHEC (Utilisateur trouvé en DB après suppression RMI)");
                    success = false;
                } else {
                    System.out.println("SUCCÈS");
                }
            }

        } catch (Exception e) {
            System.err.println("\nErreur inattendue pendant le test d'authentification: " + e.getMessage());
            e.printStackTrace();
            success = false;
        } finally {
             // Nettoyage final au cas où la suppression RMI aurait échoué mais l'utilisateur existe en DB
            if (success) { // Only try final DB cleanup if tests seemed ok until the end
                 try {
                    if(checkUserExistsInDb(testUser)) {
                        System.out.println("Nettoyage final nécessaire pour " + testUser);
                        deleteUserFromDb(testUser);
                    }
                 } catch(SQLException e) {
                     System.err.println("Erreur lors du nettoyage final en DB: " + e.getMessage());
                 }
            }
        }
        return success;
    }

    private static boolean testSmtpSendAndVerify() {
        System.out.println("\n--- Test Envoi SMTP & Vérification DB ---");
        String sender = "bob@example.com";
        String recipient = "charlie@example.com";
        String subject = "SMTP Test " + System.currentTimeMillis(); // Unique subject
        String body = "Corps de l'email de test SMTP.";
        boolean success = true;
        int emailId = -1;

        try {
            // 1. Envoyer l'email via SMTP
            System.out.print("Envoi de l'email via SMTP (Sujet: " + subject + ")... ");
            boolean sent = sendSmtpEmail(sender, recipient, subject, body);
            if (!sent) {
                System.out.println("ÉCHEC (La méthode d'envoi a retourné false)");
                return false; // Pas la peine de continuer si l'envoi échoue
            }
            System.out.println("SUCCÈS (Tentative d'envoi SMTP terminée)");

            // AJOUT: Petite pause pour laisser le temps à la DB de commiter/rendre visible
            System.out.println("Pause de 500ms avant vérification DB...");
            Thread.sleep(500);

            // 2. Vérifier l'email dans la DB (via JDBC)
            System.out.print("Vérification de l'email en DB... ");
            emailId = findEmailIdInDb(recipient, subject); // Recherche par destinataire et sujet unique
            if (emailId == -1) {
                System.out.println("ÉCHEC (Email non trouvé en DB)");
                success = false;
            } else {
                System.out.println("SUCCÈS (Email trouvé avec ID: " + emailId + ")");
            }

        } catch (InterruptedException ie) {
            System.err.println("Thread interrompu pendant la pause.");
            Thread.currentThread().interrupt(); // Restaurer le statut d'interruption
            success = false;
        } catch (Exception e) {
            System.err.println("\nErreur inattendue pendant le test SMTP: " + e.getMessage());
            e.printStackTrace();
            success = false;
        } finally {
            // 3. Nettoyage : Supprimer l'email de la DB
            if (emailId != -1) {
                System.out.print("Nettoyage de l'email de test (ID: " + emailId + ")... ");
                try {
                    if (deleteEmailFromDbById(emailId)) {
                        System.out.println("SUCCÈS");
                    } else {
                        System.out.println("ÉCHEC");
                        // Ne pas marquer le test global comme échoué juste pour le nettoyage
                    }
                } catch (SQLException e) {
                    System.err.println("Erreur lors du nettoyage de l'email en DB: " + e.getMessage());
                }
            }
        }
        return success;
    }

     private static boolean testPop3RetrieveAndDelete() {
        System.out.println("\n--- Test Récupération/Suppression POP3 & Vérification DB ---");
        String user = "charlie"; // Nom d'utilisateur pour POP3
        String userEmail = "charlie@example.com"; // Email complet pour l'envoi initial
        String pass = "password_charlie"; // Mot de passe de charlie
        String testSender = "bob@example.com";
        String testSubject = "POP3 Test " + System.currentTimeMillis(); // Sujet unique
        String testBody = "Email pour test POP3.";
        boolean success = true;
        int emailId = -1;

        Socket pop3Socket = null;
        PrintWriter pop3Out = null;
        BufferedReader pop3In = null;

        try {
            // 1. Setup: Envoyer un email à l'utilisateur pour le test
            System.out.print("Setup: Envoi de l'email de test POP3 (Sujet: " + testSubject + ")... ");
            if (!sendSmtpEmail(testSender, userEmail, testSubject, testBody)) {
                 System.out.println("ÉCHEC (Impossible d'envoyer l'email initial)");
                 return false;
            }
             // Attendre un court instant pour que l'email soit traité et stocké
            // Thread.sleep(1000); // Ancienne pause
            // AJOUT: Pause un peu plus longue pour être sûr
            System.out.println("Pause de 1000ms après envoi SMTP de setup...");
            Thread.sleep(1000);

            emailId = findEmailIdInDb(userEmail, testSubject);
            if (emailId == -1) {
                System.out.println("ÉCHEC (Email de setup non trouvé en DB)");
                return false;
            }
            System.out.println("SUCCÈS (Email de setup envoyé et trouvé en DB, ID: " + emailId + ")");


            // 2. Connexion POP3 et Authentification
            System.out.print("Connexion et Authentification POP3 pour " + user + "... ");
            pop3Socket = new Socket(POP3_HOST, POP3_PORT);
            pop3Out = new PrintWriter(pop3Socket.getOutputStream(), true);
            pop3In = new BufferedReader(new InputStreamReader(pop3Socket.getInputStream()));

            if (!readPop3Response(pop3In).startsWith("+OK")) throw new IOException("Réponse POP3 initiale invalide");
            pop3Out.println("USER " + user);
            if (!readPop3Response(pop3In).startsWith("+OK")) throw new IOException("Commande USER échouée");
            pop3Out.println("PASS " + pass);
            if (!readPop3Response(pop3In).startsWith("+OK")) throw new IOException("Commande PASS échouée");
            System.out.println("SUCCÈS");

            // 3. Trouver le numéro du message (via LIST)
            System.out.print("Recherche du message via LIST... ");
            pop3Out.println("LIST");
            String listResponse = readPop3Response(pop3In); // +OK ...
            if (!listResponse.startsWith("+OK")) throw new IOException("Commande LIST échouée");

            int messageNumber = -1;
            String line;
            int currentLineNum = 1;
            // Lire les lignes jusqu'au "." final
            List<String> listLines = new ArrayList<>();
            while (!(line = readPop3Response(pop3In)).equals(".")) {
                 listLines.add(line);
                 // On ne peut pas se fier à l'ID de la DB ici, il faut trouver le bon message
                 // Pour ce test, on suppose que c'est le seul/dernier message.
                 // Une approche plus robuste chercherait l'ID via RETR + parsing, mais c'est complexe.
                 // On va juste prendre le dernier message listé pour ce test simple.
                 messageNumber = currentLineNum;
                 currentLineNum++;
            }

            if (messageNumber == -1) {
                 // Essayer avec STAT pour voir s'il y a des messages
                 pop3Out.println("STAT");
                 String statResponse = readPop3Response(pop3In);
                 if (statResponse.startsWith("+OK")) {
                     String[] parts = statResponse.split(" ");
                     if (Integer.parseInt(parts[1]) > 0) {
                         // Il y a des messages, mais LIST n'a rien retourné ou on n'a pas trouvé le nôtre?
                         // Pour ce test, on va supposer que le dernier message est le nôtre.
                         messageNumber = Integer.parseInt(parts[1]);
                         System.out.print("(Trouvé via STAT: " + messageNumber + ") ");
                     }
                 }
            }


            if (messageNumber == -1) {
                throw new IOException("Impossible de trouver le numéro du message de test via LIST/STAT");
            }
            System.out.println("SUCCÈS (Message trouvé, numéro: " + messageNumber + ")");


            // 4. Récupération (RETR) - Optionnel mais bon à vérifier
            System.out.print("Récupération du message " + messageNumber + " via RETR... ");
            pop3Out.println("RETR " + messageNumber);
            if (!readPop3Response(pop3In).startsWith("+OK")) throw new IOException("Commande RETR échouée");
            // Lire le contenu jusqu'au "."
            while (!(line = readPop3Response(pop3In)).equals(".")) {
                // On pourrait vérifier le contenu ici si nécessaire
            }
            System.out.println("SUCCÈS");


            // 5. Suppression (DELE)
            System.out.print("Suppression du message " + messageNumber + " via DELE... ");
            pop3Out.println("DELE " + messageNumber);
             if (!readPop3Response(pop3In).startsWith("+OK")) throw new IOException("Commande DELE échouée");
            System.out.println("SUCCÈS");

            // 6. Quitter (QUIT) - Finalise la suppression
            System.out.print("Déconnexion via QUIT... ");
            pop3Out.println("QUIT");
            if (!readPop3Response(pop3In).startsWith("+OK")) throw new IOException("Commande QUIT échouée");
            System.out.println("SUCCÈS");
            closePop3(pop3Socket, pop3Out, pop3In); // Fermer la connexion


            // 7. Vérification de la Suppression en DB (via JDBC)
            System.out.print("Vérification de la suppression en DB (ID: " + emailId + ")... ");
             // Attendre un peu que le serveur traite la suppression après QUIT
            Thread.sleep(500);
            if (checkEmailExistsInDbById(emailId)) {
                 System.out.println("ÉCHEC (Email toujours présent en DB)");
                 success = false;
            } else {
                 System.out.println("SUCCÈS");
            }

        } catch (InterruptedException ie) {
            System.err.println("Thread interrompu pendant la pause.");
            Thread.currentThread().interrupt(); // Restaurer le statut d'interruption
            success = false;
        } catch (Exception e) {
            System.err.println("\nErreur inattendue pendant le test POP3: " + e.getMessage());
            e.printStackTrace();
            success = false;
        } finally {
            closePop3(pop3Socket, pop3Out, pop3In); // Assurer la fermeture
            // Nettoyage final en DB au cas où POP3 aurait échoué avant QUIT
            if (emailId != -1) {
                try {
                    if (checkEmailExistsInDbById(emailId)) {
                         System.out.println("Nettoyage final nécessaire pour l'email POP3 (ID: " + emailId + ")");
                         deleteEmailFromDbById(emailId);
                    }
                } catch (SQLException sqle) {
                     System.err.println("Erreur lors du nettoyage final de l'email POP3 en DB: " + sqle.getMessage());
                }
            }
        }
        return success;
    }


    // --- Méthodes Utilitaires (Helpers) ---

    private static void printTestResult(String testName, boolean success) {
        System.out.println("--- Résultat Test [" + testName + "]: " + (success ? "SUCCÈS" : "ÉCHEC") + " ---");
    }

    // Helper: Récupérer le service RMI
    private static AuthService getAuthService() {
        try {
            Registry registry = LocateRegistry.getRegistry(RMI_HOST, RMI_PORT);
            return (AuthService) registry.lookup(RMI_SERVICE_NAME);
        } catch (Exception e) {
            System.err.println("Impossible de récupérer le service RMI '" + RMI_SERVICE_NAME + "': " + e.getMessage());
            // e.printStackTrace();
            return null;
        }
    }

    // Helper: Obtenir une connexion DB
    private static Connection getDbConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    // Helper: Vérifier si un utilisateur existe en DB
    private static boolean checkUserExistsInDb(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = getDbConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

     // Helper: Supprimer un utilisateur de la DB
    private static boolean deleteUserFromDb(String username) throws SQLException {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = getDbConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    // Helper: Envoyer un email via SMTP (adapté de l'ancien BulkEmailSenderTest)
    private static boolean sendSmtpEmail(String from, String to, String subject, String body) {
        try (Socket socket = new Socket(SMTP_HOST, SMTP_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            if (!readSmtpResponse(in).startsWith("220")) return false; // Welcome

            out.println("HELO " + InetAddress.getLocalHost().getHostName());
            if (!readSmtpResponse(in).startsWith("250")) return false; // HELO OK

            out.println("MAIL FROM:<" + from + ">");
            if (!readSmtpResponse(in).startsWith("250")) return false; // MAIL FROM OK

            out.println("RCPT TO:<" + to + ">");
            if (!readSmtpResponse(in).startsWith("250")) return false; // RCPT TO OK

            out.println("DATA");
            if (!readSmtpResponse(in).startsWith("354")) return false; // DATA OK

            out.println("From: " + from);
            out.println("To: " + to);
            out.println("Subject: " + subject);
            out.println();
            out.println(body);
            out.println(".");
            if (!readSmtpResponse(in).startsWith("250")) return false; // Message accepted OK

            out.println("QUIT");
            readSmtpResponse(in); // Read QUIT response

            return true;

        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi SMTP: " + e.getMessage());
            // e.printStackTrace();
            return false;
        }
    }

     // Helper: Lire une réponse SMTP (avec log simple)
    private static String readSmtpResponse(BufferedReader in) throws IOException {
        String response = in.readLine();
        System.out.println("SMTP Serveur: " + response); // Décommenté pour debug SMTP détaillé
        if (response == null) throw new IOException("Réponse nulle du serveur SMTP");
        return response;
    }

     // Helper: Trouver l'ID d'un email en DB par destinataire et sujet
    private static int findEmailIdInDb(String recipient, String subject) throws SQLException {
        System.out.println("findEmailIdInDb - Searching for recipient='" + recipient + "', subject='" + subject + "'"); // Log parameters
        String sql = "SELECT id FROM emails WHERE recipient = ? AND subject = ? ORDER BY timestamp DESC LIMIT 1";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int foundId = -1;

        try {
            conn = getDbConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, recipient);
            pstmt.setString(2, subject);

            System.out.println("findEmailIdInDb - Executing query: " + pstmt.toString()); // Log the prepared statement (may not show values)
            rs = pstmt.executeQuery();

            if (rs.next()) {
                foundId = rs.getInt("id");
                System.out.println("findEmailIdInDb - Found email with ID: " + foundId);
            } else {
                System.out.println("findEmailIdInDb - No matching email found.");
                // Tentative de recherche plus large pour le débogage
                System.out.println("findEmailIdInDb - Trying broader search for recipient='" + recipient + "'...");
                sql = "SELECT id, subject, timestamp FROM emails WHERE recipient = ? ORDER BY timestamp DESC LIMIT 5";
                try (PreparedStatement pstmtBroad = conn.prepareStatement(sql)) {
                    pstmtBroad.setString(1, recipient);
                    try (ResultSet rsBroad = pstmtBroad.executeQuery()) {
                        int count = 0;
                        while(rsBroad.next()) {
                            count++;
                            System.out.println("  -> Found recent email: ID=" + rsBroad.getInt("id") + ", Subject=" + rsBroad.getString("subject") + ", Time=" + rsBroad.getTimestamp("timestamp"));
                        }
                        if (count == 0) {
                             System.out.println("  -> No recent emails found for this recipient either.");
                        }
                    }
                } catch (SQLException broadEx) {
                     System.err.println("  -> Error during broader search: " + broadEx.getMessage());
                }
            }
        } catch (SQLException e) {
             System.err.println("findEmailIdInDb - SQL Error during search: " + e.getMessage());
             throw e; // Re-throw the exception after logging
        } finally {
             // Close resources
            if (rs != null) try { rs.close(); } catch (SQLException e) { /* ignored */ }
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) { /* ignored */ }
            if (conn != null) try { conn.close(); } catch (SQLException e) { /* ignored */ }
        }
        return foundId;
    }

    // Helper: Vérifier si un email existe par ID
    private static boolean checkEmailExistsInDbById(int emailId) throws SQLException {
         String sql = "SELECT COUNT(*) FROM emails WHERE id = ?";
        try (Connection conn = getDbConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, emailId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // Helper: Supprimer un email de la DB par ID
    private static boolean deleteEmailFromDbById(int emailId) throws SQLException {
        // On suppose qu'il existe une procédure delete_email(id)
        String sql = "{CALL delete_email(?)}";
        try (Connection conn = getDbConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {
            cstmt.setInt(1, emailId);
            // L'exécution de la procédure peut ou non retourner un nombre de lignes affectées.
            // On considère que si l'exécution ne lève pas d'exception, c'est un succès.
            cstmt.execute();
            return true; // Simplification: on suppose que ça marche si pas d'erreur SQL
        } catch (SQLException e) {
             System.err.println("Erreur SQL lors de l'appel à delete_email(" + emailId + "): " + e.getMessage());
             return false; // Échec si erreur SQL
        }
    }

    // Helper: Lire une réponse POP3 (avec log simple)
    private static String readPop3Response(BufferedReader in) throws IOException {
        String response = in.readLine();
        System.out.println("POP3 Serveur: " + response); // Décommenté pour debug POP3 détaillé
        if (response == null) throw new IOException("Réponse nulle du serveur POP3");
        return response;
    }

    // Helper: Fermer la connexion POP3 proprement
    private static void closePop3(Socket socket, PrintWriter out, BufferedReader in) {
        try { if (in != null) in.close(); } catch (IOException e) {}
        try { if (out != null) out.close(); } catch (Exception e) {} // PrintWriter ne lance pas IOException sur close
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException e) {}
    }
}

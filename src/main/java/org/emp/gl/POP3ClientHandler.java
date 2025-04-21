package org.emp.gl;

import java.io.*;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import org.emp.auth.AuthService;
import org.emp.auth.Email;
import org.emp.auth.MySQLEmailService;

public class POP3ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private POP3State state;
    private String currentUser; // Stocke le nom d'utilisateur (ex: "charlie")
    private List<Email> emailList;
    private AuthService authService;
    private String userDomain = "@example.com"; // Domaine par défaut pour reconstruire l'email

    public POP3ClientHandler(Socket socket) {
        this.socket = socket;
        this.state = POP3State.AUTHORIZATION;
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            authService = (AuthService) registry.lookup("AuthService");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println("+OK POP3 server ready");

            String line;
            while ((line = in.readLine()) != null) {
                String[] tokens = line.split(" ");
                String command = tokens[0].toUpperCase();

                switch (command) {
                    case "USER":
                        if (tokens.length < 2) {
                            out.println("-ERR Missing username");
                        } else {
                            String user = tokens[1];
                            if (authService.userExists(user)) {
                                currentUser = user;
                                out.println("+OK User accepted");
                            } else {
                                out.println("-ERR No such user");
                            }
                        }
                        break;

                    case "PASS":
                        if (currentUser == null) {
                            out.println("-ERR USER required before PASS");
                        } else if (tokens.length < 2) {
                            out.println("-ERR Missing password");
                        } else {
                            String password = tokens[1];
                            // Authentification via RMI (qui utilise MySQLAuthService -> authenticate_user)
                            if (authService.authenticate(currentUser, password)) {
                                state = POP3State.TRANSACTION;
                                loadEmails(); // Charger les emails APRÈS authentification réussie
                                out.println("+OK Password accepted, " + emailList.size() + " messages ready."); // Inclure le nombre de messages
                            } else {
                                out.println("-ERR Invalid password");
                                currentUser = null; // Réinitialiser currentUser si le mot de passe est faux
                            }
                        }
                        break;

                    case "STAT":
                        if (state != POP3State.TRANSACTION) {
                            out.println("-ERR Not in transaction state");
                        } else {
                            int count = emailList.size();
                            int totalSize = 0;
                            for (Email email : emailList) {
                                totalSize += email.getContent().length();
                            }
                            out.println("+OK " + count + " " + totalSize);
                        }
                        break;

                    case "LIST":
                        if (state != POP3State.TRANSACTION) {
                            out.println("-ERR Not in transaction state");
                        } else {
                            out.println("+OK Listing messages");
                            int index = 1;
                            for (Email email : emailList) {
                                out.println(index++ + " " + email.getContent().length());
                            }
                            out.println(".");
                        }
                        break;

                    case "RETR":
                        if (state != POP3State.TRANSACTION) {
                            out.println("-ERR Not in transaction state");
                        } else if (tokens.length < 2) {
                            out.println("-ERR Missing message number");
                        } else {
                            int msgNum = Integer.parseInt(tokens[1]) - 1;
                            if (msgNum < 0 || msgNum >= emailList.size()) {
                                out.println("-ERR No such message");
                            } else {
                                Email email = emailList.get(msgNum);
                                out.println("+OK Message follows");
                                BufferedReader reader = new BufferedReader(new StringReader(email.getContent()));
                                String msgLine;
                                while ((msgLine = reader.readLine()) != null) {
                                    out.println(msgLine);
                                }
                                out.println(".");
                            }
                        }
                        break;

                    case "DELE":
                        if (state != POP3State.TRANSACTION) {
                            out.println("-ERR Not in transaction state");
                        } else if (tokens.length < 2) {
                            out.println("-ERR Missing message number");
                        } else {
                            int msgNum = Integer.parseInt(tokens[1]) - 1;
                            if (msgNum < 0 || msgNum >= emailList.size()) {
                                out.println("-ERR No such message");
                            } else {
                                Email email = emailList.get(msgNum);
                                if (MySQLEmailService.deleteEmail(email.getId())) {
                                    emailList.remove(msgNum);
                                    out.println("+OK Message deleted");
                                } else {
                                    out.println("-ERR Could not delete message");
                                }
                            }
                        }
                        break;

                    case "QUIT":
                        out.println("+OK Goodbye");
                        return;

                    default:
                        out.println("-ERR Command not recognized");
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("POP3 Connection error: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException e) { }
        }
    }

    private void loadEmails() {
        if (currentUser == null) {
            System.err.println("POP3 Handler: Tentative de charger les emails sans utilisateur authentifié.");
            emailList = new ArrayList<>(); // Assurer que la liste est vide
            return;
        }
        // Construire l'adresse email complète pour la recherche en DB
        String fullEmailAddress = currentUser + userDomain;
        System.out.println("POP3 Handler: Chargement des emails pour l'adresse: " + fullEmailAddress);
        emailList = MySQLEmailService.fetchEmails(fullEmailAddress); // Utiliser l'adresse complète
        System.out.println("POP3 Handler: " + emailList.size() + " emails chargés.");
    }
}

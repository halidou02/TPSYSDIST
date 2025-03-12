package org.emp.gl;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class POP3ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String currentUser;
    private List<File> emailFiles; // Liste des emails dans le répertoire utilisateur
    private Set<Integer> deletedMessages; // Indices des messages marqués pour suppression

    public POP3ClientHandler(Socket socket) {
        this.socket = socket;
        this.deletedMessages = new HashSet<>();
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Envoi du message de bienvenue
            out.println("+OK POP3 server ready");

            boolean authenticated = false;
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("POP3 Received: " + line);
                String[] tokens = line.split(" ");
                String command = tokens[0].toUpperCase();

                switch (command) {
                    case "USER":
                        if (tokens.length < 2) {
                            out.println("-ERR Missing username");
                        } else {
                            String user = tokens[1];
                            if (checkUserExists(user)) {
                                currentUser = user;
                                loadEmails();
                                out.println("+OK User accepted");
                            } else {
                                out.println("-ERR No such user");
                            }
                        }
                        break;

                    case "PASS":
                        if (currentUser == null) {
                            out.println("-ERR USER required before PASS");
                        } else {
                            // Pour simplifier, on accepte tout mot de passe
                            authenticated = true;
                            out.println("+OK Password accepted");
                        }
                        break;

                    case "STAT":
                        if (!authenticated) {
                            out.println("-ERR Not authenticated");
                        } else {
                            int count = emailFiles.size() - deletedMessages.size();
                            int totalSize = 0;
                            for (int i = 0; i < emailFiles.size(); i++) {
                                if (!deletedMessages.contains(i)) {
                                    totalSize += (int) emailFiles.get(i).length();
                                }
                            }
                            out.println("+OK " + count + " " + totalSize);
                        }
                        break;

                    case "LIST":
                        if (!authenticated) {
                            out.println("-ERR Not authenticated");
                        } else {
                            out.println("+OK Listing messages:");
                            for (int i = 0; i < emailFiles.size(); i++) {
                                if (!deletedMessages.contains(i)) {
                                    out.println((i + 1) + " " + emailFiles.get(i).length());
                                }
                            }
                            out.println(".");
                        }
                        break;

                    case "RETR":
                        if (!authenticated) {
                            out.println("-ERR Not authenticated");
                        } else if (tokens.length < 2) {
                            out.println("-ERR Missing message number");
                        } else {
                            try {
                                int msgNum = Integer.parseInt(tokens[1]) - 1;
                                if (msgNum < 0 || msgNum >= emailFiles.size() || deletedMessages.contains(msgNum)) {
                                    out.println("-ERR No such message");
                                } else {
                                    File email = emailFiles.get(msgNum);
                                    out.println("+OK Message follows");
                                    BufferedReader emailReader = new BufferedReader(new FileReader(email));
                                    String emailLine;
                                    while ((emailLine = emailReader.readLine()) != null) {
                                        out.println(emailLine);
                                    }
                                    emailReader.close();
                                    out.println(".");
                                }
                            } catch (NumberFormatException e) {
                                out.println("-ERR Invalid message number");
                            }
                        }
                        break;

                    case "DELE":
                        if (!authenticated) {
                            out.println("-ERR Not authenticated");
                        } else if (tokens.length < 2) {
                            out.println("-ERR Missing message number");
                        } else {
                            try {
                                int msgNum = Integer.parseInt(tokens[1]) - 1;
                                if (msgNum < 0 || msgNum >= emailFiles.size() || deletedMessages.contains(msgNum)) {
                                    out.println("-ERR No such message");
                                } else {
                                    deletedMessages.add(msgNum);
                                    out.println("+OK Message marked for deletion");
                                }
                            } catch (NumberFormatException e) {
                                out.println("-ERR Invalid message number");
                            }
                        }
                        break;

                    case "RSET":
                        if (!authenticated) {
                            out.println("-ERR Not authenticated");
                        } else {
                            deletedMessages.clear();
                            out.println("+OK Deletion marks removed");
                        }
                        break;

                    case "NOOP":
                        out.println("+OK");
                        break;

                    case "QUIT":
                        if (authenticated) {
                            // Appliquer les suppressions marquées
                            for (Integer index : deletedMessages) {
                                File email = emailFiles.get(index);
                                if (email.exists()) {
                                    email.delete();
                                }
                            }
                        }
                        out.println("+OK Goodbye");
                        return; // Fin de la session

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

    // Vérifie l'existence du répertoire utilisateur dans "mailserver/"
    private boolean checkUserExists(String user) {
        File userDir = new File("mailserver/" + user);
        return userDir.exists() && userDir.isDirectory();
    }

    // Charge la liste des emails présents dans le répertoire de l'utilisateur
    private void loadEmails() {
        emailFiles = new ArrayList<>();
        File userDir = new File("mailserver/" + currentUser);
        File[] files = userDir.listFiles();
        if (files != null) {
            // Tri par nom (supposé être un timestamp)
            Arrays.sort(files, (f1, f2) -> f1.getName().compareTo(f2.getName()));
            emailFiles.addAll(Arrays.asList(files));
        }
    }
}


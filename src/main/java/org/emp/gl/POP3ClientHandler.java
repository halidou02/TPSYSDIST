package org.emp.gl;

import java.io.*;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import org.emp.auth.AuthService;  // Import du service d'authentification

public class POP3ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private POP3State state;
    private String currentUser;
    private List<File> emailFiles;
    private Set<Integer> deletedMessages;
    private AuthService authService; // Référence au service RMI

    public POP3ClientHandler(Socket socket) {
        this.socket = socket;
        this.state = POP3State.AUTHORIZATION;
        this.deletedMessages = new HashSet<>();
        try {
            // Connexion au registre RMI
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            authService = (AuthService) registry.lookup("AuthService");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
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
                        if (state != POP3State.AUTHORIZATION) {
                            out.println("-ERR Command not allowed in current state");
                        } else if (tokens.length < 2) {
                            out.println("-ERR Missing username");
                        } else {
                            String user = tokens[1];
                            try {
                                if (authService.userExists(user)) {
                                    currentUser = user;
                                    loadEmails();
                                    out.println("+OK User accepted");
                                } else {
                                    out.println("-ERR No such user");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                out.println("-ERR Internal server error");
                            }
                        }
                        break;
                    case "PASS":
                        if (state != POP3State.AUTHORIZATION || currentUser == null) {
                            out.println("-ERR USER required before PASS");
                        } else if (tokens.length < 2) {
                            out.println("-ERR Missing password");
                        } else {
                            String password = tokens[1];
                            try {
                                if (authService.authenticate(currentUser, password)) {
                                    state = POP3State.TRANSACTION;
                                    out.println("+OK Password accepted");
                                } else {
                                    out.println("-ERR Invalid password");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                out.println("-ERR Internal server error");
                            }
                        }
                        break;
                    // ... (le reste des commandes STAT, LIST, RETR, DELE, RSET, NOOP, QUIT reste inchangé)
                    case "STAT":
                        if (state != POP3State.TRANSACTION) {
                            out.println("-ERR Not in transaction state");
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
                    // ... (les autres cas restent identiques)
                    case "QUIT":
                        if (state == POP3State.TRANSACTION) {
                            for (Integer index : deletedMessages) {
                                File email = emailFiles.get(index);
                                if (email.exists()) {
                                    email.delete();
                                }
                            }
                        }
                        state = POP3State.UPDATE;
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
        emailFiles = new ArrayList<>();
        File userDir = new File("mailserver/" + currentUser);
        File[] files = userDir.listFiles();
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName));
            emailFiles.addAll(Arrays.asList(files));
        }
    }
}

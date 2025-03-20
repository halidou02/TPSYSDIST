package org.emp.gl;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class POP3ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private POP3State state;
    private String currentUser;
    private List<File> emailFiles;
    private Set<Integer> deletedMessages;

    public POP3ClientHandler(Socket socket) {
        this.socket = socket;
        this.state = POP3State.AUTHORIZATION;
        this.deletedMessages = new HashSet<>();
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
                        if (state != POP3State.AUTHORIZATION || currentUser == null) {
                            out.println("-ERR USER required before PASS");
                        } else if (tokens.length < 2) {
                            out.println("-ERR Missing password");
                        } else {
                            String password = tokens[1];
                            if (authenticateUser(currentUser, password)) {
                                state = POP3State.TRANSACTION;
                                out.println("+OK Password accepted");
                            } else {
                                out.println("-ERR Invalid password");
                            }
                        }
                        break;
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
                    case "LIST":
                        if (state != POP3State.TRANSACTION) {
                            out.println("-ERR Not in transaction state");
                        } else {
                            out.println("+OK Listing messages");
                            for (int i = 0; i < emailFiles.size(); i++) {
                                if (!deletedMessages.contains(i)) {
                                    out.println((i + 1) + " " + emailFiles.get(i).length());
                                }
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
                        if (state != POP3State.TRANSACTION) {
                            out.println("-ERR Not in transaction state");
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
                        if (state != POP3State.TRANSACTION) {
                            out.println("-ERR Not in transaction state");
                        } else {
                            deletedMessages.clear();
                            out.println("+OK Deletion marks removed");
                        }
                        break;
                    case "NOOP":
                        if (state != POP3State.TRANSACTION) {
                            out.println("-ERR Not in transaction state");
                        } else {
                            out.println("+OK");
                        }
                        break;
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

    private boolean checkUserExists(String user) {
        File userDir = new File("mailserver/" + user);
        return userDir.exists() && userDir.isDirectory();
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

    private boolean authenticateUser(String username, String password) {
        File file = new File("src/main/resources/user.txt");
        if (!file.exists()) {
            System.err.println("Fichier users.txt introuvable pour l'authentification.");
            return false;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String fileUser = parts[0].trim();
                    String filePass = parts[1].trim();
                    if (fileUser.equals(username) && filePass.equals(password)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}

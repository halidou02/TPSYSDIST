package org.emp.gl;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;
import java.net.Socket;

public class SMTPClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private SMTPState state;
    private String mailFrom;
    private List<String> recipients;
    private StringBuilder dataBuffer;

    public SMTPClientHandler(Socket socket) {
        this.socket = socket;
        this.state = SMTPState.INIT;
        this.recipients = new ArrayList<>();
        this.dataBuffer = new StringBuilder();
    }

    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Envoi du message de bienvenue (code 220)
            out.println("220 Welcome to Java SMTP Server");

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Received: " + line);

                if (line.toUpperCase().startsWith("HELO") || line.toUpperCase().startsWith("EHLO")) {
                    state = SMTPState.GREETED;
                    out.println("250 Hello");
                } else if (line.toUpperCase().startsWith("MAIL FROM:")) {
                    if (state == SMTPState.GREETED) {
                        String sender = line.substring(10).trim();
                        if (!isValidEmail(sender)) {
                            out.println("501 Syntax error in parameters or arguments");
                            continue;
                        }
                        mailFrom = sender;
                        state = SMTPState.MAIL_RECEIVED;
                        out.println("250 OK");
                    } else {
                        out.println("503 Bad sequence of commands");
                    }
                } else if (line.toUpperCase().startsWith("RCPT TO:")) {
                    if (state == SMTPState.MAIL_RECEIVED || state == SMTPState.RCPT_RECEIVED) {
                        String recipient = line.substring(8).trim();
                        if (!isValidEmail(recipient)) {
                            out.println("501 Syntax error in parameters or arguments");
                            continue;
                        }
                        if (checkUserExists(recipient)) {
                            recipients.add(recipient);
                            state = SMTPState.RCPT_RECEIVED;
                            out.println("250 OK");
                        } else {
                            out.println("550 No such user here");
                        }
                    } else {
                        out.println("503 Bad sequence of commands");
                    }
                } else if (line.toUpperCase().startsWith("DATA")) {
                    if (state == SMTPState.RCPT_RECEIVED) {
                        out.println("354 End data with <CR><LF>.<CR><LF>");
                        state = SMTPState.RECEIVING_DATA;
                        while ((line = in.readLine()) != null) {
                            if (line.equals(".")) {
                                break;
                            }
                            dataBuffer.append(line).append("\r\n");
                        }
                        saveEmail(mailFrom, recipients, dataBuffer.toString());
                        out.println("250 OK: Message accepted");
                        state = SMTPState.GREETED;
                        mailFrom = null;
                        recipients.clear();
                        dataBuffer.setLength(0);
                    } else {
                        out.println("503 Bad sequence of commands");
                    }
                } else if (line.toUpperCase().startsWith("QUIT")) {
                    out.println("221 Bye");
                    break;
                } else {
                    out.println("500 Command not recognized");
                }
            }
        } catch (IOException e) {
            System.err.println("Connexion interrompue: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException e) { }
        }
    }

    // Vérifie l'existence d'un répertoire pour l'utilisateur (extraction du nom avant '@')
    private boolean checkUserExists(String email) {
        String user = email.split("@")[0];
        File userDir = new File("mailserver/" + user);
        return userDir.exists() && userDir.isDirectory();
    }

    // Sauvegarde l'email dans le dossier de chaque destinataire sous "timestamp.txt"
    private void saveEmail(String mailFrom, List<String> recipients, String data) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        for (String recipient : recipients) {
            String user = recipient.split("@")[0];
            File userDir = new File("mailserver/" + user);
            if (!userDir.exists()) {
                userDir.mkdirs();
            }
            File emailFile = new File(userDir, timestamp + ".txt");
            try (FileWriter fw = new FileWriter(emailFile)) {
                fw.write("From: " + mailFrom + "\r\n");
                fw.write("To: " + String.join(", ", recipients) + "\r\n");
                fw.write(data);
                System.out.println("Email saved for " + recipient + " in file: " + emailFile.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Validation simple de l'adresse email
    private boolean isValidEmail(String email) {
        String regex = "^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,}$";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(email).matches();
    }
}

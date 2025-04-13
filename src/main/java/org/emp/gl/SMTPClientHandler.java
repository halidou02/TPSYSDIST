package org.emp.gl;

import java.io.*;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;
import org.emp.auth.AuthService;  // Assurez-vous d'importer le package du service RMI

public class SMTPClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private SMTPState state;
    private String mailFrom;
    private List<String> recipients;
    private StringBuilder dataBuffer;
    private AuthService authService; // Référence au service RMI

    public SMTPClientHandler(Socket socket) {
        this.socket = socket;
        this.state = SMTPState.START;
        this.recipients = new ArrayList<>();
        this.dataBuffer = new StringBuilder();
        try {
            // Connexion au registre RMI (sur localhost et port 1099 par défaut)
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

            out.println("220 Welcome to Java SMTP Server");

            String line;
            while ((line = in.readLine()) != null) {
                String upperLine = line.toUpperCase();
                if (upperLine.startsWith("HELO") || upperLine.startsWith("EHLO")) {
                    if (state == SMTPState.START || state == SMTPState.MESSAGE_RECEIVED) {
                        state = SMTPState.HELO_RECEIVED;
                        out.println("250 Hello");
                    } else {
                        out.println("503 Bad sequence of commands");
                    }
                } else if (upperLine.startsWith("MAIL FROM:")) {
                    if (state == SMTPState.HELO_RECEIVED) {
                        String sender = line.substring(10).trim();
                        if (!isValidEmail(sender)) {
                            out.println("501 Syntax error in parameters or arguments");
                            continue;
                        }
                        // Pour SMTP, on peut aussi vérifier que l'expéditeur est un utilisateur connu
                        String user = sender.split("@")[0];
                        try {
                            if (!authService.userExists(user)) {
                                out.println("550 No such user here");
                                continue;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            out.println("451 Internal server error");
                            continue;
                        }
                        mailFrom = sender;
                        state = SMTPState.MAIL_FROM;
                        out.println("250 OK");
                    } else {
                        out.println("503 Bad sequence of commands");
                    }
                } else if (upperLine.startsWith("RCPT TO:")) {
                    if (state == SMTPState.MAIL_FROM || state == SMTPState.RCPT_TO_RECEIVED) {
                        String recipient = line.substring(8).trim();
                        if (!isValidEmail(recipient)) {
                            out.println("501 Syntax error in parameters or arguments");
                            continue;
                        }
                        // Vérification via le service RMI
                        String user = recipient.split("@")[0];
                        try {
                            if (authService.userExists(user)) {
                                recipients.add(recipient);
                                state = SMTPState.RCPT_TO_RECEIVED;
                                out.println("250 OK");
                            } else {
                                out.println("550 No such user here");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            out.println("451 Internal server error");
                        }
                    } else {
                        out.println("503 Bad sequence of commands");
                    }
                } else if (upperLine.startsWith("DATA")) {
                    if (state == SMTPState.RCPT_TO_RECEIVED) {
                        out.println("354 End data with <CR><LF>.<CR><LF>");
                        state = SMTPState.DATA;
                        while ((line = in.readLine()) != null) {
                            if (line.equals(".")) {
                                break;
                            }
                            dataBuffer.append(line).append("\r\n");
                        }
                        saveEmail(mailFrom, recipients, dataBuffer.toString());
                        out.println("250 OK: Message accepted");
                        state = SMTPState.MESSAGE_RECEIVED;
                        mailFrom = null;
                        recipients.clear();
                        dataBuffer.setLength(0);
                    } else {
                        out.println("503 Bad sequence of commands");
                    }
                } else if (upperLine.startsWith("QUIT")) {
                    state = SMTPState.QUIT_RECEIVED;
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
                fw.write("To: " + String.join(", ", recipients) + "\r\n\r\n");
                fw.write(data);
                System.out.println("Email saved for " + recipient + " in file: " + emailFile.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isValidEmail(String email) {
        String regex = "^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,}$";
        return email.matches(regex);
    }
}

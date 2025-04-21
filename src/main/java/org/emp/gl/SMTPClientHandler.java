package org.emp.gl;

import java.io.*;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.regex.*;
import org.emp.auth.AuthService;
import org.emp.auth.MySQLEmailService;

public class SMTPClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private SMTPState state;
    private String mailFrom;
    private List<String> recipients;
    private StringBuilder dataBuffer;
    private AuthService authService;
    private boolean authServiceAvailable = false; // Flag to track RMI service availability

    public SMTPClientHandler(Socket socket) {
        this.socket = socket;
        this.state = SMTPState.START;
        this.recipients = new ArrayList<>();
        this.dataBuffer = new StringBuilder();
        try {
            // Récupération du service d'authentification via RMI (pour vérifier l'existence des utilisateurs)
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            authService = (AuthService) registry.lookup("AuthService");
            authServiceAvailable = (authService != null); // Set flag based on lookup result
            if (!authServiceAvailable) {
                System.err.println("SMTP Handler: AuthService RMI lookup returned null.");
            }
        } catch (Exception e) {
            System.err.println("SMTP Handler: Failed to connect or lookup AuthService via RMI.");
            e.printStackTrace();
            authService = null; // Ensure it's null on error
            authServiceAvailable = false;
        }
    }

    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Check if Auth Service is available right at the start
            if (!authServiceAvailable) {
                out.println("451 Requested action aborted: local error in processing (Auth Service Unavailable)");
                socket.close();
                return; // Stop processing if auth service is missing
            }

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
                        // Check again in case something went wrong, though unlikely if checked at start
                        if (!authServiceAvailable || authService == null) {
                            out.println("451 Requested action aborted: local error in processing (Auth Service Unavailable)");
                            break; // Exit loop
                        }

                        // Extraire l'adresse email en supprimant les chevrons potentiels
                        String senderWithBrackets = line.substring(10).trim();
                        String sender = senderWithBrackets.replaceAll("[<>]", ""); // Supprimer < et >

                        if (!isValidEmail(sender)) {
                            out.println("501 Syntax error in parameters or arguments (Invalid email format: " + sender + ")");
                            continue;
                        }
                        // Vérification via le service d'authentification
                        // Extraire le nom d'utilisateur (partie avant @)
                        String user = sender.contains("@") ? sender.split("@")[0] : sender;
                        try {
                            if (!authService.userExists(user)) {
                                out.println("550 No such user here (Sender: " + user + ")");
                                continue;
                            }
                        } catch (RemoteException e) {
                            System.err.println("SMTP Handler: RMI error during userExists check for sender: " + user);
                            e.printStackTrace();
                            out.println("421 Service not available, closing transmission channel (RMI Error)");
                            break; // Exit loop on RMI error
                        }

                        mailFrom = sender; // Utiliser l'adresse sans chevrons
                        state = SMTPState.MAIL_FROM;
                        out.println("250 OK");
                    } else {
                        out.println("503 Bad sequence of commands");
                    }
                } else if (upperLine.startsWith("RCPT TO:")) {
                    if (state == SMTPState.MAIL_FROM || state == SMTPState.RCPT_TO_RECEIVED) {
                        if (!authServiceAvailable || authService == null) { // Check auth service
                            out.println("451 Requested action aborted: local error in processing (Auth Service Unavailable)");
                            break; // Exit loop
                        }

                        // Extraire l'adresse email en supprimant les chevrons potentiels
                        String recipientWithBrackets = line.substring(8).trim();
                        String recipient = recipientWithBrackets.replaceAll("[<>]", ""); // Supprimer < et >

                        if (!isValidEmail(recipient)) {
                            out.println("501 Syntax error in parameters or arguments (Invalid email format: " + recipient + ")");
                            continue;
                        }
                        // Extraire le nom d'utilisateur (partie avant @)
                        String user = recipient.contains("@") ? recipient.split("@")[0] : recipient;
                        try {
                            if (!authService.userExists(user)) {
                                out.println("550 No such user here (Recipient: " + user + ")");
                                continue;
                            }
                        } catch (RemoteException e) {
                            System.err.println("SMTP Handler: RMI error during userExists check for recipient: " + user);
                            e.printStackTrace();
                            out.println("421 Service not available, closing transmission channel (RMI Error)");
                            break; // Exit loop on RMI error
                        }

                        recipients.add(recipient); // Utiliser l'adresse sans chevrons
                        state = SMTPState.RCPT_TO_RECEIVED;
                        out.println("250 OK");
                    } else {
                        out.println("503 Bad sequence of commands");
                    }
                } else if (upperLine.startsWith("DATA")) {
                    if (state == SMTPState.RCPT_TO_RECEIVED) {
                        out.println("354 End data with <CR><LF>.<CR><LF>");
                        state = SMTPState.DATA;
                        dataBuffer.setLength(0); // Clear buffer before reading data
                        while ((line = in.readLine()) != null) {
                            if (line.equals(".")) {
                                break;
                            }
                            // Handle potential leading dots (byte-stuffing) - simple version
                            if (line.startsWith("..")) {
                                line = line.substring(1);
                            }
                            dataBuffer.append(line).append("\r\n"); // Use CRLF as per SMTP standard
                        }

                        // --- Extraction du Sujet ---
                        String subject = extractSubjectFromData(dataBuffer.toString());
                        String emailContent = dataBuffer.toString(); // Full data including headers

                        // Appel à la procédure stockée pour insérer l'email dans MySQL
                        System.out.println("SMTP Handler: Storing email - From: " + mailFrom + ", To: " + recipients + ", Subject: " + subject);
                        boolean storedOk = MySQLEmailService.storeEmail(mailFrom, recipients, subject, emailContent); // Passer le sujet extrait
                        if (storedOk) {
                            out.println("250 OK: Message accepted");
                            System.out.println("SMTP Handler: Email from " + mailFrom + " to " + recipients + " stored successfully."); // Log success
                        } else {
                            out.println("554 Transaction failed (storage error)");
                            System.err.println("SMTP Handler: Failed to store email from " + mailFrom + " to " + recipients); // Log failure
                        }
                        resetState();
                    } else {
                        out.println("503 Bad sequence of commands");
                    }
                } else if (upperLine.startsWith("QUIT")) {
                    out.println("221 Bye");
                    break;
                } else {
                    out.println("500 Command not recognized");
                }
            }
        } catch (IOException e) {
            // Log specific IO errors if needed, otherwise just indicate connection issue
            if (!socket.isClosed()) { // Avoid logging error if we closed it intentionally (e.g., QUIT)
                System.err.println("SMTP Handler: Connexion interrompue ou erreur I/O: " + e.getMessage());
            }
        } catch (Exception e) {
            // Catch-all for unexpected errors like NullPointerException
            System.err.println("SMTP Handler: Erreur inattendue dans le handler: " + e.getMessage());
            e.printStackTrace();
            // Try to send a generic server error message if possible
            try {
                if (!socket.isClosed() && out != null) {
                    out.println("421 Service not available, closing transmission channel (Internal Server Error)");
                }
            } catch (Exception ex) {
                // Ignore errors during error reporting
            }
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("SMTP Handler: Erreur lors de la fermeture du socket: " + e.getMessage());
            }
        }
    }

    private void resetState() {
        state = SMTPState.MESSAGE_RECEIVED;
        mailFrom = null;
        recipients.clear();
        dataBuffer.setLength(0);
    }

    private boolean isValidEmail(String email) {
        String regex = "^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,}$";
        return email.matches(regex);
    }

    // --- Helper pour extraire le sujet ---
    private String extractSubjectFromData(String data) {
        String subject = ""; // Default to empty string
        try (BufferedReader reader = new BufferedReader(new StringReader(data))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Stop searching for headers after the first empty line
                if (line.trim().isEmpty()) {
                    break;
                }
                String upperLine = line.toUpperCase();
                if (upperLine.startsWith("SUBJECT:")) {
                    subject = line.substring("SUBJECT:".length()).trim();
                    break; // Found the subject, no need to read further headers
                }
            }
        } catch (IOException e) {
            // Should not happen with StringReader, but handle anyway
            System.err.println("SMTP Handler: Error reading data buffer for subject extraction: " + e.getMessage());
        }
        return subject;
    }
}

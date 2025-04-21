package org.emp.auth;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySQLEmailService {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/maildb?serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    public static boolean storeEmail(String sender, List<String> recipients, String subject, String content) {
        boolean overallSuccess = true; // Track success across all recipients
        Connection conn = null; // Declare connection outside try-with-resources to manage commit/rollback

        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            boolean autoCommit = conn.getAutoCommit(); // Check current autocommit status
            System.out.println("MySQLEmailService.storeEmail - AutoCommit status: " + autoCommit);
            if (autoCommit) {
                 conn.setAutoCommit(false); // Disable autocommit to manage transaction manually
                 System.out.println("MySQLEmailService.storeEmail - AutoCommit disabled for manual transaction.");
            }

            String sql = "{CALL store_email(?, ?, ?, ?)}";
            try (CallableStatement cstmt = conn.prepareCall(sql)) {
                for (String recipient : recipients) {
                    try {
                        // Log values before calling procedure
                        System.out.println("MySQLEmailService.storeEmail - Calling store_email with:");
                        System.out.println("  Sender: " + sender);
                        System.out.println("  Recipient: " + recipient);
                        System.out.println("  Subject: " + subject);
                        // System.out.println("  Content: " + content); // Content can be long, maybe skip logging it fully

                        cstmt.setString(1, sender);
                        cstmt.setString(2, recipient);
                        cstmt.setString(3, subject != null ? subject : "");
                        cstmt.setString(4, content);
                        cstmt.execute();
                        System.out.println("MySQLEmailService.storeEmail - Attempted to store email for: " + recipient); // Log attempt

                    } catch (SQLException ex) {
                        System.err.println("MySQLEmailService.storeEmail - SQL Error storing email for recipient " + recipient + ": " + ex.getMessage());
                        ex.printStackTrace();
                        overallSuccess = false; // Mark as failed if any recipient fails
                        // Don't break, try other recipients if possible
                    }
                }
            }

            if (overallSuccess) {
                System.out.println("MySQLEmailService.storeEmail - Committing transaction.");
                conn.commit(); // Commit the transaction if all executions were successful
            } else {
                 System.err.println("MySQLEmailService.storeEmail - Rolling back transaction due to errors.");
                 conn.rollback(); // Rollback if any part failed
            }

        } catch (SQLException e) {
            System.err.println("MySQLEmailService.storeEmail - SQL Connection or general error: " + e.getMessage());
            e.printStackTrace();
            overallSuccess = false; // Mark as failed on connection or commit/rollback error
            // Try to rollback if connection was established but commit failed
            if (conn != null) {
                try {
                     System.err.println("MySQLEmailService.storeEmail - Attempting rollback after general error.");
                     conn.rollback();
                } catch (SQLException rbEx) {
                     System.err.println("MySQLEmailService.storeEmail - Error during rollback: " + rbEx.getMessage());
                }
            }
        } finally {
            // Restore autocommit and close connection
            if (conn != null) {
                try {
                    // Restore original autocommit state if we changed it
                    if (!conn.getAutoCommit() && conn.getMetaData().supportsTransactions()) { // Check if we disabled it
                         System.out.println("MySQLEmailService.storeEmail - Restoring AutoCommit to true.");
                         conn.setAutoCommit(true);
                    }
                } catch (SQLException e) {
                     System.err.println("MySQLEmailService.storeEmail - Error restoring autocommit: " + e.getMessage());
                }
                try {
                    System.out.println("MySQLEmailService.storeEmail - Closing connection.");
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("MySQLEmailService.storeEmail - Error closing connection: " + e.getMessage());
                }
            }
        }
        return overallSuccess;
    }

    // Récupère la liste des emails pour un destinataire en appelant fetch_emails.
    public static List<Email> fetchEmails(String recipient) {
        List<Email> emails = new ArrayList<>();
        System.out.println("MySQLEmailService.fetchEmails - Attempting to fetch emails for: " + recipient); // Log recipient
        Connection conn = null;
        CallableStatement cstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            String sql = "{CALL fetch_emails(?)}";
            System.out.println("MySQLEmailService.fetchEmails - Preparing call: " + sql);
            cstmt = conn.prepareCall(sql);
            cstmt.setString(1, recipient);

            System.out.println("MySQLEmailService.fetchEmails - Executing procedure...");
            boolean hasResult = cstmt.execute();
            System.out.println("MySQLEmailService.fetchEmails - Procedure executed. Has ResultSet: " + hasResult);

            if (hasResult) {
                rs = cstmt.getResultSet();
                int count = 0;
                System.out.println("MySQLEmailService.fetchEmails - Processing ResultSet...");
                while (rs.next()) {
                    count++;
                    try {
                        Email email = new Email(
                                rs.getInt("id"),
                                rs.getString("sender"),
                                rs.getString("recipient"),
                                rs.getString("subject"),
                                rs.getString("content"),
                                rs.getTimestamp("timestamp")
                        );
                        emails.add(email);
                         System.out.println("MySQLEmailService.fetchEmails - Added email with ID: " + email.getId());
                    } catch (SQLException innerEx) {
                         System.err.println("MySQLEmailService.fetchEmails - Error processing row " + count + ": " + innerEx.getMessage());
                         // Continue processing other rows if possible
                    }
                }
                System.out.println("MySQLEmailService.fetchEmails - Finished processing ResultSet. Total emails found: " + count);
            } else {
                 System.out.println("MySQLEmailService.fetchEmails - Procedure did not return a ResultSet.");
                 // Check if there was an update count instead (shouldn't happen for a SELECT-like procedure)
                 int updateCount = cstmt.getUpdateCount();
                 System.out.println("MySQLEmailService.fetchEmails - Update count: " + updateCount);
            }
        } catch (SQLException e) {
            System.err.println("MySQLEmailService.fetchEmails - SQL Error: " + e.getMessage());
            e.printStackTrace(); // Print full stack trace for detailed error
        } finally {
            // Close resources in reverse order of creation
             System.out.println("MySQLEmailService.fetchEmails - Closing resources...");
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { /* ignored */ }
            }
            if (cstmt != null) {
                try { cstmt.close(); } catch (SQLException e) { /* ignored */ }
            }
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { /* ignored */ }
            }
             System.out.println("MySQLEmailService.fetchEmails - Resources closed.");
        }
        System.out.println("MySQLEmailService.fetchEmails - Returning " + emails.size() + " emails.");
        return emails;
    }

    // Supprime un email en appelant la procédure delete_email.
    public static boolean deleteEmail(int emailId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            CallableStatement cstmt = conn.prepareCall("{CALL delete_email(?)}");
            cstmt.setInt(1, emailId);
            cstmt.execute();
            // Check if delete was successful (might depend on procedure definition or DB settings)
            // For simplicity, assume success if no exception. A better check might involve getUpdateCount or procedure output param.
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}

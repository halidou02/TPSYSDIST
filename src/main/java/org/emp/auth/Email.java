package org.emp.auth;

import java.sql.Timestamp;

public class Email {
    private int id;
    private String sender;
    private String recipient;
    private String subject;
    private String content;
    private Timestamp timestamp;

    public Email(int id, String sender, String recipient, String subject, String content, Timestamp timestamp) {
        this.id = id;
        this.sender = sender;
        this.recipient = recipient;
        this.subject = subject;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Getters et éventuellement setters...
    public int getId() { return id; }
    public String getSender() { return sender; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }
    public Timestamp getTimestamp() { return timestamp; }
}

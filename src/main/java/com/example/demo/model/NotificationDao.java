package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "NotificationMaster")
public class NotificationDao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message", nullable = false)
    private String message;
    @Column(name = "recipient")
    private String recipient;
    @Column(name = "type")
    private String type; // e.g., "email", "sms", etc.
    @Column(name = "status")
    private String status; // e.g., "sent", "failed", etc.
    @Column(name = "timestamp")
    private String timestamp; // e.g., "2024-06-01T12:00:00Z"

    // Constructors
    public NotificationDao() {}

    public NotificationDao(String message, String recipient, String type, String status, String timestamp) {
        this.message = message;
        this.recipient = recipient;
        this.type = type;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
}

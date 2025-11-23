package com.example.crowdcleaning.models;

public class NotificationModel {
    private String title;
    private String message;
    private String audience;
    private String timestamp;
    private String sentBy;

    // Required empty constructor for Firestore
    public NotificationModel() {
    }

    public NotificationModel(String title, String message, String audience, String timestamp, String sentBy) {
        this.title = title;
        this.message = message;
        this.audience = audience;
        this.timestamp = timestamp;
        this.sentBy = sentBy;
    }

    // Getters and setters
    public String getTitle() {
        return title != null ? title : "No Title";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message != null ? message : "No Message";
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAudience() {
        return audience != null ? audience : "All Users";
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getTimestamp() {
        return timestamp != null ? timestamp : "Unknown time";
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSentBy() {
        return sentBy != null ? sentBy : "Admin";
    }

    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }

    public String getFormattedTimestamp() {
        return getTimestamp();
    }
}
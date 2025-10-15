package com.example.crowdcleaning.models;

public class NotificationModel {
    private String title;
    private String message;
    private String audience;
    private String timestamp;
    private String sentBy;

    public NotificationModel() {
        // Empty constructor needed for Firebase
    }

    public NotificationModel(String title, String message, String audience, String timestamp, String sentBy) {
        this.title = title;
        this.message = message;
        this.audience = audience;
        this.timestamp = timestamp;
        this.sentBy = sentBy;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSentBy() {
        return sentBy;
    }

    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }
}
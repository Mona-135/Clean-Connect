package com.example.crowdcleaning.models;

public class RecentActivity {
    private String userName;
    private String description;
    private String status;
    private String timestamp;

    public RecentActivity() {}

    public RecentActivity(String userName, String description, String status, String timestamp) {
        this.userName = userName;
        this.description = description;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
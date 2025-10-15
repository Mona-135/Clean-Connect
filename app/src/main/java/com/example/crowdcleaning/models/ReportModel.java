package com.example.crowdcleaning.models;

public class ReportModel {
    private String id;
    private String title;
    private String description;
    private String address;
    private String status;
    private String imageUrl;
    private long timestamp;
    private long upvotes;
    private double latitude;
    private double longitude;

    public ReportModel() {
        // Default constructor required for Firestore
    }

    public ReportModel(String id, String title, String description, String address,
                       String status, String imageUrl, long timestamp, long upvotes,
                       double latitude, double longitude) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.address = address;
        this.status = status;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.upvotes = upvotes;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getUpvotes() { return upvotes; }
    public void setUpvotes(long upvotes) { this.upvotes = upvotes; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
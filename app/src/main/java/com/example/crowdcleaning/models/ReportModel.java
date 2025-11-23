package com.example.crowdcleaning.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private String volunteerName;
    private String userName;
    private String volunteerAssigned;
    private String userEmail;
    private List<String> imageUrls;

    public ReportModel() {}

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

    // Getters and Setters
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

    public String getImageUrl() {
        // If imageUrl is empty but we have imageUrls, return the first one
        if ((imageUrl == null || imageUrl.isEmpty()) && imageUrls != null && !imageUrls.isEmpty()) {
            return imageUrls.get(0);
        }
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getUpvotes() { return upvotes; }
    public void setUpvotes(long upvotes) { this.upvotes = upvotes; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getVolunteerName() { return volunteerName; }
    public void setVolunteerName(String volunteerName) { this.volunteerName = volunteerName; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getVolunteerAssigned() { return volunteerAssigned; }
    public void setVolunteerAssigned(String volunteerAssigned) { this.volunteerAssigned = volunteerAssigned; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public List<String> getImageUrls() {
        if (imageUrls == null) {
            // If imageUrls is null but imageUrl exists, create a list with it
            if (imageUrl != null && !imageUrl.isEmpty()) {
                return Arrays.asList(imageUrl);
            }
            return new ArrayList<>();
        }
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    // Helper method to check if report has any images
    public boolean hasImages() {
        return (imageUrl != null && !imageUrl.isEmpty()) ||
                (imageUrls != null && !imageUrls.isEmpty());
    }

    // Helper method to get all image URLs (combines both single imageUrl and imageUrls list)
    public List<String> getAllImageUrls() {
        List<String> allUrls = new ArrayList<>();

        // Add single imageUrl if it exists
        if (imageUrl != null && !imageUrl.isEmpty()) {
            allUrls.add(imageUrl);
        }

        // Add all imageUrls if they exist
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (String url : imageUrls) {
                if (url != null && !url.isEmpty() && !allUrls.contains(url)) {
                    allUrls.add(url);
                }
            }
        }

        return allUrls;
    }

    @Override
    public String toString() {
        return "ReportModel{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", address='" + address + '\'' +
                ", status='" + status + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", imageUrls=" + (imageUrls != null ? imageUrls.size() : 0) +
                ", timestamp=" + timestamp +
                ", upvotes=" + upvotes +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
// Create a new file: StatusConstants.java
package com.example.crowdcleaning.utils;

// Add to StatusConstants.java
public class StatusConstants {
    public static boolean isAvailableStatus(String status) {
        if (status == null) return false;
        switch (status.toLowerCase()) {
            case "reported":
            case "pending":
            case "new":
            case "open":
                return true;
            default:
                return false;
        }
    }

    public static boolean isCompletedStatus(String status) {
        if (status == null) return false;
        switch (status.toLowerCase()) {
            case "completed":
            case "cleaned":
            case "done":
                return true;
            default:
                return false;
        }
    }
}
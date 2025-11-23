package com.example.crowdcleaning.utils;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.util.UUID;

public class FirebaseUtils {

    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(String error);
    }

    public static void uploadImage(byte[] imageData, String folder, UploadCallback callback) {
        try {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            String filename = UUID.randomUUID().toString() + ".jpg";
            StorageReference storageRef = storage.getReference()
                    .child(folder)
                    .child(filename);

            UploadTask uploadTask = storageRef.putBytes(imageData);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                storageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            callback.onSuccess(uri.toString());
                        })
                        .addOnFailureListener(e -> {
                            callback.onFailure("Failed to get download URL: " + e.getMessage());
                        });
            }).addOnFailureListener(e -> {
                callback.onFailure("Upload failed: " + e.getMessage());
            });
        } catch (Exception e) {
            callback.onFailure("Upload exception: " + e.getMessage());
        }
    }

    // Add this method for ReportGarbageActivity compatibility
    public static UploadTask uploadImage(byte[] imageData, String folder) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        String filename = UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = storage.getReference()
                .child(folder)
                .child(filename);
        return storageRef.putBytes(imageData);
    }

    public static StorageReference getStorageReference(String path) {
        return FirebaseStorage.getInstance().getReference().child(path);
    }
}
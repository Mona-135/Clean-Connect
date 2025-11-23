package com.example.crowdcleaning.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.crowdcleaning.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CompleteTaskActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText editTextCompletionNotes;
    private Button buttonUploadImages, buttonSubmitCompletion;
    private ImageView imagePreview1, imagePreview2;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    private String reportId;
    private List<Uri> imageUris = new ArrayList<>();
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_task);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        currentUser = auth.getCurrentUser();

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        // Get report ID from intent
        reportId = getIntent().getStringExtra("report_id");
        if (reportId == null) {
            Toast.makeText(this, "Error: No report ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Check if user is logged in
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        editTextCompletionNotes = findViewById(R.id.editTextCompletionNotes);
        buttonUploadImages = findViewById(R.id.buttonUploadImages);
        buttonSubmitCompletion = findViewById(R.id.buttonSubmitCompletion);
        imagePreview1 = findViewById(R.id.imagePreview1);
        imagePreview2 = findViewById(R.id.imagePreview2);
    }

    private void setupClickListeners() {
        buttonUploadImages.setOnClickListener(v -> openImagePicker());
        buttonSubmitCompletion.setOnClickListener(v -> submitCompletion());
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Completion Images"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUris.clear();

            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < Math.min(count, 2); i++) {
                    imageUris.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                imageUris.add(data.getData());
            }

            updateImagePreviews();
        }
    }

    private void updateImagePreviews() {
        // Reset previews to default placeholder
        imagePreview1.setImageResource(android.R.drawable.ic_menu_gallery);
        imagePreview2.setImageResource(android.R.drawable.ic_menu_gallery);

        // Load images using Picasso for better handling
        if (imageUris.size() > 0) {
            Picasso.get()
                    .load(imageUris.get(0))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(imagePreview1);
        }
        if (imageUris.size() > 1) {
            Picasso.get()
                    .load(imageUris.get(1))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(imagePreview2);
        }

        // Update button text based on number of images
        if (imageUris.size() >= 2) {
            buttonUploadImages.setText("Maximum Images Selected");
            buttonUploadImages.setEnabled(false);
        } else {
            buttonUploadImages.setText("Select Images");
            buttonUploadImages.setEnabled(true);
        }
    }

    private void submitCompletion() {
        String completionNotes = editTextCompletionNotes.getText().toString().trim();

        if (completionNotes.isEmpty()) {
            editTextCompletionNotes.setError("Please add completion notes");
            editTextCompletionNotes.requestFocus();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        progressDialog.setMessage("Completing task...");
        progressDialog.show();

        // Update report status to completed
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "completed");
        updates.put("completionNotes", completionNotes);
        updates.put("completedAt", new Date());
        updates.put("completedBy", currentUser.getUid());
        updates.put("completedByName", currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : "Volunteer");

        db.collection("reports").document(reportId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Upload images if any
                    if (!imageUris.isEmpty()) {
                        uploadCompletionImages();
                    } else {
                        progressDialog.dismiss();
                        finishWithSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to complete task: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void uploadCompletionImages() {
        progressDialog.setMessage("Uploading images...");

        List<String> imageUrls = new ArrayList<>();
        final int totalImages = imageUris.size();
        final int[] uploadedCount = {0};

        if (totalImages == 0) {
            finishWithSuccess();
            return;
        }

        for (int i = 0; i < imageUris.size(); i++) {
            Uri imageUri = imageUris.get(i);
            StorageReference fileReference = storageReference.child("completion_images/" + UUID.randomUUID().toString());

            fileReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                            imageUrls.add(uri.toString());
                            uploadedCount[0]++;

                            // When all images are uploaded, update the report
                            if (uploadedCount[0] == totalImages) {
                                updateReportWithImages(imageUrls);
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        // Continue even if some images fail
                        uploadedCount[0]++;
                        if (uploadedCount[0] == totalImages && !imageUrls.isEmpty()) {
                            updateReportWithImages(imageUrls);
                        } else if (uploadedCount[0] == totalImages) {
                            progressDialog.dismiss();
                            finishWithSuccess();
                        }
                    });
        }
    }

    private void updateReportWithImages(List<String> imageUrls) {
        db.collection("reports").document(reportId)
                .update("completionImages", imageUrls)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    finishWithSuccess();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to save images but task completed", Toast.LENGTH_SHORT).show();
                    finishWithSuccess();
                });
    }

    private void finishWithSuccess() {
        Toast.makeText(this, "Task completed successfully!", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
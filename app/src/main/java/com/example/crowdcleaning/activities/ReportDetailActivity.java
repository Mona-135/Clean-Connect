package com.example.crowdcleaning.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.crowdcleaning.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportDetailActivity extends AppCompatActivity {

    private TextView textTitle, textDescription, textAddress, textStatus, textTimestamp, textUserName;
    private ImageView imageReport;
    private Button buttonBack, buttonUpdateStatus;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String reportId;
    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);

        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
        loadReportDetails();
    }

    private void initializeViews() {
        textTitle = findViewById(R.id.textTitle);
        textDescription = findViewById(R.id.textDescription);
        textAddress = findViewById(R.id.textAddress);
        textStatus = findViewById(R.id.textStatus);
        textTimestamp = findViewById(R.id.textTimestamp);
        textUserName = findViewById(R.id.textUserName);
        imageReport = findViewById(R.id.imageReport);
        buttonBack = findViewById(R.id.buttonBack);
        buttonUpdateStatus = findViewById(R.id.buttonUpdateStatus);
        progressBar = findViewById(R.id.progressBar); // Add this to your XML

        // Get data from intent
        reportId = getIntent().getStringExtra("report_id");
        userType = getIntent().getStringExtra("user_type");

        // Show loading initially
        showLoading(true);
    }

    private void setupClickListeners() {
        buttonBack.setOnClickListener(v -> finish());

        buttonUpdateStatus.setOnClickListener(v -> {
            updateReportStatus();
        });
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        // Disable interaction while loading
        buttonUpdateStatus.setEnabled(!isLoading);
    }

    private void loadReportDetails() {
        if (reportId == null) {
            Toast.makeText(this, "Report ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("reports").document(reportId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);

                    if (documentSnapshot.exists()) {
                        displayReportData(documentSnapshot);
                    } else {
                        Toast.makeText(this, "Report not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to load report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayReportData(DocumentSnapshot document) {
        // Get report data
        String title = document.getString("title");
        String description = document.getString("description");
        String address = document.getString("address");
        String status = document.getString("status");
        String userName = document.getString("userName");
        String userEmail = document.getString("userEmail");

        // Format timestamp
        String timestampStr = formatTimestamp(document.get("timestamp"));

        // Update UI
        textTitle.setText(title != null ? title : "No Title");
        textDescription.setText(description != null ? description : "No Description");
        textAddress.setText(address != null ? address : "No Address");
        textStatus.setText(status != null ? status.toUpperCase() : "UNKNOWN");
        textTimestamp.setText(timestampStr);

        String displayName = userName != null ? userName :
                (userEmail != null ? userEmail : "Anonymous");
        textUserName.setText("Reported by: " + displayName);

        // Apply status-based styling
        applyStatusStyling(status);

        // Load image
        loadReportImage(document);

        // Update button based on status and user type
        updateButtonStatus(status);
    }

    private String formatTimestamp(Object timestampObj) {
        if (timestampObj == null) {
            return "Unknown";
        }

        try {
            Date date;
            if (timestampObj instanceof Timestamp) {
                Timestamp timestamp = (Timestamp) timestampObj;
                date = timestamp.toDate();
            } else if (timestampObj instanceof Long) {
                Long timestampLong = (Long) timestampObj;
                date = new Date(timestampLong);
            } else {
                return "Invalid date";
            }

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
            return sdf.format(date);
        } catch (Exception e) {
            return "Invalid date";
        }
    }

    private void applyStatusStyling(String status) {
        // You can set different text colors based on status
        int color;
        switch (status != null ? status : "") {
            case "reported":
                color = getResources().getColor(android.R.color.holo_red_dark);
                break;
            case "assigned":
            case "in_progress":
                color = getResources().getColor(android.R.color.holo_orange_dark);
                break;
            case "completed":
            case "cleaned":
                color = getResources().getColor(android.R.color.holo_green_dark);
                break;
            default:
                color = getResources().getColor(android.R.color.darker_gray);
        }
        textStatus.setTextColor(color);
    }

    private void loadReportImage(DocumentSnapshot document) {
        List<String> imageUrls = (List<String>) document.get("imageUrls");
        if (imageUrls != null && !imageUrls.isEmpty()) {
            String imageUrl = imageUrls.get(0);

            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder) // Create this drawable
                    .error(R.drawable.ic_error_image) // Create this drawable
                    .fit()
                    .centerCrop()
                    .into(imageReport);
        } else {
            // No image available
            imageReport.setImageResource(R.drawable.ic_no_image); // Create this drawable
        }
    }

    private void updateButtonStatus(String status) {
        if (!"volunteer".equals(userType)) {
            buttonUpdateStatus.setVisibility(View.GONE);
            return;
        }

        switch (status != null ? status : "") {
            case "reported":
                buttonUpdateStatus.setText("Accept Task");
                buttonUpdateStatus.setVisibility(View.VISIBLE);
                buttonUpdateStatus.setEnabled(true);
                break;
            case "assigned":
            case "in_progress":
                buttonUpdateStatus.setText("Mark as Completed");
                buttonUpdateStatus.setVisibility(View.VISIBLE);
                buttonUpdateStatus.setEnabled(true);
                break;
            case "completed":
            case "cleaned":
                buttonUpdateStatus.setText("Task Completed");
                buttonUpdateStatus.setVisibility(View.VISIBLE);
                buttonUpdateStatus.setEnabled(false);
                break;
            default:
                buttonUpdateStatus.setVisibility(View.GONE);
        }
    }

    private void updateReportStatus() {
        if (reportId == null) return;

        showLoading(true);
        buttonUpdateStatus.setEnabled(false);

        db.collection("reports").document(reportId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String currentStatus = documentSnapshot.getString("status");
                        String newStatus = determineNewStatus(currentStatus);

                        // Update the status in Firestore
                        db.collection("reports").document(reportId)
                                .update("status", newStatus)
                                .addOnSuccessListener(aVoid -> {
                                    showLoading(false);
                                    Toast.makeText(this, "Status updated to: " + newStatus, Toast.LENGTH_SHORT).show();
                                    loadReportDetails(); // Refresh the details
                                })
                                .addOnFailureListener(e -> {
                                    showLoading(false);
                                    buttonUpdateStatus.setEnabled(true);
                                    Toast.makeText(this, "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        showLoading(false);
                        buttonUpdateStatus.setEnabled(true);
                        Toast.makeText(this, "Report no longer exists", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    buttonUpdateStatus.setEnabled(true);
                    Toast.makeText(this, "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String determineNewStatus(String currentStatus) {
        switch (currentStatus != null ? currentStatus : "") {
            case "reported":
                return "assigned";
            case "assigned":
            case "in_progress":
                return "completed";
            default:
                return currentStatus;
        }
    }
}
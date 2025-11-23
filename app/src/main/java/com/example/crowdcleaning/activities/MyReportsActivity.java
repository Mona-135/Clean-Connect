package com.example.crowdcleaning.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.crowdcleaning.R;
import com.example.crowdcleaning.adapters.ReportAdapter;
import com.example.crowdcleaning.models.ReportModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MyReportsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewReports;
    private TextView textEmptyState;
    private Button buttonBack;
    private ReportAdapter reportAdapter;
    private List<ReportModel> reportList;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private boolean isAdmin = false;
    private ProgressDialog progressDialog;

    private static final String TAG = "MyReportsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reports);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        isAdmin = getIntent().getBooleanExtra("isAdmin", false);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        initializeViews();
        setupRecyclerView();
        setupAdapterListener();

        // Check authentication before loading data
        if (currentUser == null && !isAdmin) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadMyReports();
    }

    private void initializeViews() {
        recyclerViewReports = findViewById(R.id.recyclerViewReports);
        textEmptyState = findViewById(R.id.textEmptyState);
        buttonBack = findViewById(R.id.buttonBack);

        buttonBack.setOnClickListener(v -> finish());

        if (isAdmin) {
            // Update the toolbar title for admin
            TextView toolbarTitle = findViewById(R.id.textViewToolbarTitle);
            if (toolbarTitle != null) {
                toolbarTitle.setText("All Reports - Admin View");
            }
            textEmptyState.setText("No reports found in the system");
        } else {
            // Update the toolbar title for citizen
            TextView toolbarTitle = findViewById(R.id.textViewToolbarTitle);
            if (toolbarTitle != null) {
                toolbarTitle.setText("My Reports");
            }
            textEmptyState.setText("No reports found\nSubmit your first report to see it here");
        }
    }

    private void setupRecyclerView() {
        reportList = new ArrayList<>();
        String userType = isAdmin ? "admin" : "citizen";
        reportAdapter = new ReportAdapter(reportList, userType);
        recyclerViewReports.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewReports.setAdapter(reportAdapter);
    }

    private void setupAdapterListener() {
        reportAdapter.setOnReportActionListener(new ReportAdapter.OnReportActionListener() {
            @Override
            public void onAcceptTask(ReportModel report) {
                // Only for volunteers - show message if accessed by others
                if (!isAdmin) {
                    Toast.makeText(MyReportsActivity.this,
                            "Volunteer feature - please use Volunteer Dashboard", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MyReportsActivity.this,
                            "Assign volunteer for: " + report.getTitle(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onViewDetails(ReportModel report) {
                // Navigate to report details
                openReportDetails(report);
            }

            @Override
            public void onMarkComplete(ReportModel report) {
                // Only for volunteers - show message if accessed by others
                if (!isAdmin) {
                    Toast.makeText(MyReportsActivity.this,
                            "Volunteer feature - please use Volunteer Dashboard", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MyReportsActivity.this,
                            "Mark complete: " + report.getTitle(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAdminAction(ReportModel report) {
                // Only for admin users
                if (isAdmin) {
                    showAdminOptions(report);
                } else {
                    Toast.makeText(MyReportsActivity.this,
                            "Admin feature only", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openReportDetails(ReportModel report) {
        Intent intent = new Intent(MyReportsActivity.this, ReportDetailActivity.class);
        intent.putExtra("report_id", report.getId());
        intent.putExtra("user_type", isAdmin ? "admin" : "citizen");
        startActivity(intent);
    }

    private void showAdminOptions(ReportModel report) {
        // Implement admin-specific actions here
        // For now, just show a toast
        Toast.makeText(this,
                "Admin options for: " + report.getTitle(),
                Toast.LENGTH_SHORT).show();

        // You can implement:
        // - Delete report
        // - Assign to volunteer
        // - Change status
        // - Edit details
    }

    private void loadMyReports() {
        if (currentUser == null && !isAdmin) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Show loading
        progressDialog.setMessage("Loading reports...");
        progressDialog.show();

        try {
            Query query;
            if (isAdmin) {
                query = db.collection("reports")
                        .orderBy("timestamp", Query.Direction.DESCENDING);
            } else {
                String userId = currentUser.getUid();
                query = db.collection("reports")
                        .whereEqualTo("userId", userId)
                        .orderBy("timestamp", Query.Direction.DESCENDING);
            }

            query.get().addOnCompleteListener(task -> {
                progressDialog.dismiss();

                if (task.isSuccessful()) {
                    processQueryResults(task);
                } else {
                    String errorMessage = "Failed to load reports";
                    if (task.getException() != null) {
                        errorMessage += ": " + task.getException().getMessage();
                        Log.e(TAG, "Firestore error: ", task.getException());
                    }
                    showErrorState(errorMessage);
                }
            });

        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "Exception loading reports: ", e);
            showErrorState("Unexpected error occurred");
        }
    }

    private void showErrorState(String message) {
        runOnUiThread(() -> {
            Toast.makeText(MyReportsActivity.this, message, Toast.LENGTH_LONG).show();
            textEmptyState.setText(message + "\nPlease try again later");
            textEmptyState.setVisibility(View.VISIBLE);
            recyclerViewReports.setVisibility(View.GONE);
        });
    }
    // Add this new method to process query results
    private void processQueryResults(com.google.android.gms.tasks.Task<QuerySnapshot> task) {
        progressDialog.dismiss();

        if (task.isSuccessful()) {
            reportList.clear();
            int reportCount = 0;

            for (QueryDocumentSnapshot document : task.getResult()) {
                ReportModel report = documentToReportModel(document);
                if (report != null) {
                    reportList.add(report);
                    reportCount++;

                    // Debug logging
                    Log.d(TAG, "Loaded report: " + report.getTitle() +
                            ", ID: " + report.getId() +
                            ", Status: " + report.getStatus() +
                            ", User: " + report.getUserName());
                }
            }

            reportAdapter.notifyDataSetChanged();

            if (reportList.isEmpty()) {
                textEmptyState.setVisibility(View.VISIBLE);
                recyclerViewReports.setVisibility(View.GONE);
                Log.d(TAG, "No reports found for user");
            } else {
                textEmptyState.setVisibility(View.GONE);
                recyclerViewReports.setVisibility(View.VISIBLE);
                Log.d(TAG, "Successfully loaded " + reportCount + " reports");

                // Update toolbar with count for admin
                if (isAdmin) {
                    updateToolbarWithCount(reportCount);
                }
            }

        } else {
            String errorMessage = "Failed to load reports";
            if (task.getException() != null) {
                errorMessage += ": " + task.getException().getMessage();
                Log.e(TAG, "Error loading reports", task.getException());
            }

            Toast.makeText(MyReportsActivity.this, errorMessage, Toast.LENGTH_LONG).show();

            // Show error state
            textEmptyState.setText("Error loading reports\nPlease try again");
            textEmptyState.setVisibility(View.VISIBLE);
            recyclerViewReports.setVisibility(View.GONE);
        }
    }

    private void updateToolbarWithCount(int count) {
        TextView toolbarTitle = findViewById(R.id.textViewToolbarTitle);
        if (toolbarTitle != null) {
            toolbarTitle.setText("All Reports (" + count + ")");
        }
    }

    private ReportModel documentToReportModel(QueryDocumentSnapshot document) {
        try {
            if (document == null || !document.exists()) {
                return null;
            }

            String id = document.getId();
            String title = document.getString("title");
            String description = document.getString("description");
            String address = document.getString("address");
            String status = document.getString("status");

            // Validate required fields
            if (title == null || description == null) {
                Log.w(TAG, "Skipping document with missing required fields: " + id);
                return null;
            }

            // Handle image URLs safely
            List<String> imageUrls = null;
            try {
                imageUrls = (List<String>) document.get("imageUrls");
            } catch (Exception e) {
                Log.w(TAG, "Error reading imageUrls for document: " + id);
            }

            String imageUrl = (imageUrls != null && !imageUrls.isEmpty()) ? imageUrls.get(0) : "";

            // Handle timestamp
            long timestampMillis = System.currentTimeMillis();
            try {
                Object timestampObj = document.get("timestamp");
                if (timestampObj instanceof com.google.firebase.Timestamp) {
                    timestampMillis = ((com.google.firebase.Timestamp) timestampObj).toDate().getTime();
                } else if (timestampObj instanceof Long) {
                    timestampMillis = (Long) timestampObj;
                }
            } catch (Exception e) {
                Log.w(TAG, "Error reading timestamp for document: " + id);
            }

            // Get additional fields
            Long upvotes = getSafeLong(document, "upvotes");
            Double latitude = getSafeDouble(document, "latitude");
            Double longitude = getSafeDouble(document, "longitude");

            String volunteerName = document.getString("volunteerName");
            String userName = document.getString("userName");
            String volunteerAssigned = document.getString("volunteerAssigned");

            ReportModel report = new ReportModel(
                    id,
                    title,
                    description,
                    address != null ? address : "Address not provided",
                    status != null ? status : "reported",
                    imageUrl,
                    timestampMillis,
                    upvotes != null ? upvotes : 0,
                    latitude != null ? latitude : 0.0,
                    longitude != null ? longitude : 0.0
            );

            // Set additional fields
            report.setVolunteerName(volunteerName);
            report.setUserName(userName);
            report.setVolunteerAssigned(volunteerAssigned);

            return report;

        } catch (Exception e) {
            Log.e(TAG, "Error converting document to ReportModel", e);
            return null;
        }
    }

    // Helper methods for safe type casting
    private Long getSafeLong(QueryDocumentSnapshot document, String field) {
        try {
            return document.getLong(field);
        } catch (Exception e) {
            return null;
        }
    }

    private Double getSafeDouble(QueryDocumentSnapshot document, String field) {
        try {
            return document.getDouble(field);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data when returning to activity
        if (currentUser != null || isAdmin) {
            loadMyReports();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
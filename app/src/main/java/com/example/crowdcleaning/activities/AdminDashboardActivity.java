package com.example.crowdcleaning.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.crowdcleaning.R;
import com.example.crowdcleaning.adapters.RecentActivityAdapter;
import com.example.crowdcleaning.models.RecentActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView textTotalReports, textCleanedReports, textInProgress, textPending;
    private Button buttonViewAllReports, buttonSendNotification, buttonGenerateReport, buttonLogout, buttonBack;
    private RecyclerView recyclerViewRecent;
    private RecentActivityAdapter recentActivityAdapter;
    private List<RecentActivity> recentActivityList;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private static final String TAG = "AdminDashboard";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();

        // Check if user is admin
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        loadData();
    }

    private void initializeViews() {
        try {
            textTotalReports = findViewById(R.id.textTotalReports);
            textCleanedReports = findViewById(R.id.textCleanedReports);
            textInProgress = findViewById(R.id.textInProgress);
            textPending = findViewById(R.id.textPending);

            buttonViewAllReports = findViewById(R.id.buttonViewAllReports);
            buttonSendNotification = findViewById(R.id.buttonSendNotification);
            buttonGenerateReport = findViewById(R.id.buttonGenerateReport);
            buttonLogout = findViewById(R.id.buttonLogout);
            buttonBack = findViewById(R.id.buttonBack);

            recyclerViewRecent = findViewById(R.id.recyclerViewRecent);
            recentActivityList = new ArrayList<>();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            Toast.makeText(this, "Error initializing dashboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        buttonBack.setOnClickListener(v -> navigateToMainActivity());

        buttonViewAllReports.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, MyReportsActivity.class);
            intent.putExtra("isAdmin", true);
            startActivity(intent);
        });

        // Removed Manage Users button functionality

        buttonSendNotification.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, NotificationActivity.class);
            startActivity(intent);
        });

        buttonGenerateReport.setOnClickListener(v -> generateReport());

        buttonLogout.setOnClickListener(v -> logout());
    }

    private void setupRecyclerView() {
        recentActivityAdapter = new RecentActivityAdapter(recentActivityList);
        recyclerViewRecent.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRecent.setAdapter(recentActivityAdapter);
    }

    private void loadData() {
        loadStatistics();
        loadRecentActivity();
    }

    private void loadStatistics() {
        textTotalReports.setText("...");
        textCleanedReports.setText("...");
        textInProgress.setText("...");
        textPending.setText("...");

        db.collection("reports")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int totalReports = task.getResult().size();
                        final int[] counts = new int[3]; // [completed, inProgress, pending]

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String status = document.getString("status");
                            if (status != null) {
                                switch (status.toLowerCase()) {
                                    case "cleaned":
                                    case "completed":
                                        counts[0]++;
                                        break;
                                    case "in_progress":
                                    case "in progress":
                                    case "assigned":
                                        counts[1]++;
                                        break;
                                    default:
                                        counts[2]++;
                                        break;
                                }
                            } else {
                                counts[2]++;
                            }
                        }

                        runOnUiThread(() -> {
                            textTotalReports.setText(String.valueOf(totalReports));
                            textCleanedReports.setText(String.valueOf(counts[0]));
                            textInProgress.setText(String.valueOf(counts[1]));
                            textPending.setText(String.valueOf(counts[2]));
                        });

                    } else {
                        runOnUiThread(() -> {
                            textTotalReports.setText("0");
                            textCleanedReports.setText("0");
                            textInProgress.setText("0");
                            textPending.setText("0");
                            Log.e(TAG, "Error loading statistics", task.getException());
                        });
                    }
                });
    }

    private void loadRecentActivity() {
        db.collection("reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        recentActivityList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                String description = document.getString("description");
                                String status = document.getString("status");
                                String userName = document.getString("userName");
                                String userEmail = document.getString("userEmail");
                                String location = document.getString("address");
                                String title = document.getString("title");

                                String timestampStr = "Recently";
                                Object timestampObj = document.get("timestamp");
                                if (timestampObj instanceof com.google.firebase.Timestamp) {
                                    com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) timestampObj;
                                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                                    timestampStr = sdf.format(timestamp.toDate());
                                } else if (timestampObj instanceof Long) {
                                    Long timestampLong = (Long) timestampObj;
                                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                                    timestampStr = sdf.format(new Date(timestampLong));
                                }

                                if (userName == null || userName.isEmpty()) {
                                    userName = userEmail != null ? userEmail.split("@")[0] : "Anonymous";
                                }

                                if (description == null || description.isEmpty()) {
                                    description = title != null ? title : "Cleaning report";
                                    if (location != null && !location.isEmpty()) {
                                        description += " at " + location;
                                    }
                                }

                                RecentActivity activity = new RecentActivity(
                                        userName,
                                        description,
                                        formatStatus(status),
                                        timestampStr
                                );

                                recentActivityList.add(activity);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing document: " + document.getId(), e);
                            }
                        }

                        runOnUiThread(() -> {
                            if (recentActivityList.isEmpty()) {
                                recentActivityList.add(new RecentActivity(
                                        "System",
                                        "No recent activity found",
                                        "Info",
                                        "Just now"
                                ));
                            }
                            recentActivityAdapter.notifyDataSetChanged();
                        });

                    } else {
                        runOnUiThread(() -> {
                            recentActivityList.clear();
                            recentActivityList.add(new RecentActivity(
                                    "System",
                                    "Failed to load recent activity",
                                    "Error",
                                    "Just now"
                            ));
                            recentActivityAdapter.notifyDataSetChanged();
                            Log.e(TAG, "Error loading recent activity", task.getException());
                        });
                    }
                });
    }

    private String formatStatus(String status) {
        if (status == null) return "Pending";
        switch (status.toLowerCase()) {
            case "cleaned":
            case "completed":
                return "Completed";
            case "in_progress":
            case "in progress":
            case "assigned":
                return "In Progress";
            default:
                return "Pending";
        }
    }

    private void generateReport() {
        try {
            Toast.makeText(this, "Generating report...", Toast.LENGTH_SHORT).show();

            int total, completed, inProgress, pending;
            try {
                total = Integer.parseInt(textTotalReports.getText().toString());
                completed = Integer.parseInt(textCleanedReports.getText().toString());
                inProgress = Integer.parseInt(textInProgress.getText().toString());
                pending = Integer.parseInt(textPending.getText().toString());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Error: Invalid statistics data", Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder report = new StringBuilder();
            report.append("=== CrowdClean Analytics Report ===\n\n");
            report.append("STATISTICS OVERVIEW:\n");
            report.append("• Total Reports: ").append(total).append("\n");
            report.append("• Completed: ").append(completed).append("\n");
            report.append("• In Progress: ").append(inProgress).append("\n");
            report.append("• Pending: ").append(pending).append("\n\n");

            double completionRate = total > 0 ? (completed * 100.0 / total) : 0;
            double progressRate = total > 0 ? ((completed + inProgress) * 100.0 / total) : 0;

            report.append("PERFORMANCE METRICS:\n");
            report.append("• Completion Rate: ").append(String.format("%.1f%%", completionRate)).append("\n");
            report.append("• Overall Progress: ").append(String.format("%.1f%%", progressRate)).append("\n\n");

            report.append("RECENT ACTIVITY SUMMARY:\n");
            if (recentActivityList.isEmpty()) {
                report.append("• No recent activity\n");
            } else {
                for (int i = 0; i < Math.min(recentActivityList.size(), 5); i++) {
                    RecentActivity activity = recentActivityList.get(i);
                    report.append("• ").append(activity.getUserName())
                            .append(" - ").append(activity.getDescription())
                            .append(" (").append(activity.getStatus()).append(")\n");
                }
            }
            report.append("\n");

            addUserStatistics(report, total, completed);

        } catch (Exception e) {
            Toast.makeText(this, "Error generating report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error generating report", e);
        }
    }

    private void addUserStatistics(StringBuilder report, int totalReports, int completedReports) {
        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int totalUsers = task.getResult().size();
                        int adminCount = 0;
                        int activeUsers = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String role = document.getString("role");
                            Boolean isActive = document.getBoolean("isActive");

                            if (role != null && "admin".equals(role.toLowerCase())) {
                                adminCount++;
                            }
                            if (isActive != null && isActive) {
                                activeUsers++;
                            }
                        }

                        report.append("USER STATISTICS:\n");
                        report.append("• Total Users: ").append(totalUsers).append("\n");
                        report.append("• Administrators: ").append(adminCount).append("\n");
                        report.append("• Regular Users: ").append(totalUsers - adminCount).append("\n");
                        report.append("• Active Users: ").append(activeUsers).append("\n");
                        report.append("• Inactive Users: ").append(totalUsers - activeUsers).append("\n\n");

                    } else {
                        report.append("USER STATISTICS:\n");
                        report.append("• Failed to load user statistics\n\n");
                    }

                    report.append("Generated on: ")
                            .append(new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm:ss", Locale.getDefault())
                                    .format(new Date()));

                    showReportDialog(report.toString(), totalReports, completedReports);
                });
    }

    private void showReportDialog(String report, int totalReports, int completedReports) {
        runOnUiThread(() -> {
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Generated Report");
                builder.setMessage(report);
                builder.setPositiveButton("Share", (dialog, which) -> shareReport(report));
                builder.setNegativeButton("Close", null);
                builder.setNeutralButton("Save", (dialog, which) -> saveReportToFirestore(report, totalReports, completedReports));
                builder.show();
            } catch (Exception e) {
                new AlertDialog.Builder(this)
                        .setTitle("Generated Report")
                        .setMessage(report.length() > 500 ? report.substring(0, 500) + "..." : report)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void shareReport(String report) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "CrowdClean Analytics Report");
            shareIntent.putExtra(Intent.EXTRA_TEXT, report);
            startActivity(Intent.createChooser(shareIntent, "Share Report via"));
        } catch (Exception e) {
            Toast.makeText(this, "Error sharing report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveReportToFirestore(String report, int totalReports, int completedReports) {
        try {
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("content", report);
            reportData.put("timestamp", new Date());
            reportData.put("totalReports", totalReports);
            reportData.put("completed", completedReports);
            reportData.put("type", "analytics_report");
            reportData.put("generatedBy", currentUser != null ? currentUser.getEmail() : "admin");

            db.collection("reports_history")
                    .add(reportData)
                    .addOnSuccessListener(documentReference -> Toast.makeText(this, "Report saved to history", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save report", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Toast.makeText(this, "Error preparing report data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void logout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout?");
        builder.setPositiveButton("Yes", (dialog, which) -> performLogout());
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void performLogout() {
        auth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToMainActivity() {
        try {
            Intent intent = new Intent(AdminDashboardActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to MainActivity", e);
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    public void onBackPressed() {
        // Call super.onBackPressed() first to maintain proper behavior
        // Then show the confirmation dialog
        super.onBackPressed();
        showExitConfirmation();
    }

    private void showExitConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit Admin Dashboard");
        builder.setMessage("Are you sure you want to exit?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            // Call super.onBackPressed() to ensure proper back navigation
            super.onBackPressed();
        });
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}
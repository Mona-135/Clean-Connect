package com.example.crowdcleaning.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.crowdcleaning.R;
import com.example.crowdcleaning.adapters.RecentActivityAdapter;
import com.example.crowdcleaning.models.RecentActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    // Statistics TextViews
    private TextView textTotalReports, textCleanedReports, textInProgress, textPending;

    // Buttons
    private Button buttonViewAllReports, buttonManageUsers, buttonSendNotification, buttonGenerateReport, buttonLogout;

    // RecyclerView
    private RecyclerView recyclerViewRecent;
    private RecentActivityAdapter recentActivityAdapter;
    private List<RecentActivity> recentActivityList;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        initializeViews();

        // Setup click listeners
        setupClickListeners();

        // Load statistics data
        loadStatistics();

        // Setup RecyclerView for recent activity
        setupRecyclerView();

        // Load recent activity
        loadRecentActivity();
    }

    private void initializeViews() {
        // Statistics TextViews
        textTotalReports = findViewById(R.id.textTotalReports);
        textCleanedReports = findViewById(R.id.textCleanedReports);
        textInProgress = findViewById(R.id.textInProgress);
        textPending = findViewById(R.id.textPending);

        // Buttons
        buttonViewAllReports = findViewById(R.id.buttonViewAllReports);
        buttonManageUsers = findViewById(R.id.buttonManageUsers);
        buttonSendNotification = findViewById(R.id.buttonSendNotification);
        buttonGenerateReport = findViewById(R.id.buttonGenerateReport);
        buttonLogout = findViewById(R.id.buttonLogout);

        // RecyclerView
        recyclerViewRecent = findViewById(R.id.recyclerViewRecent);

        // Initialize recent activity list
        recentActivityList = new ArrayList<>();
    }

    private void setupClickListeners() {
        buttonViewAllReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewAllReports();
            }
        });

        buttonManageUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manageUsers();
            }
        });

        buttonSendNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendNotification();
            }
        });

        buttonGenerateReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateReport();
            }
        });

        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });
    }

    private void setupRecyclerView() {
        recentActivityAdapter = new RecentActivityAdapter(recentActivityList);
        recyclerViewRecent.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRecent.setAdapter(recentActivityAdapter);
    }

    private void loadStatistics() {
        // Load total reports count
        db.collection("reports")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int totalReports = task.getResult().size();
                        textTotalReports.setText(String.valueOf(totalReports));

                        // Count reports by status
                        int cleanedCount = 0;
                        int inProgressCount = 0;
                        int pendingCount = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String status = document.getString("status");
                            if (status != null) {
                                switch (status) {
                                    case "Completed":
                                    case "Cleaned":
                                        cleanedCount++;
                                        break;
                                    case "In Progress":
                                    case "Assigned":
                                        inProgressCount++;
                                        break;
                                    case "Reported":
                                    case "Pending":
                                        pendingCount++;
                                        break;
                                }
                            }
                        }

                        textCleanedReports.setText(String.valueOf(cleanedCount));
                        textInProgress.setText(String.valueOf(inProgressCount));
                        textPending.setText(String.valueOf(pendingCount));

                    } else {
                        Toast.makeText(AdminDashboardActivity.this,
                                "Failed to load statistics: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadRecentActivity() {
        // Load recent reports (last 10)
        db.collection("reports")
                .orderBy("timestamp")
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        recentActivityList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String description = document.getString("description");
                            String status = document.getString("status");
                            String timestamp = document.getString("timestamp");
                            String userName = document.getString("userName");

                            if (userName == null) {
                                userName = "Unknown User";
                            }

                            RecentActivity activity = new RecentActivity(
                                    userName,
                                    description != null ? description : "No description",
                                    status != null ? status : "Reported",
                                    timestamp != null ? timestamp : "Just now"
                            );

                            recentActivityList.add(activity);
                        }

                        recentActivityAdapter.notifyDataSetChanged();

                    } else {
                        Toast.makeText(AdminDashboardActivity.this,
                                "Failed to load recent activity",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void viewAllReports() {
        // You can create a new activity for viewing all reports
        Toast.makeText(this, "Opening All Reports...", Toast.LENGTH_SHORT).show();
        // Intent intent = new Intent(this, AllReportsActivity.class);
        // startActivity(intent);
    }

    private void manageUsers() {
        Toast.makeText(this, "Opening User Management...", Toast.LENGTH_SHORT).show();
        // Intent intent = new Intent(this, UserManagementActivity.class);
        // startActivity(intent);
    }

    private void sendNotification() {
        Toast.makeText(this, "Send Notification feature coming soon!", Toast.LENGTH_SHORT).show();
        // Implement notification sending logic here
    }

    private void generateReport() {
        Toast.makeText(this, "Generating Report...", Toast.LENGTH_SHORT).show();
        // Implement report generation logic here
        generateAnalyticsReport();
    }

    private void generateAnalyticsReport() {
        // Simple report generation - you can enhance this
        StringBuilder report = new StringBuilder();
        report.append("CrowdClean Analytics Report\n\n");
        report.append("Total Reports: ").append(textTotalReports.getText()).append("\n");
        report.append("Cleaned: ").append(textCleanedReports.getText()).append("\n");
        report.append("In Progress: ").append(textInProgress.getText()).append("\n");
        report.append("Pending: ").append(textPending.getText()).append("\n");
        report.append("\nGenerated on: ").append(java.util.Calendar.getInstance().getTime());

        // Show report in a dialog or share it
        Toast.makeText(this, "Report generated!\n" + report.toString(), Toast.LENGTH_LONG).show();
    }

    private void logout() {
        auth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when activity resumes
        loadStatistics();
        loadRecentActivity();
    }
}
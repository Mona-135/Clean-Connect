package com.example.crowdcleaning.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.crowdcleaning.R;
import com.example.crowdcleaning.adapters.ReportAdapter;
import com.example.crowdcleaning.models.ReportModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class VolunteerDashboardActivity extends AppCompatActivity {

    private TextView textViewWelcome, textAssigned, textCompleted, textPending;
    private Button buttonViewReports, buttonMyTasks, buttonHome, buttonProfile, buttonLogout;
    private RecyclerView recyclerViewReports;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private ReportAdapter reportAdapter;
    private List<ReportModel> reportList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadUserData();
        loadReports();
        loadStats();
    }

    private void initializeViews() {
        // TextViews
        textViewWelcome = findViewById(R.id.textViewWelcome);
        textAssigned = findViewById(R.id.textAssigned);
        textCompleted = findViewById(R.id.textCompleted);
        textPending = findViewById(R.id.textPending);

        // Buttons
        buttonViewReports = findViewById(R.id.buttonViewReports);
        buttonMyTasks = findViewById(R.id.buttonMyTasks);
        buttonHome = findViewById(R.id.buttonHome);
        buttonProfile = findViewById(R.id.buttonProfile);
        buttonLogout = findViewById(R.id.buttonLogout);

        // RecyclerView
        recyclerViewReports = findViewById(R.id.recyclerViewReports);
    }

    private void setupRecyclerView() {
        reportList = new ArrayList<>();
        reportAdapter = new ReportAdapter(reportList, "volunteer");

        recyclerViewReports.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewReports.setAdapter(reportAdapter);
    }

    private void setupClickListeners() {
        buttonViewReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // View all reports (including unassigned ones)
                loadAllReports();
            }
        });

        buttonMyTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // View only assigned reports
                loadMyTasks();
            }
        });

        buttonHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Refresh dashboard
                refreshDashboard();
            }
        });

        buttonProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to profile activity
                Intent intent = new Intent(VolunteerDashboardActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });
    }

    private void loadUserData() {
        if (currentUser != null) {
            // Fetch user details from Firestore
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String userName = documentSnapshot.getString("name");
                            textViewWelcome.setText("Welcome, " + (userName != null ? userName : "Volunteer") + "!");
                        }
                    });
        }
    }

    private void loadReports() {
        // Load recent reports (last 10 reports)
        db.collection("garbage_reports")
                .orderBy("timestamp")
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        reportList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ReportModel report = documentToReportModel(document);
                            if (report != null) {
                                reportList.add(0, report); // Add to beginning for reverse chronological order
                            }
                        }
                        reportAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Failed to load reports", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadAllReports() {
        // Load all available reports for volunteers to pick up
        db.collection("garbage_reports")
                .whereEqualTo("status", "reported") // Only show reported (unassigned) reports
                .orderBy("timestamp")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        reportList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ReportModel report = documentToReportModel(document);
                            if (report != null) {
                                reportList.add(report);
                            }
                        }
                        reportAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "Showing all available reports", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to load reports", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadMyTasks() {
        if (currentUser == null) return;

        // Load reports assigned to current volunteer
        db.collection("garbage_reports")
                .whereEqualTo("volunteerAssigned", currentUser.getUid())
                .orderBy("timestamp")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        reportList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ReportModel report = documentToReportModel(document);
                            if (report != null) {
                                reportList.add(report);
                            }
                        }
                        reportAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "Showing your assigned tasks", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to load your tasks", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadStats() {
        if (currentUser == null) return;

        // Load assigned count
        db.collection("garbage_reports")
                .whereEqualTo("volunteerAssigned", currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        textAssigned.setText(String.valueOf(task.getResult().size()));
                    }
                });

        // Load completed count
        db.collection("garbage_reports")
                .whereEqualTo("volunteerAssigned", currentUser.getUid())
                .whereEqualTo("status", "cleaned")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        textCompleted.setText(String.valueOf(task.getResult().size()));
                    }
                });

        // Load pending count (assigned but not completed)
        db.collection("garbage_reports")
                .whereEqualTo("volunteerAssigned", currentUser.getUid())
                .whereEqualTo("status", "in_progress")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        textPending.setText(String.valueOf(task.getResult().size()));
                    }
                });
    }

    private ReportModel documentToReportModel(QueryDocumentSnapshot document) {
        try {
            String id = document.getId();
            String title = document.getString("title");
            String description = document.getString("description");
            String address = document.getString("address");
            String status = document.getString("status");
            String imageUrl = document.getString("imageUrl");
            Long timestamp = document.getLong("timestamp");
            Long upvotes = document.getLong("upvotes");
            Double latitude = document.getDouble("latitude");
            Double longitude = document.getDouble("longitude");

            return new ReportModel(
                    id,
                    title != null ? title : "No Title",
                    description != null ? description : "No Description",
                    address != null ? address : "No Address",
                    status != null ? status : "reported",
                    imageUrl != null ? imageUrl : "",
                    timestamp != null ? timestamp : System.currentTimeMillis(),
                    upvotes != null ? upvotes : 0,
                    latitude != null ? latitude : 0.0,
                    longitude != null ? longitude : 0.0
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void refreshDashboard() {
        loadReports();
        loadStats();
        Toast.makeText(this, "Dashboard refreshed", Toast.LENGTH_SHORT).show();
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(VolunteerDashboardActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to dashboard
        loadStats();
    }
}
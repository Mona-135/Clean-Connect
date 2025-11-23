package com.example.crowdcleaning.activities;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.crowdcleaning.R;
import com.example.crowdcleaning.adapters.ReportAdapter;
import com.example.crowdcleaning.models.ReportModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class VolunteerDashboardActivity extends AppCompatActivity {

    private TextView textViewWelcome, textAssigned, textCompleted, textPending;
    private Button buttonViewAllReports, buttonMyTasks, buttonHome, buttonProfile, buttonLogout;
    private RecyclerView recyclerViewReports;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private ReportAdapter reportAdapter;
    private List<ReportModel> reportList;

    private boolean showingMyTasks = false;

    // Listener registrations for real-time updates
    private ListenerRegistration assignedListener;
    private ListenerRegistration completedListener;
    private ListenerRegistration pendingListener;

    private static final String TAG = "VolunteerDashboard";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_dashboard);

        Log.d(TAG, "=== VOLUNTEER DASHBOARD STARTED ===");

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Check if user is logged in
        if (currentUser == null) {
            Log.e(TAG, "User is null - redirecting to login");
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Log.d(TAG, "User logged in: " + currentUser.getUid() + " - " + currentUser.getEmail());

        initializeViews();
        setupRecyclerView();
        setupClickListeners();

        // Load initial data
        checkNetworkAndLoadData();

        // Add debug check
        debugCheckAllReports();
    }

    private void initializeViews() {
        // TextViews
        textViewWelcome = findViewById(R.id.textViewWelcome);
        textAssigned = findViewById(R.id.textAssigned);
        textCompleted = findViewById(R.id.textCompleted);
        textPending = findViewById(R.id.textPending);

        // Buttons
        buttonViewAllReports = findViewById(R.id.buttonViewReports);
        buttonMyTasks = findViewById(R.id.buttonMyTasks);
        buttonHome = findViewById(R.id.buttonHome);
        buttonProfile = findViewById(R.id.buttonProfile);
        buttonLogout = findViewById(R.id.buttonLogout);

        // RecyclerView
        recyclerViewReports = findViewById(R.id.recyclerViewReports);

        // Progress
        progressBar = findViewById(R.id.progressBar);

        // SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Initialize with default values
        textAssigned.setText("0");
        textCompleted.setText("0");
        textPending.setText("0");

        Log.d(TAG, "Views initialized successfully");
    }

    private void setupRecyclerView() {
        reportList = new ArrayList<>();
        reportAdapter = new ReportAdapter(reportList, "volunteer");

        // Set up the adapter listener for volunteer actions
        reportAdapter.setOnReportActionListener(new ReportAdapter.OnReportActionListener() {
            @Override
            public void onAcceptTask(ReportModel report) {
                acceptTask(report);
            }

            @Override
            public void onViewDetails(ReportModel report) {
                viewReportDetails(report);
            }

            @Override
            public void onMarkComplete(ReportModel report) {
                markTaskComplete(report);
            }

            @Override
            public void onAdminAction(ReportModel report) {
                // Not used in volunteer dashboard
            }
        });

        recyclerViewReports.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewReports.setAdapter(reportAdapter);

        Log.d(TAG, "RecyclerView setup completed");
    }

    private void setupClickListeners() {
        buttonViewAllReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "View All Reports button clicked");
                if (!isNetworkAvailable()) {
                    showNoInternetToast();
                    return;
                }
                showingMyTasks = false;
                buttonViewAllReports.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                buttonMyTasks.setBackgroundColor(getResources().getColor(R.color.gray));
                loadAvailableReports();
            }
        });

        buttonMyTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "My Tasks button clicked");
                if (!isNetworkAvailable()) {
                    showNoInternetToast();
                    return;
                }
                showingMyTasks = true;
                buttonMyTasks.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                buttonViewAllReports.setBackgroundColor(getResources().getColor(R.color.gray));
                loadMyTasks();
            }
        });

        buttonHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Home/Refresh button clicked");
                refreshDashboard();
            }
        });

        buttonProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

        // Setup swipe refresh
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(TAG, "Swipe to refresh triggered");
                refreshDashboard();
                // Stop the refreshing indicator when done
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        Log.d(TAG, "Click listeners setup completed");
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean available = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        Log.d(TAG, "Network available: " + available);
        return available;
    }

    private void showNoInternetToast() {
        Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
    }

    // UPDATED METHOD: Handle network errors better
    private void handleNetworkError(Exception e) {
        runOnUiThread(() -> {
            if (e instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
                FirebaseFirestoreException.Code errorCode = firestoreException.getCode();

                if (errorCode == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Toast.makeText(this, "Permission denied. Please contact admin.", Toast.LENGTH_LONG).show();
                } else if (errorCode == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    Toast.makeText(this, "Network unavailable. Please check connection.", Toast.LENGTH_LONG).show();
                } else if (errorCode == FirebaseFirestoreException.Code.CANCELLED) {
                    Toast.makeText(this, "Operation was cancelled.", Toast.LENGTH_LONG).show();
                } else if (errorCode == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) {
                    Toast.makeText(this, "Operation timed out. Please try again.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            Log.e(TAG, "Operation failed", e);
        });
    }

    private void checkNetworkAndLoadData() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "Starting to load all data...");
        loadUserData();
        loadStats(); // This now sets up real-time listeners
        loadAvailableReports(); // Default view
    }

    private void loadUserData() {
        if (currentUser != null) {
            Log.d(TAG, "Loading user data for: " + currentUser.getUid());
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String userName = documentSnapshot.getString("name");
                            String userRole = documentSnapshot.getString("role");
                            Log.d(TAG, "User data loaded - Name: " + userName + ", Role: " + userRole);
                            textViewWelcome.setText("Welcome, " + (userName != null ? userName : "Volunteer") + "!");
                        } else {
                            Log.w(TAG, "User document does not exist in Firestore");
                            textViewWelcome.setText("Welcome, Volunteer!");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load user data: " + e.getMessage());
                        textViewWelcome.setText("Welcome, Volunteer!");
                        handleNetworkError(e);
                    });
        }
    }

    private void loadStats() {
        if (currentUser == null) {
            Log.e(TAG, "Cannot load stats - currentUser is null");
            return;
        }

        Log.d(TAG, "Loading statistics for user: " + currentUser.getUid());

        // Use real-time listeners for better updates
        setupStatsListeners();
    }

    private void setupStatsListeners() {
        // Remove existing listeners to avoid duplicates
        removeStatsListeners();

        // Real-time listener for assigned count
        assignedListener = db.collection("reports")
                .whereEqualTo("volunteerAssigned", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed for assigned count: " + error.getMessage());
                        runOnUiThread(() -> textAssigned.setText("0"));
                        return;
                    }

                    if (value != null) {
                        int assignedCount = value.size();
                        Log.d(TAG, "Assigned count updated: " + assignedCount);
                        runOnUiThread(() -> textAssigned.setText(String.valueOf(assignedCount)));
                    }
                });

        // Real-time listener for completed count
        completedListener = db.collection("reports")
                .whereEqualTo("volunteerAssigned", currentUser.getUid())
                .whereEqualTo("status", "completed")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed for completed count: " + error.getMessage());
                        runOnUiThread(() -> textCompleted.setText("0"));
                        return;
                    }

                    if (value != null) {
                        int completedCount = value.size();
                        Log.d(TAG, "Completed count updated: " + completedCount);
                        runOnUiThread(() -> textCompleted.setText(String.valueOf(completedCount)));
                    }
                });

        // Real-time listener for pending count
        pendingListener = db.collection("reports")
                .whereEqualTo("volunteerAssigned", currentUser.getUid())
                .whereIn("status", Arrays.asList("assigned", "in_progress", "pending", "accepted"))
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed for pending count: " + error.getMessage());
                        runOnUiThread(() -> textPending.setText("0"));
                        return;
                    }

                    if (value != null) {
                        int pendingCount = value.size();
                        Log.d(TAG, "Pending count updated: " + pendingCount);
                        runOnUiThread(() -> textPending.setText(String.valueOf(pendingCount)));
                    }
                });

        Log.d(TAG, "Real-time stats listeners setup completed");
    }

    private void removeStatsListeners() {
        // Remove existing listeners to avoid duplicates
        if (assignedListener != null) {
            assignedListener.remove();
            assignedListener = null;
        }
        if (completedListener != null) {
            completedListener.remove();
            completedListener = null;
        }
        if (pendingListener != null) {
            pendingListener.remove();
            pendingListener = null;
        }
        Log.d(TAG, "Previous stats listeners removed");
    }

    private void loadAvailableReports() {
        Log.d(TAG, "Loading available reports (unassigned)");
        showLoading(true);

        reportAdapter.setReportType("available");

        // More flexible query to catch all available reports
        db.collection("reports")
                .whereIn("status", Arrays.asList("reported", "pending", "new", "open", "submitted"))
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    showLoading(false);

                    if (error != null) {
                        Log.e(TAG, "Failed to load available reports: " + error.getMessage());
                        Toast.makeText(this, "Failed to load reports", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        reportList.clear();
                        int count = 0;
                        for (QueryDocumentSnapshot document : value) {
                            ReportModel report = documentToReportModel(document);
                            if (report != null && isReportAvailable(report)) {
                                reportList.add(report);
                                count++;
                                Log.d(TAG, "Available report: " + report.getTitle() +
                                        " - Status: " + report.getStatus() +
                                        " - Volunteer: " + report.getVolunteerAssigned());
                            }
                        }
                        reportAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Available reports loaded: " + count + " reports");

                        if (reportList.isEmpty()) {
                            Toast.makeText(this, "No available reports to accept", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private boolean isReportAvailable(ReportModel report) {
        // Check if report is truly available (unassigned)
        String volunteerAssigned = report.getVolunteerAssigned();
        boolean isUnassigned = volunteerAssigned == null ||
                volunteerAssigned.isEmpty() ||
                "null".equals(volunteerAssigned) ||
                "unassigned".equalsIgnoreCase(volunteerAssigned.trim());

        String status = report.getStatus();
        boolean isAvailableStatus = "reported".equals(status) ||
                "pending".equals(status) ||
                "new".equals(status) ||
                "open".equals(status) ||
                "submitted".equals(status);

        return isUnassigned && isAvailableStatus;
    }

    private void loadMyTasks() {
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Loading my tasks for user: " + currentUser.getUid());
        showLoading(true);

        reportAdapter.setReportType("myTasks");

        // Load reports assigned to current volunteer
        db.collection("reports")
                .whereEqualTo("volunteerAssigned", currentUser.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        reportList.clear();
                        int count = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ReportModel report = documentToReportModel(document);
                            if (report != null) {
                                reportList.add(report);
                                count++;
                                Log.d(TAG, "My task: " + report.getTitle() + " - Status: " + report.getStatus());
                            }
                        }
                        reportAdapter.notifyDataSetChanged();
                        Log.d(TAG, "My tasks loaded: " + count + " reports");

                        if (reportList.isEmpty()) {
                            Toast.makeText(this, "No tasks assigned to you", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "Failed to load my tasks: " + error);
                        Toast.makeText(this, "Failed to load your tasks", Toast.LENGTH_SHORT).show();
                        handleNetworkError(task.getException());
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Failed to load my tasks: " + e.getMessage());
                    Toast.makeText(this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    handleNetworkError(e);
                });
    }

    // ADD DEBUG METHOD TO CHECK ALL REPORTS
    private void debugCheckAllReports() {
        Log.d(TAG, "=== DEBUG: Checking ALL reports with details ===");

        db.collection("reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "=== TOTAL REPORTS: " + task.getResult().size() + " ===");
                        int availableCount = 0;
                        int myTasksCount = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String id = document.getId();
                            String title = document.getString("title");
                            String status = document.getString("status");
                            String volunteerAssigned = document.getString("volunteerAssigned");
                            String volunteerName = document.getString("volunteerName");
                            Object timestamp = document.get("timestamp");

                            Log.d(TAG, "REPORT - ID: " + id +
                                    ", Title: " + title +
                                    ", Status: " + status +
                                    ", VolunteerAssigned: " + volunteerAssigned +
                                    ", VolunteerName: " + volunteerName);

                            // Count reports for current user
                            if (currentUser != null && currentUser.getUid().equals(volunteerAssigned)) {
                                myTasksCount++;
                                Log.d(TAG, "*** THIS IS MY TASK ***");
                            }

                            // Count available reports
                            if (isReportAvailable(documentToReportModel(document))) {
                                availableCount++;
                                Log.d(TAG, "*** THIS IS AVAILABLE ***");
                            }
                        }

                        Log.d(TAG, "=== SUMMARY ===");
                        Log.d(TAG, "My tasks: " + myTasksCount);
                        Log.d(TAG, "Available reports: " + availableCount);
                        Log.d(TAG, "Current user ID: " + (currentUser != null ? currentUser.getUid() : "null"));

                    } else {
                        Log.e(TAG, "Debug query failed: " + task.getException());
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
            String volunteerAssigned = document.getString("volunteerAssigned");
            String volunteerName = document.getString("volunteerName");
            String userName = document.getString("userName");
            String userEmail = document.getString("userEmail");

            // FIX: Properly handle image URLs - check both single imageUrl and imageUrls array
            String imageUrl = "";
            List<String> imageUrls = new ArrayList<>();

            try {
                // First try to get imageUrls array (from ReportGarbageActivity)
                Object imageUrlsObj = document.get("imageUrls");
                if (imageUrlsObj instanceof List) {
                    imageUrls = (List<String>) imageUrlsObj;
                    if (imageUrls != null && !imageUrls.isEmpty()) {
                        imageUrl = imageUrls.get(0); // Use first image for thumbnail
                    }
                }

                // If no imageUrls found, try single imageUrl field (from AddReportActivity)
                if (imageUrl.isEmpty()) {
                    String singleImageUrl = document.getString("imageUrl");
                    if (singleImageUrl != null && !singleImageUrl.isEmpty()) {
                        imageUrl = singleImageUrl;
                        imageUrls = Arrays.asList(singleImageUrl); // Convert to list for consistency
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error reading image URLs for document: " + id, e);
            }

            // Handle timestamp
            long timestampMillis;
            Object timestampObj = document.get("timestamp");
            if (timestampObj instanceof com.google.firebase.Timestamp) {
                timestampMillis = ((com.google.firebase.Timestamp) timestampObj).toDate().getTime();
            } else if (timestampObj instanceof Long) {
                timestampMillis = (Long) timestampObj;
            } else if (timestampObj instanceof Date) {
                timestampMillis = ((Date) timestampObj).getTime();
            } else {
                timestampMillis = System.currentTimeMillis();
            }

            Long upvotes = document.getLong("upvotes");
            Double latitude = document.getDouble("latitude");
            Double longitude = document.getDouble("longitude");

            ReportModel report = new ReportModel(
                    id,
                    title != null ? title : "No Title",
                    description != null ? description : "No Description",
                    address != null ? address : "No Address",
                    status != null ? status : "reported",
                    imageUrl, // Now properly set
                    timestampMillis,
                    upvotes != null ? upvotes : 0,
                    latitude != null ? latitude : 0.0,
                    longitude != null ? longitude : 0.0
            );

            // Set additional fields
            report.setVolunteerAssigned(volunteerAssigned);
            report.setVolunteerName(volunteerName);
            report.setUserName(userName);
            report.setUserEmail(userEmail);
            report.setImageUrls(imageUrls); // Set the full list of image URLs

            Log.d(TAG, "Loaded report - ID: " + id +
                    ", Image URL: " + (imageUrl.isEmpty() ? "No image" : imageUrl) +
                    ", Total images: " + imageUrls.size());

            return report;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to ReportModel: " + e.getMessage());
            return null;
        }
    }

    private void acceptTask(ReportModel report) {
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isNetworkAvailable()) {
            showNoInternetToast();
            return;
        }

        Log.d(TAG, "Accepting task: " + report.getId());

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Accept Task")
                .setMessage("Are you sure you want to accept this task: " + report.getTitle() + "?")
                .setPositiveButton("Accept", (dialog, which) -> {
                    performAcceptTask(report);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performAcceptTask(ReportModel report) {
        Log.d(TAG, "Performing accept task for: " + report.getId());

        // Get current user name for the report
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(userDocument -> {
                    String volunteerName = userDocument.getString("name");
                    if (volunteerName == null) {
                        volunteerName = currentUser.getDisplayName();
                        if (volunteerName == null) {
                            volunteerName = "Volunteer";
                        }
                    }

                    // Update the report to assign it to the current volunteer
                    db.collection("reports").document(report.getId())
                            .update(
                                    "volunteerAssigned", currentUser.getUid(),
                                    "volunteerName", volunteerName,
                                    "status", "assigned",
                                    "assignedAt", new Date()
                            )
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Task accepted successfully!");
                                Toast.makeText(this, "Task accepted successfully!", Toast.LENGTH_SHORT).show();
                                refreshDashboard();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to accept task: " + e.getMessage());
                                Toast.makeText(this, "Failed to accept task", Toast.LENGTH_SHORT).show();
                                handleNetworkError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get user data: " + e.getMessage());
                    Toast.makeText(this, "Failed to get user data", Toast.LENGTH_SHORT).show();
                    handleNetworkError(e);
                });
    }

    private void markTaskComplete(ReportModel report) {
        Log.d(TAG, "Marking task complete: " + report.getId());
        Intent intent = new Intent(this, CompleteTaskActivity.class);
        intent.putExtra("report_id", report.getId());
        startActivityForResult(intent, 1001);
    }

    private void viewReportDetails(ReportModel report) {
        Log.d(TAG, "Viewing report details: " + report.getId());
        Intent intent = new Intent(VolunteerDashboardActivity.this, ReportDetailActivity.class);
        intent.putExtra("report_id", report.getId());
        intent.putExtra("user_type", "volunteer");
        startActivity(intent);
    }

    private void refreshDashboard() {
        Log.d(TAG, "Refreshing dashboard - showingMyTasks: " + showingMyTasks);
        if (!isNetworkAvailable()) {
            showNoInternetToast();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        // Force refresh all data
        loadUserData();
        // Stats are automatically updated by real-time listeners

        // Refresh based on current view with slight delay to ensure data consistency
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (showingMyTasks) {
                    loadMyTasks();
                } else {
                    loadAvailableReports();
                }
                // Stop refresh indicator when data loading is complete
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 500);

        // Also debug current state
        debugCheckAllReports();
    }

    private void logoutUser() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        Log.d(TAG, "Logging out user");
        // Remove listeners before logout
        removeStatsListeners();
        mAuth.signOut();
        Intent intent = new Intent(VolunteerDashboardActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            recyclerViewReports.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            recyclerViewReports.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK) {
            Log.d(TAG, "Task completed successfully, refreshing dashboard");
            refreshDashboard();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed");
        if (currentUser != null) {
            if (!isNetworkAvailable()) {
                showNoInternetToast();
                return;
            }
            // Stats are automatically updated by real-time listeners
            if (showingMyTasks) {
                loadMyTasks();
            } else {
                loadAvailableReports();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Activity paused");
        // We keep listeners active for real-time updates
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity destroyed");
        // Remove listeners to prevent memory leaks
        removeStatsListeners();
    }
}
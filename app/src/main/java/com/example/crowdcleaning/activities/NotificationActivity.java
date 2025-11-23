package com.example.crowdcleaning.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.crowdcleaning.R;
import com.example.crowdcleaning.adapters.NotificationAdapter;
import com.example.crowdcleaning.models.NotificationModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationActivity extends AppCompatActivity {

    private RadioGroup radioGroupAudience;
    private EditText editTextTitle, editTextMessage;
    private Button buttonSend;
    private RecyclerView recyclerViewNotifications;
    private ImageButton buttonBack;

    private NotificationAdapter notificationAdapter;
    private List<NotificationModel> notificationList;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private SimpleDateFormat dateFormat;

    private static final String TAG = "NotificationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        Log.d(TAG, "NotificationActivity onCreate started");

        // Initialize Firebase components
        initializeFirebase();

        // Initialize UI components
        initializeViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup click listeners
        setupClickListeners();

        // Load notifications
        loadNotifications();

        Log.d(TAG, "NotificationActivity created successfully");
    }

    private void initializeFirebase() {
        try {
            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();
            currentUser = auth.getCurrentUser();
            dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed: " + e.getMessage());
            Toast.makeText(this, "Firebase service unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeViews() {
        radioGroupAudience = findViewById(R.id.radioGroupAudience);
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        recyclerViewNotifications = findViewById(R.id.recyclerViewNotifications);
        buttonBack = findViewById(R.id.buttonBack);

        // Initialize notification list
        notificationList = new ArrayList<>();

        // Set default audience
        if (radioGroupAudience != null) {
            radioGroupAudience.check(R.id.radioAllUsers);
        }

        Log.d(TAG, "All views initialized successfully");
    }

    private void setupRecyclerView() {
        try {
            notificationAdapter = new NotificationAdapter(notificationList);
            recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewNotifications.setAdapter(notificationAdapter);
            Log.d(TAG, "RecyclerView setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView: " + e.getMessage());
            Toast.makeText(this, "Error setting up notifications list", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        // Send button click listener
        buttonSend.setOnClickListener(v -> {
            Log.d(TAG, "Send button clicked");
            sendNotification();
        });

        // Back button click listener
        buttonBack.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            goBackToDashboard();
        });
    }

    private void sendNotification() {
        if (db == null) {
            Toast.makeText(this, "Database service unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Please login to send notifications", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = editTextTitle.getText().toString().trim();
        String message = editTextMessage.getText().toString().trim();

        Log.d(TAG, "Sending notification - Title: " + title + ", Message: " + message);

        // Validation
        if (title.isEmpty()) {
            editTextTitle.setError("Title is required");
            editTextTitle.requestFocus();
            return;
        }

        if (message.isEmpty()) {
            editTextMessage.setError("Message is required");
            editTextMessage.requestFocus();
            return;
        }

        // Get selected audience
        String audience = getSelectedAudience();
        Log.d(TAG, "Selected audience: " + audience);

        // Create notification data
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("message", message);
        notification.put("audience", audience);
        notification.put("timestamp", new Date());
        notification.put("sentBy", currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : "Admin");
        notification.put("sentByEmail", currentUser.getEmail());

        // Show loading
        buttonSend.setEnabled(false);
        buttonSend.setText("Sending...");

        // Save to Firestore
        db.collection("notifications")
                .add(notification)
                .addOnCompleteListener(task -> {
                    // Reset button state
                    buttonSend.setEnabled(true);
                    buttonSend.setText("SEND NOTIFICATION");

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Notification sent successfully");
                        Toast.makeText(NotificationActivity.this,
                                "Notification sent successfully!", Toast.LENGTH_SHORT).show();

                        // Clear form
                        editTextTitle.setText("");
                        editTextMessage.setText("");
                        radioGroupAudience.check(R.id.radioAllUsers);

                        // Reload notifications
                        loadNotifications();

                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "Failed to send notification: " + errorMessage);
                        Toast.makeText(NotificationActivity.this,
                                "Failed to send notification: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Reset button state
                    buttonSend.setEnabled(true);
                    buttonSend.setText("SEND NOTIFICATION");
                    Log.e(TAG, "Failed to send notification: " + e.getMessage());
                    Toast.makeText(NotificationActivity.this,
                            "Failed to send notification: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private String getSelectedAudience() {
        if (radioGroupAudience == null) {
            return "All Users";
        }

        int selectedId = radioGroupAudience.getCheckedRadioButtonId();

        if (selectedId == R.id.radioAllUsers) {
            return "All Users";
        } else if (selectedId == R.id.radioVolunteers) {
            return "Volunteers Only";
        } else if (selectedId == R.id.radioCitizens) {
            return "Citizens Only";
        } else {
            return "All Users";
        }
    }

    private void loadNotifications() {
        if (db == null) {
            Log.e(TAG, "Firestore not initialized, cannot load notifications");
            return;
        }

        Log.d(TAG, "Loading notifications from Firestore");

        db.collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Notifications loaded successfully, count: " +
                                (task.getResult() != null ? task.getResult().size() : 0));

                        notificationList.clear();

                        if (task.getResult() != null) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    String title = document.getString("title");
                                    String message = document.getString("message");
                                    String audience = document.getString("audience");
                                    Date timestamp = document.getDate("timestamp");
                                    String sentBy = document.getString("sentBy");

                                    // Handle null values with defaults
                                    if (title == null) title = "No Title";
                                    if (message == null) message = "No Message";
                                    if (audience == null) audience = "All Users";
                                    if (sentBy == null) sentBy = "Admin";
                                    if (timestamp == null) timestamp = new Date();

                                    // Format timestamp
                                    String timeString = formatTimestamp(timestamp);

                                    // Create notification model
                                    NotificationModel notification = new NotificationModel(
                                            title,
                                            message,
                                            audience,
                                            timeString,
                                            sentBy
                                    );

                                    notificationList.add(notification);

                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing notification document: " + e.getMessage());
                                }
                            }
                        }

                        // Update adapter
                        if (notificationAdapter != null) {
                            notificationAdapter.notifyDataSetChanged();
                        }

                        // Show empty state if no notifications
                        if (notificationList.isEmpty()) {
                            Log.d(TAG, "No notifications found");
                        }

                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "Failed to load notifications: " + errorMessage);
                        Toast.makeText(this,
                                "Failed to load notifications: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load notifications: " + e.getMessage());
                    Toast.makeText(this,
                            "Failed to load notifications: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private String formatTimestamp(Date timestamp) {
        if (timestamp == null) return "Unknown time";

        long timeDiff = System.currentTimeMillis() - timestamp.getTime();

        if (timeDiff < 60000) return "Just now";
        else if (timeDiff < 3600000) {
            long minutes = timeDiff / 60000;
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else if (timeDiff < 86400000) {
            long hours = timeDiff / 3600000;
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else {
            return dateFormat.format(timestamp);
        }
    }

    private void goBackToDashboard() {
        try {
            Intent intent = new Intent(NotificationActivity.this, AdminDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            Log.d(TAG, "Navigating back to Admin Dashboard");
        } catch (Exception e) {
            Log.e(TAG, "Error navigating back to dashboard: " + e.getMessage());
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button pressed");
        goBackToDashboard();
        super.onBackPressed(); // Added this line to fix the error
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "NotificationActivity resumed");

        // Reload notifications when activity becomes visible
        loadNotifications();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "NotificationActivity started");

        // Check if user is still logged in
        if (auth != null) {
            currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
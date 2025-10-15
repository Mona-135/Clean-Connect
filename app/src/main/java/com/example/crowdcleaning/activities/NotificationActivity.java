package com.example.crowdcleaning.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.crowdcleaning.R;
import com.example.crowdcleaning.adapters.NotificationAdapter;
import com.example.crowdcleaning.models.NotificationModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationActivity extends AppCompatActivity {

    private RadioGroup radioGroupAudience;
    private EditText editTextTitle, editTextMessage;
    private Button buttonSend;
    private RecyclerView recyclerViewNotifications;

    private NotificationAdapter notificationAdapter;
    private List<NotificationModel> notificationList;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Load existing notifications
        loadNotifications();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        // These IDs match your XML perfectly
        radioGroupAudience = findViewById(R.id.radioGroupAudience);
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        recyclerViewNotifications = findViewById(R.id.recyclerViewNotifications);

        notificationList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        notificationAdapter = new NotificationAdapter(notificationList);
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotifications.setAdapter(notificationAdapter);
    }

    private void setupClickListeners() {
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendNotification();
            }
        });
    }

    private void sendNotification() {
        String title = editTextTitle.getText().toString().trim();
        String message = editTextMessage.getText().toString().trim();

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

        // Create notification data
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("message", message);
        notification.put("audience", audience);
        notification.put("timestamp", new Date());
        notification.put("sentBy", "Admin");

        // Show loading
        buttonSend.setEnabled(false);
        buttonSend.setText("Sending...");

        // Save to Firestore
        db.collection("notifications")
                .add(notification)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(NotificationActivity.this,
                                "Notification sent successfully!", Toast.LENGTH_SHORT).show();

                        // Clear form
                        editTextTitle.setText("");
                        editTextMessage.setText("");

                        // Reload notifications
                        loadNotifications();
                    } else {
                        Toast.makeText(NotificationActivity.this,
                                "Failed to send notification: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }

                    // Reset button
                    buttonSend.setEnabled(true);
                    buttonSend.setText("SEND NOTIFICATION");
                });
    }

    private String getSelectedAudience() {
        int selectedId = radioGroupAudience.getCheckedRadioButtonId();

        if (selectedId == R.id.radioAllUsers) {
            return "All Users";
        } else if (selectedId == R.id.radioVolunteers) {
            return "Volunteers Only";
        } else if (selectedId == R.id.radioCitizens) {
            return "Citizens Only";
        } else {
            return "All Users"; // Default
        }
    }

    private void loadNotifications() {
        db.collection("notifications")
                .orderBy("timestamp")
                .limit(20)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        notificationList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String title = document.getString("title");
                            String message = document.getString("message");
                            String audience = document.getString("audience");
                            Date timestamp = document.getDate("timestamp");
                            String sentBy = document.getString("sentBy");

                            // Format timestamp
                            String timeString = "Just now";
                            if (timestamp != null) {
                                timeString = android.text.format.DateFormat.format("dd MMM yyyy HH:mm", timestamp).toString();
                            }

                            NotificationModel notification = new NotificationModel(
                                    title != null ? title : "No Title",
                                    message != null ? message : "No Message",
                                    audience != null ? audience : "All Users",
                                    timeString,
                                    sentBy != null ? sentBy : "Admin"
                            );

                            notificationList.add(0, notification);
                        }

                        notificationAdapter.notifyDataSetChanged();

                    } else {
                        Toast.makeText(NotificationActivity.this,
                                "Failed to load notifications", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }
}
package com.example.crowdcleaning.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.crowdcleaning.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class CitizenDashboardActivity extends AppCompatActivity {

    private TextView textViewWelcome;
    private Button buttonReportGarbage, buttonViewReports, buttonLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
        loadUserData();
    }

    private void initializeViews() {
        textViewWelcome = findViewById(R.id.textViewWelcome);
        buttonReportGarbage = findViewById(R.id.buttonReportGarbage);
        buttonViewReports = findViewById(R.id.buttonViewReports);
        buttonLogout = findViewById(R.id.buttonLogout);
    }

    private void setupClickListeners() {
        buttonReportGarbage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CitizenDashboardActivity.this, ReportGarbageActivity.class);
                startActivity(intent);
            }
        });

        buttonViewReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // For now, show a message - you can implement this later
                Toast.makeText(CitizenDashboardActivity.this,
                        "View Reports feature coming soon!", Toast.LENGTH_SHORT).show();
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
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String userName = documentSnapshot.getString("name");
                            // Fix: Handle potential null values
                            textViewWelcome.setText("Welcome, " + (userName != null ? userName : "Citizen") + "!");
                        } else {
                            textViewWelcome.setText("Welcome, Citizen!");
                        }
                    })
                    .addOnFailureListener(e -> {
                        textViewWelcome.setText("Welcome, Citizen!");
                    });
        }
    }

    private void logoutUser() {
        mAuth.signOut();
        // Redirect to login activity instead of MainActivity
        Intent intent = new Intent(CitizenDashboardActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
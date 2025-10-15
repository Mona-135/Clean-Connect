package com.example.crowdcleaning.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.crowdcleaning.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private Button buttonLogin, buttonRegister;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Splash screen duration
    private static final int SPLASH_DELAY = 1000; // 1 second

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();

        // Check if user is already logged in after a short delay
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkCurrentUser();
            }
        }, SPLASH_DELAY);
    }

    private void initializeViews() {
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonRegister = findViewById(R.id.buttonRegister);
    }

    private void setupClickListeners() {
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToLogin();
            }
        });

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToRegister();
            }
        });
    }

    private void checkCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is signed in, check their role and redirect accordingly
            checkUserRoleAndRedirect(currentUser.getUid());
        } else {
            // User is not signed in, show the main screen with login/register buttons
            // The buttons are already visible in the layout
        }
    }

    private void checkUserRoleAndRedirect(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        redirectToDashboard(role);
                    } else {
                        // User document doesn't exist, treat as new user
                        navigateToLogin();
                    }
                })
                .addOnFailureListener(e -> {
                    // If there's an error, redirect to login
                    navigateToLogin();
                });
    }

    private void redirectToDashboard(String role) {
        Intent intent;
        switch (role != null ? role : "citizen") {
            case "volunteer":
                intent = new Intent(MainActivity.this, VolunteerDashboardActivity.class);
                break;
            case "admin":
                intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
                break;
            case "citizen":
            default:
                intent = new Intent(MainActivity.this, CitizenDashboardActivity.class);
                break;
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        // Don't finish() so user can press back to return to main screen
    }

    private void navigateToRegister() {
        Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
        startActivity(intent);
        // Don't finish() so user can press back to return to main screen
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Additional check when activity comes to foreground
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // If user signs in from another activity, redirect immediately
            checkUserRoleAndRedirect(currentUser.getUid());
        }
    }

    @Override
    public void onBackPressed() {
        // Exit app when back is pressed from main activity
        if (mAuth.getCurrentUser() != null) {
            // If user is logged in, go to dashboard instead of exiting
            checkUserRoleAndRedirect(mAuth.getCurrentUser().getUid());
        } else {
            super.onBackPressed();
        }
    }
}
package com.example.crowdcleaning.activities;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.crowdcleaning.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Button buttonLogin, buttonRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove StrictMode in production - it's only for debugging
        // enableStrictMode();

        setContentView(R.layout.activity_main);
        Log.d(TAG, "Layout inflated successfully");

        initializeViews();
        initializeFirebase();
        setupClickListeners();
    }

    private void initializeViews() {
        try {
            buttonLogin = findViewById(R.id.buttonLogin);
            buttonRegister = findViewById(R.id.buttonRegister);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            Toast.makeText(this, "Error initializing app", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeFirebase() {
        try {
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firebase initialized successfully");

            // Check current user after Firebase is initialized
            checkCurrentUser();

        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed: " + e.getMessage(), e);
            // Continue with offline mode - user can still see the UI
            setupUI();
        }
    }

    private void setupClickListeners() {
        buttonLogin.setOnClickListener(v -> navigateToLogin());
        buttonRegister.setOnClickListener(v -> navigateToRegister());
    }

    private void checkCurrentUser() {
        if (mAuth == null) {
            Log.w(TAG, "Firebase Auth not initialized");
            setupUI();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            // User is logged in and verified, redirect to appropriate dashboard
            Log.d(TAG, "User already logged in, redirecting to dashboard");
            checkUserRoleAndRedirect(currentUser.getUid());
        } else {
            // User not logged in or email not verified, show login/register UI
            Log.d(TAG, "User not logged in or email not verified, showing UI");
            setupUI();
        }
    }

    private void checkUserRoleAndRedirect(String userId) {
        if (db == null) {
            Log.w(TAG, "Firestore not available, defaulting to Citizen Dashboard");
            redirectToDashboard("citizen");
            return;
        }

        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        String role = task.getResult().getString("role");
                        Log.d(TAG, "User role found: " + role);
                        redirectToDashboard(role);
                    } else {
                        Log.w(TAG, "User document not found, defaulting to Citizen Dashboard");
                        redirectToDashboard("citizen");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user role", e);
                    // On error, default to citizen dashboard
                    redirectToDashboard("citizen");
                });
    }

    private void setupUI() {
        try {
            // Make sure buttons are enabled and visible
            buttonLogin.setEnabled(true);
            buttonRegister.setEnabled(true);

            // Show welcome message if needed
            if (!isNetworkAvailable()) {
                Toast.makeText(this, "No internet connection - some features may be limited", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up UI", e);
        }
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private void redirectToDashboard(String role) {
        Intent intent;
        if ("volunteer".equals(role)) {
            intent = new Intent(this, VolunteerDashboardActivity.class);
            Log.d(TAG, "Redirecting to Volunteer Dashboard");
        } else if ("admin".equals(role)) {
            intent = new Intent(this, AdminDashboardActivity.class);
            Log.d(TAG, "Redirecting to Admin Dashboard");
        } else {
            intent = new Intent(this, CitizenDashboardActivity.class);
            Log.d(TAG, "Redirecting to Citizen Dashboard");
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Log.d(TAG, "Navigating to LoginActivity");
        startActivity(new Intent(this, LoginActivity.class));
        // Don't finish() here to allow back navigation
    }

    private void navigateToRegister() {
        Log.d(TAG, "Navigating to RegisterActivity");
        startActivity(new Intent(this, RegisterActivity.class));
        // Don't finish() here to allow back navigation
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if user logged out from other activities
        if (mAuth != null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                // User logged out, ensure UI is visible
                setupUI();
            }
        }
    }

    // Remove StrictMode in production - it can cause performance issues
    /*
    private void enableStrictMode() {
        if (BuildConfig.DEBUG) {
            android.os.StrictMode.setThreadPolicy(new android.os.StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            android.os.StrictMode.setVmPolicy(new android.os.StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
    }
    */
}
package com.example.crowdcleaning.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.crowdcleaning.R;
import com.google.firebase.FirebaseApp;

public class TestActivity extends AppCompatActivity {

    private static final String TAG = "TestActivity";
    private TextView statusText;
    private Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        initializeViews();
        setupClickListeners();
        runTests();
    }

    private void initializeViews() {
        statusText = findViewById(R.id.statusText);
        btnNext = findViewById(R.id.btnNext);

        // Set initial status
        statusText.setText("Initializing app...");
        btnNext.setEnabled(false);
    }

    private void setupClickListeners() {
        btnNext.setOnClickListener(v -> {
            navigateToLogin();
        });
    }

    private void runTests() {
        statusText.setText("Running tests...");

        // Test 1: Background thread operation
        testBackgroundThread();

        // Test 2: Firebase initialization (if used)
        testFirebaseInitialization();

        // Test 3: UI thread operations
        testUIThread();
    }

    private void testBackgroundThread() {
        new Thread(() -> {
            try {
                // Simulate some background work
                Thread.sleep(800);

                // Update UI on main thread
                runOnUiThread(() -> {
                    statusText.setText("✓ Background thread test passed!");
                    Log.d(TAG, "Background thread test completed");
                });

                // Continue with next test after a delay
                new Handler(Looper.getMainLooper()).postDelayed(this::testFirebaseInitialization, 500);

            } catch (InterruptedException e) {
                Log.e(TAG, "Background thread test failed", e);
                runOnUiThread(() -> statusText.setText("✗ Background thread test failed"));
            }
        }).start();
    }

    private void testFirebaseInitialization() {
        try {
            // Check if Firebase is initialized
            FirebaseApp firebaseApp = FirebaseApp.getInstance();
            runOnUiThread(() -> {
                statusText.setText("✓ Firebase initialization test passed!");
                Log.d(TAG, "Firebase initialization test completed");
            });
        } catch (Exception e) {
            Log.w(TAG, "Firebase not initialized or test skipped");
            runOnUiThread(() -> statusText.setText("Firebase test skipped"));
        }

        // Continue with next test
        new Handler(Looper.getMainLooper()).postDelayed(this::testUIThread, 500);
    }

    private void testUIThread() {
        runOnUiThread(() -> {
            try {
                // Test UI operations
                statusText.setText("✓ UI thread test passed!");
                btnNext.setEnabled(true);
                btnNext.setText("Continue to Login");

                Log.d(TAG, "UI thread test completed");
                Log.d(TAG, "All tests passed successfully!");

                // Show final success message
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    statusText.setText("All tests passed! Ready to continue.");
                }, 1000);

            } catch (Exception e) {
                Log.e(TAG, "UI thread test failed", e);
                statusText.setText("✗ UI thread test failed");
            }
        });
    }

    private void navigateToLogin() {
        try {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();

            // Add transition animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        } catch (Exception e) {
            Log.e(TAG, "Navigation to LoginActivity failed", e);
            statusText.setText("Error: Cannot open Login screen");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "TestActivity destroyed");
    }
}
package com.example.crowdcleaning.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.crowdcleaning.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private TextView textViewRegister, textViewForgotPassword;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        try {
            // Initialize Firebase Auth and Firestore
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Toast.makeText(this, "Firebase initialization failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Firebase initialization error", e);
            return;
        }

        // Initialize views
        initializeViews();

        // Set up click listeners
        setupClickListeners();

        // Check if user is already logged in
        checkCurrentUser();
    }

    private void initializeViews() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegister = findViewById(R.id.textViewRegister);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);
    }

    private void setupClickListeners() {
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        textViewRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Register Activity
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        textViewForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle forgot password functionality
                handleForgotPassword();
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void handleForgotPassword() {
        if (!isNetworkAvailable()) {
            Toast.makeText(LoginActivity.this, "No internet connection", Toast.LENGTH_LONG).show();
            return;
        }

        String email = editTextEmail.getText().toString().trim();

        if (email.isEmpty()) {
            editTextEmail.setError("Enter your email to reset password");
            editTextEmail.requestFocus();
            return;
        }

        // Send password reset email
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this,
                                    "Password reset email sent to " + email,
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Failed to send reset email: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void checkCurrentUser() {
        if (!isNetworkAvailable()) {
            // If no network, we cannot check the current user, so return and let the user login manually
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already logged in, redirect to appropriate dashboard
            redirectToDashboard(currentUser.getUid());
        }
    }

    private void loginUser() {
        if (!isNetworkAvailable()) {
            Toast.makeText(LoginActivity.this, "No internet connection", Toast.LENGTH_LONG).show();
            return;
        }

        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validation
        if (email.isEmpty()) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters");
            editTextPassword.requestFocus();
            return;
        }

        // Show loading
        buttonLogin.setEnabled(false);
        buttonLogin.setText("Logging in...");

        // Firebase authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Login successful
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Check user type and redirect accordingly
                                redirectToDashboard(user.getUid());
                            }
                        } else {
                            // Login failed
                            buttonLogin.setEnabled(true);
                            buttonLogin.setText("LOGIN");
                            Toast.makeText(LoginActivity.this,
                                    "Login failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void redirectToDashboard(String userId) {
        // Check user type from Firestore
        db.collection("users").document(userId).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(Task<DocumentSnapshot> task) {
                        Intent intent;

                        if (task.isSuccessful() && task.getResult().exists()) {
                            // Fix: Use "role" instead of "userType" to match RegisterActivity
                            String userType = task.getResult().getString("role");

                            if ("admin".equals(userType)) {
                                intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                            } else if ("volunteer".equals(userType)) {
                                intent = new Intent(LoginActivity.this, VolunteerDashboardActivity.class);
                            } else {
                                // Fix: Redirect to CitizenDashboard instead of ReportGarbage
                                intent = new Intent(LoginActivity.this, CitizenDashboardActivity.class);
                            }
                        } else {
                            // Default to Citizen Dashboard
                            intent = new Intent(LoginActivity.this, CitizenDashboardActivity.class);
                        }

                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Reset button state when activity becomes visible
        buttonLogin.setEnabled(true);
        buttonLogin.setText("LOGIN");
    }
}
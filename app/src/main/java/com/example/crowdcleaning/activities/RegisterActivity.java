package com.example.crowdcleaning.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.crowdcleaning.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextName, editTextEmail, editTextPassword, editTextPhone;
    private RadioGroup radioGroupRole;
    private Button buttonRegister;
    private TextView textViewLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Log.d(TAG, "RegisterActivity created");

        // Initialize Firebase Auth and Firestore
        try {
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
            Toast.makeText(this, "Authentication service unavailable", Toast.LENGTH_SHORT).show();
        }

        initializeViews();
        setupClickListeners();
        testInputFields();
    }

    private void initializeViews() {
        try {
            editTextName = findViewById(R.id.editTextName);
            editTextEmail = findViewById(R.id.editTextEmail);
            editTextPassword = findViewById(R.id.editTextPassword);
            editTextPhone = findViewById(R.id.editTextPhone);
            radioGroupRole = findViewById(R.id.radioGroupRole);
            buttonRegister = findViewById(R.id.buttonRegister);
            textViewLogin = findViewById(R.id.textViewLogin);

            // Debug: Add focus listeners to check if fields are receiving focus
            setupDebugListeners();

            Log.d(TAG, "All views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            Toast.makeText(this, "Error initializing form", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupDebugListeners() {
        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            String fieldName = "Unknown";
            if (v == editTextName) fieldName = "Name";
            else if (v == editTextEmail) fieldName = "Email";
            else if (v == editTextPassword) fieldName = "Password";
            else if (v == editTextPhone) fieldName = "Phone";

            Log.d(TAG, fieldName + " field focus: " + hasFocus);
        };

        editTextName.setOnFocusChangeListener(focusListener);
        editTextEmail.setOnFocusChangeListener(focusListener);
        editTextPassword.setOnFocusChangeListener(focusListener);
        editTextPhone.setOnFocusChangeListener(focusListener);
    }

    private void testInputFields() {
        // Test if fields can receive focus programmatically
        editTextName.postDelayed(() -> {
            boolean nameFocused = editTextName.requestFocus();
            Log.d(TAG, "Name field focus request: " + nameFocused);
        }, 500);
    }

    private void setupClickListeners() {
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Register button clicked");
                registerUser();
            }
        });

        textViewLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Login link clicked");
                // Navigate to Login Activity
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void registerUser() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String role = getSelectedRole();

        Log.d(TAG, "Registration attempt - Name: " + name + ", Email: " + email + ", Role: " + role);

        // Validation
        if (TextUtils.isEmpty(name)) {
            editTextName.setError("Full name is required");
            editTextName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Please enter a valid email");
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters");
            editTextPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            editTextPhone.setError("Phone number is required");
            editTextPhone.requestFocus();
            return;
        }

        // Basic phone validation (at least 10 digits)
        String digitsOnly = phone.replaceAll("\\D", "");
        if (digitsOnly.length() < 10) {
            editTextPhone.setError("Please enter a valid phone number");
            editTextPhone.requestFocus();
            return;
        }

        // Show loading
        buttonRegister.setEnabled(false);
        buttonRegister.setText("Creating Account...");

        // Create user with Firebase Auth
        if (mAuth == null) {
            Toast.makeText(this, "Authentication service unavailable", Toast.LENGTH_SHORT).show();
            buttonRegister.setEnabled(true);
            buttonRegister.setText("REGISTER");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase auth successful");
                        // Registration success, save user data to Firestore
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user.getUid(), name, email, phone, role);
                        } else {
                            // This should not happen, but handle it
                            buttonRegister.setEnabled(true);
                            buttonRegister.setText("REGISTER");
                            Toast.makeText(RegisterActivity.this,
                                    "Registration failed: User is null",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Registration failed
                        buttonRegister.setEnabled(true);
                        buttonRegister.setText("REGISTER");
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Registration failed";
                        Log.e(TAG, "Registration failed: " + errorMessage);
                        Toast.makeText(RegisterActivity.this,
                                "Registration failed: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String userId, String name, String email, String phone, String role) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("phone", phone);
        user.put("role", role);
        user.put("createdAt", System.currentTimeMillis());
        user.put("status", "active");

        db.collection("users").document(userId)
                .set(user)
                .addOnCompleteListener(task -> {
                    buttonRegister.setEnabled(true);
                    buttonRegister.setText("REGISTER");

                    if (task.isSuccessful()) {
                        Log.d(TAG, "User data saved to Firestore");
                        Toast.makeText(RegisterActivity.this,
                                "Registration successful!", Toast.LENGTH_SHORT).show();

                        // Send email verification (optional but recommended)
                        sendEmailVerification();

                        // Navigate to appropriate activity based on role
                        navigateToDashboard(role);
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Failed to save user data";
                        Log.e(TAG, "Firestore save failed: " + errorMessage);
                        Toast.makeText(RegisterActivity.this,
                                "Failed to save user data: " + errorMessage,
                                Toast.LENGTH_LONG).show();

                        // Optional: Delete the Firebase Auth user if Firestore save fails
                        // to maintain data consistency
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser != null) {
                            currentUser.delete();
                        }
                    }
                });
    }

    private void sendEmailVerification() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this,
                                    "Verification email sent to " + user.getEmail(),
                                    Toast.LENGTH_LONG).show();
                        }
                        // Don't show error for email verification failure as registration is still successful
                    });
        }
    }

    private String getSelectedRole() {
        int selectedId = radioGroupRole.getCheckedRadioButtonId();

        if (selectedId == R.id.radioCitizen) {
            return "citizen";
        } else if (selectedId == R.id.radioVolunteer) {
            return "volunteer";
        } else if (selectedId == R.id.radioAdmin) {
            return "admin";
        } else {
            return "citizen"; // Default role
        }
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        switch (role) {
            case "volunteer":
                intent = new Intent(RegisterActivity.this, VolunteerDashboardActivity.class);
                break;
            case "admin":
                intent = new Intent(RegisterActivity.this, AdminDashboardActivity.class);
                break;
            case "citizen":
            default:
                intent = new Intent(RegisterActivity.this, CitizenDashboardActivity.class);
                break;
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already signed in, redirecting");
            // User is already signed in, redirect to appropriate dashboard
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");
                            navigateToDashboard(role != null ? role : "citizen");
                        } else {
                            navigateToDashboard("citizen");
                        }
                    })
                    .addOnFailureListener(e -> {
                        navigateToDashboard("citizen");
                    });
        }
    }
}
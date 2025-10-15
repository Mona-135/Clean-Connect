package com.example.crowdcleaning.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextPhone = findViewById(R.id.editTextPhone);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);
    }

    private void setupClickListeners() {
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        textViewLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

        // Show loading
        buttonRegister.setEnabled(false);
        buttonRegister.setText("Creating Account...");

        // Create user with Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registration success, save user data to Firestore
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user.getUid(), name, email, phone, role);
                        }
                    } else {
                        // Registration failed
                        buttonRegister.setEnabled(true);
                        buttonRegister.setText("REGISTER");
                        Toast.makeText(RegisterActivity.this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(RegisterActivity.this,
                                "Registration successful!", Toast.LENGTH_SHORT).show();

                        // Navigate to appropriate activity based on role
                        navigateToDashboard(role);
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                "Failed to save user data: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
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
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, redirect to appropriate dashboard
            // Fetch role from Firestore and navigate directly
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
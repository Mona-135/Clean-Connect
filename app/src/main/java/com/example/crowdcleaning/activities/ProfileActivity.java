package com.example.crowdcleaning.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.crowdcleaning.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private TextView textViewName, textViewEmail, textViewPhone, textViewRole;
    private Button buttonBack; // Changed from ImageButton to Button
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
        loadUserProfile();
    }

    private void initializeViews() {
        textViewName = findViewById(R.id.textViewName);
        textViewEmail = findViewById(R.id.textViewEmail);
        textViewPhone = findViewById(R.id.textViewPhone);
        textViewRole = findViewById(R.id.textViewRole);
        buttonBack = findViewById(R.id.buttonBack); // Now this matches the Button in XML
    }

    private void setupClickListeners() {
        buttonBack.setOnClickListener(v -> onBackPressed());
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Set email from Firebase Auth (always available)
            textViewEmail.setText(currentUser.getEmail());

            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String phone = documentSnapshot.getString("phone");
                            String role = documentSnapshot.getString("role");

                            textViewName.setText(name != null ? name : "Not set");
                            textViewPhone.setText(phone != null ? phone : "Not set");
                            textViewRole.setText(role != null ? role.toUpperCase() : "CITIZEN");
                        } else {
                            textViewName.setText("Not set");
                            textViewPhone.setText("Not set");
                            textViewRole.setText("CITIZEN");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish(); // Go back to previous activity
    }
}
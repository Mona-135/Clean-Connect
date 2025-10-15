package com.example.crowdcleaning.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.crowdcleaning.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private TextView textViewName, textViewEmail, textViewPhone, textViewRole;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        loadUserProfile();
    }

    private void initializeViews() {
        textViewName = findViewById(R.id.textViewName);
        textViewEmail = findViewById(R.id.textViewEmail);
        textViewPhone = findViewById(R.id.textViewPhone);
        textViewRole = findViewById(R.id.textViewRole);
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            String phone = documentSnapshot.getString("phone");
                            String role = documentSnapshot.getString("role");

                            textViewName.setText(name != null ? name : "Not set");
                            textViewEmail.setText(email != null ? email : currentUser.getEmail());
                            textViewPhone.setText(phone != null ? phone : "Not set");
                            textViewRole.setText(role != null ? role.toUpperCase() : "Not set");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
package com.example.crowdcleaning.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.crowdcleaning.R;
import com.example.crowdcleaning.utils.FirebaseUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddReportActivity extends AppCompatActivity {

    private static final String TAG = "AddReportActivity";

    // UI Elements
    private EditText editTextTitle, editTextDescription, editTextAddress;
    private Button buttonTakePhoto, buttonSelectPhoto, buttonSubmit, buttonGetLocation;
    private ImageView imageViewPreview;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;

    // Image
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int REQUEST_LOCATION_PERMISSION = 100;
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int REQUEST_STORAGE_PERMISSION = 102;

    private Uri currentImageUri;
    private byte[] imageData;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Progress Dialog
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_report);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check if user is logged in
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
        getCurrentLocation();
    }

    private void initializeViews() {
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextAddress = findViewById(R.id.editTextAddress);
        buttonTakePhoto = findViewById(R.id.buttonTakePhoto);
        buttonSelectPhoto = findViewById(R.id.buttonSelectPhoto);
        buttonSubmit = findViewById(R.id.buttonSubmit);
        buttonGetLocation = findViewById(R.id.buttonGetLocation);
        imageViewPreview = findViewById(R.id.imageViewPreview);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Submitting report...");
        progressDialog.setCancelable(false);
    }

    private void setupClickListeners() {
        buttonTakePhoto.setOnClickListener(v -> takePhoto());
        buttonSelectPhoto.setOnClickListener(v -> selectPhoto());
        buttonSubmit.setOnClickListener(v -> submitReport());
        buttonGetLocation.setOnClickListener(v -> getCurrentLocation());
    }

    private void takePhoto() {
        if (checkCameraPermission()) {
            dispatchTakePictureIntent();
        } else {
            requestCameraPermission();
        }
    }

    private void selectPhoto() {
        if (checkStoragePermission()) {
            dispatchSelectPictureIntent();
        } else {
            requestStoragePermission();
        }
    }

    private void dispatchTakePictureIntent() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = createImageFile();
                if (photoFile != null) {
                    currentImageUri = FileProvider.getUriForFile(this,
                            "com.example.crowdcleaning.fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentImageUri);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error taking photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error taking photo", e);
        }
    }

    private void dispatchSelectPictureIntent() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickPhotoIntent.setType("image/*");
        startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
    }

    private void getCurrentLocation() {
        if (checkLocationPermission()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            currentLatitude = location.getLatitude();
                            currentLongitude = location.getLongitude();
                            Toast.makeText(this,
                                    "Location captured: " + currentLatitude + ", " + currentLongitude,
                                    Toast.LENGTH_SHORT).show();

                            // Optionally update address field with reverse geocoding
                            if (editTextAddress.getText().toString().isEmpty()) {
                                editTextAddress.setText("Lat: " + currentLatitude + ", Lng: " + currentLongitude);
                            }
                        } else {
                            Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error getting location", e);
                    });
        } else {
            requestLocationPermission();
        }
    }

    private void submitReport() {
        String title = editTextTitle.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String address = editTextAddress.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(title)) {
            editTextTitle.setError("Title is required");
            editTextTitle.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            editTextDescription.setError("Description is required");
            editTextDescription.requestFocus();
            return;
        }

        if (currentLatitude == 0.0 || currentLongitude == 0.0) {
            Toast.makeText(this, "Please capture location first", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        if (imageData != null) {
            // Upload image first, then save report
            uploadImageAndSaveReport(title, description, address);
        } else {
            // Save report without image
            saveReportToFirestore(title, description, address, null);
        }
    }

    private void uploadImageAndSaveReport(String title, String description, String address) {
        FirebaseUtils.uploadImage(imageData, "report_images", new FirebaseUtils.UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                saveReportToFirestore(title, description, address, downloadUrl);
            }

            @Override
            public void onFailure(String error) {
                progressDialog.dismiss();
                Toast.makeText(AddReportActivity.this, "Image upload failed: " + error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Image upload failed: " + error);
            }
        });
    }

    private void saveReportToFirestore(String title, String description, String address, String imageUrl) {
        Map<String, Object> report = new HashMap<>();
        report.put("userId", currentUser.getUid());
        report.put("userEmail", currentUser.getEmail());
        report.put("userName", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : currentUser.getEmail().split("@")[0]);
        report.put("title", title);
        report.put("description", description);
        report.put("address", address);
        report.put("latitude", currentLatitude);
        report.put("longitude", currentLongitude);
        report.put("imageUrl", imageUrl);
        report.put("status", "pending");
        report.put("timestamp", new Date());
        report.put("createdAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

        db.collection("reports")
                .add(report)
                .addOnSuccessListener(documentReference -> {
                    progressDialog.dismiss();
                    Toast.makeText(AddReportActivity.this, "Report submitted successfully!", Toast.LENGTH_SHORT).show();

                    // Navigate back to citizen dashboard
                    Intent intent = new Intent(AddReportActivity.this, CitizenDashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(AddReportActivity.this, "Failed to submit report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error saving report", e);
                });
    }

    // Permission handling methods
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_LOCATION_PERMISSION);
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA_PERMISSION);
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case REQUEST_LOCATION_PERMISSION:
                    getCurrentLocation();
                    break;
                case REQUEST_CAMERA_PERMISSION:
                    dispatchTakePictureIntent();
                    break;
                case REQUEST_STORAGE_PERMISSION:
                    dispatchSelectPictureIntent();
                    break;
            }
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE:
                    if (currentImageUri != null) {
                        loadImageFromUri(currentImageUri);
                    }
                    break;

                case REQUEST_IMAGE_PICK:
                    if (data != null && data.getData() != null) {
                        currentImageUri = data.getData();
                        loadImageFromUri(currentImageUri);
                    }
                    break;
            }
        }
    }

    private void loadImageFromUri(Uri imageUri) {
        try {
            // Display image preview - using built-in drawables to avoid missing resource errors
            Picasso.get()
                    .load(imageUri)
                    .placeholder(android.R.drawable.ic_menu_gallery) // Use built-in gallery icon
                    .error(android.R.drawable.ic_menu_report_image) // Use built-in error icon
                    .into(imageViewPreview);

            // Convert URI to byte array for Firebase upload
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    byteBuffer.write(buffer, 0, len);
                }
                imageData = byteBuffer.toByteArray();
                inputStream.close();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error loading image", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
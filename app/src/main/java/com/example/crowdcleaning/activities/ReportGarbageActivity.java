package com.example.crowdcleaning.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.crowdcleaning.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportGarbageActivity extends AppCompatActivity {

    // UI Components
    private EditText editTextTitle, editTextDescription, editTextAddress, editTextLatitude, editTextLongitude;
    private Button buttonBack, buttonGetLocation, buttonUploadImage, buttonSubmitReport;
    private HorizontalScrollView horizontalScrollView;
    private LinearLayout imagesContainer;
    private TextView textViewImageCount;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    // Image Upload
    private static final int PICK_IMAGES_REQUEST = 200;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 300;
    private static final int MAX_IMAGES = 3;

    private List<Bitmap> selectedImageBitmaps;
    private List<String> selectedImageNames;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_garbage); // Use XML layout

        // Initialize image lists
        selectedImageBitmaps = new ArrayList<>();
        selectedImageNames = new ArrayList<>();

        initializeViews();
        setupClickListeners();
        initializeLocationClient();
        updateImageDisplay();
    }

    private void initializeViews() {
        // Initialize views from XML
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextAddress = findViewById(R.id.editTextAddress);
        editTextLatitude = findViewById(R.id.editTextLatitude);
        editTextLongitude = findViewById(R.id.editTextLongitude);
        buttonBack = findViewById(R.id.buttonBack);
        buttonGetLocation = findViewById(R.id.buttonGetLocation);
        buttonUploadImage = findViewById(R.id.buttonUploadImage);
        buttonSubmitReport = findViewById(R.id.buttonSubmitReport);
        horizontalScrollView = findViewById(R.id.horizontalScrollView);
        imagesContainer = findViewById(R.id.imagesContainer);
        textViewImageCount = findViewById(R.id.textViewImageCount);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
    }

    private void setupClickListeners() {
        // Back button - return to previous activity
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Get current location
        buttonGetLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentLocation();
            }
        });

        // Upload image
        buttonUploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImages();
            }
        });

        // Submit report
        buttonSubmitReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReport();
            }
        });
    }

    private void initializeLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void getCurrentLocation() {
        // Check location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchLocation();
        }
    }

    private void fetchLocation() {
        progressDialog.setMessage("Getting your location...");
        progressDialog.show();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            progressDialog.dismiss();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        progressDialog.dismiss();

                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            editTextLatitude.setText(String.format(Locale.getDefault(), "%.6f", latitude));
                            editTextLongitude.setText(String.format(Locale.getDefault(), "%.6f", longitude));

                            Toast.makeText(ReportGarbageActivity.this, "Location updated successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ReportGarbageActivity.this, "Unable to get location. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(ReportGarbageActivity.this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void selectImages() {
        // Check if we've reached the maximum number of images
        if (selectedImageBitmaps.size() >= MAX_IMAGES) {
            Toast.makeText(this, "Maximum " + MAX_IMAGES + " images allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check storage permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST_CODE);
        } else {
            openImagePicker();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Pictures"), PICK_IMAGES_REQUEST);
    }

    private void updateImageDisplay() {
        // Clear existing images
        imagesContainer.removeAllViews();

        // Display all selected images
        for (int i = 0; i < selectedImageBitmaps.size(); i++) {
            // Create image view for each bitmap
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(selectedImageBitmaps.get(i));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(180, 180);
            params.setMargins(8, 8, 8, 8);
            imageView.setLayoutParams(params);

            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundResource(R.drawable.image_border);
            imageView.setPadding(4, 4, 4, 4);

            // Add delete button functionality
            final int position = i;
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeImage(position);
                }
            });

            imagesContainer.addView(imageView);
        }

        // Update image count text
        if (selectedImageBitmaps.isEmpty()) {
            textViewImageCount.setText("No images selected");
            buttonUploadImage.setText("Select Photos");
            buttonUploadImage.setEnabled(true);
        } else {
            int remaining = MAX_IMAGES - selectedImageBitmaps.size();
            textViewImageCount.setText(selectedImageBitmaps.size() + " image(s) selected" +
                    (remaining > 0 ? " (" + remaining + " more allowed)" : " (Maximum reached)"));

            if (remaining > 0) {
                buttonUploadImage.setText("Add More Photos");
                buttonUploadImage.setEnabled(true);
            } else {
                buttonUploadImage.setText("Maximum Reached");
                buttonUploadImage.setEnabled(false);
            }
        }
    }

    private void removeImage(int position) {
        if (position >= 0 && position < selectedImageBitmaps.size()) {
            selectedImageBitmaps.remove(position);
            selectedImageNames.remove(position);
            updateImageDisplay();
            Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitReport() {
        // Get all input values
        String title = editTextTitle.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String address = editTextAddress.getText().toString().trim();
        String latitudeStr = editTextLatitude.getText().toString().trim();
        String longitudeStr = editTextLongitude.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(title)) {
            showError(editTextTitle, "Please enter a location title");
            return;
        }

        if (TextUtils.isEmpty(description)) {
            showError(editTextDescription, "Please enter a description");
            return;
        }

        if (TextUtils.isEmpty(address)) {
            showError(editTextAddress, "Please enter an address");
            return;
        }

        if (TextUtils.isEmpty(latitudeStr) || TextUtils.isEmpty(longitudeStr)) {
            Toast.makeText(this, "Please get your location coordinates", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageBitmaps.isEmpty()) {
            Toast.makeText(this, "Please upload at least one image of the garbage spot", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse coordinates
        try {
            double latitude = Double.parseDouble(latitudeStr);
            double longitude = Double.parseDouble(longitudeStr);

            // Validate coordinate ranges
            if (latitude < -90 || latitude > 90) {
                showError(editTextLatitude, "Invalid latitude (-90 to 90)");
                return;
            }

            if (longitude < -180 || longitude > 180) {
                showError(editTextLongitude, "Invalid longitude (-180 to 180)");
                return;
            }

            // All validations passed - submit report
            processReportSubmission(title, description, address, latitude, longitude);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid coordinate format", Toast.LENGTH_SHORT).show();
        }
    }

    private void showError(EditText editText, String message) {
        editText.setError(message);
        editText.requestFocus();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void processReportSubmission(String title, String description, String address,
                                         double latitude, double longitude) {
        progressDialog.setMessage("Submitting your report with " + selectedImageBitmaps.size() + " images...");
        progressDialog.show();

        // Simulate API call or database insertion
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        progressDialog.dismiss();

                        // Create report object with multiple images
                        GarbageReport report = new GarbageReport(
                                title,
                                description,
                                address,
                                latitude,
                                longitude,
                                selectedImageBitmaps,
                                selectedImageNames
                        );

                        // Show success message
                        showSubmissionSuccess(report);
                    }
                },
                2000); // 2 second delay to simulate network call
    }

    private void showSubmissionSuccess(GarbageReport report) {
        String successMessage = String.format(
                "Report Submitted Successfully!\n\n" +
                        "Title: %s\n" +
                        "Location: %s\n" +
                        "Coordinates: %.6f, %.6f\n" +
                        "Images: %d\n" +
                        "Thank you for helping keep our environment clean!",
                report.getTitle(),
                report.getAddress(),
                report.getLatitude(),
                report.getLongitude(),
                report.getImages().size()
        );

        Toast.makeText(this, "Report submitted successfully with " + report.getImages().size() + " images!", Toast.LENGTH_LONG).show();

        // Clear form and go back
        clearForm();
        finish();
    }

    private void clearForm() {
        editTextTitle.setText("");
        editTextDescription.setText("");
        editTextAddress.setText("");
        editTextLatitude.setText("");
        editTextLongitude.setText("");
        selectedImageBitmaps.clear();
        selectedImageNames.clear();
        updateImageDisplay();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                // Multiple images selected
                int count = data.getClipData().getItemCount();
                int totalToAdd = Math.min(count, MAX_IMAGES - selectedImageBitmaps.size());

                for (int i = 0; i < totalToAdd; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    processSelectedImage(imageUri);
                }

                if (count > totalToAdd) {
                    Toast.makeText(this, "Only " + totalToAdd + " images added (max " + MAX_IMAGES + " allowed)", Toast.LENGTH_SHORT).show();
                }
            } else if (data.getData() != null) {
                // Single image selected
                if (selectedImageBitmaps.size() < MAX_IMAGES) {
                    Uri imageUri = data.getData();
                    processSelectedImage(imageUri);
                } else {
                    Toast.makeText(this, "Maximum " + MAX_IMAGES + " images allowed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void processSelectedImage(Uri imageUri) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);

            if (bitmap != null) {
                selectedImageBitmaps.add(bitmap);
                selectedImageNames.add(getImageName(imageUri));
                updateImageDisplay();
                Toast.makeText(this, "Image added", Toast.LENGTH_SHORT).show();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }

    private String getImageName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try {
                android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result != null ? result : "image_" + System.currentTimeMillis() + ".jpg";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                Toast.makeText(this, "Location permission is required to get your current location", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Storage permission is required to select images", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Updated Garbage Report Model Class for multiple images
    public static class GarbageReport {
        private String title;
        private String description;
        private String address;
        private double latitude;
        private double longitude;
        private List<Bitmap> images;
        private List<String> imageNames;
        private String timestamp;

        public GarbageReport(String title, String description, String address,
                             double latitude, double longitude, List<Bitmap> images, List<String> imageNames) {
            this.title = title;
            this.description = description;
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
            this.images = new ArrayList<>(images);
            this.imageNames = new ArrayList<>(imageNames);
            this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        }

        // Getters
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getAddress() { return address; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public List<Bitmap> getImages() { return images; }
        public List<String> getImageNames() { return imageNames; }
        public String getTimestamp() { return timestamp; }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
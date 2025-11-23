package com.example.crowdcleaning.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.crowdcleaning.R;
import com.example.crowdcleaning.utils.FirebaseUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private static final int MAX_IMAGE_SIZE = 1024; // Max dimension for resizing

    private List<Bitmap> selectedImageBitmaps;
    private List<String> selectedImageNames;
    private ProgressDialog progressDialog;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Add TAG for logging
    private static final String TAG = "ReportGarbageActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_garbage);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize image lists
        selectedImageBitmaps = new ArrayList<>();
        selectedImageNames = new ArrayList<>();

        initializeViews();
        setupClickListeners();
        initializeLocationClient();
        updateImageDisplay();
    }

    private void initializeViews() {
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
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        buttonGetLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentLocation();
            }
        });

        buttonUploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImages();
            }
        });

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

        // Use getCurrentLocation for more accurate location
        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        progressDialog.dismiss();

                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            editTextLatitude.setText(String.format(Locale.getDefault(), "%.6f", latitude));
                            editTextLongitude.setText(String.format(Locale.getDefault(), "%.6f", longitude));

                            // Get address from coordinates (you can implement this using Geocoder)
                            getAddressFromLocation(latitude, longitude);

                            Toast.makeText(ReportGarbageActivity.this, "Location updated successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            // Fallback to last location
                            getLastLocation();
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        // Fallback to last location
                        getLastLocation();
                    }
                });
    }

    private void getLastLocation() {
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

                            getAddressFromLocation(latitude, longitude);
                            Toast.makeText(ReportGarbageActivity.this, "Location updated successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ReportGarbageActivity.this, "Unable to get location. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(ReportGarbageActivity.this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        // Simple implementation - you can use Geocoder for actual address reverse geocoding
        String address = String.format(Locale.getDefault(), "Lat: %.6f, Long: %.6f", latitude, longitude);
        editTextAddress.setText(address);

        // For actual address, you would use:
        // Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        // List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
        // if (addresses != null && !addresses.isEmpty()) {
        //     String fullAddress = addresses.get(0).getAddressLine(0);
        //     editTextAddress.setText(fullAddress);
        // }
    }

    // NEW METHOD: Check and request permissions based on Android version
    private boolean checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        STORAGE_PERMISSION_REQUEST_CODE);
                return false;
            }
        } else {
            // Android 12 and below uses READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    // UPDATED METHOD: Uses the new permission checking
    private void selectImages() {
        if (selectedImageBitmaps.size() >= MAX_IMAGES) {
            Toast.makeText(this, "Maximum " + MAX_IMAGES + " images allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        if (checkAndRequestPermissions()) {
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
        imagesContainer.removeAllViews();

        for (int i = 0; i < selectedImageBitmaps.size(); i++) {
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(selectedImageBitmaps.get(i));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(180, 180);
            params.setMargins(8, 8, 8, 8);
            imageView.setLayoutParams(params);

            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundResource(android.R.drawable.btn_default);
            imageView.setPadding(4, 4, 4, 4);

            final int position = i;
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeImage(position);
                }
            });

            imagesContainer.addView(imageView);
        }

        updateImageCountText();
    }

    private void updateImageCountText() {
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
            // Recycle bitmap to free memory
            Bitmap bitmap = selectedImageBitmaps.get(position);
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }

            selectedImageBitmaps.remove(position);
            selectedImageNames.remove(position);
            updateImageDisplay();
            Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitReport() {
        String title = editTextTitle.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String address = editTextAddress.getText().toString().trim();
        String latitudeStr = editTextLatitude.getText().toString().trim();
        String longitudeStr = editTextLongitude.getText().toString().trim();

        // Validation
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

        if (currentUser == null) {
            Toast.makeText(this, "Please login to submit report", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double latitude = Double.parseDouble(latitudeStr);
            double longitude = Double.parseDouble(longitudeStr);

            if (latitude < -90 || latitude > 90) {
                showError(editTextLatitude, "Invalid latitude (-90 to 90)");
                return;
            }

            if (longitude < -180 || longitude > 180) {
                showError(editTextLongitude, "Invalid longitude (-180 to 180)");
                return;
            }

            // All validations passed - submit report to Firebase
            uploadImagesAndSubmitReport(title, description, address, latitude, longitude);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid coordinate format", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImagesAndSubmitReport(String title, String description, String address,
                                             double latitude, double longitude) {
        progressDialog.setMessage("Uploading images...");
        progressDialog.show();

        List<String> imageUrls = new ArrayList<>();
        final int totalImages = selectedImageBitmaps.size();
        final int[] uploadedCount = {0};

        for (int i = 0; i < selectedImageBitmaps.size(); i++) {
            Bitmap bitmap = selectedImageBitmaps.get(i);

            // Compress image
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageData = baos.toByteArray();

            // Upload to Firebase Storage
            UploadTask uploadTask = FirebaseUtils.uploadImage(imageData, "report_images");

            final int currentIndex = i;
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // Get download URL
                    taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String imageUrl = uri.toString();
                            imageUrls.add(imageUrl);

                            uploadedCount[0]++;

                            // Update progress message
                            progressDialog.setMessage("Uploading images... (" + uploadedCount[0] + "/" + totalImages + ")");

                            if (uploadedCount[0] == totalImages) {
                                // All images uploaded, now save report to Firestore
                                saveReportToFirestore(title, description, address, latitude, longitude, imageUrls);
                            }
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(ReportGarbageActivity.this,
                            "Failed to upload image " + (currentIndex + 1) + ": " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void saveReportToFirestore(String title, String description, String address,
                                       double latitude, double longitude, List<String> imageUrls) {
        progressDialog.setMessage("Saving report...");

        // Create report data with proper user identification
        Map<String, Object> report = new HashMap<>();
        report.put("title", title);
        report.put("description", description);
        report.put("address", address);
        report.put("latitude", latitude);
        report.put("longitude", longitude);
        report.put("imageUrls", imageUrls);
        report.put("userId", currentUser.getUid());
        report.put("userEmail", currentUser.getEmail());
        report.put("userName", currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : "Anonymous User");
        report.put("status", "reported");
        report.put("timestamp", new Date());
        report.put("upvotes", 0);
        report.put("volunteerAssigned", ""); // Ensure this is empty string, not null
        report.put("volunteerName", ""); // Add this field
        report.put("category", "garbage");

        // Add debug logging
        Log.d(TAG, "Saving report with data: " + report.toString());

        // Save to Firestore
        db.collection("reports")
                .add(report)
                .addOnSuccessListener(documentReference -> {
                    progressDialog.dismiss();
                    String reportId = documentReference.getId();

                    Log.d(TAG, "Report submitted successfully! ID: " + reportId);
                    Toast.makeText(ReportGarbageActivity.this,
                            "Report submitted successfully!",
                            Toast.LENGTH_LONG).show();
                    clearForm();

                    // Navigate back to main dashboard
                    Intent intent = new Intent(ReportGarbageActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error saving report: " + e.getMessage());
                    Toast.makeText(ReportGarbageActivity.this,
                            "Failed to submit report: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showError(EditText editText, String message) {
        editText.setError(message);
        editText.requestFocus();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void clearForm() {
        editTextTitle.setText("");
        editTextDescription.setText("");
        editTextAddress.setText("");
        editTextLatitude.setText("");
        editTextLongitude.setText("");

        // Clear and recycle bitmaps
        for (Bitmap bitmap : selectedImageBitmaps) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        selectedImageBitmaps.clear();
        selectedImageNames.clear();
        System.gc();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
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
                // Resize bitmap to reduce memory usage
                Bitmap resizedBitmap = resizeBitmap(bitmap, MAX_IMAGE_SIZE);
                selectedImageBitmaps.add(resizedBitmap);
                selectedImageNames.add(getImageName(imageUri));
                updateImageDisplay();

                // Recycle original bitmap to save memory
                if (bitmap != resizedBitmap) {
                    bitmap.recycle();
                }

                Toast.makeText(this, "Image added", Toast.LENGTH_SHORT).show();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float ratio = (float) width / height;
        int newWidth, newHeight;

        if (ratio > 1) {
            newWidth = maxSize;
            newHeight = (int) (maxSize / ratio);
        } else {
            newHeight = maxSize;
            newWidth = (int) (maxSize * ratio);
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up bitmaps to prevent memory leaks
        for (Bitmap bitmap : selectedImageBitmaps) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        selectedImageBitmaps.clear();

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
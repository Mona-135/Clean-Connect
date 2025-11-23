package com.example.crowdcleaning.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.crowdcleaning.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class CitizenDashboardActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TextView textWelcome;
    private Button buttonAddReport, buttonLogout, buttonRefresh; // Removed buttonMyReports

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String TAG = "CitizenDashboard";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_dashboard);

        Log.d(TAG, "CitizenDashboardActivity created");

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check if user is logged in
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
        setupMap();
        loadUserReportsCount();
    }

    private void initializeViews() {
        textWelcome = findViewById(R.id.textWelcome);
        buttonAddReport = findViewById(R.id.buttonAddReport);
        buttonLogout = findViewById(R.id.buttonLogout);
        buttonRefresh = findViewById(R.id.buttonRefresh);
        // Removed: buttonMyReports = findViewById(R.id.buttonMyReports);

        // Set welcome message
        if (currentUser != null && currentUser.getEmail() != null) {
            String email = currentUser.getEmail();
            String username = email.split("@")[0];
            textWelcome.setText("Welcome, " + username + "!");
        }

        Log.d(TAG, "Views initialized");
    }

    private void setupClickListeners() {
        buttonAddReport.setOnClickListener(v -> {
            Log.d(TAG, "Add Report button clicked");
            Intent intent = new Intent(CitizenDashboardActivity.this, AddReportActivity.class);
            startActivity(intent);
        });

        // Removed the buttonMyReports click listener entirely

        buttonRefresh.setOnClickListener(v -> {
            Log.d(TAG, "Refresh button clicked");
            refreshData();
        });

        buttonLogout.setOnClickListener(v -> logoutUser());
    }

    private void setupMap() {
        Log.d(TAG, "Setting up map");

        // Create map fragment
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();

        // Add fragment to activity
        getSupportFragmentManager().beginTransaction()
                .add(R.id.mapContainer, mapFragment)
                .commit();

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "GoogleMap is ready");

        // Configure map settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        // Check location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            // Request location permission
            Log.d(TAG, "Requesting location permission");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Load reports on map
        loadReportsOnMap();
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && mMap != null) {
            try {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                Log.d(TAG, "Location enabled on map");
            } catch (SecurityException e) {
                Log.e(TAG, "Location permission error", e);
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadUserReportsCount() {
        if (currentUser == null) {
            Log.w(TAG, "User not logged in, cannot load reports count");
            return;
        }

        Log.d(TAG, "Loading user reports count for user: " + currentUser.getUid());

        db.collection("reports")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int reportCount = task.getResult().size();
                        Log.d(TAG, "User has " + reportCount + " reports");

                        if (reportCount == 0) {
                            Toast.makeText(CitizenDashboardActivity.this,
                                    "No reports found. Add your first report!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(CitizenDashboardActivity.this,
                                    "You have " + reportCount + " reports", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Error loading reports count", task.getException());
                        Toast.makeText(CitizenDashboardActivity.this,
                                "Error loading reports count", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadReportsOnMap() {
        if (currentUser == null) {
            Log.w(TAG, "User not logged in, cannot load reports");
            return;
        }

        if (mMap == null) {
            Log.w(TAG, "Map not ready, cannot load reports");
            return;
        }

        Log.d(TAG, "Loading reports on map for user: " + currentUser.getUid());

        db.collection("reports")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mMap != null) {
                        mMap.clear();
                        Log.d(TAG, "Cleared existing markers");

                        boolean hasValidReports = false;
                        LatLng firstReportLocation = null;
                        int validReportsCount = 0;
                        int geocodedReportsCount = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                String documentId = document.getId();
                                Double latitude = document.getDouble("latitude");
                                Double longitude = document.getDouble("longitude");
                                String title = document.getString("title");
                                String description = document.getString("description");
                                String status = document.getString("status");
                                String address = document.getString("address");

                                Log.d(TAG, "Processing report: " + title +
                                        " - Coords: " + latitude + ", " + longitude +
                                        " - Address: " + address);

                                // Check if we have valid coordinates
                                if (isValidCoordinate(latitude, longitude)) {
                                    LatLng reportLocation = new LatLng(latitude, longitude);

                                    // Store the first valid report location to center the map
                                    if (firstReportLocation == null) {
                                        firstReportLocation = reportLocation;
                                    }

                                    // Create marker snippet with status and address
                                    String snippet = "Status: " + (status != null ? status : "Unknown");
                                    if (address != null && !address.isEmpty()) {
                                        snippet += "\nAddress: " + address;
                                    }
                                    if (description != null && !description.isEmpty()) {
                                        snippet += "\n" + description;
                                    }

                                    mMap.addMarker(new MarkerOptions()
                                            .position(reportLocation)
                                            .title(title != null ? title : "Garbage Report")
                                            .snippet(snippet));

                                    hasValidReports = true;
                                    validReportsCount++;

                                    Log.d(TAG, "✓ Added marker using coordinates for: " + title);

                                } else if (address != null && !address.isEmpty()) {
                                    // If coordinates are invalid but we have an address, try geocoding
                                    Log.d(TAG, "Coordinates invalid, attempting to geocode address: " + address);
                                    geocodeAndAddMarker(address, title, description, status, documentId);
                                    geocodedReportsCount++;
                                } else {
                                    Log.w(TAG, "Report has no valid coordinates or address: " + title);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing report document", e);
                            }
                        }

                        Log.d(TAG, "Map loading complete - Valid reports: " + validReportsCount +
                                ", Geocoded reports: " + geocodedReportsCount);

                        // Center the map on the report locations
                        if (hasValidReports && firstReportLocation != null) {
                            // Center on the first report with zoom level 14
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstReportLocation, 14f));
                            Log.d(TAG, "Map centered on report location: " + firstReportLocation);
                        } else {
                            // If no reports with valid locations, get current location as fallback
                            Log.d(TAG, "No valid report locations, using fallback");
                            getCurrentLocationAsFallback();
                        }
                    } else {
                        Log.e(TAG, "Failed to load reports: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                        getCurrentLocationAsFallback();
                    }
                });
    }

    private boolean isValidCoordinate(Double latitude, Double longitude) {
        boolean isValid = latitude != null && longitude != null &&
                latitude != 0.0 && longitude != 0.0 &&
                latitude >= -90 && latitude <= 90 &&
                longitude >= -180 && longitude <= 180;

        Log.d(TAG, "Coordinate validation: " + latitude + ", " + longitude + " -> " + isValid);
        return isValid;
    }

    private void geocodeAndAddMarker(String address, String title, String description, String status, String documentId) {
        if (TextUtils.isEmpty(address)) {
            Log.w(TAG, "Cannot geocode empty address");
            return;
        }

        Log.d(TAG, "Geocoding address: " + address);

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocationName(address, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address location = addresses.get(0);
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    Log.d(TAG, "Geocoding successful: " + address + " -> " + latitude + ", " + longitude);

                    runOnUiThread(() -> {
                        if (mMap != null) {
                            LatLng reportLocation = new LatLng(latitude, longitude);
                            String snippet = "Status: " + (status != null ? status : "Unknown") +
                                    "\nAddress: " + address;

                            if (description != null && !description.isEmpty()) {
                                snippet += "\n" + description;
                            }

                            mMap.addMarker(new MarkerOptions()
                                    .position(reportLocation)
                                    .title(title != null ? title : "Garbage Report")
                                    .snippet(snippet));

                            // Update the report in Firestore with the geocoded coordinates
                            updateReportWithCoordinates(documentId, latitude, longitude);

                            Log.d(TAG, "✓ Added marker using geocoding for: " + title);
                        }
                    });
                } else {
                    Log.w(TAG, "Geocoding failed - no results for address: " + address);
                    runOnUiThread(() ->
                            Toast.makeText(this, "Could not find location for: " + address, Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoding error for address: " + address, e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Error finding location for address", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void updateReportWithCoordinates(String documentId, double latitude, double longitude) {
        if (documentId == null) {
            Log.w(TAG, "Cannot update report - documentId is null");
            return;
        }

        Log.d(TAG, "Updating report " + documentId + " with coordinates: " + latitude + ", " + longitude);

        db.collection("reports").document(documentId)
                .update("latitude", latitude, "longitude", longitude)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "✓ Successfully updated report coordinates"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to update report coordinates", e));
    }

    private void getCurrentLocationAsFallback() {
        Log.d(TAG, "Getting current location as fallback");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null && mMap != null) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f));

                            // Add a marker for current location
                            mMap.addMarker(new MarkerOptions()
                                    .position(currentLatLng)
                                    .title("Your Current Location")
                                    .snippet("No reports yet. Add your first report!"));

                            Log.d(TAG, "Map centered on current location: " + currentLatLng);
                        } else {
                            // Use default location if current location not available
                            useDefaultLocation();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get current location", e);
                        useDefaultLocation();
                    });
        } else {
            // If no permission, use default location
            useDefaultLocation();
        }
    }

    private void useDefaultLocation() {
        if (mMap != null) {
            LatLng defaultLocation = new LatLng(40.7128, -74.0060); // New York
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f));
            mMap.addMarker(new MarkerOptions()
                    .position(defaultLocation)
                    .title("Default Location")
                    .snippet("Enable location or add reports"));

            Log.d(TAG, "Using default location: " + defaultLocation);
        }
    }

    private void refreshData() {
        Log.d(TAG, "Refreshing all data");
        loadUserReportsCount();
        if (mMap != null) {
            loadReportsOnMap();
        } else {
            Log.w(TAG, "Map not ready for refresh");
        }
        Toast.makeText(this, "Refreshing data...", Toast.LENGTH_SHORT).show();
    }

    private void logoutUser() {
        Log.d(TAG, "Logging out user");
        auth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(CitizenDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted");
                if (mMap != null) {
                    enableMyLocation();
                    // Reload to show reports with proper location
                    loadReportsOnMap();
                }
            } else {
                Log.w(TAG, "Location permission denied");
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                // Still load reports, but they'll use the coordinates from Firestore
                loadReportsOnMap();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed");
        loadUserReportsCount();
        if (mMap != null) {
            loadReportsOnMap();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity destroyed");
    }
}
package com.example.airquality2;

// Import statements for Android UI components and utilities
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fragment that displays detailed air quality information for a selected city
 * Handles fetching, displaying, and saving air quality data
 */
public class DetailsFragment extends Fragment {
    // Tag for logging - helps identify log messages from this class
    private static final String TAG = "DetailsFragment";

    // UI components - declared as instance variables for access across methods
    private TextView textDetails;        // Displays the main air quality data
    private TextView textCityInfo;       // Shows city location information
    private TextView textAqiCategory;    // Shows AQI category (Good, Moderate, etc.)
    private View aqiColor;              // Color indicator for air quality level
    private Button btnSave;             // Button to save data to database
    private Button btnBack;             // Button to return to search screen

    // Data variables
    private String cityName;            // Name of the city being searched
    private String resultText = "";     // Stores the air quality data text
    private String cityInfoText = "";   // Stores city information text
    private boolean hasValidData = false; // Flag to track if we have valid air quality data

    /**
     * Called when the fragment view is being created
     * Inflates the layout and sets up initial data fetching
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the fragment layout from XML
        View view = inflater.inflate(R.layout.fragment_details, container, false);

        // Find the details text view in the inflated layout
        TextView textViewDetails = view.findViewById(R.id.textDetails);

        // Check if arguments were passed to this fragment
        if (getArguments() != null) {
            // Extract the city name from the arguments bundle
            cityName = getArguments().getString("CITY");

            // Set loading message while data is being fetched
            if (textViewDetails != null) {
                textViewDetails.setText("Loading air quality...");
            }

            // Start fetching air quality data for the city
            fetchAirQualityData();
        }

        // Return the inflated view
        return view;
    }

    /**
     * Called after the view has been created
     * Initialize UI components and set up event listeners
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize all UI components by finding them in the view
        textDetails = view.findViewById(R.id.textDetails);
        textCityInfo = view.findViewById(R.id.textCityInfo);
        aqiColor = view.findViewById(R.id.aqiColorIndicator);
        textAqiCategory = view.findViewById(R.id.textAqiCategory);
        btnSave = view.findViewById(R.id.btnSave);
        btnBack = view.findViewById(R.id.btnBack);

        // Set up click listeners for buttons
        setupButtonListeners();
    }

    /**
     * Fetches air quality data for the specified city using AirQualityFetcher
     * Handles both success and error responses
     */
    private void fetchAirQualityData() {
        // Call the AirQualityFetcher with city name and callback interface
        AirQualityFetcher.fetchData(cityName, new AirQualityFetcher.AirQualityCallback() {

            /**
             * Called when data is successfully fetched
             * @param result The air quality data string
             * @param cityInfo The city information string
             */
            @Override
            public void onSuccess(String result, String cityInfo) {
                // Store the received data in instance variables
                resultText = result;
                cityInfoText = cityInfo;
                hasValidData = true; // Mark data as valid

                // Log the received data for debugging
                Log.d(TAG, "Air quality data received: " + result);
                Log.d(TAG, "City info received: " + cityInfo);

                // Update the UI with the new data
                updateUI();
            }

            /**
             * Called when there's an error fetching data
             * @param error The error message
             */
            @Override
            public void onError(String error) {
                // Store error information
                resultText = "Error: " + error;
                cityInfoText = "Location: Information not available";
                hasValidData = false; // Mark data as invalid

                // Log the error for debugging
                Log.e(TAG, "Error fetching air quality data: " + error);

                // Update UI to show error
                updateUI();
            }
        });
    }

    /**
     * Updates the UI components with the current data
     * Must be called on the UI thread
     */
    private void updateUI() {
        // Check if the fragment is still attached to an activity
        if (getActivity() != null) {
            // Run on UI thread to safely update UI components
            getActivity().runOnUiThread(() -> {
                // Update the main details text
                if (textDetails != null) {
                    textDetails.setText(resultText);

                    // Update city information display
                    if (textCityInfo != null) {
                        textCityInfo.setText(cityInfoText);
                    }

                    // Extract AQI value from the result text
                    int aqi = extractAQI(resultText);

                    // Update the color indicator based on AQI
                    if (aqiColor != null) {
                        aqiColor.setBackgroundColor(getColorForAQI(aqi));
                    }

                    // Update the AQI category text and color
                    if (textAqiCategory != null) {
                        String categoryText = getAQICategoryText(aqi);
                        textAqiCategory.setText(categoryText);
                        textAqiCategory.setTextColor(getColorForAQI(aqi));
                    }

                    // Update the save button state based on data validity
                    updateSaveButtonState();
                }
            });
        }
    }

    /**
     * Updates the save button's enabled state and text based on data validity
     */
    private void updateSaveButtonState() {
        if (btnSave != null) {
            // Enable save button only if we have valid data
            if (hasValidData && isValidAirQualityData(resultText)) {
                btnSave.setEnabled(true);
                btnSave.setText("Save City");
            } else {
                // Disable save button if data is invalid
                btnSave.setEnabled(false);
                btnSave.setText("No Data to Save");
            }
        }
    }

    /**
     * Validates if the provided data string contains valid air quality information
     * @param data The data string to validate
     * @return true if data is valid, false otherwise
     */
    private boolean isValidAirQualityData(String data) {
        // Check for null or empty data
        if (data == null || data.trim().isEmpty()) {
            return false;
        }

        // Convert to lowercase for case-insensitive checking
        String lowerData = data.toLowerCase();

        // Check if the data contains error indicators
        if (lowerData.contains("error") ||
                lowerData.contains("exception") ||
                lowerData.contains("not found") ||
                lowerData.contains("invalid") ||
                lowerData.contains("failed")) {
            return false;
        }

        // Check if data contains AQI information (basic validation)
        return data.contains("AQI:") || extractAQI(data) != -1;
    }

    /**
     * Sets up click listeners for the Save and Back buttons
     */
    private void setupButtonListeners() {
        // Set up Save button click listener
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                Log.d(TAG, "Save button clicked");

                try {
                    // Combine city info and air quality data for storage
                    String combinedData = cityInfoText + "\n\n" + resultText;

                    // Create database helper instance
                    AirQualityDatabaseHelper db = new AirQualityDatabaseHelper(requireContext());

                    // Attempt to save the data
                    boolean success = db.saveResult(cityName, combinedData);

                    if (success) {
                        // Show success message
                        Toast.makeText(getContext(), "Data saved successfully!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Data saved successfully for city: " + cityName);
                    } else {
                        // Show failure message
                        Toast.makeText(getContext(), "Failed to save - city may not exist or data is invalid", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Failed to save data to database");
                    }

                } catch (Exception e) {
                    // Handle any exceptions during save operation
                    Toast.makeText(getContext(), "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Exception while saving data", e);
                }
            });
        } else {
            // Log error if save button is not found in layout
            Log.e(TAG, "Save button is null - check your layout file");
        }

        // Set up Back button click listener
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Log.d(TAG, "Back button clicked");
                // Finish the current activity to return to the previous screen
                requireActivity().finish();
            });
        } else {
            // Log error if back button is not found in layout
            Log.e(TAG, "Back button is null - check your layout file");
        }
    }

    /**
     * Extracts the AQI (Air Quality Index) number from the result text
     * @param text The text containing air quality data
     * @return The AQI value, or -1 if not found or invalid
     */
    private int extractAQI(String text) {
        // Check for null or empty input
        if (text == null || text.isEmpty()) {
            return -1; // Invalid AQI
        }

        // First attempt: Look for "AQI:" pattern followed by a number
        Pattern pattern = Pattern.compile("AQI[:\\s]*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        // If first pattern found, return the AQI value
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // Continue to alternative pattern if parsing fails
            }
        }

        // Alternative approach: Look for any 1-3 digit number that could be AQI
        pattern = Pattern.compile("\\b(\\d{1,3})\\b");
        matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                int value = Integer.parseInt(matcher.group(1));
                // Validate that the number is within typical AQI range (0-500)
                if (value >= 0 && value <= 500) {
                    return value;
                }
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        // Return -1 if no valid AQI value could be extracted
        return -1;
    }

    /**
     * Returns the appropriate color for the given AQI value
     * Based on standard AQI color coding system
     * @param aqi The AQI value
     * @return Color integer for the AQI level
     */
    private int getColorForAQI(int aqi) {
        if (aqi < 0) {
            return Color.GRAY; // Unknown/Invalid AQI
        } else if (aqi <= 50) {
            return Color.parseColor("#00E400"); // Good (Bright Green)
        } else if (aqi <= 100) {
            return Color.parseColor("#FFFF00"); // Moderate (Yellow)
        } else if (aqi <= 150) {
            return Color.parseColor("#FF8C00"); // Unhealthy for Sensitive Groups (Orange)
        } else if (aqi <= 200) {
            return Color.parseColor("#FF0000"); // Unhealthy (Red)
        } else if (aqi <= 300) {
            return Color.parseColor("#8B008B"); // Very Unhealthy (Purple)
        } else {
            return Color.parseColor("#800000"); // Hazardous (Maroon)
        }
    }

    /**
     * Returns the descriptive text for the given AQI value
     * @param aqi The AQI value
     * @return String describing the air quality category
     */
    private String getAQICategoryText(int aqi) {
        if (aqi < 0) {
            return "Unknown";
        } else if (aqi <= 50) {
            return "Good (AQI: " + aqi + ")";
        } else if (aqi <= 100) {
            return "Moderate (AQI: " + aqi + ")";
        } else if (aqi <= 150) {
            return "Unhealthy for Sensitive Groups (AQI: " + aqi + ")";
        } else if (aqi <= 200) {
            return "Unhealthy (AQI: " + aqi + ")";
        } else if (aqi <= 300) {
            return "Very Unhealthy (AQI: " + aqi + ")";
        } else {
            return "Hazardous (AQI: " + aqi + ")";
        }
    }
}
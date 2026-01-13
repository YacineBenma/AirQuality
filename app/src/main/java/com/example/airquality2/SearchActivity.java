package com.example.airquality2;

// Import statements for Android components and database operations
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Main search activity that allows users to:
 * 1. Search for air quality data by city name
 * 2. View and manage previously saved cities
 * 3. Delete saved cities with confirmation
 */
public class SearchActivity extends AppCompatActivity {

    // UI component declarations
    EditText editTextCity;          // Input field for city name
    Button buttonSearch;            // Button to trigger search
    LinearLayout savedCitiesContainer; // Container for saved city buttons
    TextView savedCitiesTitle;      // Title text for saved cities section

    /**
     * Called when the activity is being created
     * Initializes UI components and sets up event listeners
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Call parent class onCreate method
        super.onCreate(savedInstanceState);

        // Set the layout for this activity
        setContentView(R.layout.activity_search);

        // Initialize UI components by finding them in the layout
        editTextCity = findViewById(R.id.editTextCity);
        buttonSearch = findViewById(R.id.buttonSearch);
        savedCitiesContainer = findViewById(R.id.savedCitiesContainer);
        savedCitiesTitle = findViewById(R.id.savedCitiesTitle);

        // Set up click listener for the search button
        buttonSearch.setOnClickListener(view -> {
            // Get the text from input field and remove leading/trailing spaces
            String cityName = editTextCity.getText().toString().trim();

            // Check if city name is not empty
            if (!cityName.isEmpty()) {
                // Create intent to start ResultActivity with city name
                Intent intent = new Intent(SearchActivity.this, ResultActivity.class);
                intent.putExtra("CITY_NAME", cityName);
                startActivity(intent);
            } else {
                // Show error message if no city name entered
                editTextCity.setError("Please enter a city");
            }
        });

        // Load and display saved cities when activity starts
        loadSavedCities();
    }

    /**
     * Called when the activity becomes visible to the user
     * Refreshes the saved cities list in case it was modified
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh saved cities list when returning to this activity
        loadSavedCities();
    }

    /**
     * Loads saved cities from database and creates UI elements for them
     * Each city gets a button to search and a delete button
     */
    private void loadSavedCities() {
        // Create database helper instance
        AirQualityDatabaseHelper dbHelper = new AirQualityDatabaseHelper(this);

        // Get list of saved cities from database
        List<String> savedCities = getSavedCities(dbHelper);

        // Clear any existing city buttons from the container
        savedCitiesContainer.removeAllViews();

        // Check if there are any saved cities
        if (savedCities.isEmpty()) {
            // Hide the title if no saved cities
            savedCitiesTitle.setVisibility(View.GONE);
        } else {
            // Show the title if there are saved cities
            savedCitiesTitle.setVisibility(View.VISIBLE);

            // Create UI elements for each saved city
            for (String city : savedCities) {
                // Create horizontal layout to hold city button and delete button
                LinearLayout cityLayout = new LinearLayout(this);
                cityLayout.setOrientation(LinearLayout.HORIZONTAL);
                cityLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

                // Create main city button (takes most of the horizontal space)
                Button cityButton = new Button(this);
                cityButton.setText(city);
                // Set layout parameters with weight to take most of the space
                LinearLayout.LayoutParams cityButtonParams = new LinearLayout.LayoutParams(
                        0,                                    // Width: 0 (will be determined by weight)
                        LinearLayout.LayoutParams.WRAP_CONTENT, // Height: wrap content
                        1.0f                                  // Weight: 1.0 (takes most space)
                );
                cityButtonParams.setMargins(0, 8, 8, 8); // Set margins around button
                cityButton.setLayoutParams(cityButtonParams);

                // Set click listener to search for this city when button is pressed
                cityButton.setOnClickListener(v -> {
                    // Create intent to search for this specific city
                    Intent intent = new Intent(SearchActivity.this, ResultActivity.class);
                    intent.putExtra("CITY_NAME", city);
                    startActivity(intent);
                });

                // Create delete button (red X button)
                Button deleteButton = new Button(this);
                deleteButton.setText("✕"); // X symbol for delete
                // Style the delete button with red background
                deleteButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                deleteButton.setTextColor(getResources().getColor(android.R.color.white));
                // Set layout parameters for delete button
                LinearLayout.LayoutParams deleteButtonParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                deleteButtonParams.setMargins(0, 8, 0, 8); // Set margins
                deleteButton.setLayoutParams(deleteButtonParams);

                // Set click listener to show confirmation dialog before deleting
                deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog(city));

                // Add both buttons to the horizontal layout
                cityLayout.addView(cityButton);
                cityLayout.addView(deleteButton);

                // Add the complete horizontal layout to the main container
                savedCitiesContainer.addView(cityLayout);
            }
        }
    }

    /**
     * Shows confirmation dialog before deleting a city
     * @param cityName The name of the city to be deleted
     */
    private void showDeleteConfirmationDialog(String cityName) {
        // Create database helper to get additional info about the city
        AirQualityDatabaseHelper dbHelper = new AirQualityDatabaseHelper(this);

        // Get count of results for this city (currently not used in message)
        int resultCount = dbHelper.getCityResultCount(cityName);

        // Create confirmation message
        String message = "Are you sure you want to delete \"" + cityName + "\"?";

        // Build and show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Delete City")           // Dialog title
                .setMessage(message)              // Dialog message
                .setPositiveButton("Delete", (dialog, which) -> deleteCity(cityName)) // Delete action
                .setNegativeButton("Cancel", null) // Cancel action (does nothing)
                .show();
    }

    /**
     * Deletes a city from the database and refreshes the UI
     * @param cityName The name of the city to delete
     */
    private void deleteCity(String cityName) {
        // Create database helper instance
        AirQualityDatabaseHelper dbHelper = new AirQualityDatabaseHelper(this);

        // Attempt to delete the city from database
        boolean deleted = dbHelper.deleteCity(cityName);

        if (deleted) {
            // Show success message
            Toast.makeText(this, "\"" + cityName + "\" deleted successfully", Toast.LENGTH_SHORT).show();
            // Refresh the saved cities list to reflect the deletion
            loadSavedCities();
        } else {
            // Show failure message
            Toast.makeText(this, "Failed to delete \"" + cityName + "\"", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Retrieves list of unique saved cities from the database
     * @param dbHelper Database helper instance
     * @return List of unique city names
     */
    private List<String> getSavedCities(AirQualityDatabaseHelper dbHelper) {
        // Create list to store city names
        List<String> cities = new ArrayList<>();
        // Use Set to automatically handle duplicates
        Set<String> uniqueCities = new HashSet<>();

        try {
            // Get readable database instance
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            // Execute query to get distinct city names, ordered alphabetically
            Cursor cursor = db.rawQuery("SELECT DISTINCT city FROM results ORDER BY city", null);

            // Check if cursor has results
            if (cursor.moveToFirst()) {
                // Loop through all results
                do {
                    // Get city name from current row
                    String city = cursor.getString(0);
                    // Add to set if not null and not empty (after trimming spaces)
                    if (city != null && !city.trim().isEmpty()) {
                        uniqueCities.add(city.trim());
                    }
                } while (cursor.moveToNext()); // Move to next row
            }

            // Close cursor and database to free resources
            cursor.close();
            db.close();

        } catch (Exception e) {
            // Log any database errors
            e.printStackTrace();
        }

        // Convert Set back to List and return
        cities.addAll(uniqueCities);
        return cities;
    }
}
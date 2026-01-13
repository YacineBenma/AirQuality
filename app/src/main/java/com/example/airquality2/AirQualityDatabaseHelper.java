package com.example.airquality2;

// Import statements for Android database and content handling
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

/**
 * Database helper class for managing air quality data storage
 * Extends SQLiteOpenHelper to handle database creation and version management
 */
public class AirQualityDatabaseHelper extends SQLiteOpenHelper {

    // Database configuration constants
    private static final String DB_NAME = "airquality.db"; // Name of the database file
    private static final int DB_VERSION = 1; // Database version for upgrades
    private static final String TAG = "AirQualityDB"; // Log tag for debugging

    /**
     * Constructor for the database helper
     * @param context Application context for database operations
     */
    public AirQualityDatabaseHelper(Context context) {
        // Call parent constructor with database name, factory (null), and version
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * Called when database is created for the first time
     * Creates the results table with required columns
     * @param db SQLite database instance
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create results table with auto-incrementing ID, city name, and result data
        db.execSQL("CREATE TABLE results (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "city TEXT, result TEXT);");
    }

    /**
     * Called when database needs to be upgraded
     * Currently empty as we're on version 1
     * @param db SQLite database instance
     * @param oldVersion Previous database version
     * @param newVersion New database version
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // No upgrade logic needed for version 1
    }

    /**
     * Saves air quality result to database with data validation
     * @param city Name of the city
     * @param result Air quality data string
     * @return true if saved successfully, false otherwise
     */
    public boolean saveResult(String city, String result) {
        // Validate city name is not null or empty
        if (city == null || city.trim().isEmpty()) {
            Log.e(TAG, "Cannot save: city is null or empty");
            return false;
        }

        // Validate result data is not null or empty
        if (result == null || result.trim().isEmpty()) {
            Log.e(TAG, "Cannot save: result is null or empty");
            return false;
        }

        // Check if result contains error messages
        if (isErrorResult(result)) {
            Log.e(TAG, "Cannot save: result contains error - " + result);
            return false;
        }

        // Verify the result contains valid air quality data
        if (!containsValidAirQualityData(result)) {
            Log.e(TAG, "Cannot save: result does not contain valid air quality data - " + result);
            return false;
        }

        try {
            // Get writable database instance
            SQLiteDatabase db = getWritableDatabase();

            // Create content values for insertion
            ContentValues values = new ContentValues();
            values.put("city", city.trim()); // Store trimmed city name
            values.put("result", result.trim()); // Store trimmed result data

            // Insert data into results table
            long rowId = db.insert("results", null, values);
            db.close(); // Close database connection

            // Check if insertion was successful
            if (rowId != -1) {
                Log.d(TAG, "Successfully saved data for city: " + city);
                return true;
            } else {
                Log.e(TAG, "Failed to insert data into database");
                return false;
            }
        } catch (Exception e) {
            // Log any exceptions during database operations
            Log.e(TAG, "Exception while saving to database", e);
            return false;
        }
    }

    /**
     * Checks if the result string contains error indicators
     * @param result The result string to check
     * @return true if result contains error indicators, false otherwise
     */
    private boolean isErrorResult(String result) {
        // Return true if result is null
        if (result == null) return true;

        // Convert to lowercase for case-insensitive comparison
        String lowerResult = result.toLowerCase().trim();

        // Check for various error indicators in the result
        return lowerResult.contains("error") ||
                lowerResult.contains("exception") ||
                lowerResult.contains("not found") ||
                lowerResult.contains("invalid") ||
                lowerResult.contains("failed") ||
                lowerResult.contains("city not found") ||
                lowerResult.contains("no data") ||
                lowerResult.contains("unable to fetch") ||
                lowerResult.contains("connection") ||
                lowerResult.startsWith("error code:") ||
                lowerResult.startsWith("exception:") ||
                lowerResult.matches(".*error\\s*code\\s*:\\s*\\d+.*") || // Matches "error code: 404" etc.
                lowerResult.matches(".*http\\s*error.*"); // HTTP errors
    }

    /**
     * Validates if the result contains proper air quality data
     * @param result The result string to validate
     * @return true if result contains valid air quality data, false otherwise
     */
    private boolean containsValidAirQualityData(String result) {
        // Check for null or empty result
        if (result == null || result.trim().isEmpty()) {
            return false;
        }

        String data = result.trim();

        // Check if it contains AQI data - most important indicator
        boolean hasAQI = data.contains("AQI:") || data.matches(".*AQI\\s*:\\s*\\d+.*");

        // Check if it contains pollutant data (PM2.5, PM10, CO, etc.)
        boolean hasPollutants = data.contains("PM2.5") ||
                data.contains("PM10") ||
                data.contains("CO:") ||
                data.contains("NO₂:") ||
                data.contains("O₃:") ||
                data.contains("SO₂:");

        // Must have both AQI and pollutant data to be considered valid
        return hasAQI && hasPollutants;
    }

    /**
     * Gets the count of saved results for a specific city
     * @param cityName Name of the city to count results for
     * @return Number of results saved for the city
     */
    public int getCityResultCount(String cityName) {
        // Return 0 if city name is null or empty
        if (cityName == null || cityName.trim().isEmpty()) {
            return 0;
        }

        int count = 0;
        try {
            // Get readable database instance
            SQLiteDatabase db = getReadableDatabase();

            // Query to count results for the specified city
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM results WHERE city = ?",
                    new String[]{cityName.trim()});

            // Get the count from the first row
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }

            // Close cursor and database
            cursor.close();
            db.close();
        } catch (Exception e) {
            // Log any exceptions during database operations
            Log.e(TAG, "Exception while getting city result count", e);
        }

        return count;
    }

    /**
     * Deletes all results for a specific city
     * @param cityName Name of the city to delete results for
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteCity(String cityName) {
        // Return false if city name is null or empty
        if (cityName == null || cityName.trim().isEmpty()) {
            return false;
        }

        try {
            // Get writable database instance
            SQLiteDatabase db = getWritableDatabase();

            // Delete all rows where city matches the specified name
            int deletedRows = db.delete("results", "city = ?", new String[]{cityName.trim()});
            db.close(); // Close database connection

            // Log the number of deleted rows
            Log.d(TAG, "Deleted " + deletedRows + " rows for city: " + cityName);
            return deletedRows > 0; // Return true if at least one row was deleted
        } catch (Exception e) {
            // Log any exceptions during database operations
            Log.e(TAG, "Exception while deleting city", e);
            return false;
        }
    }

    /**
     * Retrieves stored air quality details for a specific city
     * @param cityName Name of the city to get details for
     * @return Air quality result string or null if not found
     */
    public String getCityDetails(String cityName) {
        // Return null if city name is null or empty
        if (cityName == null || cityName.trim().isEmpty()) {
            return null;
        }

        try {
            // Get readable database instance
            SQLiteDatabase db = getReadableDatabase();

            // Query to get the first result for the specified city
            Cursor cursor = db.rawQuery("SELECT result FROM results WHERE city = ? LIMIT 1",
                    new String[]{cityName.trim()});

            // If a result is found, return it
            if (cursor.moveToFirst()) {
                String result = cursor.getString(0);
                cursor.close(); // Close cursor
                db.close(); // Close database
                return result;
            }

            // Close cursor and database if no result found
            cursor.close();
            db.close();
        } catch (Exception e) {
            // Log any exceptions during database operations
            Log.e(TAG, "Exception while getting city details", e);
        }

        return null; // Return null if no data found or exception occurred
    }
}
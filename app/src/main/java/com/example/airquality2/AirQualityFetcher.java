package com.example.airquality2;

// Import statements for Android threading and JSON handling
import android.os.Handler;
import android.os.Looper;

// JSON parsing libraries
import org.json.JSONArray;
import org.json.JSONObject;

// Java networking and I/O libraries
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for fetching air quality data from API
 * Handles both air quality and city information retrieval
 */
public class AirQualityFetcher {

    /**
     * Callback interface for handling API responses
     * Provides success and error callback methods
     */
    public interface AirQualityCallback {
        /**
         * Called when API request is successful
         * @param result Formatted air quality data string
         * @param cityInfo Formatted city information string
         */
        void onSuccess(String result, String cityInfo);

        /**
         * Called when API request fails
         * @param error Error message describing the failure
         */
        void onError(String error);
    }

    /**
     * Fetches air quality data for the specified city
     * @param cityName Name of the city to fetch data for
     * @param callback Callback interface to handle the response
     */
    public static void fetchData(final String cityName, final AirQualityCallback callback) {
        // Create single-threaded executor for background network operations
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Create handler for posting results back to main UI thread
        Handler handler = new Handler(Looper.getMainLooper());

        // Execute network operations in background thread
        executor.execute(() -> {
            try {
                // First, fetch city information (population, coordinates, etc.)
                String cityInfo = fetchCityInfo(cityName);

                // Build API URL for air quality data with URL encoding
                String apiUrl = "https://api.api-ninjas.com/v1/airquality?city=" +
                        java.net.URLEncoder.encode(cityName, "UTF-8");

                // Create URL object and open HTTP connection
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                // Set request method to GET
                conn.setRequestMethod("GET");

                // Add API key to request header for authentication
                conn.setRequestProperty("X-Api-Key", Constants.API_NINJAS_KEY);

                // Get HTTP response code
                int code = conn.getResponseCode();

                // Handle successful response (HTTP 200)
                if (code == 200) {
                    // Create buffered reader to read response data
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));

                    // Build response string
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close(); // Close the reader

                    String responseText = sb.toString().trim();

                    // Check if response is empty or contains empty JSON
                    if (responseText.isEmpty() || responseText.equals("{}") || responseText.equals("[]")) {
                        // Post error to main thread
                        handler.post(() -> callback.onError("City not found - no air quality data available"));
                        return;
                    }

                    // Parse JSON response
                    JSONObject json = new JSONObject(responseText);

                    // Validate that required overall_aqi field exists
                    if (!json.has("overall_aqi")) {
                        handler.post(() -> callback.onError("City not found - invalid response from server"));
                        return;
                    }

                    // Extract overall AQI value
                    int overallAqi = json.getInt("overall_aqi");

                    // Validate that all required pollutant data exists
                    if (!json.has("PM2.5") || !json.has("PM10") || !json.has("CO") ||
                            !json.has("NO2") || !json.has("O3") || !json.has("SO2")) {
                        handler.post(() -> callback.onError("City not found - incomplete air quality data"));
                        return;
                    }

                    // Extract pollutant data objects
                    JSONObject pm25 = json.getJSONObject("PM2.5");
                    JSONObject pm10 = json.getJSONObject("PM10");
                    JSONObject co = json.getJSONObject("CO");
                    JSONObject no2 = json.getJSONObject("NO2");
                    JSONObject o3 = json.getJSONObject("O3");
                    JSONObject so2 = json.getJSONObject("SO2");

                    // Format result string with all air quality data
                    String result =
                            "AQI: " + overallAqi + "\n" +
                                    "PM2.5: " + pm25.getDouble("concentration") + " (" + pm25.getInt("aqi") + ")\n" +
                                    "PM10: " + pm10.getDouble("concentration") + " (" + pm10.getInt("aqi") + ")\n" +
                                    "CO: " + co.getDouble("concentration") + " (" + co.getInt("aqi") + ")\n" +
                                    "NO₂: " + no2.getDouble("concentration") + " (" + no2.getInt("aqi") + ")\n" +
                                    "O₃: " + o3.getDouble("concentration") + " (" + o3.getInt("aqi") + ")\n" +
                                    "SO₂: " + so2.getDouble("concentration") + " (" + so2.getInt("aqi") + ")";

                    // Post success result to main thread
                    handler.post(() -> callback.onSuccess(result, cityInfo));

                    // Handle specific HTTP error codes
                } else if (code == 404) {
                    handler.post(() -> callback.onError("City not found - please check the city name"));
                } else if (code == 400) {
                    handler.post(() -> callback.onError("Invalid city name - please enter a valid city"));
                } else if (code == 401) {
                    handler.post(() -> callback.onError("API authentication failed"));
                } else if (code == 429) {
                    handler.post(() -> callback.onError("Too many requests - please try again later"));
                } else {
                    // Handle other HTTP error codes
                    handler.post(() -> callback.onError("City not found - Error code: " + code));
                }

                // Handle JSON parsing exceptions
            } catch (org.json.JSONException e) {
                handler.post(() -> callback.onError("City not found - invalid data received"));

                // Handle network I/O exceptions
            } catch (java.io.IOException e) {
                handler.post(() -> callback.onError("Network error - please check your connection"));

                // Handle any other exceptions
            } catch (Exception e) {
                handler.post(() -> callback.onError("City not found - Exception: " + e.getMessage()));
            }
        });
    }

    /**
     * Fetches city information (population, coordinates, etc.) from API
     * @param cityName Name of the city to fetch information for
     * @return Formatted city information string
     */
    private static String fetchCityInfo(String cityName) {
        try {
            // Build API URL for city information with URL encoding
            String apiUrl = "https://api.api-ninjas.com/v1/city?name=" +
                    java.net.URLEncoder.encode(cityName, "UTF-8");

            // Create URL object and open HTTP connection
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Set request method to GET
            conn.setRequestMethod("GET");

            // Add API key to request header for authentication
            conn.setRequestProperty("X-Api-Key", Constants.API_NINJAS_KEY);

            // Get HTTP response code
            int code = conn.getResponseCode();

            // Handle successful response
            if (code == 200) {
                // Create buffered reader to read response data
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));

                // Build response string
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close(); // Close the reader

                String responseText = sb.toString().trim();

                // Check if response is empty
                if (responseText.isEmpty() || responseText.equals("[]")) {
                    return "Location: Information not available";
                }

                // Parse JSON array response
                JSONArray jsonArray = new JSONArray(responseText);

                // Process first city result if available
                if (jsonArray.length() > 0) {
                    JSONObject cityData = jsonArray.getJSONObject(0);

                    StringBuilder cityInfo = new StringBuilder();

                    // Add city name if available
                    if (cityData.has("name")) {
                        cityInfo.append("City: ").append(cityData.getString("name"));
                    }

                    // Add country if available
                    if (cityData.has("country")) {
                        cityInfo.append("\nCountry: ").append(cityData.getString("country"));
                    }

                    // Add formatted population if available
                    if (cityData.has("population")) {
                        int population = cityData.getInt("population");
                        cityInfo.append("\nPopulation: ").append(String.format("%,d", population));
                    }

                    // Add coordinates if available
                    if (cityData.has("latitude") && cityData.has("longitude")) {
                        double lat = cityData.getDouble("latitude");
                        double lon = cityData.getDouble("longitude");
                        cityInfo.append("\nCoordinates: ").append(String.format("%.4f, %.4f", lat, lon));
                    }

                    // Add timezone if available
                    if (cityData.has("timezone")) {
                        cityInfo.append("\nTimezone: ").append(cityData.getString("timezone"));
                    }

                    return cityInfo.toString();
                } else {
                    // Return default message if no city data found
                    return "Location: Information not available";
                }
            } else {
                // Return default message for HTTP errors
                return "Location: Information not available";
            }
        } catch (Exception e) {
            // Return default message for any exceptions
            return "Location: Information not available";
        }
    }
}
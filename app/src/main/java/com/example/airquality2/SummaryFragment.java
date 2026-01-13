package com.example.airquality2;

// Import statements for Android Fragment components
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

/**
 * Simple fragment that displays a summary with the city name
 * This fragment shows basic information about the selected city
 */
public class SummaryFragment extends Fragment {

    // Variable to store the city name passed to this fragment
    private String cityName;

    /**
     * Required empty public constructor for Fragment
     * Android system uses this to recreate fragments when needed
     */
    public SummaryFragment() {
        // Required empty public constructor
    }

    /**
     * Called when the fragment view is being created
     * Inflates the layout and sets up the city name display
     * @param inflater Used to inflate the fragment layout
     * @param container Parent view that the fragment will be attached to
     * @param savedInstanceState Previous state data (if any)
     * @return The inflated view for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the fragment layout from XML
        View view = inflater.inflate(R.layout.fragment_summary, container, false);

        // Check if arguments were passed to this fragment
        if (getArguments() != null) {
            // Extract the city name from the arguments bundle
            cityName = getArguments().getString("CITY");
        }

        // Find the TextView in the inflated layout
        TextView textViewCity = view.findViewById(R.id.textCitySummary);

        // Set the text to display the city name
        textViewCity.setText("City: " + cityName);

        // Return the configured view
        return view;
    }
}
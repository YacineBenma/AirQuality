package com.example.airquality2;

// Import statements for Android Activity and Fragment management
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * Activity that displays air quality results using two fragments:
 * SummaryFragment and DetailsFragment
 * This activity serves as a container for the fragments
 */
public class ResultActivity extends AppCompatActivity {

    // Static variable to store city name - accessible from other classes
    public static String CITY_NAME;

    /**
     * Called when the activity is being created
     * Sets up the layout and loads both fragments with city data
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Call parent class onCreate method
        super.onCreate(savedInstanceState);

        // Set the layout for this activity
        setContentView(R.layout.activity_result);

        // Extract city name from the intent that started this activity
        CITY_NAME = getIntent().getStringExtra("CITY_NAME");

        // Create and configure SummaryFragment
        SummaryFragment summaryFragment = new SummaryFragment();
        // Create bundle to pass city name to fragment
        Bundle bundle = new Bundle();
        bundle.putString("CITY", CITY_NAME);
        // Attach the bundle to the fragment
        summaryFragment.setArguments(bundle);

        // Create and configure DetailsFragment
        DetailsFragment detailsFragment = new DetailsFragment();
        // Reuse the same bundle for DetailsFragment (contains city name)
        detailsFragment.setArguments(bundle);

        // Get the fragment manager to handle fragment transactions
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Begin a new fragment transaction
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Replace the summary container with SummaryFragment
        transaction.replace(R.id.fragment_summary, summaryFragment);

        // Replace the details container with DetailsFragment
        transaction.replace(R.id.fragment_details, detailsFragment);

        // Commit the transaction to apply the changes
        transaction.commit();
    }
}
package ru.gelin.android.sendtosd;

import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;

/**
 *  Application preferences.
 */
public class PreferencesActivity extends PreferenceActivity {

	Handler handler = new Handler();
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.app_preferences);
    }
    
}

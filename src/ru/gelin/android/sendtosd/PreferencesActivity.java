package ru.gelin.android.sendtosd;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 *  Application preferences.
 */
public class PreferencesActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.app_preferences);
    }
    
}

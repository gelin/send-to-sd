package ru.gelin.android.sendtosd;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;

/**
 *  Application preferences.
 */
public class PreferencesActivity extends PreferenceActivity implements Constants {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.app_preferences);
        
        ListPreference initialFolder = (ListPreference)findPreference(PREF_INITIAL_FOLDER);
        initialFolder.setSummary(initialFolder.getEntry());
        initialFolder.setOnPreferenceChangeListener(new InitialFolderChangeListener());
    }
    
    class InitialFolderChangeListener implements OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ListPreference initialFolder = (ListPreference)preference;
            int selectedIndex = initialFolder.findIndexOfValue(String.valueOf(newValue));
            if (selectedIndex < 0) {
                return false;
            }
            initialFolder.setSummary(initialFolder.getEntries()[selectedIndex]);
            return true;
        }
    }
    
}

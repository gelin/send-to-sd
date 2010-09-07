package ru.gelin.android.sendtosd;

import android.app.Dialog;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.View;
import android.view.ViewGroup;

/**
 *  Application preferences.
 */
public class PreferencesActivity extends PreferenceActivity implements Constants {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.app_preferences);
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (PREF_HOW_TO_USE.equals(preference.getKey())) {
            showHowToUse();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
    
    /**
     *  Displays dialog with "How to use" information
     */
    void showHowToUse() {
        Dialog dialog = new Dialog(this);
        dialog.setTitle(R.string.how_to_use);
        View layout = getLayoutInflater().inflate(
                R.layout.how_to_use_dialog, 
                (ViewGroup)findViewById(R.id.how_to_use_dialog_root));
        dialog.setContentView(layout);
        dialog.show();
    }
    
}

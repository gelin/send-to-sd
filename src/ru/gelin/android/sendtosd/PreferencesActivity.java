package ru.gelin.android.sendtosd;

import ru.gelin.android.sendtosd.donate.PurchaseDatabase.DonateStatus;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

/**
 *  Application preferences.
 */
public class PreferencesActivity extends PreferenceActivity {

	static final String PREF_DONATE = "donate";
	static final String PREF_DONATE_CATEGORY = "donate_category";
	
	Preference donateCategory;
	Preference donate;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.app_preferences);
        this.donateCategory = findPreference(PREF_DONATE_CATEGORY);
    	this.donate = findPreference(PREF_DONATE);
    }
    
    void updateDonateStatus(DonateStatus status) {
    	Preference category = findPreference(PREF_DONATE_CATEGORY);
    	switch (status) {
    	case NONE:
    		getPreferenceScreen().removePreference(donateCategory);
    		break;
    	case EXPECTING:
    		//if (!category) {
    			
    		//}
    	}
    }

}

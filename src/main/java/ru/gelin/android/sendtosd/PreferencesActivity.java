package ru.gelin.android.sendtosd;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import ru.gelin.android.sendtosd.donate.DonateStatus;
import ru.gelin.android.sendtosd.donate.DonateStatusListener;
import ru.gelin.android.sendtosd.donate.Donation;
import ru.gelin.android.sendtosd.fs.StartPaths;
import ru.gelin.android.sendtosd.permissions.PermissionChecker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static ru.gelin.android.sendtosd.PreferenceParams.DEFAULT_INITIAL_FOLDER;
import static ru.gelin.android.sendtosd.PreferenceParams.PREF_INITIAL_FOLDER;


/**
 * Application preferences.
 */
public class PreferencesActivity extends PreferenceActivity implements DonateStatusListener {

    private static final String PREF_DONATE = "donate";
    private static final String PREF_DONATE_CATEGORY = "donate_category";

    /**
     * Special package name for donate version of the app
     */
    private static final String DONATE_PACKAGE_NAME = "ru.gelin.android.sendtosd.donate";

    private static final int REQUEST_CODE = 1;

    private Preference donateCategory;
    private Preference donate;

    private Donation donation;
    private Handler handler = new Handler();

    private final PermissionChecker permissions = new PermissionChecker(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.app_preferences);
        updateInitialFolderView();

        this.donateCategory = findPreference(PREF_DONATE_CATEGORY);
        this.donate = findPreference(PREF_DONATE);

        if (DONATE_PACKAGE_NAME.equals(getPackageName())) {
            updateDonateView(DonateStatus.PURCHASED);
        } else {
            updateDonateView(DonateStatus.NONE);
        }

        this.donation = new Donation(this, this);
        this.donate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                preference.setEnabled(false);
                startDonatePurchase();
                return true;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.donation != null) {
            this.donation.destroy();
            this.donation = null;
        }
    }

    private void updateInitialFolderView() {
        ListPreference initialFolder = (ListPreference) findPreference(PREF_INITIAL_FOLDER);

        initialFolder.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (permissions.isReadGranted()) {
                    return false;
                }
                permissions.requestReadPermission();
                return true;
            }
        });

        String value = initialFolder.getValue();
        List<String> entries = new ArrayList<>();
        List<String> values = new ArrayList<>();
        entries.add(getString(R.string.last_folder));
        values.add(DEFAULT_INITIAL_FOLDER);
        for (File path : new StartPaths(this, true).getPaths()) {
            String pathName = String.valueOf(path);
            entries.add(pathName);
            values.add(pathName);
        }
        initialFolder.setEntries(entries.toArray(new String[]{}));
        initialFolder.setEntryValues(values.toArray(new String[]{}));
        initialFolder.setValue(value);
    }

    private void updateDonateView(DonateStatus status) {
        Preference category = findPreference(PREF_DONATE_CATEGORY);
        switch (status) {
            case NONE:
                getPreferenceScreen().removePreference(this.donateCategory);
                break;
            case EXPECTING:
                if (category == null) {
                    getPreferenceScreen().addPreference(this.donateCategory);
                }
                this.donate.setTitle(R.string.donate);
                this.donate.setSummary(R.string.donate_summary);
                this.donate.setEnabled(true);
                break;
            case PURCHASED:
                if (category == null) {
                    getPreferenceScreen().addPreference(this.donateCategory);
                }
                this.donate.setTitle(R.string.donate_thanks);
                this.donate.setSummary("");
                this.donate.setEnabled(false);
                break;
        }
    }

    @Override
    public void onDonateStatusChanged(DonateStatus status) {
        updateDonateView(status);
    }

    private void startDonatePurchase() {
        if (this.donation == null) {
            return;
        }
        PendingIntent intent = this.donation.getPurchaseIntent();
        if (intent == null) {
            return;
        }
        try {
            startIntentSenderForResult(intent.getIntentSender(), REQUEST_CODE, new Intent(), 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.w(Tag.TAG, "startIntentSenderForResult() failed", e);
            return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) {
            return;
        }
        if (resultCode != RESULT_OK) {
            updateDonateView(DonateStatus.EXPECTING);
            return;
        }
        if (this.donation == null) {
            return;
        }
        this.donation.processPurchaseResult(data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        this.permissions.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}

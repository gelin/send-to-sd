package ru.gelin.android.sendtosd.intent;

import static ru.gelin.android.sendtosd.IntentParams.EXTRA_PATH;
import static ru.gelin.android.sendtosd.PreferenceParams.PREF_INITIAL_FOLDER;
import static ru.gelin.android.sendtosd.PreferenceParams.DEFAULT_INITIAL_FOLDER;
import static ru.gelin.android.sendtosd.PreferenceParams.PREF_LAST_FOLDER;
import static ru.gelin.android.sendtosd.Tag.TAG;

import java.io.File;
import java.io.IOException;

import ru.gelin.android.sendtosd.PreferenceParams.InitialFolder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 *  Extracts some necessary information from the intent.
 */
public class IntentInfo {
    
    /** Current context */
    Context context;
    /** Processing intent */
    Intent intent;
    /** Application preferences */
    SharedPreferences preferences;
    
    /**
     *  Creates the intent info.
     *  @param  context current context
     *  @param  intent  intent to process
     *  @throws IntentException if it's not possible to create the info from the intent
     */
    public IntentInfo(Context context, Intent intent) throws IntentException {
        this.context = context;
        this.intent = intent;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!validate()) {
            throw new IntentException("invalid intent: " + intent);
        }
    }
    
    /**
     *  Returns false if this intent doesn't contain STREAM or TEXT extras.
     */
    boolean validate() {
        return intent.hasExtra(Intent.EXTRA_STREAM) || 
                intent.hasExtra(Intent.EXTRA_TEXT);
    }
    
    /**
     *  Returns path on the external storage provided with the intent.
     */
    public File getPath() {
        String path = this.intent.getStringExtra(EXTRA_PATH);
        if (path == null) {
            return getDefaultPath();
        }
        try {
            return new File(path).getCanonicalFile();
        } catch (IOException e) {
            return new File(path);
        }
    }

    /**
     *  Returns true if the intent is initial intent for the application.
     *  I.e. not modified intent for sub-activities.
     */
    public boolean isInitial() {
        return !this.intent.hasExtra(EXTRA_PATH);
    }

    /**
     *  Returns default path. The return value can differs for different preferences. 
     */
    File getDefaultPath() {
        InitialFolder initialFolder = InitialFolder.legacyValueOf(
        		this.preferences.getString(PREF_INITIAL_FOLDER, DEFAULT_INITIAL_FOLDER));
        File root = Environment.getExternalStorageDirectory();
        if (InitialFolder.LAST_FOLDER.equals(initialFolder)) {
            String lastFolder = this.preferences.getString(PREF_LAST_FOLDER, null);
            if (lastFolder == null) {
                return root;
            }
            File lastFolderFile = new File(lastFolder);
            if (!lastFolderFile.isDirectory() || !lastFolderFile.canWrite()) {
                return root;
            }
            return lastFolderFile;
        }
        return root;
    }
    
    /**
     *  Logs debug information about the intent.
     */
    public void log() {
        Log.i(TAG, "intent: " + intent);
        /*
        Log.d(TAG, "data: " + intent.getData());
        Bundle extras = intent.getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                Log.d(TAG, "extra: " + key + " = " + extras.get(key));
            }
        }
        */
    }

}

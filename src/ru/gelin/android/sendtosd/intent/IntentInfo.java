package ru.gelin.android.sendtosd.intent;

import java.io.File;
import java.io.IOException;

import ru.gelin.android.sendtosd.Constants;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 *  Extracts some necessary information from the intent.
 */
public class IntentInfo implements Constants {
    
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
     */
    public IntentInfo(Context context, Intent intent) {
        this.context = context;
        this.intent = intent;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    /**
     *  Returns false if this intent doesn't contain STREAM or TEXT extras.
     */
    public boolean validate() {
        return intent.hasExtra(Intent.EXTRA_STREAM) || 
                intent.hasExtra(Intent.EXTRA_TEXT);
    }
    
    /**
     *  Returns path on the external storage provided with the intent.
     */
    public File getPath() {
        String path = intent.getStringExtra(EXTRA_PATH);
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
        return !intent.hasExtra(EXTRA_PATH);
    }

    /**
     *  Returns default path. The return value can differs for different preferences. 
     */
    File getDefaultPath() {
        String initialFolder = preferences.getString(PREF_INITIAL_FOLDER, 
                VAL_LAST_FOLDER);
        File root = Environment.getExternalStorageDirectory();
        if (VAL_LAST_FOLDER.equals(initialFolder)) {
            String lastFolder = preferences.getString(PREF_LAST_FOLDER, null);
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

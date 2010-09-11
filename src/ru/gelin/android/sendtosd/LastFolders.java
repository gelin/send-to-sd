package ru.gelin.android.sendtosd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

/**
 *  Singleton storage for last folders.
 */
public class LastFolders implements Constants {
    
    /** Last folders preferences prefix */
    public static final String PREF_LAST_FOLDERS_PREFIX = "last_folder_";
    /** Max number of last folders to store */
    public static final int MAX_NUMBER = 50;
    
    /** Preferences */
    SharedPreferences preferences;
    /** Instance */
    static LastFolders instance;
    
    private LastFolders(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    /**
     *  Returns the instance of the storage.
     */
    public static LastFolders getInstance(Context context) {
        if (instance == null) {
            instance = new LastFolders(context);
        }
        return instance;
    }
    
    /**
     *  Returns true if there are no last folders in the storage.
     */
    public boolean isEmpty() {
        if (preferences.contains(PREF_LAST_FOLDERS_PREFIX + 0)) {
            return false;
        }
        return true;
    }
    
    /**
     *  Returns "number" last folders from the storage.
     *  Non-existed or not-writable folders are excluded.
     */
    public List<File> get(int number) {
        List<File> result = new ArrayList<File>();
        for (int i = 0; i < MAX_NUMBER; i++) {
            String pref = preferences.getString(PREF_LAST_FOLDERS_PREFIX + i, null);
            if (pref == null) {
                continue;
            }
            File file = new File(pref);
            if (!file.isDirectory() || !file.canWrite()) {
                continue;
            }
            result.add(file);
            if (result.size() >= number) {
                break;
            }
        }
        return result;
    }

    /**
     *  Puts the last folder to the list.
     */
    public void put(File file) {
        File canonicalFile;
        try {
            canonicalFile = file.getCanonicalFile();
        } catch (IOException e) {
            canonicalFile = file;
        }
        List<File> folders = get(MAX_NUMBER);
        folders.remove(canonicalFile);
        folders.add(0, canonicalFile);
        Editor editor = preferences.edit();
        editor.putString(PREF_LAST_FOLDER, canonicalFile.toString());
        for (int i = 0; i < folders.size(); i++) {
            editor.putString(PREF_LAST_FOLDERS_PREFIX + i, folders.get(i).toString());
        }
        for (int i = folders.size(); i < MAX_NUMBER; i++) {
            editor.remove(PREF_LAST_FOLDERS_PREFIX + i);
        }
        editor.commit();
    }
    
}

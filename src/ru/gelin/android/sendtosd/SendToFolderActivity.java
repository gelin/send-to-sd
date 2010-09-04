package ru.gelin.android.sendtosd;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.widget.Toast;

/**
 *  Activity which displays the list of folders
 *  and allows to save the file to folder.
 */
public class SendToFolderActivity extends PreferenceActivity implements Constants {
    
    /** "Save here" preference key */
    public static final String PREF_SAVE_HERE = "save_here";
    /** "Folders" preference key */
    public static final String PREF_FOLDERS = "folders";
    
    /** Filename to save */
    String fileName;
    /** Intent utilities */
    IntentUtils utils;
    /** Current path */
    File path;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        if (intent == null) {
            error(R.string.unsupported_file);
            return;
        }
        utils = new IntentUtils(this, intent);
        utils.logIntentInfo();
        
        try {
            fileName = utils.getFileName();
        } catch (Exception e) {
            error(R.string.unsupported_file);
            return;
        }
        setTitle(fileName);
        
        addPreferencesFromResource(R.xml.folder_preferences);
        
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            error(R.string.no_sd_card);
        }
        path = utils.getPath();
        Preference saveHere = findPreference(PREF_SAVE_HERE);
        saveHere.setSummary(path.toString());
        
        listFolder();
    }
    
    /**
     *  Fills the list of subfolders.
     */
    void listFolder() {
        PreferenceCategory folders = (PreferenceCategory)findPreference(PREF_FOLDERS);
        if (!"/".equals(path.getAbsolutePath())) {
            Preference upFolder = new Preference(this);
            upFolder.setTitle("..");
            folders.addPreference(upFolder);
        }
        File[] subFolders = path.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        List<File> sortedFolders = Arrays.asList(subFolders);
        Collections.sort(sortedFolders);
        for (File subFolder : sortedFolders) {
            Preference folderPref = new Preference(this);
            folderPref.setTitle(subFolder.getName());
            folderPref.setEnabled(subFolder.canWrite());
            folders.addPreference(folderPref);
        }
    }
    
    /**
     *  Shows error message and exits the activity.
     */
    void error(int messageId) {
        Toast.makeText(this, messageId, Toast.LENGTH_LONG);
        finish();
    }

}

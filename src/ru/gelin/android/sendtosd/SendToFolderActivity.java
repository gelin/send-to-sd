package ru.gelin.android.sendtosd;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.EditText;
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
            error(R.string.unsupported_file, e);
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
        
        updateFileNameIfExists();
        
        listFolder();
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (PREF_SAVE_HERE.equals(preference.getKey())) {
            saveFile();
            return true;
        }
        changeFolder(preference.getTitle().toString());
        return true;
    }
    
    /**
     *  Changes the current folder.
     */
    void changeFolder(String folder) {
        Intent intent = getIntent();
        intent.putExtra(EXTRA_PATH, new File(path, folder).toString());
        intent.setClass(this, SendToFolderActivity.class);
        startActivity(intent);
    }
    
    /**
     *  Saves the file.
     */
    void saveFile() {
        showFileNameDialog();
        try {
            InputStream in = utils.getFileStream();
            OutputStream out = new FileOutputStream(new File(path, fileName));
            byte[] buf = new byte[1024];
            int read;
            while ((read = in.read(buf)) > 0) {
                out.write(buf, 0, read);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            error(R.string.unsupported_file, e);
        }
    }
    
    /**
     *  Displays dialog which allows to edit the file name.
     */
    void showFileNameDialog() {
        final EditText view = new EditText(this);
        view.setText(fileName);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.choose_file_name);
        builder.setView(view);
        builder.setPositiveButton(R.string.save_file, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                fileName = view.getText().toString();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
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
     *  Update file name if it is already exists.
     */
    void updateFileNameIfExists() {
        if (!new File(path, fileName).exists()) {
            return;
        }
        int index = 1;
        int dotIndex = fileName.lastIndexOf('.');
        String newName;
        do {
            if (dotIndex < 0) {
                newName = fileName + "-" + index;
            } else {
                newName = fileName.substring(0, dotIndex) + "-" + index + 
                    fileName.substring(dotIndex);
            }
        } while (new File(path, newName).exists());
        fileName = newName;
    }
    
    /**
     *  Shows error message and exits the activity.
     */
    void error(int messageId) {
        Toast.makeText(this, messageId, Toast.LENGTH_LONG);
        finish();
    }
    
    /**
     *  Shows error message and exits the activity.
     */
    void error(int messageId, Throwable exception) {
        Log.e(TAG, exception.toString(), exception);
        Toast.makeText(this, messageId, Toast.LENGTH_LONG);
        finish();
    }

}

package ru.gelin.android.sendtosd;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.gelin.android.sendtosd.intent.IntentFile;
import ru.gelin.android.sendtosd.intent.IntentInfo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

/**
 *  Activity which displays the list of folders
 *  and allows to save the file to folder.
 */
public class SendToFolderActivity extends PreferenceActivity 
        implements Constants, FileSaver, FolderChanger {
    
    /** "Copy here" preference key */
    public static final String PREF_COPY_HERE = "copy_here";
    /** "Move here" preference key */
    public static final String PREF_MOVE_HERE = "move_here";
    /** Last folders preference category key */
    public static final String PREF_LAST_FOLDERS = "last_folders";
    /** "Folders" preference key */
    public static final String PREF_FOLDERS = "folders";
    /** Request code for directory traversing */
    public static final int REQ_CODE_FOLDER = 0;

    /** Intent information */
    IntentInfo intentInfo;
    /** File to save from intent */
    IntentFile intentFile;
    /** Filename to save */
    String fileName;
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
        intentInfo = new IntentInfo(this, intent);
        intentInfo.logIntentInfo();
        if (!intentInfo.validate()) {
            error(R.string.unsupported_file);
            return;
        }
        intentFile = intentInfo.getFile();
        
        try {
            fileName = intentFile.getName();
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
        path = intentInfo.getPath();
        
        updateFileNameIfExists();
        
        CopyHerePreference copyHerePreference = (CopyHerePreference)findPreference(PREF_COPY_HERE);
        MoveHerePreference moveHerePreference = (MoveHerePreference)findPreference(PREF_MOVE_HERE);
        copyHerePreference.setFileSaver(this);
        moveHerePreference.setFileSaver(this);
        if (!path.canWrite()) {
            copyHerePreference.setEnabled(false);
            moveHerePreference.setEnabled(false);
        }
        if (!intentFile.isDeletable()) {
            getPreferenceScreen().removePreference(moveHerePreference);
        }
        
        listFolders();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (intentInfo.isInitial()) {
            listLastFolders();
        } else {
            Preference lastFolders = findPreference(PREF_LAST_FOLDERS);
            if (lastFolders != null) {
                getPreferenceScreen().removePreference(lastFolders);
            }
        }
    }
    
    /**
     *  Returns the current folder.
     */
    public File getPath() {
        return path;
    }
    /**
     *  Returns the file name to save.
     */
    public String getFileName() {
        return fileName;
    }
    /**
     *  Sets the file name to save.
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    /**
     *  Changes the current folder.
     */
    public void changeFolder(File folder) {
        Intent intent = getIntent();
        intent.putExtra(EXTRA_PATH, folder.toString());
        intent.setClass(this, SendToFolderActivity.class);
        intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        startActivityForResult(intent, REQ_CODE_FOLDER);
    }
    
    /**
     *  Copies the file.
     */
    public void copyFile() {
        LastFolders lastFolders = LastFolders.getInstance(this);
        lastFolders.put(path);
        try {
            intentFile.saveAs(new File(path, fileName));
        } catch (Exception e) {
            warn(R.string.file_is_not_copied, e);
            return;
        }
        complete(R.string.file_is_copied);
    }
    
    /**
     *  Moves the file.
     */
    public void moveFile() {
        LastFolders lastFolders = LastFolders.getInstance(this);
        lastFolders.put(path);
        try {
            intentFile.saveAs(new File(path, fileName));
        } catch (Exception e) {
            warn(R.string.file_is_not_moved, e);
            return;
        }
        try {
            intentFile.delete();
        } catch (Exception e) {
            Log.w(TAG, e.toString(), e);
            complete(R.string.file_is_not_deleted);
            return;
        }
        complete(R.string.file_is_moved);
    }
    
    /**
     *  Fills the list of last folders.
     */
    void listLastFolders() {
        PreferenceCategory lastFoldersCategory = 
                (PreferenceCategory)findPreference(PREF_LAST_FOLDERS);
        
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean(PREF_SHOW_LAST_FOLDERS, true)) {
            getPreferenceScreen().removePreference(lastFoldersCategory);
            return;
        }
        LastFolders lastFolders = LastFolders.getInstance(this);
        if (lastFolders.isEmpty()) {
            getPreferenceScreen().removePreference(lastFoldersCategory);
            return;
        }
        
        int lastFoldersNumber;
        try {
            lastFoldersNumber = Integer.parseInt(preferences.getString(
                    PREF_LAST_FOLDERS_NUMBER, DEFAULT_LAST_FOLDERS_NUMBER));
        } catch (NumberFormatException e) {
            lastFoldersNumber = DEFAULT_LAST_FOLDERS_NUMBER_INT;
        }
        for (File folder : lastFolders.get(lastFoldersNumber)) {
            //Log.d(TAG, folder.toString());
            PathFolderPreference folderPref = new PathFolderPreference(this, folder, this);
            lastFoldersCategory.addPreference(folderPref);
        }
    }
    
    /**
     *  Fills the list of subfolders.
     */
    void listFolders() {
        PreferenceCategory folders = (PreferenceCategory)findPreference(PREF_FOLDERS);
        folders.removeAll();
        if (!"/".equals(path.getAbsolutePath())) {
            Preference upFolder = new FolderPreference(this, path.getParentFile(), this);
            upFolder.setTitle("..");
            folders.addPreference(upFolder);
        }
        File[] subFolders = path.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        if (subFolders == null) {
            return;
        }
        List<File> sortedFolders = Arrays.asList(subFolders);
        Collections.sort(sortedFolders);
        for (File subFolder : sortedFolders) {
            File folder;
            try {
                folder = subFolder.getCanonicalFile();
            } catch (IOException e) {
                folder = subFolder;
            }
            Preference folderPref = new FolderPreference(this, folder, this);
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
            index++;
        } while (new File(path, newName).exists());
        fileName = newName;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_new_folder:
            showNewFolderDialog();
            break;
        case R.id.menu_preferences:
            startActivity(new Intent(this, PreferencesActivity.class));
            break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     *  Displays the New Folder dialog.
     */
    void showNewFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.new_folder);
        View content = getLayoutInflater().inflate(R.layout.new_folder_dialog, 
                (ViewGroup)findViewById(R.id.how_to_use_dialog_root));
        final EditText edit = (EditText)content.findViewById(R.id.new_folder_name);
        builder.setView(content);
        builder.setPositiveButton(R.string.create_folder, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                createFolder(edit.getText().toString());
            }
        });
        Dialog dialog = builder.create();
        //http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/java/android/preference/DialogPreference.java;h=bbad2b6d432ce44ad05ddbc44487000b150135ef;hb=HEAD
        Window window = dialog.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.show();
    }
    
    /**
     *  Creates the new folder.
     */
    void createFolder(String folderName) {
        File newFolder = new File(path, folderName);
        boolean result = newFolder.mkdirs();
        if (result) {
            Toast.makeText(this, R.string.folder_created, Toast.LENGTH_LONG).show();
            listFolders();
        } else {
            Toast.makeText(this, R.string.folder_not_created, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     *  Shows the error message.
     */
    void warn(int messageId) {
        error(messageId, true, null);
    }
    
    /**
     *  Shows the error message and disables the activity.
     */
    void error(int messageId) {
        error(messageId, false, null);
    }
    
    /**
     *  Shows and logs the error message.
     */
    void warn(int messageId, Throwable exception) {
        error(messageId, true, exception);
    }
    
    /**
     *  Shows and logs the error message and disables the activity.
     */
    void error(int messageId, Throwable exception) {
        error(messageId, false, exception);
    }
    
    /**
     *  Shows and logs the error message and disables the activity.
     */
    void error(int messageId, boolean enabled, Throwable exception) {
        if (exception != null) {
            Log.e(TAG, exception.toString(), exception);
        }
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show();
        setResult(RESULT_CANCELED);
        getPreferenceScreen().setEnabled(enabled);
        //finish();
    }
    
    /**
     *  Complete the action.
     */
    void complete(int messageId) {
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show();
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            finish();
        }
    }

}

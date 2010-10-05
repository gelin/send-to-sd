package ru.gelin.android.sendtosd;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.gelin.android.sendtosd.intent.IntentInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
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
 *  Base class for activities to copy/move file/files to folder.
 *  Responses for the directory listing and traversing.
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
    /** Current path */
    File path;
    /** Last folders preference. Saved here to remove from or add to hierarchy. */
    Preference lastFolders;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.folder_preferences);
        
        lastFolders = findPreference(PREF_LAST_FOLDERS);
        
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            error(R.string.no_sd_card);
        }
    }
    
    /**
     *  Called when intent information is created.
     *  Loads list of folders.
     */
    protected void onPostCreateIntentInfo() {
        path = intentInfo.getPath();
        listFolders();
    }
    
    /**
     *  Called when all information about files is loaded.
     *  Disables Copy/Move Here for non-writable folders.
     *  Hides Move Here for non-deletable files.
     */
    protected void onPostLoadFileInfo() {
        CopyHerePreference copyHerePreference = (CopyHerePreference)findPreference(PREF_COPY_HERE);
        MoveHerePreference moveHerePreference = (MoveHerePreference)findPreference(PREF_MOVE_HERE);
        copyHerePreference.setFileSaver(this);
        moveHerePreference.setFileSaver(this);
        if (!path.canWrite()) {
            copyHerePreference.setEnabled(false);
            moveHerePreference.setEnabled(false);
        }
        if (!hasDeletableFile()) {
            getPreferenceScreen().removePreference(moveHerePreference);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Preference existedLastFolders = findPreference(PREF_LAST_FOLDERS);
        if (intentInfo.isInitial()) {
            if (existedLastFolders == null) {
                getPreferenceScreen().addPreference(lastFolders);
            }
            listLastFolders();
        } else {
            if (existedLastFolders != null) {
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
     *  Return true if the intent has deletable file which can be moved.
     *  This implementation always returns false.
     */
    public boolean hasDeletableFile() {
        return false;
    }
    
    /**
     *  Changes the current folder.
     */
    public void changeFolder(File folder) {
        startActivityForResult(getChangeFolderIntent(folder), REQ_CODE_FOLDER);
    }
    
    /**
     *  Creates the intent to change the folder.
     */
    Intent getChangeFolderIntent(File folder) {
        Intent intent = new Intent(getIntent());
        intent.putExtra(EXTRA_PATH, folder.toString());
        //intent.setComponent(getIntent().getComponent());
        intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        return intent;
    }

    /**
     *  Saves the current folder to the list of last folders.
     */
    public void copyFile() {
        LastFolders lastFolders = LastFolders.getInstance(this);
        lastFolders.put(path);
    }
    
    /**
     *  Saves the current folder to the list of last folders.
     */
    public void moveFile() {
        LastFolders lastFolders = LastFolders.getInstance(this);
        lastFolders.put(path);
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
        lastFoldersCategory.removeAll();
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
     *  Returns unique file name for this folder.
     *  If the filename is not exists in the current folder,
     *  it returns unchanged.
     *  Otherwise the integer suffix will be added to the filename:
     *  "-1", "-2" etc...
     */
    String getUniqueFileName(String fileName) {
        if (!new File(path, fileName).exists()) {
            return fileName;
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
        return newName;
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
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
        View content = getLayoutInflater().inflate(R.layout.edit_text_dialog, 
                (ViewGroup)findViewById(R.id.edit_text_dialog_root));
        final EditText edit = (EditText)content.findViewById(R.id.edit_text);
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
    
    /**
     *  Complete the action.
     */
    void complete(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            finish();
        }
    }

    /**
     *  Runs the first Runnable in the separated thread, 
     *  runs the second Runnable when the first finishes and the dialog closes.
     *  @param  dialogMessageId ID of the message to display on dialog
     *  @param  thread  action to run in the thread
     *  @param  onStop  action to call on thread stop and dialog close
     */
    protected void runWithProgress(int dialogMessageId, final Runnable thread, 
            final Runnable onStop) {
        final ProgressDialog progress = ProgressDialog.show(
                this, "", getString(dialogMessageId), true);
        if (onStop != null) {
            progress.setOnDismissListener(new OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    onStop.run();
                }
            });
        }
        progress.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                thread.run();
                progress.dismiss();
            }
        }).start();
    }

}

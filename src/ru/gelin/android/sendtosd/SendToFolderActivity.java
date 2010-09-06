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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
    /** Request code for directory traversing */
    public static final int REQ_CODE_FOLDER = 0;
    
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
            showFileNameDialog();
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
        intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        startActivityForResult(intent, REQ_CODE_FOLDER);
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
                saveFile();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    /**
     *  Saves the file.
     */
    void saveFile() {
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
            return;
        }
        complete();
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
            index++;
        } while (new File(path, newName).exists());
        fileName = newName;
    }
    
    /**
     *  Shows error message and exits the activity.
     */
    void error(int messageId) {
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show();
        setResult(RESULT_CANCELED);
        finish();
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
        case R.id.menu_preferences:
            startActivity(new Intent(this, PreferencesActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     *  Shows error message and exits the activity.
     */
    void error(int messageId, Throwable exception) {
        Log.e(TAG, exception.toString(), exception);
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show();
        setResult(RESULT_CANCELED);
        finish();
    }
    
    /**
     *  Complete the action.
     */
    void complete() {
        Toast.makeText(this, R.string.file_is_saved, Toast.LENGTH_LONG).show();
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

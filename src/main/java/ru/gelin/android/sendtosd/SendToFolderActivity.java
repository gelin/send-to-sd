package ru.gelin.android.sendtosd;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import ru.gelin.android.sendtosd.fs.StartPaths;
import ru.gelin.android.sendtosd.intent.IntentException;
import ru.gelin.android.sendtosd.intent.IntentInfo;
import ru.gelin.android.sendtosd.permissions.PermissionChecker;
import ru.gelin.android.sendtosd.permissions.StorageVolumeChecker;
import ru.gelin.android.sendtosd.preferences.action.CopyHerePreference;
import ru.gelin.android.sendtosd.preferences.action.FileSaver;
import ru.gelin.android.sendtosd.preferences.folder.FolderChanger;
import ru.gelin.android.sendtosd.preferences.folder.FolderPreference;
import ru.gelin.android.sendtosd.preferences.action.MoveHerePreference;
import ru.gelin.android.sendtosd.preferences.folder.MountPointFolderPreference;
import ru.gelin.android.sendtosd.preferences.folder.PathFolderPreference;
import ru.gelin.android.sendtosd.progress.DummyProgress;
import ru.gelin.android.sendtosd.progress.Progress;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static ru.gelin.android.sendtosd.PreferenceParams.*;
import static ru.gelin.android.sendtosd.Tag.TAG;

/**
 * Base class for activities to copy/move file/files to folder.
 * Responses for the directory listing and traversing.
 */
public abstract class SendToFolderActivity extends PreferenceActivity
    implements FileSaver, FolderChanger {

    /**
     * "Copy here" preference key
     */
    private static final String PREF_COPY_HERE = "copy_here";
    /**
     * "Move here" preference key
     */
    private static final String PREF_MOVE_HERE = "move_here";
    /**
     * Last folders preference category key
     */
    private static final String PREF_LAST_FOLDERS = "last_folders";
    /**
     * Mount points preference category key
     */
    private static final String PREF_MOUNT_POINTS = "mount_points";
    /**
     * "Folders" preference key
     */
    private static final String PREF_FOLDERS = "folders";

    /**
     * Key to store the current path
     */
    private static final String KEY_PATH = "path";
    /**
     * Key to store the path history
     */
    private static final String KEY_PATH_HISTORY = "path_history";

    /**
     * New Folder dialog ID
     */
    private static final int NEW_FOLDER_DIALOG = 0;
    /**
     * Copy progress dialog ID
     */
    static final int COPY_DIALOG = 1;
    /**
     * Move progress dialog ID
     */
    static final int MOVE_DIALOG = 2;

    /**
     * Index of the list head to use list as a stack
     */
    private static final int HEAD = 0;

    /**
     * Intent information
     */
    IntentInfo intentInfo;
    /**
     * Current path
     */
    File path;
    /**
     * History of paths
     */
    final List<File> pathHistory = new LinkedList<>();
    /**
     * List of current subfolders
     */
    private List<File> folders;
    /**
     * Wrapper for MediaScanner
     */
    MediaScanner mediaScanner;

    /**
     * Move here preference. Saved here to remove from or add to hierarchy.
     */
    private MoveHerePreference moveHerePreference;
    /**
     * Last folders preference. Saved here to remove from or add to hierarchy.
     */
    private Preference lastFoldersPreference;
    /**
     * Mount points preference. Saved here to show or hide.
     */
    private Preference mountPointsPreference;

    /**
     * Dialog to show the progress
     */
    volatile Progress progress = new DummyProgress();   //can be used from other threads

    /**
     * Permission checker.
     */
    final PermissionChecker permissions = new PermissionChecker(this);

    /**
     * StorageVolume access checker.
     */
    final StorageVolumeChecker volumeAccess = new StorageVolumeChecker(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);

        this.mediaScanner = new MediaScanner(this);
        addPreferencesFromResource(R.xml.folder_preferences);
        this.lastFoldersPreference = findPreference(PREF_LAST_FOLDERS);
        this.mountPointsPreference = findPreference(PREF_MOUNT_POINTS);
        this.moveHerePreference = (MoveHerePreference) findPreference(PREF_MOVE_HERE);
        if (getIntent() == null) {
            error(R.string.unsupported_intent);
            return;
        }

        try {
            this.intentInfo = getIntentInfo();
            this.intentInfo.log();
            this.path = this.intentInfo.getPath();
            new InitTask().execute();
        } catch (Throwable e) {
            error(R.string.unsupported_intent, e);
            return;
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_PATH)) {
                this.path = new File(savedInstanceState.getString(KEY_PATH));
            }
            if (savedInstanceState.containsKey(KEY_PATH_HISTORY)) {
                this.pathHistory.clear();
                @SuppressWarnings("unchecked")
                Collection<File> restoredHistory = (Collection<File>) savedInstanceState.getSerializable(KEY_PATH_HISTORY);
                this.pathHistory.addAll(restoredHistory);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_PATH, this.path.toString());
        outState.putSerializable(KEY_PATH_HISTORY, (Serializable) this.pathHistory);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLastFolders();
        updateMountPoints();
        volumeAccess.requestAccess(this.path);
        permissions.requestReadPermission();
    }

    class InitTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected Void doInBackground(Void... params) {
            onInit();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            onPostInit();
            setProgressBarIndeterminateVisibility(false);
            //Log.d(TAG, "history: " + SendToFolderActivity.this.pathHistory);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mediaScanner.disconnect();
    }

    /**
     * Creates IntentInfo.
     *
     * @throws IntentException if it's not possible to create intent info
     */
    abstract protected IntentInfo getIntentInfo() throws IntentException;

    /**
     * The method is called in a separate thread during the activity creation.
     * Avoid UI changes here!
     */
    protected void onInit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getExternalFilesDirs(null); // create a writable folder on external storages
        } else {
            getExternalFilesDir(null);  // create a writable folder on external storage
        }

        this.folders = getFolders(this.path);
    }

    /**
     * The method is called in the UI thread when {@link #onInit} finishes.
     * Fills folders list.
     * Enables Copy/Move Here for writable folders.
     * Hides Move Here for non-deletable files.
     */
    protected void onPostInit() {
        fillFolders();

        CopyHerePreference copyHerePreference = (CopyHerePreference) findPreference(PREF_COPY_HERE);
        Preference moveHere = findPreference(PREF_MOVE_HERE);

        if (hasMovableFile()) {
            if (moveHere == null) {
                getPreferenceScreen().addPreference(this.moveHerePreference);
            }
        } else {
            getPreferenceScreen().removePreference(this.moveHerePreference);
        }

        copyHerePreference.setFileSaver(this);
        this.moveHerePreference.setFileSaver(this);

        boolean enable = this.path.canWrite();
        copyHerePreference.setEnabled(enable);
        this.moveHerePreference.setEnabled(enable);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        MenuItem newFolderMenu = menu.findItem(R.id.menu_new_folder);
        if (newFolderMenu != null && this.path != null) {
            newFolderMenu.setEnabled(this.path.canWrite());
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_folder:
                showDialog(NEW_FOLDER_DIALOG);
                return true;
            case R.id.menu_preferences:
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case NEW_FOLDER_DIALOG: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.new_folder);
                View content = getLayoutInflater().inflate(R.layout.edit_text_dialog, null);
                final EditText edit = (EditText) content.findViewById(R.id.edit_text);
                builder.setView(content);
                builder.setPositiveButton(R.string.create_folder, new OnClickListener() {
                    //@Override
                    public void onClick(DialogInterface dialog, int which) {
                        createFolder(edit.getText().toString());
                    }
                });

                Dialog dialog = builder.create();
                //http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/java/android/preference/DialogPreference.java;h=bbad2b6d432ce44ad05ddbc44487000b150135ef;hb=HEAD
                Window window = dialog.getWindow();
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE |
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

                permissions.requestWritePermission();
                return dialog;
            }
            default:
                return null;
        }
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        switch (id) {
            case NEW_FOLDER_DIALOG: {
                AlertDialog alertDialog = (AlertDialog) dialog;
                final Button button = alertDialog.getButton(Dialog.BUTTON_POSITIVE);
                button.setEnabled(false);

                EditText edit = (EditText) dialog.findViewById(R.id.edit_text);
                edit.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        button.setEnabled(s.length() > 0);
                    }
                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        this.permissions.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        volumeAccess.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Returns the current folder.
     */
    public File getPath() {
        return this.path;
    }

    /**
     * Return true if the intent has movable file and it's reasonable to display a "Move To" action.
     * This implementation always returns false.
     */
    public boolean hasMovableFile() {
        return false;
    }

    /**
     * Changes the current folder.
     */
    public void changeFolder(File folder) {
        this.pathHistory.add(HEAD, this.path);
        this.path = folder;
        updateLastFolders();
        updateMountPoints();
        volumeAccess.requestAccess(this.path);
        new InitTask().execute();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //TODO: check, maybe it's possible to use Android 2.0 onBackPressed() method
        //Log.d(TAG, "key down: " + event);
        boolean result = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR &&
            keyCode == KeyEvent.KEYCODE_BACK) {
            result = backPress();
        }
        if (result == false) {
            result = super.onKeyDown(keyCode, event);
        }
        return result;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //Log.d(TAG, "key up: " + event);
        boolean result = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR &&
            keyCode == KeyEvent.KEYCODE_BACK) {
            result = backPress();
        }
        if (result == false) {
            result = super.onKeyUp(keyCode, event);
        }
        return result;
    }

    private boolean backPress() {
        if (this.pathHistory.isEmpty()) {
            return false;
        }
        File oldPath = this.pathHistory.remove(HEAD);
        this.path = oldPath;
        updateLastFolders();
        updateMountPoints();
        new InitTask().execute();
        return true;
    }

    /**
     * Saves the current folder to the list of last folders.
     */
    public void saveLastFolder() {
        LastFolders lastFolders = LastFolders.getInstance(this);
        lastFolders.put(this.path);
    }

    public abstract void copyFile();

    public abstract void moveFile();

    /**
     * Updates last folders group. Hides them if necessary.
     */
    private void updateLastFolders() {
        Preference existedLastFolders = findPreference(PREF_LAST_FOLDERS);
        if (this.intentInfo == null) {
            return; //not initialized, should be finished immediately from onCreate()
        }
        if (this.pathHistory.isEmpty()) {
            if (existedLastFolders == null) {
                getPreferenceScreen().addPreference(lastFoldersPreference);
            }
            listLastFolders();
        } else {
            if (existedLastFolders != null) {
                getPreferenceScreen().removePreference(lastFoldersPreference);
            }
        }
    }

    /**
     * Fills the list of last folders.
     */
    private void listLastFolders() {
        PreferenceCategory lastFoldersCategory =
            (PreferenceCategory) findPreference(PREF_LAST_FOLDERS);

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

        if (lastFoldersCategory.getPreferenceCount() <= 0) {
            getPreferenceScreen().removePreference(lastFoldersCategory);
        }
    }

    /**
     * Updates mount points group. Hides them if necessary.
     */
    private void updateMountPoints() {
        Preference existedMountPoints = findPreference(PREF_MOUNT_POINTS);
        if (this.intentInfo == null) {
            return; //not initialized, should be finished immediately from onCreate()
        }
        if (this.pathHistory.isEmpty()) {
            if (existedMountPoints == null) {
                getPreferenceScreen().addPreference(mountPointsPreference);
            }
            listMountPoints();
        } else {
            if (existedMountPoints != null) {
                getPreferenceScreen().removePreference(mountPointsPreference);
            }
        }
    }

    /**
     * Fills the list of mount points.
     */
    private void listMountPoints() {
        PreferenceCategory mountPointsCategory =
            (PreferenceCategory) findPreference(PREF_MOUNT_POINTS);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean(PREF_SHOW_MOUNT_POINTS, true)) {
            getPreferenceScreen().removePreference(mountPointsCategory);
            return;
        }

        List<File> mountPoints = new StartPaths(this, false).getPaths();
        if (mountPoints.isEmpty()) {
            getPreferenceScreen().removePreference(mountPointsCategory);
            return;
        }

        mountPointsCategory.removeAll();
        for (File folder : mountPoints) {
            Log.d(TAG, folder.toString());
            Preference folderPref = new MountPointFolderPreference(this, folder, this);
            mountPointsCategory.addPreference(folderPref);
        }
    }

    /**
     * Makes the sorted list of this folder subfolders.
     */
    private static List<File> getFolders(File path) {
        List<File> result = new ArrayList<>();
        File[] subFolders = path.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        if (subFolders == null) {
            return result;
        }
        List<File> sortedFolders = Arrays.asList(subFolders);
        Collections.sort(sortedFolders, new Comparator<File>() {
            public int compare(File file1, File file2) {
                return String.CASE_INSENSITIVE_ORDER.compare(file1.getName(), file2.getName());
            }
        });
        for (File subFolder : sortedFolders) {
            File folder;
            try {
                folder = subFolder.getCanonicalFile();
            } catch (IOException e) {
                folder = subFolder;
            }
            result.add(folder);
        }
        return result;
    }

    /**
     * Fills the list of subfolders.
     */
    private void fillFolders() {
        PreferenceCategory folders = (PreferenceCategory) findPreference(PREF_FOLDERS);
        folders.removeAll();
        if (!"/".equals(this.path.getAbsolutePath())) {
            Preference upFolder = new FolderPreference(this, this.path.getParentFile(), this);
            upFolder.setTitle("..");
            folders.addPreference(upFolder);
        }
        if (this.folders != null) {
            for (File folder : this.folders) {
                Preference folderPref = new FolderPreference(this, folder, this);
                folders.addPreference(folderPref);
            }
        }
    }

    /**
     * Runs getting of the folders list in a separate thread.
     * After updates the list of folders.
     */
    private void listFolders() {
        new ListFoldersTask().execute(this.path);
    }

    class ListFoldersTask extends AsyncTask<File, Void, List<File>> {
        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected List<File> doInBackground(File... params) {
            return getFolders(params[0]);
        }

        @Override
        protected void onPostExecute(List<File> result) {
            SendToFolderActivity.this.folders = result;
            fillFolders();
            setProgressBarIndeterminateVisibility(false);
        }
    }

    /**
     * Returns unique file name for this folder.
     * If the filename is not exists in the current folder,
     * it returns unchanged.
     * Otherwise the integer suffix will be added to the filename:
     * "-1", "-2" etc...
     */
    String getUniqueFileName(String fileName) {
        if (fileName == null) {
            Log.w(TAG, "filename is null");
            fileName = "";
        }
        if (this.path == null) {
            Log.w(TAG, "path is null");
            return fileName;
        }
        if (!new File(this.path, fileName).exists()) {
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
        } while (new File(this.path, newName).exists());
        return newName;
    }

    /**
     * Creates the new folder.
     */
    private void createFolder(String folderName) {
        File newFolder = new File(this.path, folderName);
        boolean result = newFolder.mkdirs();
        if (result) {
            Toast.makeText(this, R.string.folder_created, Toast.LENGTH_LONG).show();
            removeDialog(NEW_FOLDER_DIALOG);    //to clear folder name, don't expect to create more folders
            listFolders();
        } else {
            Toast.makeText(this, R.string.folder_not_created, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Shows the error message.
     */
    void warn(int messageId) {
        warn(messageId, null);
    }

    /**
     * Shows the error message and finishes the activity.
     */
    void error(int messageId) {
        error(messageId, null);
    }

    /**
     * Shows and logs the error message.
     *
     * @param messageId ID of the message to show
     * @param exception exception to write to logs (can be null)
     */
    void warn(int messageId, Throwable exception) {
        if (exception != null) {
            Log.w(TAG, exception.toString(), exception);
        }
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show();
    }

    /**
     * Shows and logs the error message and finished the activity
     * (with canceled result).
     *
     * @param messageId ID of the message to show
     * @param exception exception to write to logs (can be null)
     */
    void error(int messageId, Throwable exception) {
        if (exception != null) {
            Log.e(TAG, exception.toString(), exception);
        }
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * Complete the action.
     */
    void complete(int messageId) {
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * Complete the action.
     */
    void complete(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

}

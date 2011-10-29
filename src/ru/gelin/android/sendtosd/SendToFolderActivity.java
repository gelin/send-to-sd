package ru.gelin.android.sendtosd;

import static ru.gelin.android.sendtosd.PreferenceParams.DEFAULT_LAST_FOLDERS_NUMBER;
import static ru.gelin.android.sendtosd.PreferenceParams.DEFAULT_LAST_FOLDERS_NUMBER_INT;
import static ru.gelin.android.sendtosd.PreferenceParams.PREF_LAST_FOLDERS_NUMBER;
import static ru.gelin.android.sendtosd.PreferenceParams.PREF_SHOW_LAST_FOLDERS;
import static ru.gelin.android.sendtosd.Tag.TAG;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import ru.gelin.android.sendtosd.intent.IntentException;
import ru.gelin.android.sendtosd.intent.IntentInfo;
import ru.gelin.android.sendtosd.progress.DummyProgress;
import ru.gelin.android.sendtosd.progress.Progress;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

/**
 *  Base class for activities to copy/move file/files to folder.
 *  Responses for the directory listing and traversing.
 */
public abstract class SendToFolderActivity extends PreferenceActivity 
        implements FileSaver, FolderChanger {
    
    /** "Copy here" preference key */
    public static final String PREF_COPY_HERE = "copy_here";
    /** "Move here" preference key */
    public static final String PREF_MOVE_HERE = "move_here";
    /** Last folders preference category key */
    public static final String PREF_LAST_FOLDERS = "last_folders";
    /** "Folders" preference key */
    public static final String PREF_FOLDERS = "folders";
    
    /** Key to store the current path */
    static final String KEY_PATH = "path";
    /** Key to store the path history */
    static final String KEY_PATH_HISTORY = "path_history";
    
    /** New Folder dialog ID */
    static final int NEW_FOLDER_DIALOG = 0;
    /** Copy progress dialog ID */
    static final int COPY_DIALOG = 1;
    /** Move progress dialog ID */
    static final int MOVE_DIALOG = 2;
    
    /** Index of the list head to use list as a stack */
    static final int HEAD = 0;
    
    /** Intent information */
    IntentInfo intentInfo;
    /** Current path */
    File path;
    /** History of paths */
    List<File> pathHistory = new LinkedList<File>();
    /** List of current subfolders */
    List<File> folders;
    /** Wrapper for MediaScanner */
    MediaScanner mediaScanner;
    
    /** Move here preference. Saved here to remove from or add to hierarchy. */
    MoveHerePreference moveHerePreference;
    /** Last folders preference. Saved here to remove from or add to hierarchy. */
    Preference lastFoldersPreference;
    
    /** Dialog to show the progress */
    volatile Progress progress = new DummyProgress();   //can be used from other threads
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        this.mediaScanner = new MediaScanner(this);
        addPreferencesFromResource(R.xml.folder_preferences);
        this.lastFoldersPreference = findPreference(PREF_LAST_FOLDERS);
        this.moveHerePreference = (MoveHerePreference)findPreference(PREF_MOVE_HERE);
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
        		Collection<File> restoredHistory = (Collection<File>)savedInstanceState.getSerializable(KEY_PATH_HISTORY);
        		this.pathHistory.addAll(restoredHistory);
        	}
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putString(KEY_PATH, this.path.toString());
    	outState.putSerializable(KEY_PATH_HISTORY, (Serializable)this.pathHistory);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateLastFolders();
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
     *  Creates IntentInfo.
     *  @throws IntentException if it's not possible to create intent info
     */
    abstract protected IntentInfo getIntentInfo() throws IntentException;
    
    /**
     *  The method is called in a separate thread during the activity creation.
     *  Avoid UI changes here!
     */
    protected void onInit() {
        this.folders = getFolders(this.path);
    }
    
    /**
     *  The method is called in the UI thread when {@link #onInit} finishes.
     *  Fills folders list.
     *  Enables Copy/Move Here for writable folders.
     *  Hides Move Here for non-deletable files.
     */
    protected void onPostInit() {
        fillFolders();
        
        CopyHerePreference copyHerePreference = (CopyHerePreference)findPreference(PREF_COPY_HERE);
        Preference moveHere = findPreference(PREF_MOVE_HERE);
        
        if (hasDeletableFile()) {
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
            final EditText edit = (EditText)content.findViewById(R.id.edit_text);
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
            return dialog;
        }
        default:
            return null;
        }
    }
    
    /**
     *  Returns the current folder.
     */
    public File getPath() {
        return this.path;
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
    	this.pathHistory.add(HEAD, this.path);
    	this.path = folder;
    	updateLastFolders();
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
    
    boolean backPress() {
    	if (this.pathHistory.isEmpty()) {
        	return false;
        }
    	File oldPath = this.pathHistory.remove(HEAD);
    	this.path = oldPath;
    	updateLastFolders();
    	new InitTask().execute();
    	return true;
    }
    
    /**
     *  Saves the current folder to the list of last folders.
     */
    public void saveLastFolder() {
        LastFolders lastFolders = LastFolders.getInstance(this);
        lastFolders.put(this.path);
    }
    
    public abstract void copyFile();
    
    public abstract void moveFile();
    
    /** 
     * 	Updates last folders group. Hides them if necessary.
     */
    void updateLastFolders() {
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
        
        if (lastFoldersCategory.getPreferenceCount() <= 0) {
        	getPreferenceScreen().removePreference(lastFoldersCategory);
        }
    }

    /**
     *  Makes the sorted list of this folder subfolders.
     */
    static List<File> getFolders(File path) {
        List<File> result = new ArrayList<File>();
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
     *  Fills the list of subfolders.
     */
    void fillFolders() {
        PreferenceCategory folders = (PreferenceCategory)findPreference(PREF_FOLDERS);
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
     *  Runs getting of the folders list in a separate thread.
     *  After updates the list of folders.
     */
    void listFolders() {
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
     *  Returns unique file name for this folder.
     *  If the filename is not exists in the current folder,
     *  it returns unchanged.
     *  Otherwise the integer suffix will be added to the filename:
     *  "-1", "-2" etc...
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
     *  Creates the new folder.
     */
    void createFolder(String folderName) {
        File newFolder = new File(this.path, folderName);
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
        warn(messageId, null);
    }
    
    /**
     *  Shows the error message and finishes the activity.
     */
    void error(int messageId) {
        error(messageId, null);
    }
    
    /**
     *  Shows and logs the error message.
     *  @param  messageId   ID of the message to show
     *  @param  exception   exception to write to logs (can be null)
     */
    void warn(int messageId, Throwable exception) {
        if (exception != null) {
            Log.w(TAG, exception.toString(), exception);
        }
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show();
    }
    
    /**
     *  Shows and logs the error message and finished the activity
     *  (with canceled result).
     *  @param  messageId   ID of the message to show
     *  @param  exception   exception to write to logs (can be null)
     */
    void error(int messageId, Throwable exception) {
        if (exception != null) {
            Log.e(TAG, exception.toString(), exception);
        }
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show();
        finish();
    }
    
    /**
     *  Complete the action.
     */
    void complete(int messageId) {
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show();
        finish();
    }
    
    /**
     *  Complete the action.
     */
    void complete(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

}

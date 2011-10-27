package ru.gelin.android.sendtosd;

import static ru.gelin.android.sendtosd.Tag.TAG;

import java.io.File;

import ru.gelin.android.sendtosd.intent.IntentException;
import ru.gelin.android.sendtosd.intent.IntentFile;
import ru.gelin.android.sendtosd.intent.IntentInfo;
import ru.gelin.android.sendtosd.intent.SendIntentInfo;
import ru.gelin.android.sendtosd.progress.FileInfo;
import ru.gelin.android.sendtosd.progress.Progress;
import ru.gelin.android.sendtosd.progress.ProgressDialog;
import ru.gelin.android.sendtosd.progress.SingleCopyDialog;
import ru.gelin.android.sendtosd.progress.SingleMoveDialog;
import ru.gelin.android.sendtosd.progress.Progress.ProgressEvent;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

/**
 *  Activity which displays the list of folders
 *  and allows to copy/move the file to folder.
 */
public class SendActivity extends SendToFolderActivity
        implements FileSaver {

    /** Choose File Name dialog ID */
    static final int FILE_NAME_DIALOG = 10;
    
    /** Key to store the file name */
    static final String KEY_FILE_NAME = "file_name";
    
    /** File to save from intent */
    IntentFile intentFile;
    /** Filename to save */
    String fileName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.intentInfo == null) {
            return; //not initialized, should be finished immediately from super.onCreate()
        }
        try {
            SendIntentInfo sendIntentInfo = (SendIntentInfo)this.intentInfo;
            this.intentFile = sendIntentInfo.getFile();
            Log.d(TAG, String.valueOf(intentFile));
            this.fileName = sendIntentInfo.getFileName();
        } catch (Throwable e) {
            error(R.string.unsupported_file, e);
            return;
        }
        if (this.intentFile == null) {
            error(R.string.no_files);
            return;
        }
        if (savedInstanceState != null) {
        	if (savedInstanceState.containsKey(KEY_FILE_NAME)) {
        		this.fileName = savedInstanceState.getString(KEY_FILE_NAME);
        	}
        }
        setTitle(this.fileName);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putString(KEY_FILE_NAME, this.fileName);
    }
    
    @Override
    protected IntentInfo getIntentInfo() throws IntentException {
        return new SendIntentInfo(this, getIntent());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.send_options_menu, menu);
        MenuItem newFolderMenu = menu.findItem(R.id.menu_new_folder);
        if (newFolderMenu != null && this.path != null) {
            newFolderMenu.setEnabled(this.path.canWrite());
        }
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_choose_file_name:
            showDialog(FILE_NAME_DIALOG);
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case FILE_NAME_DIALOG:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.choose_file_name);
            View content = getLayoutInflater().inflate(R.layout.edit_text_dialog, null);
            final EditText edit = (EditText)content.findViewById(R.id.edit_text);
            edit.setText(this.fileName);
            builder.setView(content);
            builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
                //@Override
                public void onClick(DialogInterface dialog, int which) {
                    SendActivity.this.fileName = edit.getText().toString();
                    setTitle(SendActivity.this.fileName);
                }
            });
            Dialog dialog = builder.create();
            //http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/java/android/preference/DialogPreference.java;h=bbad2b6d432ce44ad05ddbc44487000b150135ef;hb=HEAD
            Window window = dialog.getWindow();
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE |
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            return dialog;
        case COPY_DIALOG: {
            ProgressDialog progress = new SingleCopyDialog(this);
            this.progress = progress;
            return progress;
        }
        case MOVE_DIALOG: {
            ProgressDialog progress = new SingleMoveDialog(this);
            this.progress = progress;
            return progress;
        }
        default:
            return super.onCreateDialog(id);
        }
    }

    /**
     *  Return true if the intent has deletable file which can be moved.
     *  This implementation returns true if the sending file is deletable.
     */
    public boolean hasDeletableFile() {
        if (this.intentFile == null) {
            return false;
        }
        return this.intentFile.isDeletable();
    }

    static enum Result {
        MOVED, COPIED, ERROR;
    }
    
    abstract class ProgressTask extends AsyncTask<IntentFile, ProgressEvent, Result> implements Progress {

    	protected Progress progress;
    	
		//from Progress interface
		public void progress(ProgressEvent event) {
			publishProgress(event);
		}
		
		@Override
		protected void onProgressUpdate(ProgressEvent... events) {
			//Log.d(TAG, events[0].type.toString());
			this.progress.progress(events[0]);
		}    	
    	
    }
    
    /**
     *  Copies the file.
     */
    @Override
    public void copyFile() {
    	new CopyFileTask().execute(this.intentFile);
    }
    
    class CopyFileTask extends ProgressTask {
    	
    	@Override
    	protected void onPreExecute() {
    		saveLastFolder();
    		showDialog(COPY_DIALOG);
    		this.progress = SendActivity.this.progress;
    	}
    	
		@Override
		protected Result doInBackground(IntentFile... params) {
			IntentFile intentFile = params[0];
			publishProgress(ProgressEvent.newSetFilesEvent(1));   //single file in this activity
            String uniqueFileName = getUniqueFileName(SendActivity.this.fileName);
            publishProgress(ProgressEvent.newNextFileEvent(
            		new FileInfo(uniqueFileName, intentFile.getSize())));
            try {
                intentFile.setProgress(this);
                File file = new File(SendActivity.this.path, uniqueFileName);
                intentFile.saveAs(file);
                SendActivity.this.mediaScanner.scanFile(file, intentFile.getType());
            } catch (Exception e) {
                Log.w(TAG, e.toString(), e);
                return Result.ERROR;
            }
            return Result.COPIED;
		}
		
		@Override
		protected void onPostExecute(Result result) {
            this.progress.progress(ProgressEvent.newCompleteEvent());
            removeDialog(COPY_DIALOG);
            switch (result) {
            case COPIED:
                complete(R.string.file_is_copied);
                break;
            case ERROR:
                warn(R.string.file_is_not_copied);
                break;
            }
		}

    }
    
    /**
     *  Moves the file.
     */
    @Override
    public void moveFile() {
    	new MoveFileTask().execute(this.intentFile);
    }
    
    class MoveFileTask extends ProgressTask {

    	@Override
    	protected void onPreExecute() {
    		saveLastFolder();
    		showDialog(MOVE_DIALOG);
    		this.progress = SendActivity.this.progress;
    	}
    	
		@Override
		protected Result doInBackground(IntentFile... params) {
			IntentFile intentFile = params[0];
            publishProgress(ProgressEvent.newSetFilesEvent(1));   //single file in this activity
            String uniqueFileName = getUniqueFileName(SendActivity.this.fileName);
            File dest = new File(SendActivity.this.path, uniqueFileName);
            if (intentFile.isMovable(dest)) {
                publishProgress(ProgressEvent.newNextFileEvent(new FileInfo(uniqueFileName)));
                try {
                    intentFile.moveTo(dest);
                    SendActivity.this.mediaScanner.scanFile(dest, intentFile.getType());
                    return Result.MOVED;
                } catch (Exception e) {
                    Log.w(TAG, e.toString(), e);
                    publishProgress(ProgressEvent.newUpdateFileEvent(
                    		new FileInfo(uniqueFileName, intentFile.getSize())));
                    return saveAndDeleteFile(intentFile, uniqueFileName);
                }
            } else {
                publishProgress(ProgressEvent.newNextFileEvent(
                		new FileInfo(uniqueFileName, intentFile.getSize())));
                return saveAndDeleteFile(intentFile, uniqueFileName);
            }
		}
		
		Result saveAndDeleteFile(IntentFile intentFile, String uniqueFileName) {
	        try {
	            intentFile.setProgress(this);
	            File dest = new File(SendActivity.this.path, uniqueFileName);
	            intentFile.saveAs(dest);
	            SendActivity.this.mediaScanner.scanFile(dest, intentFile.getType());
	        } catch (Exception e) {
	            Log.w(TAG, e.toString(), e);
	            return Result.ERROR;
	        }
	        try {
	            intentFile.delete();
	        } catch (Exception e) {
	            Log.w(TAG, e.toString(), e);
	            return Result.COPIED;
	        }
	        return Result.MOVED;
	    }
		
		@Override
		protected void onPostExecute(Result result) {
            this.progress.progress(ProgressEvent.newCompleteEvent());
            removeDialog(MOVE_DIALOG);
            switch (result) {
            case MOVED:
                complete(R.string.file_is_moved);
                break;
            case COPIED:
                complete(R.string.file_is_not_deleted);
                break;
            case ERROR:
                warn(R.string.file_is_not_moved);
                break;
            }
		}
		
    }
    
}

package ru.gelin.android.sendtosd;

import android.app.Dialog;
import android.os.AsyncTask;
import android.util.Log;
import ru.gelin.android.i18n.PluralForms;
import ru.gelin.android.sendtosd.intent.*;
import ru.gelin.android.sendtosd.progress.*;
import ru.gelin.android.sendtosd.progress.Progress.ProgressEvent;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

import static ru.gelin.android.sendtosd.Tag.TAG;

/**
 *  Activity which displays the list of folders
 *  and allows to copy/move multiple files to folder.
 */
public class SendMultipleActivity extends SendToFolderActivity {
    
    /** Files to save from intent */
    IntentFile[] intentFiles = new IntentFile[0];

    @Override
    protected IntentInfo getIntentInfo() throws IntentException {
        return new SendMultipleIntentInfo(this, getIntent());
    }
    
    @Override
    protected void onInit() {
        super.onInit();
        IntentFiles storage = IntentFiles.getInstance();
        if (this.pathHistory.isEmpty()) {
            try {
                this.intentFiles = ((SendMultipleIntentInfo)this.intentInfo).getFiles();
            } catch (IntentFileException e) {
                Log.e(TAG, "cannot get files list", e);
            }
            storage.init(this.intentFiles);
            for (IntentFile file : this.intentFiles) {
            	Log.d(TAG, String.valueOf(file));
            }
        } else {
            this.intentFiles = storage.getFiles();
        }
    }
    
    @Override
    protected void onPostInit() {
        if (this.intentFiles == null || this.intentFiles.length == 0) {
            error(R.string.no_files);
            return;
        }
        setTitle(MessageFormat.format(getString(R.string.files_title), 
                this.intentFiles.length, 
                PluralForms.getInstance().getForm(this.intentFiles.length)));
        super.onPostInit();
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case COPY_DIALOG: {
            ProgressDialog progress = new MultipleCopyDialog(this);
            this.progress = progress;
            return progress;
        }
        case MOVE_DIALOG: {
            ProgressDialog progress = new MultipleMoveDialog(this);
            this.progress = progress;
            return progress;
        }
        default:
            return super.onCreateDialog(id);
        }
    }

    /**
     *  Return true if the intent has deletable file which can be moved.
     *  This implementation returns true if one or more files are
     *  deletable.
     */
    public boolean hasDeletableFile() {
        if (this.intentFiles == null) {
            return false;
        }
        for (IntentFile file : this.intentFiles) {
            if (file.isDeletable()) {
                return true;
            }
        }
        return false;
    }

    static class Result {
        int moved = 0;
        int copied = 0;
        int errors = 0;
    }
    
    abstract class ProgressTask extends AsyncTask<IntentFile[], ProgressEvent, Result> implements Progress {

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
     *  Copies the files.
     */
    @Override
    public void copyFile() {
    	new CopyFileTask().execute(this.intentFiles);
    }
    
    class CopyFileTask extends ProgressTask {
    	
    	@Override
    	protected void onPreExecute() {
            saveLastFolder();
            showDialog(COPY_DIALOG);
    		this.progress = SendMultipleActivity.this.progress;
    	}
    	
    	@Override
    	protected Result doInBackground(IntentFile[]... params) {
    		IntentFile[] intentFiles = params[0];
    		Result result = new Result();
    		publishProgress(ProgressEvent.newSetFilesEvent(intentFiles.length));
            for (IntentFile file : intentFiles) {
                String uniqueFileName = getUniqueFileName(file.getName());
                publishProgress(ProgressEvent.newNextFileEvent(new FileInfo(uniqueFileName, file.getSize())));
                try {
                    file.setProgress(this);
                    File newFile = new File(SendMultipleActivity.this.path, uniqueFileName);
                    file.saveAs(newFile);
                    SendMultipleActivity.this.mediaScanner.scanFile(newFile, file.getType());
                } catch (Exception e) {
                    Log.w(TAG, e.toString(), e);
                    result.errors++;
                    continue;
                }
                result.copied++;
            }
            return result;
    	}
    	
    	@Override
    	protected void onPostExecute(Result result) {
            this.progress.progress(ProgressEvent.newCompleteEvent());
    		removeDialog(COPY_DIALOG);
            PluralForms plurals = PluralForms.getInstance();
            StringBuilder message = new StringBuilder();
            message.append(MessageFormat.format(
                    getString(R.string.files_are_copied),
                    result.copied, plurals.getForm(result.copied)));
            if (result.errors > 0) {
                message.append('\n');
                message.append(MessageFormat.format(
                        getString(R.string.errors_appeared),
                        result.errors, plurals.getForm(result.errors)));
            }
            complete(message.toString());
    	}
    	
    }
    
    /**
     *  Moves the files.
     */
    public void moveFile() {
    	new MoveFileTask().execute(this.intentFiles);
    }

    class MoveFileTask extends ProgressTask {
    	
    	@Override
    	protected void onPreExecute() {
            saveLastFolder();
            showDialog(MOVE_DIALOG);
            this.progress = SendMultipleActivity.this.progress;
    	}
    	
    	@Override
    	protected Result doInBackground(IntentFile[]... params) {
    		Result result = new Result();
    		IntentFile[] intentFiles = params[0];
            publishProgress(ProgressEvent.newSetFilesEvent(intentFiles.length));
            List<File> roots = new ExternalStorageRoots().getRoots();
            for (IntentFile file : intentFiles) {
                String uniqueFileName = getUniqueFileName(file.getName());
                File dest = new File(SendMultipleActivity.this.path, uniqueFileName);
                if (file.isMovable(dest, roots)) {
                    publishProgress(ProgressEvent.newNextFileEvent(new FileInfo(uniqueFileName)));
                    try {
                        file.moveTo(dest);
                        SendMultipleActivity.this.mediaScanner.scanFile(dest, file.getType());
                        result.moved++;
                    } catch (Exception e) {
                        Log.w(TAG, e.toString(), e);
                        publishProgress(ProgressEvent.newUpdateFileEvent(
                        		new FileInfo(uniqueFileName, file.getSize())));
                        saveAndDeleteFile(file, uniqueFileName, result);
                    }
                } else {
                    publishProgress(ProgressEvent.newNextFileEvent(new FileInfo(uniqueFileName, file.getSize())));
                    saveAndDeleteFile(file, uniqueFileName, result);
                }
            }
            return result;
    	}
    	
    	void saveAndDeleteFile(IntentFile file, String uniqueFileName, Result result) {
            try {
                file.setProgress(this);
                File dest = new File(SendMultipleActivity.this.path, uniqueFileName);
                file.saveAs(dest);
                SendMultipleActivity.this.mediaScanner.scanFile(dest, file.getType());
            } catch (Exception e) {
                Log.w(TAG, e.toString(), e);
                result.errors++;
                return;
            }
            try {
                file.delete();
            } catch (Exception e) {
                Log.w(TAG, e.toString(), e);
                result.copied++;
                return;
            }
            result.moved++;
        }
    	
    	@Override
    	protected void onPostExecute(Result result) {
            this.progress.progress(ProgressEvent.newCompleteEvent());
            removeDialog(MOVE_DIALOG);
            PluralForms plurals = PluralForms.getInstance();
            StringBuilder message = new StringBuilder();
            message.append(MessageFormat.format(
                    getString(R.string.files_are_moved),
                    result.moved, plurals.getForm(result.moved)));
            if (result.copied > 0) {
                message.append('\n');
                message.append(MessageFormat.format(
                        getString(R.string.files_are_only_copied),
                        result.copied, plurals.getForm(result.copied)));
            }
            if (result.errors > 0) {
                message.append('\n');
                message.append(MessageFormat.format(
                        getString(R.string.errors_appeared),
                        result.errors, plurals.getForm(result.errors)));
            }
            complete(message.toString());
    	}
    	
    }
    
}

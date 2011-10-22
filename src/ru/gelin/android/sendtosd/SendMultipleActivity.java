package ru.gelin.android.sendtosd;

import static ru.gelin.android.sendtosd.Tag.TAG;

import java.io.File;
import java.text.MessageFormat;

import ru.gelin.android.i18n.PluralForms;
import ru.gelin.android.sendtosd.SendActivity.Result;
import ru.gelin.android.sendtosd.intent.IntentException;
import ru.gelin.android.sendtosd.intent.IntentFile;
import ru.gelin.android.sendtosd.intent.IntentFileException;
import ru.gelin.android.sendtosd.intent.IntentInfo;
import ru.gelin.android.sendtosd.intent.SendMultipleIntentInfo;
import ru.gelin.android.sendtosd.progress.FileInfo;
import ru.gelin.android.sendtosd.progress.MultipleCopyDialog;
import ru.gelin.android.sendtosd.progress.MultipleMoveDialog;
import ru.gelin.android.sendtosd.progress.Progress;
import ru.gelin.android.sendtosd.progress.ProgressDialog;
import ru.gelin.android.sendtosd.progress.Progress.ProgressEvent;
import android.app.Dialog;
import android.os.AsyncTask;
import android.util.Log;

/**
 *  Activity which displays the list of folders
 *  and allows to copy/move multiple files to folder.
 */
public class SendMultipleActivity extends SendToFolderActivity {
    
    /** Files to save from intent */
    IntentFile[] intentFiles;

    @Override
    protected IntentInfo getIntentInfo() throws IntentException {
        return new SendMultipleIntentInfo(this, getIntent());
    }
    
    @Override
    protected void onInit() {
        super.onInit();
        IntentFiles storage = IntentFiles.getInstance();
        if (intentInfo.isInitial()) {
            try {
                intentFiles = ((SendMultipleIntentInfo)intentInfo).getFiles();
            } catch (IntentFileException e) {
                Log.e(TAG, "cannot get files list", e);
            }
            storage.init(intentFiles);
        } else {
            intentFiles = storage.getFiles();
        }
    }
    
    @Override
    protected void onPostInit() {
        if (intentFiles == null || intentFiles.length == 0) {
            error(R.string.no_files);
            return;
        }
        setTitle(MessageFormat.format(getString(R.string.files_title), 
                intentFiles.length, 
                PluralForms.getInstance().getForm(intentFiles.length)));
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
        if (intentFiles == null) {
            return false;
        }
        for (IntentFile file : intentFiles) {
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
			this.progress.equals(events[0]);
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
        saveLastFolder();
        final ResultHandler result = new ResultHandler();
        runWithProgress(MOVE_DIALOG,
            new Runnable() {
                //@Override
                public void run() {
                    progress.setFiles(intentFiles.length);
                    for (IntentFile file : intentFiles) {
                        String uniqueFileName = getUniqueFileName(file.getName());
                        File dest = new File(path, uniqueFileName);
                        if (file.isMovable(dest)) {
                            progress.nextFile(new FileInfo(uniqueFileName));
                            try {
                                file.moveTo(dest);
                                mediaScanner.scanFile(dest, file.getType());
                                result.moved++;
                            } catch (Exception e) {
                                Log.w(TAG, e.toString(), e);
                                progress.updateFile(new FileInfo(uniqueFileName, file.getSize()));
                                saveAndDeleteFile(uniqueFileName, file, result);
                            }
                        } else {
                            progress.nextFile(new FileInfo(uniqueFileName, file.getSize()));
                            saveAndDeleteFile(uniqueFileName, file, result);
                        }
                    }
                }
            },
            new Runnable() {
                //@Override
                public void run() {
                    progress.complete();
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
            });
    }

    void saveAndDeleteFile(String uniqueFileName, IntentFile file, ResultHandler result) {
        try {
            file.setProgress(progress);
            File dest = new File(path, uniqueFileName);
            file.saveAs(dest);
            mediaScanner.scanFile(dest, file.getType());
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
    
}

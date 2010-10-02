package ru.gelin.android.sendtosd;

import java.io.File;
import java.text.MessageFormat;

import ru.gelin.android.sendtosd.intent.IntentFile;
import ru.gelin.android.sendtosd.intent.SendMultipleIntentInfo;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.util.Log;

/**
 *  Activity which displays the list of folders
 *  and allows to copy/move multiple files to folder.
 */
public class SendMultipleActivity extends SendToFolderActivity {
    
    /** Files to save from intent */
    IntentFile[] intentFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        if (intent == null) {
            error(R.string.unsupported_files);
            return;
        }
        try {
            final SendMultipleIntentInfo intentInfo = new SendMultipleIntentInfo(this, intent);
            this.intentInfo = intentInfo;
            intentInfo.log();
            if (!intentInfo.validate()) {
                error(R.string.unsupported_files);
                return;
            }
            
            final IntentFiles storage = IntentFiles.getInstance();
            if (intentInfo.isInitial()) {

                runWithProgress(R.string.please_wait, 
                        new Runnable() {
                    @Override
                    public void run() {
                        intentFiles = intentInfo.getFiles();
                        storage.init(intentFiles);
                    }
                }, new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (intentFiles == null || intentFiles.length == 0) {
                            error(R.string.no_files);
                            return;
                        }
                        setTitle(MessageFormat.format(getString(R.string.files_title), intentFiles.length));
                    }
                });
                return;
                
            } else {
                intentFiles = storage.getFiles();
            }
        } catch (Throwable e) {
            error(R.string.unsupported_files);
            return;
        }
        if (intentFiles == null || intentFiles.length == 0) {
            error(R.string.no_files);
            return;
        }
        setTitle(MessageFormat.format(getString(R.string.files_title), intentFiles.length));
    }

    /**
     *  Return true if the intent has deletable file which can be moved.
     *  This implementation returns true if one or more files are
     *  deletable.
     */
    public boolean hasDeletableFile() {
        for (IntentFile file : intentFiles) {
            if (file.isDeletable()) {
                return true;
            }
        }
        return false;
    }

    /**
     *  Copies the files.
     */
    @Override
    public void copyFile() {
        super.copyFile();
        int copied = 0;
        int errors = 0;
        for (IntentFile file : intentFiles) {
            try {
                file.saveAs(new File(path, getUniqueFileName(file.getName())));
            } catch (Exception e) {
                Log.w(TAG, e.toString(), e);
                errors++;
                continue;
            }
            copied++;
        }
        complete(MessageFormat.format(getString(R.string.files_are_copied), 
                copied, errors));
    }
    
    /**
     *  Moves the files.
     */
    public void moveFile() {
        super.moveFile();
        int moved = 0;
        int copied = 0;
        int errors = 0;
        for (IntentFile file : intentFiles) {
            try {
                file.saveAs(new File(path, getUniqueFileName(file.getName())));
            } catch (Exception e) {
                Log.w(TAG, e.toString(), e);
                errors++;
                continue;
            }
            try {
                file.delete();
            } catch (Exception e) {
                Log.w(TAG, e.toString(), e);
                copied++;
                continue;
            }
            moved++;
        }
        complete(MessageFormat.format(getString(R.string.files_are_moved), 
                moved, copied, errors));
    }
    
    /**
     *  Runs the Runnable in the separated thread, closes progress dialog on finish.
     *  @param  dialogMessageId ID of the message to display on dialog
     *  @param  runnable    action to run in the thread
     *  @param  onStop  listener to call on thread stop and dialog close
     */
    void runWithProgress(int dialogMessageId, final Runnable runnable,
            OnDismissListener onStop) {
        final ProgressDialog progress = ProgressDialog.show(
                this, "", getString(dialogMessageId), true);
        progress.setOnDismissListener(onStop);
        new Thread(new Runnable() {
            @Override
            public void run() {
                runnable.run();
                progress.dismiss();
            }
        }).start();
    }

}

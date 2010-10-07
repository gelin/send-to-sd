package ru.gelin.android.sendtosd;

import java.io.File;
import java.text.MessageFormat;

import ru.gelin.android.sendtosd.intent.IntentFile;
import ru.gelin.android.sendtosd.intent.SendMultipleIntentInfo;
import android.content.Intent;
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
            onPostCreateIntentInfo();
            
            final IntentFiles storage = IntentFiles.getInstance();
            if (intentInfo.isInitial()) {
                runThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            intentFiles = intentInfo.getFiles();
                            storage.init(intentFiles);
                        }
                    },
                    new Runnable() {
                        public void run() {
                            onPostLoadFileInfo();
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
        onPostLoadFileInfo();
    }
    
    @Override
    protected void onPostLoadFileInfo() {
        if (intentFiles == null || intentFiles.length == 0) {
            error(R.string.no_files);
            return;
        }
        setTitle(MessageFormat.format(getString(R.string.files_title), intentFiles.length));
        super.onPostLoadFileInfo();
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

    static class ResultHandler {
        int moved = 0;
        int copied = 0;
        int errors = 0;
    }
    
    /**
     *  Copies the files.
     */
    @Override
    public void copyFile() {
        saveLastFolder();
        final ResultHandler result = new ResultHandler();
        runWithProgress(R.string.copying,
                new Runnable() {
                    @Override
                    public void run() {
                        for (IntentFile file : intentFiles) {
                            try {
                                file.saveAs(new File(path, getUniqueFileName(file.getName())));
                            } catch (Exception e) {
                                Log.w(TAG, e.toString(), e);
                                result.errors++;
                                continue;
                            }
                            result.copied++;
                        }
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        complete(MessageFormat.format(
                                getString(R.string.files_are_copied), 
                                result.copied, result.errors));
                    }
                });
    }
    
    /**
     *  Moves the files.
     */
    public void moveFile() {
        saveLastFolder();
        final ResultHandler result = new ResultHandler();
        runWithProgress(R.string.moving,
            new Runnable() {
                @Override
                public void run() {
                    for (IntentFile file : intentFiles) {
                        try {
                            file.saveAs(new File(path, getUniqueFileName(file.getName())));
                        } catch (Exception e) {
                            Log.w(TAG, e.toString(), e);
                            result.errors++;
                            continue;
                        }
                        try {
                            file.delete();
                        } catch (Exception e) {
                            Log.w(TAG, e.toString(), e);
                            result.copied++;
                            continue;
                        }
                        result.moved++;
                    }
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    complete(MessageFormat.format(
                            getString(R.string.files_are_moved), 
                            result.moved, result.copied, result.errors));
                }
            });
    }

}

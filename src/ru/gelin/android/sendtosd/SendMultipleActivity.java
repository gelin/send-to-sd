package ru.gelin.android.sendtosd;

import java.io.File;
import java.text.MessageFormat;

import ru.gelin.android.i18n.PluralForms;
import ru.gelin.android.sendtosd.intent.IntentFile;
import ru.gelin.android.sendtosd.intent.IntentInfo;
import ru.gelin.android.sendtosd.intent.SendMultipleIntentInfo;
import ru.gelin.android.sendtosd.progress.FileInfo;
import ru.gelin.android.sendtosd.progress.MultipleCopyDialog;
import ru.gelin.android.sendtosd.progress.MultipleMoveDialog;
import ru.gelin.android.sendtosd.progress.ProgressDialog;
import android.app.Dialog;
import android.util.Log;

/**
 *  Activity which displays the list of folders
 *  and allows to copy/move multiple files to folder.
 */
public class SendMultipleActivity extends SendToFolderActivity {
    
    /** Files to save from intent */
    IntentFile[] intentFiles;

    @Override
    protected IntentInfo getIntentInfo() {
        return new SendMultipleIntentInfo(this, getIntent());
    }
    
    @Override
    protected void onInit() {
        super.onInit();
        IntentFiles storage = IntentFiles.getInstance();
        if (intentInfo.isInitial()) {
            intentFiles = ((SendMultipleIntentInfo)intentInfo).getFiles();
            storage.init(intentFiles);
        } else {
            intentFiles = storage.getFiles();
        }
    }
    
    @Override
    protected void onPostInit() {
        super.onPostInit();
        if (intentFiles == null || intentFiles.length == 0) {
            error(R.string.no_files);
            return;
        }
        setTitle(MessageFormat.format(getString(R.string.files_title), 
                intentFiles.length, 
                PluralForms.getInstance().getForm(intentFiles.length)));
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
        runWithProgress(COPY_DIALOG,
                new Runnable() {
                    @Override
                    public void run() {
                        progress.setFiles(intentFiles.length);
                        for (IntentFile file : intentFiles) {
                            String uniqueFileName = getUniqueFileName(file.getName());
                            progress.nextFile(new FileInfo(uniqueFileName, file.getSize()));
                            try {
                                file.setProgress(progress);
                                file.saveAs(new File(path, uniqueFileName));
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
                        progress.complete();
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
                });
    }
    
    /**
     *  Moves the files.
     */
    public void moveFile() {
        saveLastFolder();
        final ResultHandler result = new ResultHandler();
        runWithProgress(MOVE_DIALOG,
            new Runnable() {
                @Override
                public void run() {
                    progress.setFiles(intentFiles.length);
                    for (IntentFile file : intentFiles) {
                        String uniqueFileName = getUniqueFileName(file.getName());
                        progress.nextFile(new FileInfo(uniqueFileName, file.getSize()));
                        try {
                            file.setProgress(progress);
                            file.saveAs(new File(path, uniqueFileName));
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

}

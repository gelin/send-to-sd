package ru.gelin.android.sendtosd;

import java.io.File;

import ru.gelin.android.sendtosd.donate.R;
import ru.gelin.android.sendtosd.intent.IntentException;
import ru.gelin.android.sendtosd.intent.IntentFile;
import ru.gelin.android.sendtosd.intent.IntentInfo;
import ru.gelin.android.sendtosd.intent.SendIntentInfo;
import ru.gelin.android.sendtosd.progress.FileInfo;
import ru.gelin.android.sendtosd.progress.ProgressDialog;
import ru.gelin.android.sendtosd.progress.SingleCopyDialog;
import ru.gelin.android.sendtosd.progress.SingleMoveDialog;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
    
    /** File to save from intent */
    IntentFile intentFile;
    /** Filename to save */
    String fileName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (intentInfo == null) {
            return; //not initialized, should be finished immediately from super.onCreate()
        }
        try {
            SendIntentInfo sendIntentInfo = (SendIntentInfo)this.intentInfo;
            intentFile = sendIntentInfo.getFile();
            fileName = sendIntentInfo.getFileName();
        } catch (Throwable e) {
            error(R.string.unsupported_file, e);
            return;
        }
        if (intentFile == null) {
            error(R.string.no_files);
            return;
        }
        setTitle(fileName);
    }
    
    @Override
    protected IntentInfo getIntentInfo() throws IntentException {
        return new SendIntentInfo(this, getIntent());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.send_options_menu, menu);
        MenuItem newFolderMenu = menu.findItem(R.id.menu_new_folder);
        if (newFolderMenu != null && path != null) {
            newFolderMenu.setEnabled(path.canWrite());
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
            View content = getLayoutInflater().inflate(R.layout.edit_text_dialog, 
                    (ViewGroup)findViewById(R.id.how_to_use_dialog_root));
            final EditText edit = (EditText)content.findViewById(R.id.edit_text);
            edit.setText(fileName);
            builder.setView(content);
            builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    fileName = edit.getText().toString();
                    setTitle(fileName);
                }
            });
            Dialog dialog = builder.create();
            //http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/java/android/preference/DialogPreference.java;h=bbad2b6d432ce44ad05ddbc44487000b150135ef;hb=HEAD
            Window window = dialog.getWindow();
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE |
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
        if (intentFile == null) {
            return false;
        }
        return intentFile.isDeletable();
    }

    /**
     *  Creates the intent to change the folder.
     */
    @Override
    Intent getChangeFolderIntent(File folder) {
        Intent intent = super.getChangeFolderIntent(folder);
        intent.putExtra(EXTRA_FILE_NAME, fileName);
        return intent;
    }
    
    static enum Result {
        MOVED, COPIED, ERROR;
    }
    
    static class ResultHandler {
        Result result;
    }
    
    /**
     *  Copies the file.
     */
    @Override
    public void copyFile() {
        saveLastFolder();
        final ResultHandler result = new ResultHandler();
        runWithProgress(COPY_DIALOG, 
                new Runnable() {
                    @Override
                    public void run() {
                        progress.setFiles(1);   //single file in this activity
                        String uniqueFileName = getUniqueFileName(fileName);
                        progress.nextFile(new FileInfo(uniqueFileName, intentFile.getSize()));
                        try {
                            intentFile.setProgress(progress);
                            File file = new File(path, uniqueFileName);
                            intentFile.saveAs(file);
                            mediaScanner.scanFile(file, intentFile.getType());
                        } catch (Exception e) {
                            Log.w(TAG, e.toString(), e);
                            result.result = Result.ERROR;
                            return;
                        }
                        result.result = Result.COPIED;
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        progress.complete();
                        switch (result.result) {
                        case COPIED:
                            complete(R.string.file_is_copied);
                            break;
                        case ERROR:
                            warn(R.string.file_is_not_copied);
                            break;
                        }
                    }
                });
    }
    
    /**
     *  Moves the file.
     */
    @Override
    public void moveFile() {
        saveLastFolder();
        final ResultHandler result = new ResultHandler();
        runWithProgress(MOVE_DIALOG, 
            new Runnable() {
                @Override
                public void run() {
                    progress.setFiles(1);   //single file in this activity
                    String uniqueFileName = getUniqueFileName(fileName);
                    File dest = new File(path, uniqueFileName);
                    if (intentFile.isMovable(dest)) {
                        progress.nextFile(new FileInfo(uniqueFileName));
                        try {
                            
                            intentFile.moveTo(dest);
                            mediaScanner.scanFile(dest, intentFile.getType());
                            result.result = Result.MOVED;
                        } catch (Exception e) {
                            Log.w(TAG, e.toString(), e);
                            progress.updateFile(new FileInfo(uniqueFileName, intentFile.getSize()));
                            result.result = saveAndDeleteFile(uniqueFileName);
                        }
                    } else {
                        progress.nextFile(new FileInfo(uniqueFileName, intentFile.getSize()));
                        result.result = saveAndDeleteFile(uniqueFileName);
                    }
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    progress.complete();
                    switch (result.result) {
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
            });
    }
    
    Result saveAndDeleteFile(String uniqueFileName) {
        try {
            intentFile.setProgress(progress);
            File dest = new File(path, uniqueFileName);
            intentFile.saveAs(dest);
            mediaScanner.scanFile(dest, intentFile.getType());
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

}

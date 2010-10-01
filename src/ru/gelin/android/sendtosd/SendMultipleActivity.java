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
            SendMultipleIntentInfo intentInfo = new SendMultipleIntentInfo(this, intent);
            this.intentInfo = intentInfo;
            intentInfo.log();
            if (!intentInfo.validate()) {
                error(R.string.unsupported_files);
                return;
            }
            intentFiles = intentInfo.getFiles();
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
                Log.w(TAG, e);
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
        //TODO
    }

}

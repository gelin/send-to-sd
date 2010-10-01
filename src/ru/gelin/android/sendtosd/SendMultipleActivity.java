package ru.gelin.android.sendtosd;

import ru.gelin.android.sendtosd.intent.IntentFile;
import ru.gelin.android.sendtosd.intent.SendMultipleIntentInfo;
import android.content.Intent;
import android.os.Bundle;

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
        //setTitle(fileName);   //TODO
    }

    /**
     *  Return true if the intent has deletable file which can be moved.
     *  This implementation returns true if one or more files are
     *  deletable.
     */
    public boolean hasDeletableFile() {
        return false;   //TODO
    }

    /**
     *  Copies the files.
     */
    @Override
    public void copyFile() {
        super.copyFile();
        //TODO
    }
    
    /**
     *  Moves the files.
     */
    public void moveFile() {
        super.moveFile();
        //TODO
    }

}

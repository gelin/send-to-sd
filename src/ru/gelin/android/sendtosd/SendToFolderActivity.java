package ru.gelin.android.sendtosd;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.webkit.MimeTypeMap;

/**
 *  Activity which displayes the list of folders
 *  and allows to save the file to folder.
 */
public class SendToFolderActivity extends Activity implements Constants {
    
    /** Filename to save */
    String fileName;
    /** Intent utilities */
    IntentUtils utils;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        if (intent == null) {
            error(R.string.unsupported_file);
            return;
        }
        utils = new IntentUtils(this, intent);
        utils.logIntentInfo();
        
        try {
            fileName = utils.getFileName();
        } catch (Exception e) {
            error(R.string.unsupported_file);
            return;
        }
        setTitle(fileName);
    }
    
    /**
     *  Shows error message and exits the activity.
     */
    void error(int messageId) {
        Toast.makeText(this, messageId, Toast.LENGTH_LONG);
        finish();
    }

}

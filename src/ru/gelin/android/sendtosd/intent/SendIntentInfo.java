package ru.gelin.android.sendtosd.intent;

import android.content.Context;
import android.content.Intent;

/**
 *  Extracts some necessary information from the SEND intent.
 */
public class SendIntentInfo extends IntentInfo {
    
    /**
     *  Creates the intent info.
     *  @param  context current context
     *  @param  intent  intent to process
     */
    public SendIntentInfo(Context context, Intent intent) {
        super(context, intent);
    }
    
    /**
     *  Returns false if this intent is not SEND or
     *  doesn't contain STREAM or TEXT extras.
     */
    @Override
    public boolean validate() {
        if (!Intent.ACTION_SEND.equals(intent.getAction())) {
            return false;
        }
        return super.validate();
    }

    /**
     *  Returns the filename provided with the intent as EXTRA_FILE_NAME
     *  or the filename of the sent file.
     */
    public String getFileName() {
        String fileName = intent.getStringExtra(EXTRA_FILE_NAME);
        if (fileName == null) {
            return getFile().getName();
        } else {
            return fileName;
        }
    }

    /**
     *  Returns the file provided with the SEND intent.
     */
    public IntentFile getFile() {
        return IntentFile.getInstance(context, intent);
    }

}

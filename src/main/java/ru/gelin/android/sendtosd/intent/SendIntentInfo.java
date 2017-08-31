package ru.gelin.android.sendtosd.intent;

import static ru.gelin.android.sendtosd.IntentParams.EXTRA_FILE_NAME;
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
     *  @throws IntentException if it's not possible to create the info from the intent
     */
    public SendIntentInfo(Context context, Intent intent) throws IntentException {
        super(context, intent);
    }
    
    /**
     *  Returns false if this intent is not SEND or
     *  doesn't contain STREAM or TEXT extras.
     */
    @Override
    boolean validate() {
        if (!Intent.ACTION_SEND.equals(this.intent.getAction())) {
            return false;
        }
        return super.validate();
    }

    /**
     *  Returns the filename provided with the intent as EXTRA_FILE_NAME
     *  or the filename of the sent file.
     */
    public String getFileName() throws IntentFileException {
        String fileName = this.intent.getStringExtra(EXTRA_FILE_NAME);
        if (fileName == null) {
            return getFile().getName();
        } else {
            return fileName;
        }
    }

    /**
     *  Returns the file provided with the SEND intent.
     */
    public IntentFile getFile() throws IntentFileException {
        return IntentFile.getInstance(this.context, this.intent);
    }

}

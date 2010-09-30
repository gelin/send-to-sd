package ru.gelin.android.sendtosd.intent;

import android.content.Context;
import android.content.Intent;

/**
 *  Extracts some necessary information from the SEND_MULTIPLE intent.
 */
public class SendMultipleIntentInfo extends IntentInfo {
    
    /**
     *  Creates the intent information.
     *  @param  context current context
     *  @param  intent  intent to process
     */
    public SendMultipleIntentInfo(Context context, Intent intent) {
        super(context, intent);
    }
    
    /**
     *  Returns false if this intent is not SEND_MULTIPLE or
     *  doesn't contain STREAM or TEXT extras.
     */
    public boolean validate() {
        if (!Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            return false;
        }
        return super.validate();
    }

    /**
     *  Returns the files provided with the SEND intent.
     */
    public IntentFile[] getFiles() {
        //TODO
        return new IntentFile[] {};
    }

    /**
     *  Logs debug information about the intent.
     */
    public void log() {
        super.log();
        /*
        if (intent.hasExtra(Intent.EXTRA_STREAM)) {
            List<Uri> uris = (List<Uri>)intent.getExtras().get(Intent.EXTRA_STREAM);
            for (Uri uri : uris) {
                Log.d(TAG, uri.toString());
            }
        }
        */
    }

}

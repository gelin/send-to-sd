package ru.gelin.android.sendtosd.intent;

import static ru.gelin.android.sendtosd.Tag.TAG;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 *  Extracts some necessary information from the SEND_MULTIPLE intent.
 */
public class SendMultipleIntentInfo extends IntentInfo {
    
    /**
     *  Creates the intent information.
     *  @param  context current context
     *  @param  intent  intent to process
     *  @throws IntentException if it's not possible to create the info from the intent
     */
    public SendMultipleIntentInfo(Context context, Intent intent) throws IntentException {
        super(context, intent);
    }
    
    /**
     *  Returns false if this intent is not SEND_MULTIPLE or
     *  doesn't contain STREAM or TEXT extras.
     */
    boolean validate() {
        if (!Intent.ACTION_SEND_MULTIPLE.equals(this.intent.getAction())) {
            return false;
        }
        return super.validate();
    }

    /**
     *  Returns the files provided with the SEND intent.
     */
    public IntentFile[] getFiles() throws IntentFileException {
        List<IntentFile> result = new ArrayList<IntentFile>();
        try {
            if (this.intent.hasExtra(Intent.EXTRA_TEXT)) {
                List<String> texts = this.intent.getStringArrayListExtra(Intent.EXTRA_TEXT);
                if (texts != null) {
                    for (String text : texts) {
                        try {
                            result.add(new TextFile(text));
                        } catch (Exception e) {
                            Log.w(TAG, e);
                        }
                    }
                }
            }
            if (this.intent.hasExtra(Intent.EXTRA_STREAM)) {
                List<Uri> uris = this.intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (uris != null) {
                    for (Uri uri : uris) {
                        try {
                            result.add(IntentFile.getInstance(context, uri));
                        } catch (Exception e) {
                            Log.w(TAG, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IntentFileException(e);
        }
        return result.toArray(new IntentFile[] {});
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

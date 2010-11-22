package ru.gelin.android.sendtosd.intent;

import java.io.File;
import java.io.IOException;

import ru.gelin.android.sendtosd.Constants;
import ru.gelin.android.sendtosd.progress.DummyProgress;
import ru.gelin.android.sendtosd.progress.Progress;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 *  A file provided with the intent to be saved on SD card.
 */
public abstract class IntentFile implements Constants {
    
    /** Object to display file operation progress. */
    volatile Progress progress = new DummyProgress();   //can be used from other threads
    
    /**
     *  Creates the concrete instance of the IntentFile from Intent.
     */
    public static IntentFile getInstance(Context context, Intent intent) {
        if (isText(intent)) {
            return new TextFile(intent);
        }
        Uri uri = getStreamUri(intent);
        String scheme = uri.getScheme();
        if ("file".equals(scheme)) {
            return new FileFile(context, intent);
        } else if ("content".equals(scheme)) {
            return new ContentFile(context, intent);
        }
        return new StreamFile(context, intent);
    }

    /**
     *  Creates the concrete instance of the IntentFile from Uri.
     */
    public static IntentFile getInstance(Context context, Uri uri) {
        String scheme = uri.getScheme();
        if ("file".equals(scheme)) {
            return new FileFile(context, uri);
        } else if ("content".equals(scheme)) {
            return new ContentFile(context, uri);
        }
        return new StreamFile(context, uri);
    }
    
    /**
     *  Sets the object to handle the progress of processing the file.
     */
    public void setProgress(Progress progress) {
        this.progress = progress;
    }
    
    /**
     *  Returns the file name. File name can be not unique.
     */
    abstract public String getName();
    
    /**
     *  Returns the file size, in bytes.
     *  Can return {@link Progress#UNKNOWN_SIZE}.
     */
    abstract public long getSize();
    
    /**
     *  Returns true if the file is deletable.
     */
    abstract public boolean isDeletable();
    
    /**
     *  Returns true if the file can be moved by one moveTo()
     *  operation instead of saveAs() and delete().
     */
    abstract public boolean isMovable();
    
    /**
     *  Saves the file as the specified location on SD card.
     *  @throws IOException if the file cannot be saved
     */
    abstract public void saveAs(File file) throws IOException;
    
    /**
     *  Moves the file to the specified location on SD card
     *  in one operation.
     *  @throws IOException if the file cannot be moved
     */
    abstract public void moveTo(File file) throws IOException;
    
    /**
     *  Deletes the original file.
     *  @throws IOException if the file cannot be deleted.
     */
    abstract public void delete() throws IOException;
    
    /**
     *  Returns true if the file is plain/text.
     */
    static boolean isText(Intent intent) {
        //return "text/plain".equals(intent.getType());
        return intent.hasExtra(Intent.EXTRA_TEXT) && 
                !intent.hasExtra(Intent.EXTRA_STREAM);  //stream is more preferable
    }
    
    /**
     *  Returns the Uri of the stream of the intent.
     */
    static Uri getStreamUri(Intent intent) {
        return (Uri)intent.getExtras().get(Intent.EXTRA_STREAM);
    }

}

package ru.gelin.android.sendtosd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;

/**
 *  A file provided with the intent to be saved on SD card.
 */
public class IntentFile {
    
    private static final String TEXT_FILE_NAME = "text.txt";
    
    /** The intent which contains information about the file */
    Intent intent;
    /** Current context */
    Context context;
    
    /**
     *  Creates the file for the SEND intent.
     */
    IntentFile(Context context, Intent intent) {
        this.context = context;
        this.intent = intent;
    }
    
    /**
     *  Tries to guess the filename from the intent.
     */
    public String getName() {
        if (isText()) {
            return TEXT_FILE_NAME;
        }
        Uri uri = getStreamUri();
        String fileName = uri.getLastPathSegment();
        if (fileName.contains(".")) {   //has extension
            return fileName;
        }
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(intent.getType());
        if (extension != null) {
            return fileName + "." + extension;
        }
        return fileName;
    }
    
    /**
     *  Saves the file as the specified file on SD card.
     */
    public void saveAs(File file) throws IOException {
        if (isText()) {
            Writer out = new FileWriter(file);
            out.write(getText());
            out.close();
        } else {
            InputStream in = getStream();
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int read;
            while ((read = in.read(buf)) > 0) {
                out.write(buf, 0, read);
            }
            out.close();
            in.close();
        }
    }
    
    /**
     *  Returns true if the file is plain/text.
     */
    boolean isText() {
        //return "text/plain".equals(intent.getType());
        return intent.hasExtra(Intent.EXTRA_TEXT) && 
                !intent.hasExtra(Intent.EXTRA_STREAM);  //stream is more preferable
    }
    
    /**
     *  Returns the Uri of the stream of the intent.
     */
    public Uri getStreamUri() {
        return (Uri)intent.getExtras().get(Intent.EXTRA_STREAM);
    }
    
    /**
     *  Returns the file text.
     */
    String getText() {
        return intent.getStringExtra(Intent.EXTRA_TEXT);
    }
    
    /**
     *  Returns the file as stream.
     */
    InputStream getStream() throws FileNotFoundException {
        Uri uri = getStreamUri();
        return context.getContentResolver().openInputStream(uri);
    }

}

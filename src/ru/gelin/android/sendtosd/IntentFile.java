package ru.gelin.android.sendtosd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

/**
 *  A file provided with the intent to be saved on SD card.
 */
public class IntentFile {
    
    private static final String TEXT_FILE_NAME = "text.txt";
    
    /** content:// URIs which are writable */
    static final String[] WRITABLE_URIS = {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString(),
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString(),
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString(),
    };
    
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
     *  Returns true if the file is file:// on filesystem.
     */
    boolean isFile() {
        if (isText()) {
            return false;
        }
        Uri uri = getStreamUri();
        return "file".equals(uri.getScheme());
    }
    
    /**
     *  Returns true if the file can be deleted.
     *  plain/text content cannot be deleted.
     *  file:// content can be deleted if the original file is writable.
     *  content:// is checked for supported providers.
     */
    boolean isDeletable() {
        if (isText()) {
            return false;
        } else if (isFile()) {
            try {
                Uri uri = getStreamUri();
                URI javaUri = new URI(uri.toString());
                File file = new File(javaUri);      //why so ugly???
                return file.isFile() && file.canWrite();
            } catch (Exception e) {
                return false;
            }
        } else {
            String uri = getStreamUri().toString();
            for (String contentUri : WRITABLE_URIS) {
                if (uri.startsWith(contentUri)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     *  Returns the Uri of the stream of the intent.
     */
    Uri getStreamUri() {
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

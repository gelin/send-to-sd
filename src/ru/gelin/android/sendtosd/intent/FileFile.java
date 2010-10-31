package ru.gelin.android.sendtosd.intent;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 *  File for file:// URI.
 *  The file is deletable if the original file contains is writable. 
 */
public class FileFile extends StreamFile {

    File file;
    
    public FileFile(Context context, Intent intent) {
        super(context, intent);
        file = getFile();
    }
    
    public FileFile(Context context, Uri uri) {
        super(context, uri);
        file = getFile();
    }
    
    /**
     *  Returns the file size.
     */
    @Override
    public long getSize() {
        return file.length();
    }
    
    /**
     *  Returns true if the original file is writable.
     */
    @Override
    public boolean isDeletable() {
        try {
            return file.isFile() && file.canWrite();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     *  Deletes the original file.
     */
    @Override
    public void delete() throws IOException {
        boolean result = file.delete();
        if (!result) {
            throw new IOException(file + " was not deleted");
        }
    }
    
    /**
     *  Returns the file as File for file:/// URIs
     */
    File getFile() {
        try {
            URI javaUri = new URI(uri.toString());
            return new File(javaUri);      //why so ugly???
        } catch (Exception e) {
            return null;
        }
    }

}

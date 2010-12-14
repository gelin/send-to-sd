package ru.gelin.android.sendtosd.intent;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

/**
 *  File for file:// URI.
 *  The file is deletable if the original file is writable.
 *  The file is movable if the original file is writable and
 *  is already located on SD card. 
 */
public class FileFile extends StreamFile {

    /** External storage directory, as string */
    static final String EX_STORAGE = 
            Environment.getExternalStorageDirectory().getAbsolutePath();

    File file;
    
    public FileFile(Context context, Intent intent) throws IntentFileException {
        super(context, intent);
        file = getFile();
    }
    
    public FileFile(Context context, Uri uri) throws IntentFileException {
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
     *  Returns true if the original file is writable
     *  and is already located on SD card.
     */
    @Override
    public boolean isMovable() {
        if (!isDeletable()) {
            return false;
        }
        if (file.getAbsolutePath().startsWith(EX_STORAGE)) {
            return true;
        }
        return false;
    }
    
    /**
     *  Moves the file using the filesystem operations.
     */
    @Override
    public void moveTo(File dest) throws IOException {
        boolean result = file.renameTo(dest);
        if (!result) {
            throw new IOException(file + " was not moved");
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
     *  @throws IntentFileException if the URI cannot be converted to file 
     */
    File getFile() throws IntentFileException {
        try {
            URI javaUri = new URI(uri.toString());
            return new File(javaUri);      //why so ugly???
        } catch (Exception e) {
            throw new IntentFileException("cannot convert URI to file", e);
        }
    }

}

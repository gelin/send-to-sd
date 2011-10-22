package ru.gelin.android.sendtosd.intent;

import java.io.File;
import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 *  File for file:// URI.
 *  The file is deletable if the original file is writable.
 *  The file is movable if the original file is writable and
 *  is already located on SD card. 
 */
public class FileFile extends AbstractFileFile {

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
            List<String> pathSegments = uri.getPathSegments();
            File result = new File("/");
            for (String segment : pathSegments) {
                result = new File(result, segment);
            }
            return result;
        } catch (Exception e) {
            throw new IntentFileException("cannot convert URI to file", e);
        }
    }

}

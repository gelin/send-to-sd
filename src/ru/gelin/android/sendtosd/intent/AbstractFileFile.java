package ru.gelin.android.sendtosd.intent;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

/**
 *  File which represents real file located on filesystem.
 *  The file is deletable if the original file is writable.
 *  The file is movable if the original file is writable and
 *  is already located on SD card. 
 */
public abstract class AbstractFileFile extends StreamFile {

    /** External storage directory, as string */
    static final String EX_STORAGE = 
            Environment.getExternalStorageDirectory().getAbsolutePath();

    File file;
    
    public AbstractFileFile(Context context, Intent intent) throws IntentFileException {
        super(context, intent);
    }
    
    public AbstractFileFile(Context context, Uri uri) throws IntentFileException {
        super(context, uri);
    }
    
    /**
     *  Returns true if the original file is writable
     *  and is already located on SD card and the move destination
     *  is located on SD card too.
     */
    @Override
    public boolean isMovable(File dest) {
        if (this.file == null) {
            return false;
        }
        if (dest == null) {
            return false;
        }
        if (!isDeletable()) {
            return false;
        }
        if (isOnExStorage(this.file) && isOnExStorage(dest)) {
            return true;
        }
        return false;
    }
    
    /**
     *  Returns true if the file is located on SD card.
     */
    static boolean isOnExStorage(File file) {
        if (file == null) {
            return false;
        }
        String path;
        try {
            path = file.getCanonicalPath();
        } catch (IOException e) {
            path = file.getAbsolutePath();
        }
        return path.startsWith(EX_STORAGE);
    }
    
    /**
     *  Moves the file using the filesystem operations.
     */
    @Override
    public void moveTo(File dest) throws IOException {
        boolean result = this.file.renameTo(dest);
        if (!result) {
            throw new IOException(this.file + " was not moved");
        }
    }

}

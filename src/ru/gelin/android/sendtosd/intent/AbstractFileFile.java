package ru.gelin.android.sendtosd.intent;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 *  File which represents real file located on filesystem.
 *  The file is deletable if the original file is writable.
 *  The file is movable if the original file is writable and
 *  is located under the same filesystem root as a destination.
 */
public abstract class AbstractFileFile extends StreamFile {

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
    public boolean isMovable(File dest, List<File> roots) {
        if (this.file == null) {
            return false;
        }
        if (dest == null) {
            return false;
        }
        if (roots == null || roots.isEmpty()) {
            return false;
        }
        if (!isDeletable()) {
            return false;
        }
        for (File root : roots) {
            if (areOnSameFilesystem(root, this.file, dest)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     *  Returns true if the files are on the same filesystem.
     */
    static boolean areOnSameFilesystem(File root, File source, File dest) {
        if (root == null || source == null || dest == null) {
            return false;
        }
        String rootPath = root.getPath();
        String sourcePath = getCanonicalPath(source);
        String destPath = getCanonicalPath(dest);
        return sourcePath.startsWith(rootPath) && destPath.startsWith(rootPath);
    }

    static String getCanonicalPath(File file) {
        String path;
        try {
            path = file.getCanonicalPath();
        } catch (IOException e) {
            path = file.getAbsolutePath();
        }
        return path;
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

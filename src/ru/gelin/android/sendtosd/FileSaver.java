package ru.gelin.android.sendtosd;

import java.io.File;

/**
 *  Interface for a class which can save the file.
 */
public interface FileSaver {
    
    /**
     *  Returns path to the current folder
     */
    public File getPath();
    
    /**
     *  Copies the file to the new location.
     */
    public void copyFile();
    
    /**
     *  Moves the file to the new location.
     */
    public void moveFile();

}

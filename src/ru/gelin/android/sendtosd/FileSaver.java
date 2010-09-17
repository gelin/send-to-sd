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
     *  Returns the file name to be saved in the folder
     */
    public String getFileName();
    /**
     *  Sets the file name to be saved in the folder.
     */
    public void setFileName(String fileName);
    
    /**
     *  Saves the file.
     */
    public void copyFile(); 

}

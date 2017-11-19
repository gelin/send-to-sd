package ru.gelin.android.sendtosd.preferences.action;

import java.io.File;

/**
 * Interface for a class which can save the file.
 */
public interface FileSaver {

    /**
     * Returns path to the current folder
     */
    File getPath();

    /**
     * Copies the file to the new location.
     */
    void copyFile();

    /**
     * Moves the file to the new location.
     */
    void moveFile();

}

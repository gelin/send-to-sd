package ru.gelin.android.sendtosd;

/**
 *  Interface to handle file copying/moving progress.
 */
public interface Progress {

    /** Unknown file size */
    public static final long UNKNOWN_SIZE = -1;
    
    /**
     *  Sets the number of files.
     */
    public void setFiles(int files);
    
    /**
     *  Starts processing the next file.
     */
    public void nextFile(long size);
    
    /**
     *  Mark next bytes of the current file as processed.
     */
    public void processBytes(long bytes);
    
}

package ru.gelin.android.sendtosd.progress;



/**
 *  Interface to handle file copying/moving progress.
 */
public interface Progress {

    /**
     *  Sets the number of files.
     */
    public void setFiles(int files);
    
    /**
     *  Starts processing the next file.
     */
    public void nextFile(File file);
    
    /**
     *  Updates the data for currently processing file.
     */
    public void updateFile(File file);
    
    /**
     *  Mark next bytes of the current file as processed.
     */
    public void processBytes(long bytes);
    
    /**
     *  Mark all as complete.
     */
    public void complete();
    
}

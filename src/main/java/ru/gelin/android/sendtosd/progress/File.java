package ru.gelin.android.sendtosd.progress;

/**
 *  A file with name and size.
 */
public interface File {
    
    /** Unknown file size */
    public static final long UNKNOWN_SIZE = -1;
    
    /**
     *  Returns the file name (without path).
     */
    public String getName();
    
    /**
     *  Returns the file size.
     *  Can returns {@link #UNKNOWN_SIZE}.
     */
    public long getSize();

}

package ru.gelin.android.sendtosd.progress;

/**
 *  Holds file name and size.
 */
public class FileInfo implements File {
    
    /** File name */
    String name;
    /** File size */
    long size = UNKNOWN_SIZE;
    
    /**
     *  Constructs information about file.
     */
    public FileInfo(String name, long size) {
        this.name = name;
        this.size = size;
    }
    
    /**
     *  Constructs information about file.
     *  The file size is unknown.
     */
    public FileInfo(String name) {
        this.name = name;
        this.size = UNKNOWN_SIZE;
    }
    
    /**
     *  Returns the file name (without path).
     */
    public String getName() {
        return this.name;
    }
    
    /**
     *  Returns the file size.
     *  Can returns {@link #UNKNOWN_SIZE}.
     */
    public long getSize() {
        return this.size;
    }

}

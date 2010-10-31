package ru.gelin.android.sendtosd;


/**
 *  Handles the progress of copying/moving files.
 *  Calls to Progress interface are safe from any thread, they are executed
 *  in UI thread.
 */
public class ProgressManager implements Progress {

    /** Number of files */
    int files = 1;
    /** Current file index */
    int file = -1;
    /** Current file size */
    long size = File.UNKNOWN_SIZE;
    /** Current file progress */
    long processed = 0;
    
    /**
     *  Private constructor. For tests.
     */
    ProgressManager() {
    }
    
    @Override
    public void setFiles(int files) {
        this.files = files;
        this.file = -1;
    }
    
    @Override
    public void nextFile(File file) {
        if (this.file < files) {
            this.file++;
            if (file != null) {
                this.size = file.getSize();
                this.processed = 0;
            }
        }
    }

    @Override
    public void processBytes(long bytes) {
        if (this.processed + bytes <= size) {
            this.processed += bytes;
        }
    }

}

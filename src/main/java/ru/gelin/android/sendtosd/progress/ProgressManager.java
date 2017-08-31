package ru.gelin.android.sendtosd.progress;

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
    /** SizeUnit to display the current file progress */
    SizeUnit unit;
    
    /**
     *  Private constructor. For tests.
     */
    ProgressManager() {
    }
    
	public void progress(ProgressEvent event) {
		switch (event.type) {
		case SET_FILES:
			setFiles(event.files);
			break;
		case NEXT_FILE:
			nextFile(event.file);
			break;
		case UPDATE_FILE:
			updateFile(event.file);
			break;
		case PROCESS_BYTES:
			processBytes(event.bytes);
			break;
		case COMPLETE:
			complete();
			break;
		}
	}
    
    void setFiles(int files) {
        this.files = files;
        this.file = -1;
    }
    
    /**
     *  Returns the total number of files to process.
     */
    public int getFiles() {
        return this.files;
    }
    
    void nextFile(File file) {
        if (this.file < files) {
            this.file++;
            if (file != null) {
                this.size = file.getSize();
                this.processed = 0;
                this.unit = findSizeUnit(this.size);
            }
        }
    }
    
    void updateFile(File file) {
        if (file != null) {
            this.size = file.getSize();
            this.processed = 0;
            this.unit = findSizeUnit(this.size);
        }
    }
    
    /**
     *  Returns the current processing file index.
     */
    public int getFile() {
        return this.file;
    }

    void processBytes(long bytes) {
        if (this.processed + bytes <= size) {
            this.processed += bytes;
        }
    }
    
    void complete() {
        this.file = this.files;
        this.processed = this.size;
    }
    
    /**
     *  Returns the SizeUnit to display the current file progress.
     */
    public SizeUnit getSizeUnit() {
        return unit;
    }
    
    /**
     *  Returns the current file size in units.
     */
    public float getSizeInUnits() {
        if (this.size == File.UNKNOWN_SIZE) {
            return File.UNKNOWN_SIZE;
        }
        return this.size / this.unit.multiplier;
    }
    
    /**
     *  Returns the current file progress in units.
     */
    public float getProgressInUnits() {
        if (this.size == File.UNKNOWN_SIZE) {
            return File.UNKNOWN_SIZE;
        }
        return this.processed / this.unit.multiplier;
    }
    
    /**
     *  Finds most appropriate size unit for the file.
     */
    SizeUnit findSizeUnit(long size) {
        for (SizeUnit unit : SizeUnit.values()) {
            if (size > unit.limit) {
                return unit;
            }
        }
        return SizeUnit.NULL;
    }

}

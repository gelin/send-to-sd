package ru.gelin.android.sendtosd.progress;

import ru.gelin.android.sendtosd.R;

public enum SizeUnit {
    
    MEGABYTE(1024 * 1024, 1 * 1024 * 1024, R.string.mb_progress),
    KILOBYTE(1024, 1 * 1024, R.string.kb_progress),
    BYTE(1, 1, R.string.b_progress),
    NULL(1, 0, R.string.dummy);
    
    /**
     *  How many bytes in one unit?
     */
    final long multiplier;
    /**
     *  Limit of the file size, starting from which to use this unit.
     */
    final long limit;
    /**
     *  ID of the resource string to display the progress.
     */
    final int progressString;
    
    SizeUnit(long multiplier, long limit, int progressString) {
        this.multiplier = multiplier;
        this.limit = limit;
        this.progressString = progressString;
    }

}

package ru.gelin.android.sendtosd;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

public class CopyHerePreference extends Preference {
    
    /** File saver */
    FileSaver fileSaver;
    
    public CopyHerePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     *  Sets the file saver.
     */
    public void setFileSaver(FileSaver fileSaver) {
        this.fileSaver = fileSaver;
        setSummary(fileSaver.getPath().toString());
    }
    
    @Override
    protected void onClick() {
        fileSaver.copyFile();
    }

}

package ru.gelin.android.sendtosd;

import android.content.Context;
import android.util.AttributeSet;

public class MoveHerePreference extends CopyHerePreference {

    public MoveHerePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    protected void onClick() {
        fileSaver.moveFile();
    }

}

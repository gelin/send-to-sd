package ru.gelin.android.sendtosd;

import android.content.Context;
import android.util.AttributeSet;

/**
 *  Subclass of DialogPreference to instantiate it.
 *  {@link http://code.google.com/p/android/issues/detail?id=3972}
 */
public class DialogPreference extends android.preference.DialogPreference {

    public DialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DialogPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

}

package ru.gelin.android.sendtosd;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 *  List preference which displays the selected value as Summary.
 */
public class SummaryListPreference extends ListPreference {

    public SummaryListPreference(Context context) {
        super(context);
    }
    public SummaryListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    public void setValue(String value) {
        super.setValue(value);
        setSummary(getEntry());
    }

}

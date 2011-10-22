package ru.gelin.android.sendtosd;

import static ru.gelin.android.sendtosd.Tag.TAG;
import java.text.MessageFormat;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;

/**
 *  List preference which displays the selected value as Summary.
 */
public class LastFoldersNumberPreference extends ListPreference {

    public LastFoldersNumberPreference(Context context) {
        super(context);
    }
    public LastFoldersNumberPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        super.onSetInitialValue(restoreValue, defaultValue);
        setSummary(formatSummary());
    }
    
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        setSummary(formatSummary());
    }
    
    String formatSummary() {
        try {
            return MessageFormat.format(
                    getContext().getString(R.string.show_last_folders_number),
                    Integer.parseInt(getValue()));
        } catch (Exception e) {
            Log.w(TAG, "failed to format summary", e);
            return "";
        }
    }

}

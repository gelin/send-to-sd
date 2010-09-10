package ru.gelin.android.sendtosd;

import android.content.Context;
import android.os.Parcelable;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

public class SaveHerePreference extends EditTextPreference {
    
    /** File saver */
    FileSaver fileSaver;
    
    public SaveHerePreference(Context context, AttributeSet attrs) {
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
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        EditText fileNameEdit = getEditText();
        fileNameEdit.setText(fileSaver.getFileName());
    }
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            EditText fileNameEdit = getEditText();
            fileSaver.setFileName(fileNameEdit.getText().toString());
            fileSaver.saveFile();
        }
        super.onDialogClosed(positiveResult);
    }
}

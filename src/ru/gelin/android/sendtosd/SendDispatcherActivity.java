package ru.gelin.android.sendtosd;

import static ru.gelin.android.sendtosd.PreferenceParams.DEFAULT_VIEW_TYPE;
import static ru.gelin.android.sendtosd.PreferenceParams.PREF_VIEW_TYPE;
import ru.gelin.android.sendtosd.PreferenceParams.ViewType;
import ru.gelin.android.sendtosd.donate.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class SendDispatcherActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent(); 
        String action = intent.getAction();
        ViewType view = ViewType.valueOf(PreferenceManager.getDefaultSharedPreferences(this).
        		getString(PREF_VIEW_TYPE, DEFAULT_VIEW_TYPE)); 
        if (Intent.ACTION_SEND.equals(action)) {
            intent.setClass(this, 
            		ViewType.DIALOG.equals(view) ? SendDialogActivity.class : SendActivity.class);
            startActivity(intent);
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            intent.setClass(this, 
            		ViewType.DIALOG.equals(view) ? SendMultipleDialogActivity.class : SendMultipleActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, R.string.unsupported_intent, Toast.LENGTH_LONG).show();
        }
        finish();
    }

}

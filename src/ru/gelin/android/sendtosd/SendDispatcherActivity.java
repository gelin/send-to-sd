package ru.gelin.android.sendtosd;

import ru.gelin.android.sendtosd.donate.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class SendDispatcherActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent(); 
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            intent.setClass(this, SendActivity.class);
            startActivity(intent);
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            intent.setClass(this, SendMultipleActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, R.string.unsupported_intent, Toast.LENGTH_LONG).show();
        }
        finish();
    }

}

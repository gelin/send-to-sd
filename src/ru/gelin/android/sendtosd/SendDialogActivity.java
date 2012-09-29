package ru.gelin.android.sendtosd;

import android.os.Bundle;

/**
 * 	Version of SendActivity, which is better displayed as dialog window.  
 */
public class SendDialogActivity extends SendActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PopupDialogUtil.showAsPopup(this);
        super.onCreate(savedInstanceState);

    }

}

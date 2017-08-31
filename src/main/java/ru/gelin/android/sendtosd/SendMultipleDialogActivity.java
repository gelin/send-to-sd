package ru.gelin.android.sendtosd;

import android.os.Bundle;

/**
 * 	Version of SendMultipleActivity, which is better displayed as dialog window. 
 */
public class SendMultipleDialogActivity extends SendMultipleActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PopupDialogUtil.showAsPopup(this);
        super.onCreate(savedInstanceState);
    }

}

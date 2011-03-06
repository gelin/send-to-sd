package ru.gelin.android.sendtosd.progress;

import ru.gelin.android.sendtosd.donate.R;
import android.app.Activity;

public class MultipleMoveDialog extends MultipleProgressDialog {

    public MultipleMoveDialog(Activity activity) {
        super(activity);
        setTitle(R.string.moving);
    }

}

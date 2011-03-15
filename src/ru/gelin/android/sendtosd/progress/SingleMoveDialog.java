package ru.gelin.android.sendtosd.progress;

import ru.gelin.android.sendtosd.R;
import android.app.Activity;

public class SingleMoveDialog extends SingleProgressDialog {

    public SingleMoveDialog(Activity activity) {
        super(activity);
        setTitle(R.string.moving);
    }

}

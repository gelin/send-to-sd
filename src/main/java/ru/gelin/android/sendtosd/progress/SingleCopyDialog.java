package ru.gelin.android.sendtosd.progress;

import ru.gelin.android.sendtosd.R;
import android.app.Activity;

public class SingleCopyDialog extends SingleProgressDialog {

    public SingleCopyDialog(Activity activity) {
        super(activity);
        setTitle(R.string.copying);
    }

}

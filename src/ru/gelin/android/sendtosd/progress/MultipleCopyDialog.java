package ru.gelin.android.sendtosd.progress;

import ru.gelin.android.sendtosd.R;
import android.app.Activity;

public class MultipleCopyDialog extends MultipleProgressDialog {

    public MultipleCopyDialog(Activity activity) {
        super(activity);
        setTitle(R.string.copying);
    }

}

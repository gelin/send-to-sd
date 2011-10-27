package ru.gelin.android.sendtosd.progress;

import ru.gelin.android.sendtosd.donate.R;
import android.app.Activity;

public class SingleProgressDialog extends ProgressDialog {

    protected SingleProgressDialog(Activity activity) {
        super(activity);
        setContentView(getLayoutInflater().inflate(R.layout.single_progress_dialog, null));
    }

}

package ru.gelin.android.sendtosd.progress;

import ru.gelin.android.sendtosd.R;
import android.app.Activity;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MultipleProgressDialog extends ProgressDialog {

    protected MultipleProgressDialog(Activity activity) {
        super(activity);
        setContentView(getLayoutInflater().inflate(R.layout.progress_dialog, null));
    }
    
    @Override
    void updateTotalProgress() {
        ProgressBar progress = (ProgressBar)findViewById(R.id.total_progress);
        progress.setMax(manager.getFiles());
        progress.setProgress(manager.getFile());
        TextView text = (TextView)findViewById(R.id.total_files);
        text.setText(getContext().getString(R.string.files_progress,
                manager.getFile(), manager.getFiles()));
    }

}

package ru.gelin.android.sendtosd.progress;

import ru.gelin.android.sendtosd.donate.R;
import android.app.Activity;
import android.app.Dialog;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ProgressDialog extends Dialog implements Progress {

    /** Activity for which the dialog is created */
    Activity activity;
    
    /** Progress manager for the dialog */
    ProgressManager manager = new ProgressManager();
    
    /** Last tooOften() check. */
    long lastTooOftenCheck = 0;
    /** Interval which is not too often */
    static long TOO_OFTEN_INTERVAL = 1000;  //1 second
    
    /**
     *  Creates the customized progress dialog for
     *  activity.
     */
    protected ProgressDialog(Activity activity) {
        super(activity);
        setCancelable(false);
        this.activity = activity;
    }
    
	public void progress(ProgressEvent event) {
		manager.progress(event);
		switch (event.type) {
		case SET_FILES:
			updateTotalProgress();
			break;
		case NEXT_FILE:
			updateFileName(event.file);
            updateFileProgress();
            updateTotalProgress();
			break;
		case UPDATE_FILE:
            updateFileName(event.file);
            updateFileProgress();
            //updateTotalProgress();
			break;
		case PROCESS_BYTES:
			if (tooOften()) {
                break;
            }
			updateFileProgress();
			break;
		case COMPLETE:
			updateFileProgress();
            updateTotalProgress();
            break;
		}
	}

    /**
     *  Here does nothing because there is no total progress when sending one file.
     */
    void updateTotalProgress() {
    }

    void updateFileName(File file) {
        if (file == null) {
            return;
        }
        TextView view = (TextView)findViewById(R.id.file_name);
        view.setText(file.getName());
    }
    
    void updateFileProgress() {
        ProgressBar progress = (ProgressBar)findViewById(R.id.file_progress);
        if (manager.getProgressInUnits() < 0) {
            progress.setIndeterminate(true);
        } else {
            progress.setIndeterminate(false);
            progress.setMax((int)(manager.getSizeInUnits() * 10));
            progress.setProgress((int)(manager.getProgressInUnits() * 10));
        }
        TextView text = (TextView)findViewById(R.id.file_size);
        text.setText(getContext().getString(manager.getSizeUnit().progressString,
                manager.getProgressInUnits(), manager.getSizeInUnits()));
    }
    
    /**
     *  Checks the amount of time passed from the last call to this method,
     *  if the method is called more frequently than once per second, returns
     *  true.
     */
    boolean tooOften() {
        long now = System.currentTimeMillis();
        if (now - lastTooOftenCheck < TOO_OFTEN_INTERVAL) {
            return true;
        }
        lastTooOftenCheck = now;
        return false;
    }

}

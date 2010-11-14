package ru.gelin.android.sendtosd.progress;

import ru.gelin.android.sendtosd.R;
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

    @Override
    public void setFiles(int files) {
        synchronized (manager) {
            manager.setFiles(files);
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                synchronized (manager) {
                    updateTotalProgress();
                }
            }
        });
    }
    
    @Override
    public void nextFile(final File file) {
        synchronized (manager) {
            manager.nextFile(file);
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                synchronized (manager) {
                    updateFileName(file);
                    updateFileProgress();
                    updateTotalProgress();
                }
            }
        });
    }

    @Override
    public void processBytes(long bytes) {
        synchronized (manager) {
            manager.processBytes(bytes);
            if (tooOften()) {
                return;
            }
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                synchronized (manager) {
                    updateFileProgress();
                }
            }
        });
    }
    
    @Override
    public void complete() {
        synchronized (manager) {
            manager.complete();
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                synchronized (manager) {
                    updateFileProgress();
                    updateTotalProgress();
                }
            }
        });
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

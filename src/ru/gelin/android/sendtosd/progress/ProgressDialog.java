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
    
    /**
     *  Creates the customized progress dialog for
     *  activity.
     */
    protected ProgressDialog(Activity activity) {
        super(activity);
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

    void updateTotalProgress() {
        ProgressBar progress = (ProgressBar)findViewById(R.id.total_progress);
        progress.setMax(manager.getFiles());
        progress.setProgress(manager.getFile());
        TextView text = (TextView)findViewById(R.id.total_files);
        text.setText(getContext().getString(R.string.files_progress,
                manager.getFile(), manager.getFiles()));
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

}

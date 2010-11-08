package ru.gelin.android.sendtosd.progress;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ProgressDialog extends AlertDialog implements Progress {

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
        ProgressBar progress = findViewById(R.id.total_progress);
        progress.setMax(manager.getFiles());
        progress.setProgress(manager.getFile());
        TextView text = findViewById(R.id.total_files);
        text.setText(getContext().getString(R.string.files_progress,
                manager.getFile(), manager.getFiles()));
    }
    
    void updateFileName(File file) {
        if (file == null) {
            return;
        }
        TextView view = findViewById(R.id.file_name);
        view.setText(file.getName());
    }
    
    void updateFileProgress() {
        ProgressBar progress = findViewById(R.id.file_progress);
        if (manager.getProgressInUnits() < 0) {
            progress.setIndeterminate(true);
        } else {
            progress.setIndeterminate(false);
            progress.setMax((int)(manager.getSizeInUnits() * 10));
            progress.setProgress((int)(manager.getProgressInUnits() * 10));
        }
        TextView text = findViewById(R.id.file_size);
        text.setText(getContext().getString(manager.getSizeUnit().progressString,
                manager.getProgressInUnits(), manager.getSizeInUnits()));
    }

}

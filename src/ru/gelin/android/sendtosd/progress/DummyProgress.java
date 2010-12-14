package ru.gelin.android.sendtosd.progress;

/**
 *  "Null object" for progress.
 *  Does nothing.
 */
public class DummyProgress implements Progress {

    @Override
    public void setFiles(int files) {
    }

    @Override
    public void nextFile(File file) {
    }
    
    @Override
    public void updateFile(File file) {
    }

    @Override
    public void processBytes(long bytes) {
    }

    @Override
    public void complete() {
    }

}

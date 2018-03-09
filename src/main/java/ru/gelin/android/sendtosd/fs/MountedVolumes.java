package ru.gelin.android.sendtosd.fs;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Takes care to find mounts of the internal or external volumes.
 */
public class MountedVolumes implements PathsSource {

    private final List<File> mounts = new ArrayList<>();

    /**
     * Scans external storages which are available.
     * Adds {@link Environment#getExternalStorageDirectory()}.
     * Also tries to get mounts from {@link Context#getExternalFilesDirs(String)}.
     */
    public MountedVolumes(Context context) {
        addPrimaryExternalStorage();
        addExternalFilesDirs(context);
    }

    /**
     * Returns found mounts.
     * @return the list of mounted and writable external storage mounts.
     */
    public List<File> getPaths() {
        return Collections.unmodifiableList(this.mounts);
    }

    private void addMount(File path) {
        if (this.mounts.contains(path)) {
            return;
        }
        this.mounts.add(path);
    }

    private void addPrimaryExternalStorage() {
        File path = Environment.getExternalStorageDirectory();
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            addMount(path.getAbsoluteFile());     // always add this path, if mounted
        }
    }

    private void addExternalFilesDirs(Context context) {
        List<File> dirs = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            dirs.addAll(Arrays.asList(context.getExternalFilesDirs(null)));
        } else {
            dirs.add(context.getExternalFilesDir(null));
        }
        for (File dir : dirs) {
            if (dir == null) {
                continue;
            }
            addMount(findReadableParent(dir.getAbsoluteFile()));    // add most common parents
        }
        for (File dir : dirs) {
            if (dir == null) {
                continue;
            }
            addMount(dir);      // add available files dirs
        }
    }

    private File findReadableParent(File file) {
        File parent = file.getParentFile();
        if (parent == null) {
            return file;
        }
        if (!parent.canRead() && file.canRead()) {    // TODO: readable or writable?
            return file;
        }
        return findReadableParent(parent);
    }

}

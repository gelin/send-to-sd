package ru.gelin.android.sendtosd.intent;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Takes care on find roots of possible mounted external storages.
 */
@Deprecated
public class ExternalStorageRoots {

    private static final File MOUNT_FILE = new File("/proc/mounts");

    private static final Set<String> FILESYSTEMS = new HashSet<>();

    static {
        FILESYSTEMS.add("ext4");
        FILESYSTEMS.add("vfat");
        FILESYSTEMS.add("fuse");
    }

    private final List<File> roots = new ArrayList<>();

    /**
     * Scans mounted filesystems and returns mount paths for filesystems which are available to write.
     * Also prepends the list by the {@link android.os.Environment#getExternalStorageDirectory()}.
     */
    public ExternalStorageRoots() {
        addPrimaryExternalStorage();
        addMounts();
        addRoots();
    }

    /**
     * Scans mounted filesystems and returns mount paths for filesystems which are available to write.
     * Also prepends the list by the {@link android.os.Environment#getExternalStorageDirectory()}.
     * Also tries to get roots from {@link android.content.Context#getExternalFilesDirs(String)}.
     */
    public ExternalStorageRoots(Context context) {
        addPrimaryExternalStorage();
        addExternalFilesDirs(context);
        addMounts();
        addRoots();
    }

    /**
     * Returns found roots.
     *
     * @return the list of mounted and writable external storage roots.
     */
    public List<File> getRoots() {
        return Collections.unmodifiableList(this.roots);
    }

    private void addRoot(File root) {
        if (this.roots.contains(root)) {
            return;
        }
        this.roots.add(root);
    }

    private void addPrimaryExternalStorage() {
        File root = Environment.getExternalStorageDirectory();
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            addRoot(root.getAbsoluteFile());     // always add this root, if mounted
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
            addRoot(findWritableParent(dir.getAbsoluteFile()));
        }
    }

    private File findWritableParent(File file) {
        File parent = file.getParentFile();
        if (parent == null) {
            return file;
        }
        if (!parent.canWrite() && file.canWrite()) {
            return file;
        }
        return findWritableParent(parent);
    }

    private void addMounts() {
        if (!(MOUNT_FILE.isFile() && MOUNT_FILE.canRead())) {
            return;
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(MOUNT_FILE));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                String[] fields = line.split("\\s+");
                if (fields.length < 3) {
                    continue;
                }
                //String dev = fields[0]
                String path = fields[1];
                String filesystem = fields[2];
                if (!FILESYSTEMS.contains(filesystem)) {
                    continue;
                }
                addWritableDir(new File(path));     // other roots are added only when writable
            }
            reader.close();
        } catch (IOException e) {
            //ignoring
        }
    }

    private void addWritableDir(File path) {
        File absPath = path.getAbsoluteFile();
        if (absPath.isDirectory() && absPath.canWrite()) {
            addRoot(path);
        }
    }

    private void addRoots() {
        for (File root : File.listRoots()) {
            addRoot(root);
        }
    }

}

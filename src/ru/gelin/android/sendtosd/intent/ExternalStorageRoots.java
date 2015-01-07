package ru.gelin.android.sendtosd.intent;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Takes care on find roots of possible mounted external storages.
 */
public class ExternalStorageRoots {

    static final File MOUNT_FILE = new File("/proc/mounts");

    static final Set FILESYSTEMS = new HashSet<String>();
    static {
        FILESYSTEMS.add("ext4");
        FILESYSTEMS.add("vfat");
        FILESYSTEMS.add("fuse");
    }

    List<File> roots = new ArrayList<File>();

    /**
     *  Scans mounted filesystems and returns mount paths for filesystems which are available to write.
     *  Also prepends the list by the {@link android.os.Environment#getExternalStorageDirectory()}
     */
    public ExternalStorageRoots() {
        addPrimaryExternalStorage();
        addMounts();
    }

    /**
     *  Returns found roots.
     *  @return the list of mounted and writable external storage roots.
     */
    public List<File> getRoots() {
        return Collections.unmodifiableList(this.roots);
    }

    void addPrimaryExternalStorage() {
        File root = Environment.getExternalStorageDirectory();
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            addWritableDir(root);
        }
    }

    void addMounts() {
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
                addWritableDir(new File(path));
            }
            reader.close();
        } catch (IOException e) {
            //ignoring
        }
    }

    void addWritableDir(File path) {
        if (this.roots.contains(path)) {
            return;
        }
        try {
            path = path.getCanonicalFile();
        } catch (IOException e) {
            //bad, but can be skipped
        }
        if (path.isDirectory() && path.canWrite()) {
            this.roots.add(path);
        }
    }

}

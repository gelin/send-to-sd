package ru.gelin.android.sendtosd.fs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Takes care to find all known roots of any filesystem.
 */
public class FsRoots {

    private static final File MOUNT_FILE = new File("/proc/mounts");

    private static final Set<String> INCLUDE_FILESYSTEMS = new HashSet<>();
    static {
        INCLUDE_FILESYSTEMS.add("ext4");
        INCLUDE_FILESYSTEMS.add("vfat");
        INCLUDE_FILESYSTEMS.add("fuse");
    }

    private static final Set<String> EXCLUDE_PREFIXES = new HashSet<>();
    static {
        EXCLUDE_PREFIXES.add("/mnt/asec");
        EXCLUDE_PREFIXES.add("/system");
        EXCLUDE_PREFIXES.add("/lta");
    }

    private final List<File> roots = new ArrayList<>();

    /**
     * Scans mounted filesystems and returns mount paths for filesystems which are available to write.
     */
    public FsRoots() {
        addMounts();
        addRoots();
    }

    /**
     * Returns found roots.
     * @return the list of mounted and writable FS roots.
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
            line:
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                String[] fields = line.split("\\s+");
                if (fields.length < 3) {
                    continue;
                }
                //String dev = fields[0]
                String path = fields[1];
                String filesystem = fields[2];
                if (!INCLUDE_FILESYSTEMS.contains(filesystem)) {
                    continue;
                }
                for (String prefix : EXCLUDE_PREFIXES) {
                    if (path.startsWith(prefix)) {
                        continue line;
                    }
                }
                addReadableDir(new File(path));     // other roots are added only when writable
            }
            reader.close();
        } catch (IOException e) {
            //ignoring
        }
    }

    private void addReadableDir(File path) {
        File absPath = path.getAbsoluteFile();
        if (absPath.isDirectory() && absPath.canRead()) {  // TODO: readable or writable?
            addRoot(path);
        }
    }

    private void addRoots() {
        for (File root : File.listRoots()) {
            addRoot(root);
        }
    }

}

package ru.gelin.android.sendtosd.fs;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Joins together FS roots and mounted points.
 */
public class StartPaths implements PathsSource {

    private static final File ROOT = new File("/");

    private List<File> paths;

    /**
     * Scans mounted filesystems and external storages to find some paths to be start point for moves.
     * @param addFsRoots true if add result of {@link File#listRoots()}
     */
    public StartPaths(Context context, boolean addFsRoots) {
        List<File> mounts = new MountedVolumes(context).getPaths();
        List<File> roots = new FsRoots(addFsRoots).getPaths();
        paths = merge(mounts, roots);
        Collections.sort(paths, new PathComparator());
        if (!addFsRoots) {
            removeRoot();
        }
    }

    public List<File> getPaths() {
        return Collections.unmodifiableList(paths);
    }

    private List<File> merge(List<File> one, List<File> two) {
        Set<File> merge = new LinkedHashSet<File>();
        merge.addAll(one);
        merge.addAll(two);
        return new ArrayList<>(merge);
    }

    private void removeRoot() {
        paths.remove(ROOT);
    }

}

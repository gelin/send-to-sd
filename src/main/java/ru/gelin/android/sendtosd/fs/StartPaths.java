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
public class StartPaths {

    private List<File> paths;

    public StartPaths() {
        List<File> mounts = new MountedVolumes().getMounts();
        List<File> roots = new FsRoots().getRoots();
        paths = merge(mounts, roots);
    }

    public StartPaths(Context context) {
        List<File> mounts = new MountedVolumes(context).getMounts();
        List<File> roots = new FsRoots().getRoots();
        paths = merge(mounts, roots);
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

}

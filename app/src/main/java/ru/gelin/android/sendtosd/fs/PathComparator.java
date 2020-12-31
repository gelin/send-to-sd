package ru.gelin.android.sendtosd.fs;

import ru.gelin.android.sendtosd.Tag;

import java.io.File;
import java.util.Comparator;

/**
 * A special kind of comparator which puts
 * paths containing the package name (like "/storage/emulated/0/Android/data/ru.gelin.android.sendtosd/files")
 * and "/" to the end of the sorted list.
 */
class PathComparator implements Comparator<File> {

    private final File ROOT = new File("/");
    private final String PACKAGE_PATH = Tag.TAG;

    @Override
    public int compare(File left, File right) {
        if (left.equals(ROOT) && !right.equals(ROOT)) {
            return 1;
        }
        if (!left.equals(ROOT) && right.equals(ROOT)) {
            return -1;
        }
        String leftPath = left.getPath();
        String rightPath = right.getPath();
        if (leftPath.contains(PACKAGE_PATH) && !rightPath.contains(PACKAGE_PATH)) {
            return 1;
        }
        if (!leftPath.contains(PACKAGE_PATH) && rightPath.contains(PACKAGE_PATH)) {
            return -1;
        }
        return left.compareTo(right);
    }

}

package ru.gelin.android.sendtosd.fs;

import java.io.File;
import java.util.List;

/**
 * Some source of filesystem paths: roots, mounted volumes, etc...
 */
public interface PathsSource {

    /**
     * Returns the list of found paths.
     */
    List<File> getPaths();

}

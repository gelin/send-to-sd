package ru.gelin.android.sendtosd.preferences.folder;

import android.content.Context;
import ru.gelin.android.sendtosd.R;
import ru.gelin.android.sendtosd.Tag;

import java.io.File;

/**
 * Displays the human readable title for the mount point, like "External storage"
 * and the full folder path as Summary.
 */
public class MountPointFolderPreference extends FolderPreference {

    public MountPointFolderPreference(Context context, File folder,
                                      FolderChanger changer) {
        super(context, folder, changer);
        setTitle(chooseTitle(folder));
        setSummary(folder.toString());
    }

    private static final String INTERNAL_MARK = "/emulated/";
    private static final String PRIVATE_MARK = Tag.TAG;
    private static final File ROOT = new File("/");

    private int chooseTitle(File folder) {
        String path = folder.getPath();
        if (path.contains(PRIVATE_MARK) && path.contains(INTERNAL_MARK)) {
            return R.string.private_internal_storage;
        }
        if (path.contains(PRIVATE_MARK)) {
            return R.string.private_external_storage;
        }
        if (path.contains(INTERNAL_MARK)) {
            return R.string.internal_storage;
        }
        if (folder.equals(ROOT)) {
            return R.string.root_storage;
        }
        return R.string.external_storage;
    }

}

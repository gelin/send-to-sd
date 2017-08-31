package ru.gelin.android.sendtosd;

import android.content.Context;

import java.io.File;

/**
 * Displays the full folder path as Summary.
 */
public class PathFolderPreference extends FolderPreference {

    public PathFolderPreference(Context context, File folder,
                                FolderChanger changer) {
        super(context, folder, changer);
        setSummary(folder.toString());
    }

}

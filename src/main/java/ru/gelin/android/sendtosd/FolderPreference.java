package ru.gelin.android.sendtosd;

import android.content.Context;
import android.preference.Preference;

import java.io.File;

/**
 * Preference which displays one folder.
 * Folder is changed on click.
 */
public class FolderPreference extends Preference {

    /**
     * Current folder
     */
    private final File folder;
    /**
     * Folder changer
     */
    private final FolderChanger changer;

    public FolderPreference(Context context, File folder, FolderChanger changer) {
        super(context);
        this.folder = folder;
        this.changer = changer;
        setTitle(folder.getName());
        setEnabled(folder.canRead());
    }

    @Override
    protected void onClick() {
        changer.changeFolder(folder);
    }

}

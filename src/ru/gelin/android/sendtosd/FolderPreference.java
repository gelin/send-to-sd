package ru.gelin.android.sendtosd;

import java.io.File;

import android.content.Context;
import android.preference.Preference;
import android.view.View;

/**
 *  Preference which displays one folder.
 *  Folder is changed on click.
 */
public class FolderPreference extends Preference {

    /** Current folder */
    File folder;
    /** Folder changer */
    FolderChanger changer;
    
    public FolderPreference(Context context, File folder, FolderChanger changer) {
        super(context);
        this.folder = folder;
        this.changer = changer;
        setTitle(folder.getName());
        setEnabled(folder.canWrite());
    }
    
    @Override
    protected void onClick() {
        changer.changeFolder(folder);
    }

}

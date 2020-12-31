package ru.gelin.android.sendtosd.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;
import ru.gelin.android.sendtosd.Tag;

import java.io.File;

import static android.app.Activity.RESULT_OK;

/**
 * Performs check for access writes to a specified folder using StorageVolume API.
 */
public class StorageVolumeChecker {

    private static final int VOLUME_REQUEST = 1;

    private final Activity activity;

    public StorageVolumeChecker(Activity activity) {
        this.activity = activity;
    }

    public boolean hasAccess(File path) {
        return path.canWrite();
    }

    public void requestAccess(File path) {
        if (hasAccess(path)) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        Log.d(Tag.TAG, "Requesting access to " + path);
        StorageManager manager = (StorageManager) activity.getSystemService(Context.STORAGE_SERVICE);
        if (manager == null) {
            return;
        }
        StorageVolume volume = manager.getStorageVolume(path);
        if (volume == null) {
            return;
        }
        Intent intent = volume.createAccessIntent(null);
        if (intent == null) {
            return;
        }
        activity.startActivityForResult(intent, VOLUME_REQUEST);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOLUME_REQUEST) {
            if (resultCode == RESULT_OK) {
                Log.d(Tag.TAG, "Access granted");
                this.activity.finish();     // don't recreate(), because we don't want to save the path which may point to an inaccessible root folder
                this.activity.startActivity(this.activity.getIntent());
            }
        }
    }

}

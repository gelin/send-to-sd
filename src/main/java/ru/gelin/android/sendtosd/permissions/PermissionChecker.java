package ru.gelin.android.sendtosd.permissions;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates some actions related to check and grant necessary permissions.
 */
public class PermissionChecker {

    private static final int PERMISSION_REQUEST = 1;

    private final Activity activity;

    private final Map<String, Integer> results = new HashMap<>();

    public PermissionChecker(Activity activity) {
        this.activity = activity;
    }

    public boolean isReadGranted() {
        return isGranted(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    public boolean isWriteGranted() {
        return isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private boolean isGranted(String permission) {
        int permissionCheck = ContextCompat.checkSelfPermission(this.activity, permission);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    public void requestReadPermission() {
        requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    public void requestWritePermission() {
        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private void requestPermission(String permission) {
        if (isGranted(permission)) {
            return;
        }
        if (!this.results.containsKey(permission)) {
            ActivityCompat.requestPermissions(this.activity, new String[]{permission}, PERMISSION_REQUEST);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];
                this.results.put(permission, grantResult);
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    this.activity.recreate();
                }
            }
        }
    }

}

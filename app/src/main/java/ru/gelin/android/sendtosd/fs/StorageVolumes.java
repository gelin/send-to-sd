package ru.gelin.android.sendtosd.fs;

import android.app.Service;
import android.content.Context;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.support.annotation.RequiresApi;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


@RequiresApi(api = Build.VERSION_CODES.N)
public class StorageVolumes implements PathsSource {

    private final List<File> storages = new ArrayList<>();

    public StorageVolumes(Context context, List<File> paths) {
        storages.addAll(paths);
        Object manager = context.getSystemService(Service.STORAGE_SERVICE);
        if (manager instanceof StorageManager) {
            filterStorageVolumes((StorageManager) manager);
        }
    }

    private void filterStorageVolumes(StorageManager manager) {
        Iterator<File> i = storages.iterator();
        while (i.hasNext()) {
            File path = i.next();
            StorageVolume volume = manager.getStorageVolume(path);
            if (volume == null) {
                i.remove();
            }
        }
    }

    @Override
    public List<File> getPaths() {
        return storages;
    }

}

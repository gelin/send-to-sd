package ru.gelin.android.sendtosd.fs;

import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.support.annotation.RequiresApi;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static android.support.test.InstrumentationRegistry.getContext;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
@RequiresApi(api = Build.VERSION_CODES.N)
public class StorageVolumesTest {

    @Test
    public void testGetVolumes() {
        StartPaths startPaths = new StartPaths(getContext(), true);
        List<File> starts = startPaths.getPaths();
        System.out.println("Starts:" + starts);

        StorageVolumes storageVolumes = new StorageVolumes(getContext(), starts);
        List<File> volumes = storageVolumes.getPaths();
        System.out.println("Volumes:" + volumes);

        StorageManager manager = getContext().getSystemService(StorageManager.class);
        for (File path : volumes) {
            StorageVolume volume = manager.getStorageVolume(path);
            System.out.print(path + "\t");
            System.out.print(volume + "\t");
            if (volume != null) {
                System.out.print(volume.getUuid());
            }
            System.out.println();
        }
        assertTrue(starts.contains(new File("/storage/emulated/0")));
    }

}

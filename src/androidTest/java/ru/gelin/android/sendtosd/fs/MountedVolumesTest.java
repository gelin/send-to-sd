package ru.gelin.android.sendtosd.fs;

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
public class MountedVolumesTest {

    @Test
    public void testGetMounts() {
        MountedVolumes mountedVolumes = new MountedVolumes(getContext());
        List<File> mounts = mountedVolumes.getMounts();
        System.out.println("Mounts:" + mounts);
        assertTrue(mounts.contains(new File("/storage/emulated/0")));
    }

}

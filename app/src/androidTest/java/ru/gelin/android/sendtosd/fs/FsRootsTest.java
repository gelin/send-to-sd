package ru.gelin.android.sendtosd.fs;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FsRootsTest {

    @Test
    public void testGetRoots() {
        FsRoots fsRoots = new FsRoots(true);
        List<File> roots = fsRoots.getPaths();
        System.out.println("Roots:" + roots);
        assertTrue(roots.contains(new File("/")));
    }

}

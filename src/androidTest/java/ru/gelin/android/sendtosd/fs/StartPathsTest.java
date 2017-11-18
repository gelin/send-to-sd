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
public class StartPathsTest {

    @Test
    public void testGetRoots() {
        StartPaths startPaths = new StartPaths(getContext());
        List<File> paths = startPaths.getPaths();
        System.out.println("Start paths:" + paths);
        assertTrue(paths.contains(new File("/")));
        assertTrue(paths.contains(new File("/storage/emulated/0")));
    }

}

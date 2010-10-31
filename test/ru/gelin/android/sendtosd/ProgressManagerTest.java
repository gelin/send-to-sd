package ru.gelin.android.sendtosd;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import ru.gelin.android.sendtosd.intent.IntentFile;

public class ProgressManagerTest {

    ProgressManager manager;
    
    @Before
    public void setUp() {
        manager = new ProgressManager();
    }
    
    static class TestFile implements File {
        
        String name;
        long size;
        
        TestFile(String name, long size) {
            this.name = name;
            this.size = size;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public long getSize() {
            return this.size;
        }
        
    }
    
    @Test
    public void testSetFiles() {
        assertEquals(1, manager.files);
        assertEquals(-1, manager.file);
        manager.nextFile(new TestFile("0", File.UNKNOWN_SIZE));
        assertEquals(0, manager.file);
        manager.nextFile(new TestFile("1", File.UNKNOWN_SIZE));
        assertEquals(1, manager.file);
        manager.setFiles(5);
        assertEquals(5, manager.files);
        assertEquals(-1, manager.file);
    }

    @Test
    public void testNextFile() {
        manager.setFiles(2);
        assertEquals(2, manager.files);
        assertEquals(-1, manager.file);
        manager.nextFile(new TestFile("0", 2048));
        assertEquals(0, manager.file);
        assertEquals(0, manager.processed);
        manager.processBytes(1024);
        assertEquals(1024, manager.processed);
        manager.nextFile(new TestFile("1", File.UNKNOWN_SIZE));
        assertEquals(1, manager.file);
        assertEquals(0, manager.processed);
        manager.nextFile(new TestFile("2", File.UNKNOWN_SIZE));
        assertEquals(2, manager.file);
        manager.nextFile(null);
        assertEquals(2, manager.file);
    }

    @Test
    public void testProcessBytes() {
        manager.setFiles(2);
        assertEquals(2, manager.files);
        assertEquals(-1, manager.file);
        manager.nextFile(new TestFile("0", 2048));
        assertEquals(0, manager.file);
        assertEquals(0, manager.processed);
        manager.processBytes(1024);
        assertEquals(0, manager.file);
        assertEquals(1024, manager.processed);
        manager.processBytes(1024);
        assertEquals(0, manager.file);
        assertEquals(2048, manager.processed);
        manager.processBytes(1024);
        assertEquals(0, manager.file);
        assertEquals(2048, manager.processed);
    }

}

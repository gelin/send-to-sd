package ru.gelin.android.sendtosd;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ProgressManagerTest {

    ProgressManager manager;
    
    @Before
    public void setUp() {
        manager = new ProgressManager();
    }
    
    @Test
    public void testSetFiles() {
        assertEquals(1, manager.files);
        assertEquals(-1, manager.file);
        manager.nextFile(Progress.UNKNOWN_SIZE);
        assertEquals(0, manager.file);
        manager.nextFile(Progress.UNKNOWN_SIZE);
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
        manager.nextFile(2048);
        assertEquals(0, manager.file);
        assertEquals(0, manager.processed);
        manager.processBytes(1024);
        assertEquals(1024, manager.processed);
        manager.nextFile(Progress.UNKNOWN_SIZE);
        assertEquals(1, manager.file);
        assertEquals(0, manager.processed);
        manager.nextFile(Progress.UNKNOWN_SIZE);
        assertEquals(2, manager.file);
        manager.nextFile(Progress.UNKNOWN_SIZE);
        assertEquals(2, manager.file);
    }

    @Test
    public void testProcessBytes() {
        manager.setFiles(2);
        assertEquals(2, manager.files);
        assertEquals(-1, manager.file);
        manager.nextFile(2048);
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

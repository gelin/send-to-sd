package ru.gelin.android.sendtosd.progress;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import ru.gelin.android.sendtosd.progress.Progress.ProgressEvent;

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
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("0", File.UNKNOWN_SIZE)));
        assertEquals(0, manager.file);
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("1", File.UNKNOWN_SIZE)));
        assertEquals(1, manager.file);
        manager.progress(ProgressEvent.newSetFilesEvent(5));
        assertEquals(5, manager.files);
        assertEquals(-1, manager.file);
    }

    @Test
    public void testNextFile() {
        manager.progress(ProgressEvent.newSetFilesEvent(2));
        assertEquals(2, manager.files);
        assertEquals(-1, manager.file);
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("0", 2048)));
        assertEquals(0, manager.file);
        assertEquals(0, manager.processed);
        manager.progress(ProgressEvent.newProcessBytesEvent(1024));
        assertEquals(1024, manager.processed);
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("1", File.UNKNOWN_SIZE)));
        assertEquals(1, manager.file);
        assertEquals(0, manager.processed);
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("2", File.UNKNOWN_SIZE)));
        assertEquals(2, manager.file);
        manager.progress(ProgressEvent.newNextFileEvent(null));
        assertEquals(2, manager.file);
    }
    
    @Test
    public void testUpdateFile() {
        manager.progress(ProgressEvent.newSetFilesEvent(2));
        assertEquals(2, manager.files);
        assertEquals(-1, manager.file);
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("0", 2048)));
        assertEquals(0, manager.file);
        assertEquals(2048, manager.size);
        assertEquals(0, manager.processed);
        manager.progress(ProgressEvent.newProcessBytesEvent(1024));
        assertEquals(1024, manager.processed);
        manager.progress(ProgressEvent.newUpdateFileEvent(new FileInfo("1", File.UNKNOWN_SIZE)));
        assertEquals(0, manager.file);
        assertEquals(File.UNKNOWN_SIZE, manager.size);
        assertEquals(0, manager.processed);
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("2", 859)));
        assertEquals(1, manager.file);
        assertEquals(859, manager.size);
        assertEquals(0, manager.processed);
    }

    @Test
    public void testProcessBytes() {
        manager.progress(ProgressEvent.newSetFilesEvent(2));
        assertEquals(2, manager.files);
        assertEquals(-1, manager.file);
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("0", 2048)));
        assertEquals(0, manager.file);
        assertEquals(0, manager.processed);
        manager.progress(ProgressEvent.newProcessBytesEvent(1024));
        assertEquals(0, manager.file);
        assertEquals(1024, manager.processed);
        manager.progress(ProgressEvent.newProcessBytesEvent(1024));
        assertEquals(0, manager.file);
        assertEquals(2048, manager.processed);
        manager.progress(ProgressEvent.newProcessBytesEvent(1024));
        assertEquals(0, manager.file);
        assertEquals(2048, manager.processed);
    }
    
    @Test
    public void testGetSizeUnit() {
        manager.progress(ProgressEvent.newSetFilesEvent(3));
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("b", 1023)));
        assertEquals(SizeUnit.BYTE, manager.getSizeUnit());
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("Kb", 1023 * 1024)));
        assertEquals(SizeUnit.KILOBYTE, manager.getSizeUnit());
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("Mb", 1024 * 1024 + 1)));
        assertEquals(SizeUnit.MEGABYTE, manager.getSizeUnit());
        manager.progress(ProgressEvent.newNextFileEvent(null));
        assertEquals(SizeUnit.MEGABYTE, manager.getSizeUnit());
    }
    
    @Test
    public void testGetSizeInUnits() {
        manager.progress(ProgressEvent.newSetFilesEvent(3));
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("b", 1023)));
        assertEquals(1023, manager.getSizeInUnits(), 0.1);
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("Kb", 1023 * 1024)));
        assertEquals(1023, manager.getSizeInUnits(), 0.1);
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("Mb", 1024 * 1024 + 1)));
        assertEquals(1, manager.getSizeInUnits(), 0.1);
        manager.progress(ProgressEvent.newNextFileEvent(null));
        assertEquals(1, manager.getSizeInUnits(), 0.1);
    }
    
    @Test
    public void testGetProgressInUnits() {
        manager.progress(ProgressEvent.newSetFilesEvent(3));
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("b", 1023)));
        manager.progress(ProgressEvent.newProcessBytesEvent(128));
        assertEquals(128, manager.getProgressInUnits(), 0.1);
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("Kb", 1023 * 1024)));
        manager.progress(ProgressEvent.newProcessBytesEvent(128 * 1024 + 512));
        assertEquals(128.5, manager.getProgressInUnits(), 0.1);
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("Mb", 1024 * 1024 + 1)));
        manager.progress(ProgressEvent.newProcessBytesEvent(1024 * 1024));
        assertEquals(1.0, manager.getProgressInUnits(), 0.1);
        manager.progress(ProgressEvent.newNextFileEvent(null));
        assertEquals(1.0, manager.getProgressInUnits(), 0.1);
    }
    
    @Test
    public void testProgressDecimals() {
        manager.progress(ProgressEvent.newSetFilesEvent(3));
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("b", 1023)));
        assertEquals(1023, manager.getSizeInUnits(), 0.1);
        manager.progress(ProgressEvent.newProcessBytesEvent(128));
        assertEquals(128, manager.getProgressInUnits(), 0.1);
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("Kb", (int)(1023.5 * 1024))));
        assertEquals(1023.5, manager.getSizeInUnits(), 0.1);
        manager.progress(ProgressEvent.newProcessBytesEvent(128 * 1024 + 512));
        assertEquals(128.5, manager.getProgressInUnits(), 0.1);
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("Mb", (int)(1023.5 * 1024 * 1024))));
        assertEquals(1023.5, manager.getSizeInUnits(), 0.1);
        manager.progress(ProgressEvent.newProcessBytesEvent(128 * 1024 * 1024 + 512 * 1024));
        assertEquals(128.5, manager.getProgressInUnits(), 0.1);
        manager.progress(ProgressEvent.newNextFileEvent(null));
        assertEquals(128.5, manager.getProgressInUnits(), 0.1);
    }
    
    @Test
    public void testComplete() {
        manager.progress(ProgressEvent.newSetFilesEvent(3));
        assertEquals(-1, manager.file);
        manager.progress(ProgressEvent.newNextFileEvent(new FileInfo("b", 1023)));
        assertEquals(0, manager.file);
        assertEquals(0, manager.processed);
        manager.progress(ProgressEvent.newCompleteEvent());
        assertEquals(3, manager.file);
        assertEquals(1023, manager.processed);
    }

}

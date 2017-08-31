package ru.gelin.android.sendtosd.progress;

/**
 *  Interface to handle file copying/moving progress.
 */
public interface Progress {

	public static enum ProgressEventType {
		SET_FILES,
		NEXT_FILE,
		UPDATE_FILE,
		PROCESS_BYTES,
		COMPLETE,
	}
	
	public static class ProgressEvent {
		
		public final ProgressEventType type;
		public final int files;
		public final File file;
		public final long bytes;
		
		private ProgressEvent(ProgressEventType type, int files, File file, long bytes) {
			this.type = type;
			this.files = files;
			this.file = file;
			this.bytes = bytes;
		}
		
		public static ProgressEvent newSetFilesEvent(int files) {
			return new ProgressEvent(ProgressEventType.SET_FILES, files, null, -1);
		}
		public static ProgressEvent newNextFileEvent(File file) {
			return new ProgressEvent(ProgressEventType.NEXT_FILE, -1, file, -1);
		}
		public static ProgressEvent newUpdateFileEvent(File file) {
			return new ProgressEvent(ProgressEventType.UPDATE_FILE, -1, file, -1);
		}
		public static ProgressEvent newProcessBytesEvent(long bytes) {
			return new ProgressEvent(ProgressEventType.PROCESS_BYTES, -1, null, bytes);
		}
		public static ProgressEvent newCompleteEvent() {
			return new ProgressEvent(ProgressEventType.COMPLETE, -1, null, -1);
		}
	}
	
    public void progress(ProgressEvent event);
    
}

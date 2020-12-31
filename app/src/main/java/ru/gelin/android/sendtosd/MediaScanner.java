package ru.gelin.android.sendtosd;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Wrapper for {@link MediaScannerConnection}.
 * Creates the connection only on the first call to {@link #scanFile}.
 * Calls to {@link #scanFile} are asynchronous, no need to wait for
 * connect.
 */
public class MediaScanner {

    /**
     * Context
     */
    private final Context context;
    /**
     * Connection to MediaScanner
     */
    private MediaScannerConnection mediaScanner;
    /**
     * Queue of the scan requests which comes while connecting
     */
    private final Queue<FileInfo> waitScans = new ConcurrentLinkedQueue<>();

    /**
     * Creates the scanner for context.
     */
    public MediaScanner(Context context) {
        this.context = context;
    }

    /**
     * Scans the file with media scanner.
     * On the first scan the connection to actual media scanner is created.
     *
     * @param file file location
     * @param type file mime type, can be null
     */
    public void scanFile(File file, String type) {
        //Log.d(TAG, "scanning " + file + " [" + type + "]");
        if (mediaScanner == null) {
            createMediaScanner();
        }
        if (isConnected()) {
            mediaScanner.scanFile(file.getAbsolutePath(), type);
        } else {
            waitScans.offer(new FileInfo(file, type));
        }
    }

    /**
     * Disconnects from the media scanner.
     */
    public void disconnect() {
        if (mediaScanner == null) {
            return;
        }
        if (!mediaScanner.isConnected()) {
            return;
        }
        mediaScanner.disconnect();
    }

    private void createMediaScanner() {
        mediaScanner = new MediaScannerConnection(context,
            new MediaScannerClient());
        mediaScanner.connect();
    }

    /**
     * Returns true if the connection to media scanner is established and
     * the queue of the scans, which came while connecting, is empty.
     */
    private boolean isConnected() {
        if (mediaScanner == null) {
            return false;
        }
        if (!mediaScanner.isConnected()) {
            return false;
        }
        return waitScans.isEmpty();
    }

    static private class FileInfo {
        public final File path;
        public final String type;

        public FileInfo(File path, String type) {
            this.path = path;
            this.type = type;
        }
    }

    private class MediaScannerClient implements MediaScannerConnectionClient {
        //@Override
        public void onMediaScannerConnected() {
            FileInfo file = waitScans.poll();
            while (file != null) {
                mediaScanner.scanFile(file.path.getAbsolutePath(), file.type);
                file = waitScans.poll();
            }
        }

        //@Override
        public void onScanCompleted(String path, Uri uri) {
            //we're not interesting in this
        }
    }

}

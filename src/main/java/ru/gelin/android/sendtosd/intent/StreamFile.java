package ru.gelin.android.sendtosd.intent;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import ru.gelin.android.sendtosd.progress.Progress.ProgressEvent;

import java.io.*;
import java.util.List;

/**
 *  Intent file which reads the content of the file from
 *  the stream from the provided content URI.
 *  This file is not deletable.
 *  This file is not movable.
 */
public class StreamFile extends IntentFile {

    /** Content resolver to read Uri */
    ContentResolver contentResolver;
    
    /** Uri of the stream */
    Uri uri;
    /** Mime type of the stream */
    String type;
    /** Flag indicating that the Uri was queried for some additional information */
    protected volatile boolean queried = false;
    
    StreamFile(Context context, Intent intent) {
        this.contentResolver = context.getContentResolver();
        this.uri = getStreamUri(intent);
        this.type = intent.getType();
    }
    
    StreamFile(Context context, Uri uri) {
        this.contentResolver = context.getContentResolver();
        this.uri = uri;
    }
    
    /**
     *  Queries the content for mime type.
     *  If the query was done before the new attempt is skipped.
     */
    void queryContent() {
        if (this.queried) {
            return;
        }
        if (this.type == null) {
            this.type = contentResolver.getType(uri);
        }
        this.queried = true;
    }
    
    /**
     *  Tries to guess the filename from the intent.
     */
    @Override
    public String getName() {
        String fileName = this.uri.getLastPathSegment();
        return addExtension(removeLeadingDots(fileName));
    }
    
    /**
     *  When was created from intent, returns the intent type.
     *  When was created from URI, returns the type requested from content provider.
     */
    public String getType() {
        queryContent();
        return this.type;
    }
    
    /**
     *  Returns the UNKNOWN_SIZE.
     *  The size of stream is unknown.
     */
    @Override
    public long getSize() {
        return ru.gelin.android.sendtosd.progress.File.UNKNOWN_SIZE;
    }

    /**
     *  Returns false.
     */
    @Override
    public boolean isDeletable() {
        return false;
    }
    
    /**
     *  Returns false.
     */
    @Override
    public boolean isMovable(File dest, List<File> roots) {
        return false;
    }
    
    /**
     *  Saves the file as the specified file on SD card.
     */
    @Override
    public void saveAs(File file) throws IOException {
        InputStream in = getStream();
        OutputStream out = new FileOutputStream(file);
        byte[] buf = new byte[1024];
        int read;
        while ((read = in.read(buf)) > 0) {
            out.write(buf, 0, read);
            this.progress.progress(ProgressEvent.newProcessBytesEvent(read));
        }
        out.close();
        in.close();
    }
    
    /**
     *  Always throws exception.
     */
    @Override
    public void moveTo(File file) throws IOException {
        throw new IOException("stream file is not movable");
    }
    
    /**
     *  Always throws exception.
     */
    @Override
    public void delete() throws IOException {
        throw new IOException("stream file is not deletable");
    }

    /**
     *  Adds extension to the file name if it hasn't it.
     */
    String addExtension(String fileName) {
        if (fileName.contains(".")) {   //has extension
            return fileName;
        }
        if (getType() != null) {
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(this.type);
            if (extension != null) {
                return fileName + "." + extension;
            }
        }
        return fileName;
    }

    /**
     *  Removes the leading dots from the filename.
     *  To make the file not-hidden.
     */
    String removeLeadingDots(String fileName) {
        String result = fileName;
        while (result.startsWith(".")) {
            result = result.substring(1);
        }
        return result;
    }
    
    /**
     *  Returns the file as stream.
     */
    InputStream getStream() throws FileNotFoundException {
        return this.contentResolver.openInputStream(this.uri);
    }
    
    @Override
    public String toString() {
    	return "stream: [" + this.type + "] " + this.uri;
    }

}

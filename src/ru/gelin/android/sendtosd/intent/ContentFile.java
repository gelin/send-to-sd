package ru.gelin.android.sendtosd.intent;

import java.io.IOException;

import ru.gelin.android.sendtosd.progress.File;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

/**
 *  File for content:// URI.
 *  Deletable only for some MediaStorage URIs.
 */
public class ContentFile extends StreamFile {

    /** content:// URIs which are writable */
    static final String[] WRITABLE_URIS = {
        //MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString(),   //???
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString(),
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString(),
    };
    
    /** Projection to select some useful data */
    String[] projection = {
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            //MediaStore.MediaColumns.DISPLAY_NAME,
            //MediaStore.MediaColumns.TITLE,
    };
    
    /** File which contains the content data (if can be selected from the content provider) */
    java.io.File data;
    /** Content size, in bytes */
    long size = File.UNKNOWN_SIZE;
    /** Flag indicating that the Uri was queried for some additional information */
    boolean queried = false;
    
    ContentFile(Context context, Intent intent) {
        super(context, intent);
        queryContent();
    }
    
    ContentFile(Context context, Uri uri) {
        super(context, uri);
        //queryContent();
    }
    
    /**
     *  Queries the content for file name and size.
     *  If the query was done before the new attempt is skipped.
     */
    void queryContent() {
        if (queried) {
            return;
        }
        try {
            Cursor cursor = contentResolver.query(uri, projection, null, null, null);
            int dataIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
            int sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
            if (cursor.moveToFirst()) {
                String data = cursor.getString(dataIndex);
                this.data = new java.io.File(data);
                this.size = cursor.getLong(sizeIndex);
            }
            cursor.close();
        } catch (Exception e) {
            //nothing to do, we have default behaviour
            Log.w(TAG, "cannot query content", e);
        }
        queried = true;
    }
    
    /**
     *  Makes the query to the content provider to select the file name.
     */
    @Override
    public String getName() {
        queryContent();
        if (data == null) {
            return super.getName();
        }
        return addExtension(data.getName());
    }
    
    /**
     *  Makes the query to the content provider to select the file size.
     */
    @Override
    public long getSize() {
        queryContent();
        return this.size;
    }
    
    /**
     *  Returns true if the file can be deleted.
     *  Returns true only for some supported providers.
     */
    @Override
    public boolean isDeletable() {
        String uri = this.uri.toString();
        for (String contentUri : WRITABLE_URIS) {
            if (uri.startsWith(contentUri)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     *  Deleted the original file via ContentResolver.
     *  @throws IOException if the file was not deleted
     */
    @Override
    public void delete() throws IOException {
        int result = contentResolver.delete(uri, null, null);
        if (result <= 0) {
            throw new IOException(uri + " was not deleted");
        }
    }

}

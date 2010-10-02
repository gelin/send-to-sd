package ru.gelin.android.sendtosd.intent;

import java.io.File;
import java.io.IOException;

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
            //MediaStore.MediaColumns.DISPLAY_NAME,
            //MediaStore.MediaColumns.TITLE,
    };
    
    /** File which contains the content data (if can be selected from the content provider) */
    File data;
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
    
    void queryContent() {
        if (queried) {
            return;
        }
        try {
            Cursor cursor = contentResolver.query(uri, projection, null, null, null);
            int dataIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
            if (cursor.moveToFirst()) {
                String data = cursor.getString(dataIndex);
                this.data = new File(data);
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
    public String getName() {
        queryContent();
        if (data == null) {
            return super.getName();
        }
        return addExtension(data.getName());
    }
    
    /**
     *  Returns true if the file can be deleted.
     *  Returns true only for some supported providers.
     */
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
    public void delete() throws IOException {
        int result = contentResolver.delete(uri, null, null);
        if (result <= 0) {
            throw new IOException(uri + " was not deleted");
        }
    }

}

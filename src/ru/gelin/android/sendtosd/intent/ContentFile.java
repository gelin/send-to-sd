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
 *  Movable only when the actual file location is known and it's on SD card already.
 */
public class ContentFile extends AbstractFileFile {

    /** content:// URIs which are writable */
    static final String[] WRITABLE_URIS = {
        //MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString(),   //???
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString(),
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString(),
    };
    
    /** Projection to select some useful data */
    String[] projection = {
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            //MediaStore.MediaColumns.DISPLAY_NAME,
            //MediaStore.MediaColumns.TITLE,
    };
    
    /** Content size, in bytes */
    long size = ru.gelin.android.sendtosd.progress.File.UNKNOWN_SIZE;

    ContentFile(Context context, Intent intent) throws IntentFileException {
        super(context, intent);
        queryContent();     //called from SEND, can init here
    }
    
    ContentFile(Context context, Uri uri) throws IntentFileException {
        super(context, uri);
        //queryContent();   //called from SEND_MULTIPLE, better to init later
    }
    
    /**
     *  Queries the content for file name, mime type and size.
     *  If the query was done before the new attempt is skipped.
     */
    @Override
    void queryContent() {
        if (queried) {
            return;
        }
        try {
            Cursor cursor = contentResolver.query(uri, projection, null, null, null);
            int dataIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
            int typeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE);
            int sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
            if (cursor.moveToFirst()) {
                String data = cursor.getString(dataIndex);
                this.file = new File(data);
                this.type = cursor.getString(typeIndex);
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
        if (file == null) {
            return super.getName();
        }
        return addExtension(file.getName());
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
     *  Returns true if the original file is writable
     *  and is already located on SD card and the move
     *  destination is on SD card too.
     */
    @Override
    public boolean isMovable(File dest) {
        queryContent();
        return super.isMovable(dest);
    }
    
    /**
     *  Moves the file using the filesystem operations.
     *  Deletes the record in content resolver.
     */
    @Override
    public void moveTo(File dest) throws IOException {
        super.moveTo(dest);
        delete();
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

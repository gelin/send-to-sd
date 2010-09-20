package ru.gelin.android.sendtosd.intent;

import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;

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
    
    public ContentFile(Context context, Intent intent) {
        super(context, intent);
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
        int result = context.getContentResolver().delete(uri, null, null);
        if (result <= 0) {
            throw new IOException(uri + " was not deleted");
        }
    }

}

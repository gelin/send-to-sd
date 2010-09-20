package ru.gelin.android.sendtosd.intent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import android.content.Context;
import android.content.Intent;

/**
 *  File with text/plain content.
 *  The file content is selected from the EXTRA_TEXT.
 *  The file is not deletable.
 */
public class TextFile extends IntentFile {

    private static final String TEXT_FILE_NAME = "text.txt";
    
    TextFile(Context context, Intent intent) {
        super(context, intent);
    }
    
    /**
     *  Returns "text.txt".
     */
    public String getName() {
        return TEXT_FILE_NAME;
    }

    /**
     *  Returns false.
     */
    public boolean isDeletable() {
        return false;
    }
    
    /**
     *  Saves the file as the specified file on SD card.
     */
    public void saveAs(File file) throws IOException {
        Writer out = new FileWriter(file);
        out.write(getText());
        out.close();
    }
    
    /**
     *  Always throws exception.
     */
    public void delete() throws IOException {
        throw new IOException("text file is not deletable");
    }
    
    /**
     *  Returns the file text.
     */
    String getText() {
        return intent.getStringExtra(Intent.EXTRA_TEXT);
    }

}

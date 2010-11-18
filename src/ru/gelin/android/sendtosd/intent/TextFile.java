package ru.gelin.android.sendtosd.intent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import android.content.Intent;

/**
 *  File with text/plain content.
 *  The file content is selected from the EXTRA_TEXT.
 *  The file is not deletable.
 */
public class TextFile extends IntentFile {

    private static final String TEXT_FILE_NAME = "text.txt";
    
    /** The text content of the file */
    String text = "";
    
    TextFile(Intent intent) {
        text = intent.getStringExtra(Intent.EXTRA_TEXT);
    }
    
    TextFile(String text) {
        this.text = text;
    }
    
    /**
     *  Returns "text.txt".
     */
    public String getName() {
        return TEXT_FILE_NAME;
    }
    
    /**
     *  Returns the UNKNOWN_SIZE.
     *  The text file is copied at once, no reason to display the progress.
     */
    public long getSize() {
        return ru.gelin.android.sendtosd.progress.File.UNKNOWN_SIZE;
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
        out.write(text);
        out.close();
    }
    
    /**
     *  Always throws exception.
     */
    public void delete() throws IOException {
        throw new IOException("text file is not deletable");
    }

}

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
 *  The file is not movable.
 */
public class TextFile extends IntentFile {

    private static final String TEXT_FILE_NAME = "text.txt";
    private static final String TEXT_MIME_TYPE = "text/plain";
    
    /** The text content of the file */
    String text = "";
    
    TextFile(Intent intent) throws IntentFileException {
        this.text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (this.text == null) {
            throw new IntentFileException("null text");
        }
    }
    
    TextFile(String text) throws IntentFileException {
        if (text == null) {
            throw new IntentFileException("null text");
        }
        this.text = text;
    }
    
    /**
     *  Returns "text.txt".
     */
    @Override
    public String getName() {
        return TEXT_FILE_NAME;
    }
    
    /**
     *  Returns "text/plain".
     */
    public String getType() {
        return TEXT_MIME_TYPE;
    }
    
    /**
     *  Returns the UNKNOWN_SIZE.
     *  The text file is copied at once, no reason to display the progress.
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
    public boolean isMovable(File dest) {
        return false;
    }
    
    /**
     *  Saves the file as the specified file on SD card.
     */
    @Override
    public void saveAs(File file) throws IOException {
        Writer out = new FileWriter(file);
        out.write(this.text);
        out.close();
    }
    
    /**
     *  Always throws exception.
     */
    @Override
    public void moveTo(File file) throws IOException {
        throw new IOException("text file is not movable");
    }
    
    /**
     *  Always throws exception.
     */
    @Override
    public void delete() throws IOException {
        throw new IOException("text file is not deletable");
    }
    
    @Override
    public String toString() {
    	return "text: " + this.text;
    }

}

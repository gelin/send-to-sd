package ru.gelin.android.sendtosd;

import ru.gelin.android.sendtosd.intent.IntentFile;

/**
 *  Singleton storage for intent files.
 */
public class IntentFiles {
    
    /** Instance */
    static IntentFiles instance;
    
    /** Array of files */
    IntentFile[] files;
    
    /**
     *  Returns the instance of the storage.
     */
    public static IntentFiles getInstance() {
        if (instance == null) {
            instance = new IntentFiles();
        }
        return instance;
    }

    /**
     *  Initializes the storage.
     */
    public void init(IntentFile[] files) {
        this.files = files;
    }
    
    /**
     *  Returns the stored files.
     */
    public IntentFile[] getFiles() {
        return files;
    }

}
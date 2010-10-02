package ru.gelin.android.sendtosd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.gelin.android.sendtosd.intent.IntentFile;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

/**
 *  Singleton storage for intent files.
 */
public class IntentFiles implements Constants {
    
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
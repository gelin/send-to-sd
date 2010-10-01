package ru.gelin.android.sendtosd;

import java.io.File;

import ru.gelin.android.sendtosd.intent.IntentFile;
import ru.gelin.android.sendtosd.intent.SendIntentInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

/**
 *  Activity which displays the list of folders
 *  and allows to copy/move the file to folder.
 */
public class SendActivity extends SendToFolderActivity
        implements FileSaver {
    
    /** File to save from intent */
    IntentFile intentFile;
    /** Filename to save */
    String fileName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        if (intent == null) {
            error(R.string.unsupported_file);
            return;
        }
        try {
            SendIntentInfo intentInfo = new SendIntentInfo(this, intent);
            this.intentInfo = intentInfo;
            intentInfo.log();
            if (!intentInfo.validate()) {
                error(R.string.unsupported_file);
                return;
            }
            intentFile = intentInfo.getFile();
            fileName = intentInfo.getFileName();
        } catch (Throwable e) {
            error(R.string.unsupported_file, e);
            return;
        }
        setTitle(fileName);
    }

    /**
     *  Return true if the intent has deletable file which can be moved.
     *  This implementation returns true if the sending file is deletable.
     */
    public boolean hasDeletableFile() {
        return intentFile.isDeletable();
    }

    /**
     *  Creates the intent to change the folder.
     */
    @Override
    Intent getChangeFolderIntent(File folder) {
        Intent intent = super.getChangeFolderIntent(folder);
        intent.putExtra(EXTRA_FILE_NAME, fileName);
        return intent;
    }
    
    /**
     *  Copies the file.
     */
    @Override
    public void copyFile() {
        super.copyFile();
        try {
            intentFile.saveAs(new File(path, getUniqueFileName(fileName)));
        } catch (Exception e) {
            warn(R.string.file_is_not_copied, e);
            return;
        }
        complete(R.string.file_is_copied);
    }
    
    /**
     *  Moves the file.
     */
    @Override
    public void moveFile() {
        super.moveFile();
        try {
            intentFile.saveAs(new File(path, getUniqueFileName(fileName)));
        } catch (Exception e) {
            warn(R.string.file_is_not_moved, e);
            return;
        }
        try {
            intentFile.delete();
        } catch (Exception e) {
            Log.w(TAG, e.toString(), e);
            complete(R.string.file_is_not_deleted);
            return;
        }
        complete(R.string.file_is_moved);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.send_options_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_choose_file_name:
            showChooseFileNameDialog();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     *  Displays the Choose File Name dialog.
     */
    void showChooseFileNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.choose_file_name);
        View content = getLayoutInflater().inflate(R.layout.edit_text_dialog, 
                (ViewGroup)findViewById(R.id.how_to_use_dialog_root));
        final EditText edit = (EditText)content.findViewById(R.id.edit_text);
        edit.setText(fileName);
        builder.setView(content);
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                fileName = edit.getText().toString();
                setTitle(fileName);
            }
        });
        Dialog dialog = builder.create();
        //http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/java/android/preference/DialogPreference.java;h=bbad2b6d432ce44ad05ddbc44487000b150135ef;hb=HEAD
        Window window = dialog.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.show();
    }

}

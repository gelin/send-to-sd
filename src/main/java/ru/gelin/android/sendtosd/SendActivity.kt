package ru.gelin.android.sendtosd

import ru.gelin.android.sendtosd.SendToFolderActivity.onCreate
import ru.gelin.android.sendtosd.SendToFolderActivity.error
import ru.gelin.android.sendtosd.SendToFolderActivity.onSaveInstanceState
import ru.gelin.android.sendtosd.SendToFolderActivity.onOptionsItemSelected
import ru.gelin.android.sendtosd.SendToFolderActivity.onCreateDialog
import ru.gelin.android.sendtosd.SendToFolderActivity.saveLastFolder
import ru.gelin.android.sendtosd.SendToFolderActivity.getUniqueFileName
import ru.gelin.android.sendtosd.SendToFolderActivity.complete
import ru.gelin.android.sendtosd.SendToFolderActivity.warn
import ru.gelin.android.sendtosd.SendToFolderActivity
import ru.gelin.android.sendtosd.preferences.action.FileSaver
import ru.gelin.android.sendtosd.intent.IntentFile
import android.os.Bundle
import ru.gelin.android.sendtosd.intent.SendIntentInfo
import android.util.Log
import ru.gelin.android.sendtosd.R
import ru.gelin.android.sendtosd.SendActivity
import kotlin.Throws
import ru.gelin.android.sendtosd.intent.IntentException
import ru.gelin.android.sendtosd.intent.IntentInfo
import android.view.Menu
import android.view.MenuItem
import android.app.Dialog
import android.app.AlertDialog
import android.view.View
import android.widget.EditText
import android.content.DialogInterface
import android.view.Window
import android.view.WindowManager
import ru.gelin.android.sendtosd.fs.StartPaths
import android.os.AsyncTask
import ru.gelin.android.sendtosd.progress.*
import ru.gelin.android.sendtosd.progress.Progress.ProgressEvent
import java.io.File
import java.lang.Exception

/**
 * Activity which displays the list of folders
 * and allows to copy/move the file to folder.
 */
open class SendActivity : SendToFolderActivity(), FileSaver {

    /**
     * File to save from intent
     */
    private lateinit var intentFile: IntentFile

    /**
     * Filename to save
     */
    private lateinit var fileName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        if (intentInfo == null) {
//            return  //not initialized, should be finished immediately from super.onCreate()
//        }
        try {
            val sendIntentInfo = intentInfo as SendIntentInfo
            intentFile = sendIntentInfo.file
            Log.d(Tag.TAG, intentFile.toString())
            fileName = sendIntentInfo.fileName
        } catch (e: Throwable) {
            error(R.string.unsupported_file, e)
            return
        }
//        if (intentFile == null) {
//            error(R.string.no_files)
//            return
//        }
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_FILE_NAME)) {
                fileName = savedInstanceState.getString(KEY_FILE_NAME)!!
            }
        }
        title = fileName
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_FILE_NAME, fileName)
    }

    override fun createIntentInfo(): IntentInfo {
        return SendIntentInfo(this, intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.send_options_menu, menu)
        val newFolderMenu = menu.findItem(R.id.menu_new_folder)
        if (newFolderMenu != null && path != null) {
            newFolderMenu.isEnabled = path.canWrite()
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_choose_file_name -> showDialog(FILE_NAME_DIALOG)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateDialog(id: Int): Dialog? {
        return when (id) {
            FILE_NAME_DIALOG -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.choose_file_name)
                val content = layoutInflater.inflate(R.layout.edit_text_dialog, null)
                val edit = content.findViewById<View>(R.id.edit_text) as EditText
                edit.setText(fileName)
                builder.setView(content)
                builder.setPositiveButton(android.R.string.ok) { dialog, which ->

                    //@Override
                    fileName = edit.text.toString()
                    title = fileName
                }
                val dialog: Dialog = builder.create()
                //http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/java/android/preference/DialogPreference.java;h=bbad2b6d432ce44ad05ddbc44487000b150135ef;hb=HEAD
                val window = dialog.window
                window!!.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                )
                dialog
            }
            COPY_DIALOG -> {
                permissions.requestWritePermission()
                val progress: ProgressDialog = SingleCopyDialog(this)
                this.progress = progress
                progress
            }
            MOVE_DIALOG -> {
                permissions.requestWritePermission()
                val progress: ProgressDialog = SingleMoveDialog(this)
                this.progress = progress
                progress
            }
            else -> super.onCreateDialog(id)
        }
    }

    /**
     * Return true if the intent has movable file and it's reasonable to display a "Move To" action.
     * This implementation returns true if the sending file is deletable or movable.
     */
    override fun hasMovableFile(): Boolean {
//        if (intentFile == null) {
//            return false
//        }
        val roots = StartPaths(this, false).paths // TODO: non atomic roots?
        return (intentFile.isDeletable
            || intentFile.isMovable(path, roots))
    }

    internal enum class Result {
        MOVED, COPIED, ERROR
    }

    internal abstract inner class ProgressTask : AsyncTask<IntentFile, ProgressEvent, Result>(), Progress {
        protected var progress: Progress? = null

        //from Progress interface
        override fun progress(event: ProgressEvent) {
            publishProgress(event)
        }

        protected override fun onProgressUpdate(vararg events: ProgressEvent) {
            //Log.d(TAG, events[0].type.toString());
            this.progress!!.progress(events[0])
        }
    }

    /**
     * Copies the file.
     */
    override fun copyFile() {
        CopyFileTask().execute(intentFile)
    }

    internal inner class CopyFileTask : ProgressTask() {
        override fun onPreExecute() {
            saveLastFolder()
            showDialog(COPY_DIALOG)
            this.progress = this@SendActivity.progress
        }

        protected override fun doInBackground(vararg params: IntentFile): Result {
            val intentFile = params[0]
            publishProgress(ProgressEvent.newSetFilesEvent(1)) //single file in this activity
            val uniqueFileName = getUniqueFileName(fileName)
            publishProgress(
                ProgressEvent.newNextFileEvent(
                    FileInfo(uniqueFileName, intentFile.size)
                )
            )
            try {
                intentFile.setProgress(this)
                val file = File(path, uniqueFileName)
                intentFile.saveAs(file)
                mediaScanner.scanFile(file, intentFile.type)
            } catch (e: Exception) {
                Log.w(Tag.TAG, e.toString(), e)
                return Result.ERROR
            }
            return Result.COPIED
        }

        protected override fun onPostExecute(result: Result) {
            this.progress!!.progress(ProgressEvent.newCompleteEvent())
            removeDialog(COPY_DIALOG)
            when (result) {
                Result.COPIED -> complete(R.string.file_is_copied)
                Result.ERROR -> warn(R.string.file_is_not_copied)
                else -> {}
            }
        }
    }

    /**
     * Moves the file.
     */
    override fun moveFile() {
        MoveFileTask().execute(intentFile)
    }

    internal inner class MoveFileTask : ProgressTask() {
        override fun onPreExecute() {
            saveLastFolder()
            showDialog(MOVE_DIALOG)
            this.progress = this@SendActivity.progress
        }

        protected override fun doInBackground(vararg params: IntentFile): Result {
            val intentFile = params[0]
            publishProgress(ProgressEvent.newSetFilesEvent(1)) //single file in this activity
            val uniqueFileName = getUniqueFileName(fileName)
            val dest = File(path, uniqueFileName)
            val roots = StartPaths(this@SendActivity, false).paths // TODO: Non atomic roots?
            return if (intentFile.isMovable(dest, roots)) {
                publishProgress(ProgressEvent.newNextFileEvent(FileInfo(uniqueFileName)))
                try {
                    intentFile.moveTo(dest)
                    mediaScanner.scanFile(dest, intentFile.type)
                    Result.MOVED
                } catch (e: Exception) {
                    Log.w(Tag.TAG, e.toString(), e)
                    publishProgress(
                        ProgressEvent.newUpdateFileEvent(
                            FileInfo(uniqueFileName, intentFile.size)
                        )
                    )
                    saveAndDeleteFile(intentFile, uniqueFileName)
                }
            } else {
                publishProgress(
                    ProgressEvent.newNextFileEvent(
                        FileInfo(uniqueFileName, intentFile.size)
                    )
                )
                saveAndDeleteFile(intentFile, uniqueFileName)
            }
        }

        fun saveAndDeleteFile(intentFile: IntentFile, uniqueFileName: String): Result {
            try {
                intentFile.setProgress(this)
                val dest = File(path, uniqueFileName)
                intentFile.saveAs(dest)
                mediaScanner.scanFile(dest, intentFile.type)
            } catch (e: Exception) {
                Log.w(Tag.TAG, e.toString(), e)
                return Result.ERROR
            }
            try {
                intentFile.delete()
            } catch (e: Exception) {
                Log.w(Tag.TAG, e.toString(), e)
                return Result.COPIED
            }
            return Result.MOVED
        }

        override fun onPostExecute(result: Result) {
            this.progress!!.progress(ProgressEvent.newCompleteEvent())
            removeDialog(MOVE_DIALOG)
            when (result) {
                Result.MOVED -> complete(R.string.file_is_moved)
                Result.COPIED -> complete(R.string.file_is_not_deleted)
                Result.ERROR -> warn(R.string.file_is_not_moved)
            }
        }
    }

    companion object {
        /**
         * Choose File Name dialog ID
         */
        private const val FILE_NAME_DIALOG = 10

        /**
         * Key to store the file name
         */
        private const val KEY_FILE_NAME = "file_name"
    }

}

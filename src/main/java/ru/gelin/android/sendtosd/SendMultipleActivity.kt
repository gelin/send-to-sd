package ru.gelin.android.sendtosd

import ru.gelin.android.sendtosd.SendToFolderActivity.onInit
import ru.gelin.android.sendtosd.SendToFolderActivity.error
import ru.gelin.android.sendtosd.SendToFolderActivity.onPostInit
import ru.gelin.android.sendtosd.SendToFolderActivity.onCreateDialog
import ru.gelin.android.sendtosd.SendToFolderActivity.saveLastFolder
import ru.gelin.android.sendtosd.SendToFolderActivity.getUniqueFileName
import ru.gelin.android.sendtosd.SendToFolderActivity.complete
import ru.gelin.android.sendtosd.SendToFolderActivity
import ru.gelin.android.sendtosd.intent.IntentFile
import kotlin.Throws
import ru.gelin.android.sendtosd.intent.IntentException
import ru.gelin.android.sendtosd.intent.IntentInfo
import ru.gelin.android.sendtosd.intent.SendMultipleIntentInfo
import ru.gelin.android.sendtosd.IntentFiles
import ru.gelin.android.sendtosd.intent.IntentFileException
import android.util.Log
import ru.gelin.android.sendtosd.R
import ru.gelin.android.i18n.PluralForms
import android.app.Dialog
import ru.gelin.android.sendtosd.fs.StartPaths
import android.os.AsyncTask
import ru.gelin.android.sendtosd.progress.*
import ru.gelin.android.sendtosd.progress.Progress.ProgressEvent
import java.io.File
import java.lang.Exception
import java.lang.StringBuilder
import java.text.MessageFormat

/**
 * Activity which displays the list of folders
 * and allows to copy/move multiple files to folder.
 */
open class SendMultipleActivity : SendToFolderActivity() {

    /**
     * Files to save from intent
     */
    private lateinit var intentFiles: Array<IntentFile>

    override fun createIntentInfo(): IntentInfo {
        return SendMultipleIntentInfo(this, intent)
    }

    override fun onInit() {
        super.onInit()
        val storage = IntentFiles.getInstance()
        if (pathHistory.isEmpty()) {
            try {
                intentFiles = (intentInfo as SendMultipleIntentInfo).files
            } catch (e: IntentFileException) {
                Log.e(Tag.TAG, "cannot get files list", e)
            }
            storage.init(intentFiles)
            for (file in intentFiles) {
                Log.d(Tag.TAG, file.toString())
            }
        } else {
            intentFiles = storage.files
        }
    }

    override fun onPostInit() {
        if (intentFiles.size == 0) {
            error(R.string.no_files)
            return
        }
        title = MessageFormat.format(
            getString(R.string.files_title),
            intentFiles.size,
            PluralForms.getInstance().getForm(intentFiles.size)
        )
        super.onPostInit()
    }

    override fun onCreateDialog(id: Int): Dialog? {
        return when (id) {
            COPY_DIALOG -> {
                permissions.requestWritePermission()
                val progress: ProgressDialog = MultipleCopyDialog(this)
                this.progress = progress
                progress
            }
            MOVE_DIALOG -> {
                permissions.requestWritePermission()
                val progress: ProgressDialog = MultipleMoveDialog(this)
                this.progress = progress
                progress
            }
            else -> super.onCreateDialog(id)
        }
    }

    /**
     * Return true if the intent has movable file and it's reasonable to display a "Move To" action.
     * This implementation returns true if one or more files are deletable or movable.
     */
    override fun hasMovableFile(): Boolean {
//        if (intentFiles == null) {
//            return false
//        }
        val roots = StartPaths(this, false).paths // TODO: non atomic roots?
        for (file in intentFiles) {
            if (file.isDeletable || file.isMovable(path, roots)) {
                return true
            }
        }
        return false
    }

    internal class Result {
        var moved = 0
        var copied = 0
        var errors = 0
    }

    internal abstract inner class ProgressTask : AsyncTask<Array<IntentFile>, ProgressEvent, Result>(), Progress {
        protected var progress: Progress? = null

        //from Progress interface
        override fun progress(event: ProgressEvent) {
            publishProgress(event)
        }

        override fun onProgressUpdate(vararg events: ProgressEvent) {
            //Log.d(TAG, events[0].type.toString());
            this.progress!!.progress(events[0])
        }
    }

    /**
     * Copies the files.
     */
    override fun copyFile() {
        CopyFileTask().execute(intentFiles)
    }

    internal inner class CopyFileTask : ProgressTask() {
        override fun onPreExecute() {
            saveLastFolder()
            showDialog(COPY_DIALOG)
            this.progress = this@SendMultipleActivity.progress
        }

        override fun doInBackground(vararg params: Array<IntentFile>): Result {
            val intentFiles = params[0]
            val result = Result()
            publishProgress(ProgressEvent.newSetFilesEvent(intentFiles.size))
            for (file in intentFiles) {
                val uniqueFileName = getUniqueFileName(file.name)
                publishProgress(ProgressEvent.newNextFileEvent(FileInfo(uniqueFileName, file.size)))
                try {
                    file.setProgress(this)
                    val newFile = File(path, uniqueFileName)
                    file.saveAs(newFile)
                    mediaScanner.scanFile(newFile, file.type)
                } catch (e: Exception) {
                    Log.w(Tag.TAG, e.toString(), e)
                    result.errors++
                    continue
                }
                result.copied++
            }
            return result
        }

        protected override fun onPostExecute(result: Result) {
            this.progress!!.progress(ProgressEvent.newCompleteEvent())
            removeDialog(COPY_DIALOG)
            val plurals = PluralForms.getInstance()
            val message = StringBuilder()
            message.append(
                MessageFormat.format(
                    getString(R.string.files_are_copied),
                    result.copied, plurals.getForm(result.copied)
                )
            )
            if (result.errors > 0) {
                message.append('\n')
                message.append(
                    MessageFormat.format(
                        getString(R.string.errors_appeared),
                        result.errors, plurals.getForm(result.errors)
                    )
                )
            }
            complete(message.toString())
        }
    }

    /**
     * Moves the files.
     */
    override fun moveFile() {
        MoveFileTask().execute(intentFiles)
    }

    internal inner class MoveFileTask : ProgressTask() {
        override fun onPreExecute() {
            saveLastFolder()
            showDialog(MOVE_DIALOG)
            this.progress = this@SendMultipleActivity.progress
        }

        protected override fun doInBackground(vararg params: Array<IntentFile>): Result {
            val result = Result()
            val intentFiles = params[0]
            publishProgress(ProgressEvent.newSetFilesEvent(intentFiles.size))
            val roots = StartPaths(this@SendMultipleActivity, false).paths // TODO: non atomic roots?
            for (file in intentFiles) {
                val uniqueFileName = getUniqueFileName(file.name)
                val dest = File(path, uniqueFileName)
                if (file.isMovable(dest, roots)) {
                    publishProgress(ProgressEvent.newNextFileEvent(FileInfo(uniqueFileName)))
                    try {
                        file.moveTo(dest)
                        mediaScanner.scanFile(dest, file.type)
                        result.moved++
                    } catch (e: Exception) {
                        Log.w(Tag.TAG, e.toString(), e)
                        publishProgress(
                            ProgressEvent.newUpdateFileEvent(
                                FileInfo(uniqueFileName, file.size)
                            )
                        )
                        saveAndDeleteFile(file, uniqueFileName, result)
                    }
                } else {
                    publishProgress(ProgressEvent.newNextFileEvent(FileInfo(uniqueFileName, file.size)))
                    saveAndDeleteFile(file, uniqueFileName, result)
                }
            }
            return result
        }

        fun saveAndDeleteFile(file: IntentFile, uniqueFileName: String, result: Result) {
            try {
                file.setProgress(this)
                val dest = File(path, uniqueFileName)
                file.saveAs(dest)
                mediaScanner.scanFile(dest, file.type)
            } catch (e: Exception) {
                Log.w(Tag.TAG, e.toString(), e)
                result.errors++
                return
            }
            try {
                file.delete()
            } catch (e: Exception) {
                Log.w(Tag.TAG, e.toString(), e)
                result.copied++
                return
            }
            result.moved++
        }

        protected override fun onPostExecute(result: Result) {
            this.progress!!.progress(ProgressEvent.newCompleteEvent())
            removeDialog(MOVE_DIALOG)
            val plurals = PluralForms.getInstance()
            val message = StringBuilder()
            message.append(
                MessageFormat.format(
                    getString(R.string.files_are_moved),
                    result.moved, plurals.getForm(result.moved)
                )
            )
            if (result.copied > 0) {
                message.append('\n')
                message.append(
                    MessageFormat.format(
                        getString(R.string.files_are_only_copied),
                        result.copied, plurals.getForm(result.copied)
                    )
                )
            }
            if (result.errors > 0) {
                message.append('\n')
                message.append(
                    MessageFormat.format(
                        getString(R.string.errors_appeared),
                        result.errors, plurals.getForm(result.errors)
                    )
                )
            }
            complete(message.toString())
        }
    }
}

package ru.gelin.android.sendtosd

import android.preference.PreferenceActivity
import ru.gelin.android.sendtosd.preferences.action.FileSaver
import ru.gelin.android.sendtosd.preferences.folder.FolderChanger
import ru.gelin.android.sendtosd.intent.IntentInfo
import java.util.LinkedList
import ru.gelin.android.sendtosd.MediaScanner
import ru.gelin.android.sendtosd.preferences.action.MoveHerePreference
import android.preference.Preference
import kotlin.jvm.Volatile
import ru.gelin.android.sendtosd.progress.Progress
import ru.gelin.android.sendtosd.progress.DummyProgress
import ru.gelin.android.sendtosd.permissions.PermissionChecker
import ru.gelin.android.sendtosd.permissions.StorageVolumeChecker
import android.os.Bundle
import android.view.Window
import ru.gelin.android.sendtosd.R
import ru.gelin.android.sendtosd.SendToFolderActivity
import ru.gelin.android.sendtosd.SendToFolderActivity.InitTask
import android.os.AsyncTask
import java.lang.Void
import kotlin.Throws
import ru.gelin.android.sendtosd.intent.IntentException
import android.os.Build
import ru.gelin.android.sendtosd.preferences.action.CopyHerePreference
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import ru.gelin.android.sendtosd.PreferencesActivity
import android.app.Dialog
import android.app.AlertDialog
import android.view.View
import android.widget.EditText
import android.content.DialogInterface
import android.view.WindowManager
import android.widget.Button
import android.text.TextWatcher
import android.text.Editable
import android.view.KeyEvent
import ru.gelin.android.sendtosd.LastFolders
import android.preference.PreferenceCategory
import android.content.SharedPreferences
import android.preference.PreferenceManager
import ru.gelin.android.sendtosd.PreferenceParams
import java.lang.NumberFormatException
import ru.gelin.android.sendtosd.preferences.folder.PathFolderPreference
import ru.gelin.android.sendtosd.fs.StartPaths
import android.util.Log
import ru.gelin.android.sendtosd.preferences.folder.MountPointFolderPreference
import ru.gelin.android.sendtosd.preferences.folder.FolderPreference
import ru.gelin.android.sendtosd.SendToFolderActivity.ListFoldersTask
import android.widget.Toast
import java.io.File
import kotlin.jvm.JvmOverloads
import java.util.ArrayList
import java.io.FileFilter
import java.util.Arrays
import java.util.Collections
import java.util.Comparator
import java.io.IOException
import java.io.Serializable

/**
 * Base class for activities to copy/move file/files to folder.
 * Responses for the directory listing and traversing.
 */
abstract class SendToFolderActivity : PreferenceActivity(), FileSaver, FolderChanger {

    /**
     * Intent information
     */
    protected lateinit var intentInfo: IntentInfo

    /**
     * Current path
     */
    private lateinit var path: File

    /**
     * History of paths
     */
    val pathHistory: MutableList<File> = LinkedList()

    /**
     * List of current subfolders
     */
    private val folders: MutableList<File> = LinkedList()

    /**
     * Wrapper for MediaScanner
     */
    lateinit var mediaScanner: MediaScanner

    /**
     * Move here preference. Saved here to remove from or add to hierarchy.
     */
    private lateinit var moveHerePreference: MoveHerePreference

    /**
     * Last folders preference. Saved here to remove from or add to hierarchy.
     */
    private lateinit var lastFoldersPreference: Preference

    /**
     * Mount points preference. Saved here to show or hide.
     */
    private lateinit var mountPointsPreference: Preference

    /**
     * Dialog to show the progress
     */
    var progress: Progress = DummyProgress() //can be used from other threads

    /**
     * Permission checker.
     */
    val permissions = PermissionChecker(this)

    /**
     * StorageVolume access checker.
     */
    private val volumeAccess = StorageVolumeChecker(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        super.onCreate(savedInstanceState)

        mediaScanner = MediaScanner(this)
        addPreferencesFromResource(R.xml.folder_preferences)
        lastFoldersPreference = findPreference(PREF_LAST_FOLDERS)
        mountPointsPreference = findPreference(PREF_MOUNT_POINTS)
        moveHerePreference = findPreference(PREF_MOVE_HERE) as MoveHerePreference

        if (intent == null) {
            error(R.string.unsupported_intent)
            return
        }

        try {
            intentInfo = createIntentInfo()
            intentInfo.log()
            path = intentInfo.path
            InitTask().execute()
        } catch (e: Throwable) {
            error(R.string.unsupported_intent, e)
            return
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_PATH)) {
                path = File(savedInstanceState.getString(KEY_PATH)!!)
            }
            if (savedInstanceState.containsKey(KEY_PATH_HISTORY)) {
                val restoredHistoryObject = savedInstanceState.getSerializable(KEY_PATH_HISTORY)
                if (restoredHistoryObject is Collection<*>) {
                    pathHistory.clear()
                    val restoredHistory = restoredHistoryObject as Collection<*>?
                    pathHistory.addAll(restoredHistory!!.mapNotNull { it as File? })
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PATH, path.toString())
        outState.putSerializable(KEY_PATH_HISTORY, pathHistory as Serializable)
    }

    override fun onResume() {
        super.onResume()
        updateLastFolders()
        updateMountPoints()
        volumeAccess.requestAccess(path)
        permissions.requestReadPermission()
    }

    internal inner class InitTask : AsyncTask<Unit, Unit, Unit>() {
        override fun onPreExecute() {
            setProgressBarIndeterminateVisibility(true)
        }

        override fun doInBackground(vararg params: Unit) {
            onInit()
            return
        }

        override fun onPostExecute(result: Unit) {
            onPostInit()
            setProgressBarIndeterminateVisibility(false)
            //Log.d(TAG, "history: " + SendToFolderActivity.this.pathHistory);
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaScanner.disconnect()
    }

    /**
     * Creates IntentInfo.
     * @throws IntentException if it's not possible to create intent info
     */
    protected abstract fun createIntentInfo(): IntentInfo

    /**
     * The method is called in a separate thread during the activity creation.
     * Avoid UI changes here!
     */
    protected open fun onInit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getExternalFilesDirs(null) // create a writable folder on external storages
        } else {
            getExternalFilesDir(null) // create a writable folder on external storage
        }
        folders.clear()
        folders.addAll(getFolders(path))
    }

    /**
     * The method is called in the UI thread when [.onInit] finishes.
     * Fills folders list.
     * Enables Copy/Move Here for writable folders.
     * Hides Move Here for non-deletable files.
     */
    protected open fun onPostInit() {
        fillFolders()
        val copyHerePreference = findPreference(PREF_COPY_HERE) as CopyHerePreference
        val moveHere = findPreference(PREF_MOVE_HERE)
        if (hasMovableFile()) {
            if (moveHere == null) {
                preferenceScreen.addPreference(moveHerePreference)
            }
        } else {
            preferenceScreen.removePreference(moveHerePreference)
        }
        copyHerePreference.setFileSaver(this)
        moveHerePreference.setFileSaver(this)
        val enable = path.canWrite()
        copyHerePreference.isEnabled = enable
        moveHerePreference.isEnabled = enable
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        val newFolderMenu = menu.findItem(R.id.menu_new_folder)
        if (newFolderMenu != null) {
            newFolderMenu.isEnabled = path.canWrite()
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_new_folder -> {
                showDialog(NEW_FOLDER_DIALOG)
                true
            }
            R.id.menu_preferences -> {
                startActivity(Intent(this, PreferencesActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateDialog(id: Int): Dialog? {
        return when (id) {
            NEW_FOLDER_DIALOG -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.new_folder)
                val content = layoutInflater.inflate(R.layout.edit_text_dialog, null)
                val edit = content.findViewById<View>(R.id.edit_text) as EditText
                builder.setView(content)
                builder.setPositiveButton(R.string.create_folder) { dialog, which ->
                    //@Override
                    createFolder(edit.text.toString())
                }
                val dialog: Dialog = builder.create()
                //http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/java/android/preference/DialogPreference.java;h=bbad2b6d432ce44ad05ddbc44487000b150135ef;hb=HEAD
                val window = dialog.window
                window!!.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                )
                permissions.requestWritePermission()
                dialog
            }
            else -> null
        }
    }

    override fun onPrepareDialog(id: Int, dialog: Dialog) {
        super.onPrepareDialog(id, dialog)
        when (id) {
            NEW_FOLDER_DIALOG -> {
                val alertDialog = dialog as AlertDialog
                val button = alertDialog.getButton(Dialog.BUTTON_POSITIVE)
                button.isEnabled = false
                val edit = dialog.findViewById<View>(R.id.edit_text) as EditText
                edit.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                        button.isEnabled = s.length > 0
                    }

                    override fun afterTextChanged(s: Editable) {}
                })
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        this.permissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        volumeAccess.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * Returns the current folder.
     */
    override fun getPath(): File {
        return path
    }

    /**
     * Return true if the intent has movable file and it's reasonable to display a "Move To" action.
     * This implementation always returns false.
     */
    open fun hasMovableFile(): Boolean {
        return false
    }

    /**
     * Changes the current folder.
     */
    override fun changeFolder(folder: File) {
        pathHistory.add(HEAD, path)
        path = folder
        updateLastFolders()
        updateMountPoints()
        volumeAccess.requestAccess(path)
        InitTask().execute()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        //TODO: check, maybe it's possible to use Android 2.0 onBackPressed() method
        //Log.d(TAG, "key down: " + event);
        var result = false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR &&
            keyCode == KeyEvent.KEYCODE_BACK
        ) {
            result = backPress()
        }
        if (result == false) {
            result = super.onKeyDown(keyCode, event)
        }
        return result
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        //Log.d(TAG, "key up: " + event);
        var result = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR &&
            keyCode == KeyEvent.KEYCODE_BACK
        ) {
            result = backPress()
        }
        if (result == false) {
            result = super.onKeyUp(keyCode, event)
        }
        return result
    }

    private fun backPress(): Boolean {
        if (pathHistory.isEmpty()) {
            return false
        }
        val oldPath = pathHistory.removeAt(HEAD)
        path = oldPath
        updateLastFolders()
        updateMountPoints()
        InitTask().execute()
        return true
    }

    /**
     * Saves the current folder to the list of last folders.
     */
    fun saveLastFolder() {
        val lastFolders = LastFolders.getInstance(this)
        lastFolders.put(path)
    }

    abstract override fun copyFile()
    abstract override fun moveFile()

    /**
     * Updates last folders group. Hides them if necessary.
     */
    private fun updateLastFolders() {
        val existedLastFolders = findPreference(PREF_LAST_FOLDERS)
//        if (intentInfo == null) {
//            return  //not initialized, should be finished immediately from onCreate()
//        }
        if (pathHistory.isEmpty()) {
            if (existedLastFolders == null) {
                preferenceScreen.addPreference(lastFoldersPreference)
            }
            listLastFolders()
        } else {
            if (existedLastFolders != null) {
                preferenceScreen.removePreference(lastFoldersPreference)
            }
        }
    }

    /**
     * Fills the list of last folders.
     */
    private fun listLastFolders() {
        val lastFoldersCategory = findPreference(PREF_LAST_FOLDERS) as PreferenceCategory
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (!preferences.getBoolean(PreferenceParams.PREF_SHOW_LAST_FOLDERS, true)) {
            preferenceScreen.removePreference(lastFoldersCategory)
            return
        }
        val lastFolders = LastFolders.getInstance(this)
        if (lastFolders.isEmpty) {
            preferenceScreen.removePreference(lastFoldersCategory)
            return
        }
        val lastFoldersNumber: Int = try {
            preferences.getString(
                PreferenceParams.PREF_LAST_FOLDERS_NUMBER, PreferenceParams.DEFAULT_LAST_FOLDERS_NUMBER
            )!!.toInt()
        } catch (e: NumberFormatException) {
            PreferenceParams.DEFAULT_LAST_FOLDERS_NUMBER_INT
        }
        lastFoldersCategory.removeAll()
        for (folder in lastFolders[lastFoldersNumber]) {
            //Log.d(TAG, folder.toString());
            val folderPref = PathFolderPreference(this, folder, this)
            lastFoldersCategory.addPreference(folderPref)
        }
        if (lastFoldersCategory.preferenceCount <= 0) {
            preferenceScreen.removePreference(lastFoldersCategory)
        }
    }

    /**
     * Updates mount points group. Hides them if necessary.
     */
    private fun updateMountPoints() {
        val existedMountPoints = findPreference(PREF_MOUNT_POINTS)
//        if (intentInfo == null) {
//            return  //not initialized, should be finished immediately from onCreate()
//        }
        if (pathHistory.isEmpty()) {
            if (existedMountPoints == null) {
                preferenceScreen.addPreference(mountPointsPreference)
            }
            listMountPoints()
        } else {
            if (existedMountPoints != null) {
                preferenceScreen.removePreference(mountPointsPreference)
            }
        }
    }

    /**
     * Fills the list of mount points.
     */
    private fun listMountPoints() {
        val mountPointsCategory = findPreference(PREF_MOUNT_POINTS) as PreferenceCategory
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (!preferences.getBoolean(PreferenceParams.PREF_SHOW_MOUNT_POINTS, true)) {
            preferenceScreen.removePreference(mountPointsCategory)
            return
        }
        val mountPoints = StartPaths(this, false).paths
        if (mountPoints.isEmpty()) {
            preferenceScreen.removePreference(mountPointsCategory)
            return
        }
        mountPointsCategory.removeAll()
        for (folder in mountPoints) {
            Log.d(Tag.TAG, folder.toString())
            val folderPref: Preference = MountPointFolderPreference(this, folder, this)
            mountPointsCategory.addPreference(folderPref)
        }
    }

    /**
     * Fills the list of subfolders.
     */
    private fun fillFolders() {
        val folders = findPreference(PREF_FOLDERS) as PreferenceCategory
        folders.removeAll()
        if ("/" != path.absolutePath) {
            val upFolder: Preference = FolderPreference(this, path.parentFile, this)
            upFolder.title = ".."
            folders.addPreference(upFolder)
        }
        if (this.folders.isNotEmpty()) {
            for (folder in this.folders) {
                val folderPref: Preference = FolderPreference(this, folder, this)
                folders.addPreference(folderPref)
            }
        }
    }

    /**
     * Runs getting of the folders list in a separate thread.
     * After updates the list of folders.
     */
    private fun listFolders() {
        ListFoldersTask().execute(path)
    }

    internal inner class ListFoldersTask : AsyncTask<File, Unit, List<File>>() {
        override fun onPreExecute() {
            setProgressBarIndeterminateVisibility(true)
        }

        protected override fun doInBackground(vararg params: File): List<File> {
            return getFolders(params[0])
        }

        override fun onPostExecute(result: List<File>) {
            folders.clear()
            folders.addAll(result)
            fillFolders()
            setProgressBarIndeterminateVisibility(false)
        }
    }

    /**
     * Returns unique file name for this folder.
     * If the filename is not exists in the current folder,
     * it returns unchanged.
     * Otherwise the integer suffix will be added to the filename:
     * "-1", "-2" etc...
     */
    fun getUniqueFileName(fileName: String?): String {
        var fileName = fileName
        if (fileName == null) {
            Log.w(Tag.TAG, "filename is null")
            fileName = ""
        }
//        if (path == null) {
//            Log.w(Tag.TAG, "path is null")
//            return fileName
//        }
        if (!File(path, fileName).exists()) {
            return fileName
        }
        var index = 1
        val dotIndex = fileName.lastIndexOf('.')
        var newName: String
        do {
            newName = if (dotIndex < 0) {
                "$fileName-$index"
            } else {
                fileName.substring(0, dotIndex) + "-" + index +
                    fileName.substring(dotIndex)
            }
            index++
        } while (File(path, newName).exists())
        return newName
    }

    /**
     * Creates the new folder.
     */
    private fun createFolder(folderName: String) {
        val newFolder = File(path, folderName)
        val result = newFolder.mkdirs()
        if (result) {
            Toast.makeText(this, R.string.folder_created, Toast.LENGTH_LONG).show()
            removeDialog(NEW_FOLDER_DIALOG) //to clear folder name, don't expect to create more folders
            listFolders()
        } else {
            Toast.makeText(this, R.string.folder_not_created, Toast.LENGTH_LONG).show()
        }
    }
    /**
     * Shows and logs the error message.
     *
     * @param messageId ID of the message to show
     * @param exception exception to write to logs (can be null)
     */
    /**
     * Shows the error message.
     */
    @JvmOverloads
    fun warn(messageId: Int, exception: Throwable? = null) {
        if (exception != null) {
            Log.w(Tag.TAG, exception.toString(), exception)
        }
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show()
    }
    /**
     * Shows and logs the error message and finished the activity
     * (with canceled result).
     *
     * @param messageId ID of the message to show
     * @param exception exception to write to logs (can be null)
     */
    /**
     * Shows the error message and finishes the activity.
     */
    @JvmOverloads
    fun error(messageId: Int, exception: Throwable? = null) {
        if (exception != null) {
            Log.e(Tag.TAG, exception.toString(), exception)
        }
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show()
        finish()
    }

    /**
     * Complete the action.
     */
    fun complete(messageId: Int) {
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show()
        finish()
    }

    /**
     * Complete the action.
     */
    fun complete(message: String?) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {
        /**
         * "Copy here" preference key
         */
        private const val PREF_COPY_HERE = "copy_here"

        /**
         * "Move here" preference key
         */
        private const val PREF_MOVE_HERE = "move_here"

        /**
         * Last folders preference category key
         */
        private const val PREF_LAST_FOLDERS = "last_folders"

        /**
         * Mount points preference category key
         */
        private const val PREF_MOUNT_POINTS = "mount_points"

        /**
         * "Folders" preference key
         */
        private const val PREF_FOLDERS = "folders"

        /**
         * Key to store the current path
         */
        private const val KEY_PATH = "path"

        /**
         * Key to store the path history
         */
        private const val KEY_PATH_HISTORY = "path_history"

        /**
         * New Folder dialog ID
         */
        private const val NEW_FOLDER_DIALOG = 0

        /**
         * Copy progress dialog ID
         */
        const val COPY_DIALOG = 1

        /**
         * Move progress dialog ID
         */
        const val MOVE_DIALOG = 2

        /**
         * Index of the list head to use list as a stack
         */
        private const val HEAD = 0

        /**
         * Makes the sorted list of this folder subfolders.
         */
        private fun getFolders(path: File?): List<File> {
            val result: MutableList<File> = ArrayList()
            val subFolders = path!!.listFiles(FileFilter { pathname -> pathname.isDirectory }) ?: return result
            val sortedFolders = Arrays.asList(*subFolders)
            Collections.sort(sortedFolders) { file1, file2 ->
                java.lang.String.CASE_INSENSITIVE_ORDER.compare(
                    file1.name,
                    file2.name
                )
            }
            for (subFolder in sortedFolders) {
                var folder: File
                folder = try {
                    subFolder.canonicalFile
                } catch (e: IOException) {
                    subFolder
                }
                result.add(folder)
            }
            return result
        }
    }
}

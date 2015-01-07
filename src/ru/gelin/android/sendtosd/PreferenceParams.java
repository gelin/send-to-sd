package ru.gelin.android.sendtosd;

/**
 * 	Some constants for preferences.
 */
public class PreferenceParams {
    
    /** "How to use" preference key */
    public static final String PREF_HOW_TO_USE = "how_to_use";
    
    /** "Initial folder" preference key */
    public static final String PREF_INITIAL_FOLDER = "initial_folder";
    /** "LAST_FOLDER" initial folder */
    public static final String LAST_FOLDER_INITIAL_FOLDER = "LAST_FOLDER";
    /** Initial folder default value */
    public static final String DEFAULT_INITIAL_FOLDER = LAST_FOLDER_INITIAL_FOLDER;
    
    /** Last folder preference key */
    public static final String PREF_LAST_FOLDER = "last_folder";
    /** Show last folders preference key */
    public static final String PREF_SHOW_LAST_FOLDERS = "show_last_folders";
    /** Last folders number preference key */
    public static final String PREF_LAST_FOLDERS_NUMBER = "last_folders_number";
    /** Default value for last folders number */
    public static final String DEFAULT_LAST_FOLDERS_NUMBER = "5";
    /** Default value for last folders number */
    public static final int DEFAULT_LAST_FOLDERS_NUMBER_INT = 5;
    
    public static enum ViewType {
    	FULL_SCREEN, DIALOG;
    }
    
    /** View type preference key */
    public static final String PREF_VIEW_TYPE = "view_type";
    /** View type default value */
    public static final String DEFAULT_VIEW_TYPE = String.valueOf(ViewType.FULL_SCREEN);
    
    private PreferenceParams() {
    	//avoid instantiation
    }

}

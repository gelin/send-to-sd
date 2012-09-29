package ru.gelin.android.sendtosd;

import android.app.Activity;
import android.view.Window;
import android.view.WindowManager;

/**
 *  Changed Activity view to be displayed as popup dialog.
 *  See http://stackoverflow.com/questions/11425020/actionbar-in-a-dialogfragment
 */
public class PopupDialogUtil {

    public static void showAsPopup(Activity activity) {
        //To show activity as dialog and dim the background, you need to declare android:theme="@style/PopupTheme" on for the chosen activity on the manifest
        activity.requestWindowFeature(Window.FEATURE_ACTION_BAR);
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.height = 850; //fixed height //TODO
        params.width = 600; //fixed width   //TODO
        params.alpha = 1.0f;
        params.dimAmount = 0.5f;
        activity.getWindow().setAttributes(params);
    }

}

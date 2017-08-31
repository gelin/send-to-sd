package ru.gelin.android.sendtosd;

import android.app.Activity;
import android.os.Build;
import android.view.Display;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

/**
 *  Changed Activity view to be displayed as popup dialog.
 *  See http://stackoverflow.com/questions/11425020/actionbar-in-a-dialogfragment
 */
public class PopupDialogUtil {

    public static void showAsPopup(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            return;
        }
        activity.requestWindowFeature(Window.FEATURE_ACTION_BAR);

        //To show activity as dialog and dim the background, you need to declare android:theme="@style/PopupTheme" on for the chosen activity on the manifest
        Window window = activity.getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        Display display = activity.getWindowManager().getDefaultDisplay();
        WindowManager.LayoutParams params = window.getAttributes();
        params.height = (int)(display.getHeight() * 0.95);
        params.width = Math.min((int)(display.getWidth() * 0.9), (int)(params.height * 0.85));
        params.gravity = Gravity.BOTTOM;
        params.alpha = 1.0f;
        params.dimAmount = 0.5f;
        window.setAttributes(params);
    }

}

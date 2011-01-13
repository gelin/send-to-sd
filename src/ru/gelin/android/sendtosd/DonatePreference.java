package ru.gelin.android.sendtosd;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 *  Preference to open Donate page.
 */
public class DonatePreference extends Preference {

    /** Donate page URL */
    static final String URL = "https://www.moneybookers.com/app/payment.pl?" +
            "pay_to_email=dnelubin%40gmail.com&language=EN&" +
            "amount=1&currency=USD&" +
            "detail1_description=Send+to+SD+card&" +
            "detail1_text=Donate+your+favorite+Android+application";
    /** Donate page URI */
    static final Uri URI = Uri.parse(URL);
    
    public DonatePreference(Context context) {
        super(context);
    }
    
    public DonatePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DonatePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     *  Opens the donate page in system browser.
     */
    public static void donate(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW, URI);
        context.startActivity(intent);
    }
    
    @Override
    protected void onClick() {
        super.onClick();
        donate(getContext());
    }
    
}

package ru.gelin.android.sendtosd.donate;

import ru.gelin.android.sendtosd.donate.Consts.PurchaseState;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class PurchaseDatabase {
	
	public static enum DonateStatus {
		NONE,
		PENDING,
		PURCHASED;
		
		public static DonateStatus valueOf(PurchaseState state) {
			switch (state) {
			case PURCHASED:
				return PURCHASED;
			default:
				return NONE;
			}
		}
		
	}
	
	/** Donate purchase status key */
	static final String PREF_DONATE = "donate";
	
	SharedPreferences preferences;
	
	public PurchaseDatabase(Context context) {
		this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	public void setStatus(PurchaseState state) {
		Editor editor = this.preferences.edit();
		editor.putString(PREF_DONATE, String.valueOf(DonateStatus.valueOf(state)));
		editor.commit();
	}
	
	public void setStatus(DonateStatus status) {
		Editor editor = this.preferences.edit();
		editor.putString(PREF_DONATE, String.valueOf(status));
		editor.commit();
	}
	
	public DonateStatus getStatus() {
		return DonateStatus.valueOf(this.preferences.getString(PREF_DONATE, String.valueOf(DonateStatus.NONE)));
	}

}

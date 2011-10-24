package ru.gelin.android.sendtosd.donate;

import ru.gelin.android.sendtosd.donate.Consts.PurchaseState;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class DonateDatabase {
	
	public static enum DonateStatus {
		NONE,
		EXPECTING,
		PENDING,
		PURCHASED;
		
		public static DonateStatus valueOf(PurchaseState state) {
			switch (state) {
			case PURCHASED:
				return PURCHASED;
			default:
				return EXPECTING;
			}
		}
		
	}
	
	/** Donate purchase status key */
	static final String PREF_DONATE = "donate";
	
	SharedPreferences preferences;
	
	public DonateDatabase(Context context) {
		this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	public void setStatus(PurchaseState state) {
		setStatus(DonateStatus.valueOf(state));
	}
	
	public void setStatus(DonateStatus status) {
		DonateStatus oldStatus = getStatus();
		switch (oldStatus) {
		case NONE:
			updateStatus(status);
			break;
		case EXPECTING:
			if (!DonateStatus.NONE.equals(status)) {	//disallow back to NONE
				updateStatus(status);
			}
			break;
		case PENDING:
			if (DonateStatus.EXPECTING.equals(status) || DonateStatus.PURCHASED.equals(status)) {
				updateStatus(status);	//allow only back to EXPECTING or forward to PURCHASED
			}
			break;
		}
		//other status transitions are disallowed.
	}
	
	void updateStatus(DonateStatus status) {
		Editor editor = this.preferences.edit();
		editor.putString(PREF_DONATE, String.valueOf(status));
		editor.commit();
	}
	
	/**
	 * 	Return the donate status.
	 *  NONE means that nothing was stored in the DB before, need to restore
	 *  transactions. 
	 */
	public DonateStatus getStatus() {
		return DonateStatus.valueOf(this.preferences.getString(PREF_DONATE, String.valueOf(DonateStatus.NONE)));
	}

}

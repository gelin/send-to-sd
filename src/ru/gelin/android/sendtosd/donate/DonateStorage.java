package ru.gelin.android.sendtosd.donate;

import ru.gelin.android.sendtosd.donate.Consts.PurchaseState;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

/**
 * 	Stores the donate status.
 * 	There are the following statuses:
 * 	<ul>
 * 	<li>NONE - default status, means that no information about donation was saved, 
 * 	the purchase history should be restored</li>
 *  <li>EXPECTING - we already checked that there was no donates, should allow the user to make the donation</li>
 *  <li>PURCHASED - the donation is done and completed, no need to start billing service, thanks the user</li>
 * 	</ul>
 */
public class DonateStorage {
	
	public static enum DonateStatus {
		NONE,
		EXPECTING,
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
	
	public DonateStorage(Context context) {
		this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	public void setStatus(PurchaseState state) {
		setStatus(DonateStatus.valueOf(state));
	}
	
	/**
	 * 	Sets the new status.
	 * 	Validates the state, only the following transitions are allowed:<br>
	 *	NONE -> EXPECTING, PURCHASED<br>
	 *  EXPECTING -> PURCHASED<br>
	 */
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

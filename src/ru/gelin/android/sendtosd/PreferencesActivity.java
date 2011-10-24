package ru.gelin.android.sendtosd;

import ru.gelin.android.sendtosd.donate.BillingService;
import ru.gelin.android.sendtosd.donate.DonateDatabase;
import ru.gelin.android.sendtosd.donate.PurchaseObserver;
import ru.gelin.android.sendtosd.donate.ResponseHandler;
import ru.gelin.android.sendtosd.donate.BillingService.RequestPurchase;
import ru.gelin.android.sendtosd.donate.BillingService.RestoreTransactions;
import ru.gelin.android.sendtosd.donate.Consts.PurchaseState;
import ru.gelin.android.sendtosd.donate.Consts.ResponseCode;
import ru.gelin.android.sendtosd.donate.DonateDatabase.DonateStatus;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;

/**
 *  Application preferences.
 */
public class PreferencesActivity extends PreferenceActivity {

	static final String PREF_DONATE = "donate";
	static final String PREF_DONATE_CATEGORY = "donate_category";
	
	/** Special package name for donate version of the app */
	static final String DONATE_PACKAGE_NAME = "ru.gelin.android.sendtosd.donate";
	
	//static final String DONATE_ITEM_ID = "donate";
	static final String DONATE_ITEM_ID = "android.test.purchased";	//for tests	
	
	Preference donateCategory;
	Preference donate;
	
	DonateDatabase donateDb;
	DonatePurchaseObserver purchaseObserver;
	BillingService billingService;
	Handler handler = new Handler();
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.app_preferences);
        this.donateCategory = findPreference(PREF_DONATE_CATEGORY);
    	this.donate = findPreference(PREF_DONATE);
    	
    	if (DONATE_PACKAGE_NAME.equals(getPackageName())) {
    		updateDonateView(DonateStatus.PURCHASED);
    	} else {
    		this.donateDb = new DonateDatabase(this);
    		DonateStatus status = this.donateDb.getStatus();
    		updateDonateView(status);
    		if (!DonateStatus.PURCHASED.equals(status)) {
    			initBilling(status);
    		}
    	}
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	ResponseHandler.register(this.purchaseObserver);
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	ResponseHandler.unregister(this.purchaseObserver);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.billingService != null) {
        	this.billingService.unbind();
        }
    }
    
    void updateDonateView(DonateStatus status) {
    	Preference category = findPreference(PREF_DONATE_CATEGORY);
    	switch (status) {
    	case NONE:
    		getPreferenceScreen().removePreference(donateCategory);
    		break;
    	case EXPECTING:
    		if (category == null) {
    			getPreferenceScreen().addPreference(donateCategory);
    		}
    		donate.setTitle(R.string.donate);
			donate.setSummary(R.string.donate_summary);
			donate.setEnabled(true);
			break;
    	case PENDING:
    		if (category == null) {
    			getPreferenceScreen().addPreference(donateCategory);
    		}
    		donate.setTitle(R.string.donate);
			donate.setSummary(R.string.donate_summary);
			donate.setEnabled(false);
			break;
    	case PURCHASED:
    		if (category == null) {
    			getPreferenceScreen().addPreference(donateCategory);
    		}
    		donate.setTitle(R.string.donate_thanks);
			donate.setSummary("");
			donate.setEnabled(false);
			break;
    	}
    }
    
    void initBilling(DonateStatus status) {
    	this.purchaseObserver = new DonatePurchaseObserver();
        this.billingService = new BillingService();
        this.billingService.setContext(this);
        ResponseHandler.register(this.purchaseObserver);
        if (this.billingService.checkBillingSupported()) {
        	if (DonateStatus.NONE.equals(status)) {
        		this.donateDb.setStatus(DonateStatus.EXPECTING);
        		updateDonateView(DonateStatus.EXPECTING);
        	}
        } else {
        	updateDonateView(DonateStatus.NONE);
        	return;
        }
        if (DonateStatus.NONE.equals(status)) {
        	this.billingService.restoreTransactions();
        }
        
        this.donate.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				PreferencesActivity.this.billingService.requestPurchase(DONATE_ITEM_ID, null);
				preference.setEnabled(false);
				return true;
			}
		});
    }
    
    class DonatePurchaseObserver extends PurchaseObserver {

    	DonateDatabase db;
    	
		public DonatePurchaseObserver() {
			super(PreferencesActivity.this, PreferencesActivity.this.handler);
			this.db = PreferencesActivity.this.donateDb;
		}

		@Override
		public void onBillingSupported(boolean supported) {
			if (supported) {
				if (DonateStatus.NONE.equals(this.db.getStatus())) {
	        		this.db.setStatus(DonateStatus.EXPECTING);
	        		updateDonateView(DonateStatus.EXPECTING);
	        	}
            } else {
            	updateDonateView(DonateStatus.NONE);
            }
		}

		@Override
		public void onPurchaseStateChange(PurchaseState purchaseState,
				String itemId, long purchaseTime, String developerPayload) {
            if (PurchaseState.PURCHASED.equals(purchaseState) &&
            		DONATE_ITEM_ID.equals(itemId)) {
            	this.db.setStatus(purchaseState);	//TODO: is this necessary?
            	updateDonateView(DonateStatus.PURCHASED);
            }
		}

		@Override
		public void onRequestPurchaseResponse(RequestPurchase request,
				ResponseCode responseCode) {
			if (responseCode == ResponseCode.RESULT_OK) {
				this.db.setStatus(DonateStatus.PENDING);
				updateDonateView(DonateStatus.PENDING);
            } else {
				this.db.setStatus(DonateStatus.EXPECTING);
				updateDonateView(DonateStatus.EXPECTING);
            }
		}

		@Override
		public void onRestoreTransactionsResponse(RestoreTransactions request,
				ResponseCode responseCode) {
     		updateDonateView(this.db.getStatus());
		}
    	
    }

}

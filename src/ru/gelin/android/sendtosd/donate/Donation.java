package ru.gelin.android.sendtosd.donate;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.vending.billing.IInAppBillingService;
import ru.gelin.android.sendtosd.Tag;

import java.util.ArrayList;

/**
 * Handles all functionality related to donation processing/verification.
 * Interacts with {@link com.android.vending.billing.IInAppBillingService}
 */
public class Donation {

    //static final String PRODUCT_ID = "android.test.purchased";	//for tests
    static final String PRODUCT_ID = "donate";

    static final int API_VERSION = 3;
    static final int RESULT_OK = 0;
    static final String ITEM_TYPE = "inapp";

    DonateStatus status = DonateStatus.NONE;
    Context context;
    DonateStatusListener listener;
    ServiceConnection connection;
    IInAppBillingService billingService;

    public Donation(Context context, DonateStatusListener listener) {
        this.context = context;
        this.listener = listener;
        init();
    }

    public void destroy() {
        if (this.billingService != null) {
            context.unbindService(this.connection);
        }
    }

    public DonateStatus getStatus() {
        return this.status;
    }

    /**
     *  Initiates the donation state, binds and makes calls to billing service.
     *  Do it in background thread, calls {@link ru.gelin.android.sendtosd.donate.DonateStatusListener}
     *  if the status is changed.
     */
    void init() {
        this.connection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                Donation.this.billingService = null;
            }
            @Override
            public void onServiceConnected(ComponentName name,
                                           IBinder service) {
                Donation.this.billingService = IInAppBillingService.Stub.asInterface(service);
                new InitBillingTask().execute();
            }
        };

        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        context.bindService(serviceIntent, this.connection, Context.BIND_AUTO_CREATE);
    }

    class InitBillingTask extends AsyncTask<Void, Void, DonateStatus> {
        @Override
        protected DonateStatus doInBackground(Void... params) {
            if (!checkBillingSupported()) {
                return DonateStatus.NONE;
            }
            if (checkDonatePurchased()) {
                return DonateStatus.PURCHASED;
            } else {
                return DonateStatus.EXPECTING;
            }
        }
        @Override
        protected void onPostExecute(DonateStatus status) {
            setStatus(status);
        }
    }

    void setStatus(DonateStatus status) {
        this.status = status;
        if (this.listener != null) {
            this.listener.onDonateStatusChanged(status);
        }
    }

    boolean checkBillingSupported() {
        if (this.billingService == null) {
            return false;
        }
        try {
            if (RESULT_OK == this.billingService.isBillingSupported(
                    API_VERSION, this.context.getPackageName(), ITEM_TYPE)) {
                return true;
            } else {
                return false;
            }
        } catch (RemoteException e) {
            Log.w(Tag.TAG, "isBillingSupported() failed", e);
            return false;
        }
    }

    boolean checkDonatePurchased() {
        if (this.billingService == null) {
            return false;
        }
        try {
            Bundle ownedItems = this.billingService.getPurchases(
                    API_VERSION, this.context.getPackageName(), ITEM_TYPE, null);
            int response = ownedItems.getInt("RESPONSE_CODE");
            if (RESULT_OK != response) {
                return false;
            }
            ArrayList<String> ownedSkus =
                    ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
            if (ownedSkus.contains(PRODUCT_ID)) {
                return true;
            } else {
                return false;
            }
        } catch (RemoteException e) {
            Log.w(Tag.TAG, "getPurchases() failed", e);
            return false;
        }
    }

}

package ru.gelin.android.sendtosd.donate;

/**
 * The interface to receive notifications on the donate status changes.
 */
public interface DonateStatusListener {

    /**
     *  Is called when the donate status is changed or updated.
     *  Is called from the main thread.
     *  @param status new status
     */
    public void onDonateStatusChanged(DonateStatus status);

}

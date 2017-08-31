package ru.gelin.android.sendtosd.donate;

/**
 *  The current known status of the donation.
 *  NONE - the donation is not expeted, the Googlel Play billing is not available.
 *  EXPECTING - we want a donate.
 *  PURCHASED - the donate was successfully purchased.
 */
public enum DonateStatus {
    NONE,
    EXPECTING,
    PURCHASED
}

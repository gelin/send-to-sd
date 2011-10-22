package ru.gelin.android.sendtosd.intent;

import ru.gelin.android.sendtosd.SendToSdException;

public class IntentException extends SendToSdException {

	private static final long serialVersionUID = 3798683935969857872L;

	public IntentException() {
        super();
    }

    public IntentException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public IntentException(String detailMessage) {
        super(detailMessage);
    }

    public IntentException(Throwable throwable) {
        super(throwable);
    }

}

package ru.gelin.android.sendtosd.intent;

import ru.gelin.android.sendtosd.SendToSdException;

public class IntentException extends SendToSdException {

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

package ru.gelin.android.sendtosd.intent;

public class IntentFileException extends IntentException {

    public IntentFileException() {
        super();
    }

    public IntentFileException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public IntentFileException(String detailMessage) {
        super(detailMessage);
    }

    public IntentFileException(Throwable throwable) {
        super(throwable);
    }

}

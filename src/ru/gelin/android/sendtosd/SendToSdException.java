package ru.gelin.android.sendtosd;

public class SendToSdException extends Exception {

    public SendToSdException() {
        super();
    }

    public SendToSdException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public SendToSdException(String detailMessage) {
        super(detailMessage);
    }

    public SendToSdException(Throwable throwable) {
        super(throwable);
    }

}

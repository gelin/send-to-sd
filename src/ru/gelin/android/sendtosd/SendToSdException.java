package ru.gelin.android.sendtosd;

public class SendToSdException extends Exception {

	private static final long serialVersionUID = 5504323369029781105L;

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

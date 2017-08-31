package ru.gelin.android.sendtosd.intent;

public class IntentFileException extends IntentException {

	private static final long serialVersionUID = -6679210790925502257L;

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

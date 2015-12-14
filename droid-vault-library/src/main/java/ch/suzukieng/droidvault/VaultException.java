package ch.suzukieng.droidvault;

/**
 * @author Alex Suzuki, Suzuki Engineering GmbH, 2015
 */
public class VaultException extends Exception {

    public VaultException() {
    }

    public VaultException(String detailMessage) {
        super(detailMessage);
    }

    public VaultException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public VaultException(Throwable throwable) {
        super(throwable);
    }
}

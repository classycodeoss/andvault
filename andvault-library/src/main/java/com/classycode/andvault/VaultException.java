package com.classycode.andvault;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2015
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

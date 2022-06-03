package org.apache.sling.distribution.common;

/**
 * A type of exception that contains additional error metadata that link to external
 * documentation and allows for distributed tracing, through specific ERROR_CODES.
 */
public class DocumentedException extends Exception {
    private ErrorCode errorCode;

    public DocumentedException(Throwable e, ErrorCode errorCode) {
        super(e);
        this.errorCode = errorCode;
    }

    public DocumentedException(Throwable e) {
        super(e);
        errorCodeNotDefined();
    }

    public DocumentedException(String string, ErrorCode errorCode) {
        super(string);
        this.errorCode = errorCode;
    }

    public DocumentedException(String string) {
        super(string);
        errorCodeNotDefined();
    }

    public DocumentedException(String string, Throwable cause, ErrorCode errorCode) {
        super(string, cause);
        this.errorCode = errorCode;
    }

    public DocumentedException(String string, Throwable cause) {
        super(string, cause);
        errorCodeNotDefined();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    private void errorCodeNotDefined() {
        this.errorCode = ErrorCode.UNKNOWN_ERROR;
    }
}

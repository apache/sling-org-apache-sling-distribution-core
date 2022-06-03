package org.apache.sling.distribution.common.error;

/**
 * This class defines specific system error codes which allow for easier propagation of exception
 * data throughout the system, and act as a single point of change for the documentation code,
 * description and the HTTP status that an exception should generate.
 */
public enum ErrorCode {
    /**
     * A generic unexpected error in the system.
     */
    UNKNOWN_ERROR("unknown", "An unexpected error has occurred.", 500),
    /**
     * Error that happens when the client tries to distribute a package that exceeds the
     * configured package size limit.
     */
    DISTRIBUTION_PACKAGE_SIZE_LIMIT_EXCEEDED("package_limit_exceeded", "Package has exceeded the" +
            " limit bytes size.", 400);

    private final String code;
    private final String description;
    private final int httpStatusCode;

    ErrorCode(String code, String description, int httpStatusCode) {
        this.code = code;
        this.description = description;
        this.httpStatusCode = httpStatusCode;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    @Override
    public String toString() {
        return code + ": " + description;
    }
}

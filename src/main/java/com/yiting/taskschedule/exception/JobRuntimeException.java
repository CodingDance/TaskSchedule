package com.yiting.taskschedule.exception;

/**
 * 
 */
public class JobRuntimeException extends RuntimeException {

    private static final long serialVersionUID = -3316039062933001109L;

    public JobRuntimeException() {
    }

    /**
     * @param message
     */
    public JobRuntimeException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public JobRuntimeException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public JobRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}

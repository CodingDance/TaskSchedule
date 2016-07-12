package com.yiting.taskschedule.exception;

/**
 * 
 * @author Feng Changjian (hzfengchj@corp.netease.com)
 * @version $Id: JobRuntimeException.java, v 1.0 2015年3月22日 下午9:35:21
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

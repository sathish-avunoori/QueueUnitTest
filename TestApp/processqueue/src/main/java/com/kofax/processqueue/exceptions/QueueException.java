package com.kofax.processqueue.exceptions;


public class QueueException extends Exception {

    /**
     * This is a constructor of the custom queue exception class with empty parameters.
     */
    public QueueException() {
    }

    /**
     * This is a constructor of the custom queue exception class with string parameter.
     * @param detailMessage
     */
    public QueueException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * This is a constructor of the custom queue exception class with string parameter and throwable parameters.
     * @param detailMessage
     * @param throwable
     */
    public QueueException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    /**
     * This is a constructor of the custom queue exception class with throwable parameter.
     * @param throwable
     */
    public QueueException(Throwable throwable) {
        super(throwable);
    }
}

package com.simplecity.amp_library.utils;

import timber.log.Timber;

public class TimeLogger {

    private long initialTime;
    private long intervalTime;

    public TimeLogger() {
        initialTime = System.currentTimeMillis();
        intervalTime = System.currentTimeMillis();
    }

    public long getIntervalTime() {
        long elapsedTime = System.currentTimeMillis() - intervalTime;
        intervalTime = System.currentTimeMillis();
        return elapsedTime;
    }

    public long getTotalTime() {
        return System.currentTimeMillis() - initialTime;
    }

    /**
     * Lpg the time since the last logInterval() was called.
     *
     * @param tag     the tag to use for the log message
     * @param message the message to output
     */
    public void logInterval(String tag, String message) {

        Timber.d(tag, message
                + "\n Interval: " + getIntervalTime()
                + "\n Total: " + getTotalTime()
        );
        intervalTime = System.currentTimeMillis();
    }

}
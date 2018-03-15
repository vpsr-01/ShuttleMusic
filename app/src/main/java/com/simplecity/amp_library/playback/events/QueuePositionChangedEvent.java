package com.simplecity.amp_library.playback.events;

public class QueuePositionChangedEvent implements MusicEventRelay.MusicEvent {

    public static final String TYPE = "QUEUE_POSITION_CHANGED";

    private int queuePosition;

    public QueuePositionChangedEvent(int queuPosition) {
        this.queuePosition = queuePosition;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public int getQueuePosition() {
        return queuePosition;
    }

    @Override
    public String toString() {
        return "QueuePositionChangedEvent{" +
                "queuePosition=" + queuePosition +
                '}';
    }
}

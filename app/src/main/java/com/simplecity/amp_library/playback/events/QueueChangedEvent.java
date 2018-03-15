package com.simplecity.amp_library.playback.events;

import com.simplecity.amp_library.model.Song;

import java.util.List;

public class QueueChangedEvent implements MusicEventRelay.MusicEvent {

    public static final String TYPE = "QUEUE_CHANGED";

    private List<Song> queue;

    public QueueChangedEvent(List<Song> queue) {
        this.queue = queue;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public List<Song> getQueue() {
        return queue;
    }

    @Override
    public String toString() {
        return "QueueChangedEvent{" +
                "queue size: " + queue.size() +
                '}';
    }
}

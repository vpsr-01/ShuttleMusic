package com.simplecity.amp_library.playback.events;

public class PlayStateChangedEvent implements MusicEventRelay.MusicEvent {

    public static final String TYPE = "PLAYSTATE_CHANGED";

    private boolean isPlaying;

    public PlayStateChangedEvent(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public boolean getIsPlaying() {
        return isPlaying;
    }

    @Override
    public String toString() {
        return "PlayStateChangedEvent{" +
                "isPlaying=" + isPlaying +
                '}';
    }
}

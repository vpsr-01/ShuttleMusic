package com.simplecity.amp_library.playback.events;

import android.support.annotation.Nullable;

import com.simplecity.amp_library.model.Song;

public class MetadataChangedEvent implements MusicEventRelay.MusicEvent {

    public static final String TYPE = "METADATA_CHANGED";

    @Nullable
    private Song song;

    public MetadataChangedEvent(@Nullable Song song) {
        this.song = song;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Nullable
    public Song getSong() {
        return song;
    }

    @Override
    public String toString() {
        return "MetadataChangedEvent{" +
                "song=" + (song != null ? song.name : null) +
                '}';
    }
}

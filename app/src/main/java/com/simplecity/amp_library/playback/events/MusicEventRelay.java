package com.simplecity.amp_library.playback.events;

import android.support.annotation.NonNull;

import com.jakewharton.rxrelay2.PublishRelay;

import io.reactivex.Observable;
import timber.log.Timber;

public class MusicEventRelay {

    public interface MusicEvent {
        String getType();
    }

    private PublishRelay<MusicEvent> relay = PublishRelay.create();

    private static MusicEventRelay instance;

    public static MusicEventRelay getInstance() {
        if (instance == null) {
            instance = new MusicEventRelay();
        }
        return instance;
    }

    public MusicEventRelay() {

    }

    public Observable<MusicEvent> getEvents() {
        return relay;
    }

    public void sendEvent(@NonNull MusicEvent event) {
        Timber.d("MusicEventRelay emitting event: %s", event.toString());
        relay.accept(event);
    }

}

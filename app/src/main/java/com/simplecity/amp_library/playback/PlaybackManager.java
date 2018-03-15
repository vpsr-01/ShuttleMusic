package com.simplecity.amp_library.playback;

import android.annotation.SuppressLint;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.playback.Playback;
import com.simplecity.amp_library.playback.settings.MusicSettings;

import java.util.List;

import io.reactivex.Completable;
import timber.log.Timber;

public class PlaybackManager implements Playback.Callback {

    interface PlaybackServiceCallback {
        void onPlaybackStart();

        void onPlaybackStop();

        void onPlaybackStateUpdated(PlaybackStateCompat playbackStateCompat);

        void showNotification();
    }

    private static final String TAG = "PlaybackManager";

    @NonNull
    private QueueManager queueManager;

    @NonNull
    private PlaybackServiceCallback playbackServiceCallback;

    @NonNull
    private Playback playback;

    @NonNull
    private MediaSessionCallback mediaSessionCallback;

    @PlaybackStateCompat.State
    private int playbackState = PlaybackStateCompat.STATE_NONE;

    public PlaybackManager(
            @NonNull QueueManager queueManager,
            @NonNull PlaybackServiceCallback playbackServiceCallback,
            @NonNull Playback playback) {
        this.queueManager = queueManager;
        this.playbackServiceCallback = playbackServiceCallback;
        this.playback = playback;
        this.playback.setCallback(this);

        mediaSessionCallback = new MediaSessionCallback();

        queueManager.reloadQueue()
                .andThen(Completable.defer(() -> prepare()))
                .andThen(Completable.defer(() -> Completable.fromAction(() -> setQueuePosition(MusicSettings.getInstance().getQueuePosition()))))
                .andThen(Completable.defer(() -> Completable.fromAction(() -> seekTo(MusicSettings.getInstance().getPlaybackPosition()))))
                .onErrorResumeNext(throwable -> {
                    Timber.e(throwable, "Failed to reload queue");
                    return Completable.complete();
                })
                .subscribe();
    }

    @SuppressLint("CheckResult")
    public void open(List<Song> songs, int position) {
        queueManager.open(songs, position);
        prepare().subscribe(this::play);
    }

    public Completable prepare() {
        Timber.d("prepare() called");
        return playback.prepare();
    }

    @SuppressLint("CheckResult")
    void play() {
        Timber.d("play() called");
        playback.start();
    }

    void pause() {
        Timber.d("pause() called");
        if (isPlaying()) {
            playback.pause();
            savePlaybackPosition();
        }
    }

    public void stop() {
        Timber.d("stop() called");
        savePlaybackPosition();
        playback.stop();
    }

    public void setQueuePosition(int position) {
        queueManager.setQueuePosition(position);
        playback.getPlaybackQueueManager().setQueuePosition(position);
    }

    public void skipToNext() {
        skipToNext(false);
    }

    /**
     * Skip to the next track, and begin playback if the skip operation was successful.
     *
     * @param force true to force the queue skip action to return 'success' in the event that we
     *              reached the end of the queue and wrapped back around to the start. i.e. if repeat
     *              is off, and we're at the end of the queue when skip is called, we'll go back to
     *              position 0, return true and continue playing.
     */
    public void skipToNext(boolean force) {
        if (queueManager.skip(force)) {
            playback.getPlaybackQueueManager().skipToNext();
            Song currentSong = queueManager.getCurrentSong();
            if (currentSong != null) {
                play();
                savePlaybackPosition();
            }
        } else {
            Log.i(TAG, "Failed to skip");
            pause();
            seekTo(0);
        }
    }

    public void skipToPrevious() {
        if (queueManager.prev()) {
            playback.getPlaybackQueueManager().skipToPrev();
            Song currentSong = queueManager.getCurrentSong();
            if (currentSong != null) {
                play();
                savePlaybackPosition();
            }
        } else {
            Log.i(TAG, "Failed to prev");
            pause();
        }
    }

    public void seekTo(long position) {
        Timber.d("seekTo() called: %d", position);
        playback.seekTo(position);
    }

    boolean isPlaying() {
        return playback.isPlaying();
    }

    public long getPosition() {
        return playback.getPosition();
    }

    public long getDuration() {
        Song currentSong = queueManager.getCurrentSong();
        if (currentSong != null) {
            return currentSong.duration;
        }
        return 0;
    }

    public List<Song> getCurrentPlaylist() {
        return queueManager.getCurrentPlaylist();
    }

    public int getQueuePosition() {
        return queueManager.getQueuePosition();
    }

    public void addToQueue(List<Song> songs) {
        queueManager.addToQueue(songs);
    }

    public void playNext(List<Song> songs) {
        queueManager.playNext(songs);
    }

    public void moveSong(int from, int to) {
        queueManager.moveSong(from, to);
        playback.getPlaybackQueueManager().moveSong(from, to);
    }

    public void clearQueue() {
        queueManager.clearQueue();
        playback.getPlaybackQueueManager().clearQueue();
    }

    public void removeSong(int position) {
        queueManager.removeSong(position, this::stop, this::play);
        playback.getPlaybackQueueManager().removeSong(position);
    }

    public void removeSongs(List<Song> songs) {
        queueManager.removeSongs(songs, this::stop, this::play);
        playback.getPlaybackQueueManager().removeSongs(songs);
    }

    public Song getCurrentSong() {
        return queueManager.getCurrentSong();
    }

    public void setShuffleMode(String shuffleMode) {
        queueManager.setShuffleMode(shuffleMode);
        playback.getPlaybackQueueManager().setShuffleMode(shuffleMode);
    }

    public String getShuffleMode() {
        return queueManager.getShuffleMode();
    }

    public void setRepeatMode(String repeatMode) {
        queueManager.setRepeatMode(repeatMode);
    }

    public String getRepeatMode() {
        return queueManager.getRepeatMode();
    }

    public MediaSessionCompat.Callback getMediaSessionCallback() {
        return mediaSessionCallback;
    }

    public void saveState() {
        queueManager.saveQueue();
        savePlaybackPosition();
    }

    public void savePlaybackPosition() {
        Timber.d("Saving playback position: %d", playback.getPosition());
        MusicSettings.getInstance().setPlaybackPosition(playback.getPosition());
    }

    private void updatePlaybackState(@Nullable String errorMessage) {
        Timber.d("updatePlaybackState() called. State: %s", getPlaybackStateString(playback.getState()));
        long position = playback.getPosition();

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder().setActions(getAvailableActions());
        @PlaybackStateCompat.State int state = playback.getState();

        if (errorMessage != null) {
            stateBuilder.setErrorMessage(PlaybackStateCompat.ERROR_CODE_UNKNOWN_ERROR, errorMessage);
        }
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        playbackServiceCallback.onPlaybackStateUpdated(stateBuilder.build());

        if (state == PlaybackStateCompat.STATE_PLAYING || (playbackState == PlaybackStateCompat.STATE_PLAYING && state == PlaybackStateCompat.STATE_PAUSED)) {
            playbackServiceCallback.showNotification();
        }

        if (state == PlaybackStateCompat.STATE_PLAYING) {
            playbackServiceCallback.onPlaybackStart();
        } else if (state == PlaybackStateCompat.STATE_PAUSED || state == PlaybackStateCompat.STATE_STOPPED) {
            playbackServiceCallback.onPlaybackStop();
        }

        playbackState = state;
    }

    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        if (playback.isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }
        return actions;
    }

    // Playback.Callback implementation

    @Override
    public void onPlaybackStateChanged(int state) {
        updatePlaybackState(null);
    }

    @Override
    public void onError(String errorMessage) {
        updatePlaybackState(errorMessage);
    }


    // MediaSession Callback

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onSkipToNext() {
            skipToNext(true);
        }

        @Override
        public void onSkipToPrevious() {
            skipToPrevious();
        }

        @Override
        public void onPlay() {
            play();
        }

        @Override
        public void onPause() {
            pause();
        }

        @Override
        public void onSeekTo(long pos) {
            seekTo(pos);
        }

        @Override
        public void onStop() {
            stop();
        }
    }

    private String getPlaybackStateString(@PlaybackStateCompat.State int state) {
        switch (state) {
            case PlaybackStateCompat.STATE_NONE:
                return "STATE_NONE";
            case PlaybackStateCompat.STATE_STOPPED:
                return "STATE_STOPPED";
            case PlaybackStateCompat.STATE_PAUSED:
                return "STATE_PAUSED";
            case PlaybackStateCompat.STATE_PLAYING:
                return "STATE_PLAYING";
            case PlaybackStateCompat.STATE_ERROR:
                return "STATE_ERROR";
        }
        return "STATE_UNKNOWN";
    }
}
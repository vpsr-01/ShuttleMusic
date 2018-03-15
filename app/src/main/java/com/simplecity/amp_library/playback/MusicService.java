package com.simplecity.amp_library.playback;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.events.MetadataChangedEvent;
import com.simplecity.amp_library.playback.events.MusicEventRelay;
import com.simplecity.amp_library.playback.events.PlayStateChangedEvent;
import com.simplecity.amp_library.playback.events.QueueChangedEvent;
import com.simplecity.amp_library.playback.events.QueuePositionChangedEvent;
import com.simplecity.amp_library.playback.playback.ExoPlayerPlayback;

import java.util.List;

import timber.log.Timber;

public class MusicService extends Service {

    private static final String TAG = "MusicService";

    // The action of the incoming Intent indicating that it contains a command
    // to be executed (see {@link #onStartCommand})
    public static final String ACTION_CMD = "com.shuttle.ACTION_CMD";

    // The key in the extras of the incoming Intent indicating the command that
    // should be executed (see {@link #onStartCommand})
    public static final String CMD_NAME = "CMD_NAME";

    // A value of a CMD_NAME key in the extras of the incoming Intent that
    // indicates that the music playback should be paused (see {@link #onStartCommand})
    public static final String CMD_PAUSE = "CMD_PAUSE";

    private final IBinder mBinder = new LocalBinder(this);

    private PlaybackManager playbackManager;

    private Equalizer equalizer;

    private MediaSessionCompat mediaSession;

    private MusicNotificationManager musicNotificationManager;

    @SuppressLint("CheckResult")
    @Override
    public void onCreate() {
        Timber.d("onCreate() called");
        super.onCreate();

        QueueManager queueManager = new QueueManager(this, metadataUpdateListener);

        playbackManager = new PlaybackManager(queueManager, playbackServiceCallback, new ExoPlayerPlayback(this, queueManager));

        equalizer = new Equalizer(this);

        mediaSession = new MediaSessionCompat(this, "MusicService");
        mediaSession.setCallback(playbackManager.getMediaSessionCallback());
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        try {
            musicNotificationManager = new MusicNotificationManager(this);
        } catch (RemoteException e) {
            throw new IllegalStateException("Could not create a MediaNotificationManager", e);
        }
    }

    @Override
    public int onStartCommand(Intent startIntent, int flags, int startId) {
        if (startIntent != null) {
            String action = startIntent.getAction();
            String command = startIntent.getStringExtra(CMD_NAME);
            if (ACTION_CMD.equals(action)) {
                if (CMD_PAUSE.equals(command)) {
                    playbackManager.pause();
                }
            } else {
                // Try to handle the intent as a media button event wrapped by MediaButtonReceiver
                MediaButtonReceiver.handleIntent(mediaSession, startIntent);
            }
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Timber.d("onBind() called");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Timber.d("onRebind() called");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Timber.d("onUnbind() called");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Timber.d("onDestroy() called");

        saveState();
        stop();

        musicNotificationManager.stopNotification();

        mediaSession.release();

        super.onDestroy();
    }

    // Playback

    @SuppressLint("CheckResult")
    public void open(@NonNull List<Song> songs, @IntRange(from = 0) int position) {
        playbackManager.open(songs, position);
    }

    public void openFile(@NonNull String filename) {

    }

    public void play() {
        playbackManager.play();
    }

    public void pause() {
        playbackManager.pause();
    }

    public void stop() {
        playbackManager.stop();
    }

    public void skip() {
        skip(false);
    }

    /**
     * Skip to the next track, and begin playback if the skip operation was successful.
     *
     * @param force true to force the queue skip action to return 'success' in the event that we
     *              reached the end of the queue and wrapped back around to the start. i.e. if repeat
     *              is off, and we're at the end of the queue when skip is called, we'll go back to
     *              position 0, return true and continue playing.
     */
    public void skip(boolean force) {
        playbackManager.skipToNext(force);
    }

    public void prev() {
        playbackManager.skipToPrevious();
    }

    public boolean isPlaying() {
        return playbackManager.isPlaying();
    }

    public long getPosition() {
        return playbackManager.getPosition();
    }

    public long getDuration() {
        return playbackManager.getDuration();
    }

    public void seekTo(long position) {
        playbackManager.seekTo(position);
    }

    public int getAudioSessionId() {
        return 0;
    }

    // Queue

    @NonNull
    public List<Song> getQueue() {
        return playbackManager.getCurrentPlaylist();
    }

    public void setQueuePosition(int queuePosition) {
        playbackManager.setQueuePosition(queuePosition);
        playbackManager.play();
    }

    public int getQueuePosition() {
        return playbackManager.getQueuePosition();
    }

    public void addToQueue(@NonNull List<Song> songs) {
        playbackManager.addToQueue(songs);
    }

    public void playNext(@NonNull List<Song> songs) {
        playbackManager.playNext(songs);
    }

    public void moveQueueItem(int from, int to) {
        playbackManager.moveSong(from, to);
    }

    public void clearQueue() {
        playbackManager.clearQueue();
    }

    public void removeSong(int position) {
        playbackManager.removeSong(position);
    }

    public void removeSongs(@NonNull List<Song> songs) {
        playbackManager.removeSongs(songs);
    }

    @Nullable
    public Song getCurrentSong() {
        return playbackManager.getCurrentSong();
    }

    public void setShuffleMode(@QueueManager.ShuffleMode String shuffleMode) {
        playbackManager.setShuffleMode(shuffleMode);
    }

    @QueueManager.ShuffleMode
    public String getShuffleMode() {
        return playbackManager.getShuffleMode();
    }

    public void setRepeatMode(@QueueManager.RepeatMode String repeatMode) {
        playbackManager.setRepeatMode(repeatMode);
    }

    @QueueManager.RepeatMode
    public String getRepeatMode() {
        return playbackManager.getRepeatMode();
    }

    // Equalizer

    public void closeEqualizerSessions(boolean internal, int audioSessionId) {
        equalizer.closeEqualizerSessions(internal, audioSessionId);
    }

    public void openEqualizerSession(boolean internal, int audioSessionId) {
        equalizer.openEqualizerSession(internal, audioSessionId);
    }

    public void updateEqualizer() {
        equalizer.update();
    }

    // Other

    public void toggleFavorite() {

    }

    void saveState() {
        playbackManager.saveState();
    }

    // PlaybackServiceCallback

    private PlaybackManager.PlaybackServiceCallback playbackServiceCallback = new PlaybackManager.PlaybackServiceCallback() {
        @Override
        public void onPlaybackStart() {
            Timber.d("onPlaybackStart()");
            mediaSession.setActive(true);
            MusicEventRelay.getInstance().sendEvent(new PlayStateChangedEvent(true));
        }

        @Override
        public void onPlaybackStop() {
            Timber.d("onPlaybackStop()");
            mediaSession.setActive(false);
            stopForeground(true);
            MusicEventRelay.getInstance().sendEvent(new PlayStateChangedEvent(false));
        }

        @Override
        public void onPlaybackStateUpdated(PlaybackStateCompat playbackStateCompat) {
            Timber.d("onPlaybackStateUpdated(), state: %d", playbackStateCompat.getState());
            mediaSession.setPlaybackState(playbackStateCompat);
        }

        @Override
        public void showNotification() {
            musicNotificationManager.startNotification();
        }
    };


    // QueueManager MetaDataUpdateListener

    private QueueManager.MetadataUpdateListener metadataUpdateListener = new QueueManager.MetadataUpdateListener() {
        @Override
        public void onSongChanged(@Nullable Song previousSong, @Nullable Song currentSong) {
            MusicEventRelay.getInstance().sendEvent(new MetadataChangedEvent(currentSong));

            if (currentSong != null) {
                mediaSession.setMetadata(currentSong.getMetadata());
            }
        }

        @Override
        public void onQueuePositionChanged(int queuePosition) {
            MusicEventRelay.getInstance().sendEvent(new QueuePositionChangedEvent(queuePosition));
        }

        @Override
        public void onQueueChanged(@NonNull List<Song> queue) {
            MusicEventRelay.getInstance().sendEvent(new QueueChangedEvent(queue));
        }
    };

    // MediaSession

    @Nullable
    public MediaSessionCompat.Token getSessionToken() {
        return mediaSession.getSessionToken();
    }
}



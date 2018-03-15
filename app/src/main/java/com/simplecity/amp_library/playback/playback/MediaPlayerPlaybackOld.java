//package com.simplecity.amp_library.playback.playback;
//
//import android.annotation.SuppressLint;
//import android.content.Context;
//import android.media.AudioAttributes;
//import android.media.AudioManager;
//import android.media.MediaPlayer;
//import android.net.Uri;
//import android.os.PowerManager;
//import android.support.annotation.NonNull;
//import android.support.annotation.Nullable;
//import android.support.v4.media.session.PlaybackStateCompat;
//
//import com.simplecity.amp_library.model.Song;
//import com.simplecity.amp_library.utils.VersionUtils;
//
//import org.jetbrains.annotations.NotNull;
//
//import java.io.IOException;
//
//import io.reactivex.Completable;
//import timber.log.Timber;
//
//public class MediaPlayerPlaybackOld extends LocalPlayback {
//
//    private MediaPlayer mediaPlayer = new MediaPlayer();
//    private MediaPlayer nextMediaPlayer = new MediaPlayer();
//
//    private boolean isInitialized = false;
//
//    private boolean isSupposedToBePlaying = false;
//
//    private Song currentSong;
//
//    @Nullable
//    private Callback callback;
//
//    public MediaPlayerPlaybackOld(Context context) {
//        super(context);
//    }
//
//    @Override
//    public Completable load(@NonNull Song song) {
//        Timber.d("load() called.. ");
//
//        return Completable.create(emitter -> {
//
//            if (mediaPlayer != null && isInitialized) {
//                emitter.onComplete();
//
//                // Our media player exists, it's initialised, and the song hasn't changed. Nothing to do.
//                if (song.equals(currentSong)) {
//                    Timber.d("..song already loaded.");
//                    return;
//                } else {
//                    // The song has changed, but our MediaPlayer is non-null and initialised, meaning
//                    // our MediaPlayer has been set to the NextMediaPlayer, and now we need to set the NextMediaPlayer
//                    if (nextMediaPlayer == null) {
//                        loadNext();
//                        currentSong = song;
//                        return;
//                    }
//                }
//            }
//
//            isInitialized = false;
//
//            if (mediaPlayer == null) {
//                mediaPlayer = new MediaPlayer();
//            } else {
//                mediaPlayer.reset();
//            }
//
//            mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
//            mediaPlayer.setOnPreparedListener(mediaPlayer -> {
//                Timber.d("..media player prepared");
//                isInitialized = true;
//                emitter.onComplete();
//            });
//            mediaPlayer.setOnErrorListener((mediaPlayer, what, extra) -> {
//                @SuppressLint("DefaultLocale")
//                String errorMessage = String.format("Media player errored %d, %d", what, extra);
//                Timber.d(errorMessage);
//
//                if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
//                    mediaPlayer.release();
//                    isInitialized = false;
//                }
//
//                if (callback != null) {
//                    callback.onError(errorMessage);
//                }
//
//                return true;
//            });
//            mediaPlayer.setOnCompletionListener(mediaPlayer -> {
//                if (nextMediaPlayer != null) {
//                    this.mediaPlayer.release();
//                    this.mediaPlayer = nextMediaPlayer;
//                    nextMediaPlayer = null;
//                }
//                if (callback != null) {
//                    callback.onCompletion();
//                }
//            });
//
//            String path = song.path;
//            Timber.d("..initialising media player with: %s", path);
//            try {
//                if (path.startsWith("content://")) {
//                    Uri uri = Uri.parse(path);
//                    mediaPlayer.setDataSource(context, uri);
//                } else {
//                    mediaPlayer.setDataSource(path);
//                }
//                if (VersionUtils.hasOreo()) {
//                    mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
//                            .setUsage(AudioAttributes.USAGE_MEDIA)
//                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//                            .build());
//                } else {
//                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//                }
//                mediaPlayer.prepareAsync();
//                currentSong = song;
//            } catch (IOException e) {
//                Timber.e(e, "..failed to initialize media player: %s", path);
//                emitter.onError(e);
//            }
//        });
//    }
//
//    void loadNext() {
//
//    }
//
//    @Override
//    public void start() {
//        super.start();
//        Timber.d("start() called");
//        if (!mediaPlayer.isPlaying()) {
//            mediaPlayer.start();
//        }
//        isSupposedToBePlaying = true;
//        onPlayerStateChanged();
//    }
//
//    @Override
//    public void pause() {
//        super.pause();
//        Timber.d("pause() called");
//        if (mediaPlayer.isPlaying()) {
//            mediaPlayer.pause();
//        }
//        isSupposedToBePlaying = false;
//        onPlayerStateChanged();
//    }
//
//    @Override
//    public void stop() {
//        super.stop();
//        Timber.d("stop() called");
//        mediaPlayer.reset();
//        isInitialized = false;
//        isSupposedToBePlaying = false;
//        onPlayerStateChanged();
//    }
//
//    @Override
//    public boolean isPlaying() {
//        return /*playOnFocusGain ||*/ isSupposedToBePlaying;
//    }
//
//    @Override
//    public void setVolume(float volume) {
//        mediaPlayer.setVolume(volume, volume);
//    }
//
//    @Override
//    @PlaybackStateCompat.State
//    public int getState() {
//        if (mediaPlayer == null || !isInitialized) {
//            return PlaybackStateCompat.STATE_NONE;
//        }
//        if (isPlaying()) {
//            return PlaybackStateCompat.STATE_PLAYING;
//        } else {
//            return PlaybackStateCompat.STATE_PAUSED;
//        }
//    }
//
//    @Override
//    public void seekTo(long position) {
//        mediaPlayer.seekTo((int) position);
//    }
//
//    @Override
//    public long getPosition() {
//        if (isInitialized) {
//            return mediaPlayer.getCurrentPosition();
//        }
//        return 0;
//    }
//
//    @Override
//    public long getDuration() {
//        if (isInitialized) {
//            return mediaPlayer.getDuration();
//        }
//        return 0;
//    }
//
//    @Override
//    public void setCallback(@Nullable Callback callback) {
//        this.callback = callback;
//    }
//
//    private void onPlayerStateChanged() {
//        if (callback != null) {
//            callback.onPlaybackStateChanged(getState());
//        }
//    }
//
//    @NotNull
//    @Override
//    public PlaybackQueueManager getPlaybackQueueManager() {
//        return new NoOpPlaybackQueueManager();
//    }
//
//    @NotNull
//    @Override
//    public Completable prepare() {
//        return null;
//    }
//
//    private class NoOpPlaybackQueueManager extends PlaybackQueueManagerAdapter {
//
//    }
//}

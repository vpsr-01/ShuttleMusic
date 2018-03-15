package com.simplecity.amp_library.playback;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.settings.MusicSettings;
import com.simplecity.amp_library.rx.UnsafeConsumer;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MusicServiceConnectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class MusicUtils {

    private static final String TAG = "MusicUtils";

    @Nullable
    private static MusicService getService() {
        if (MusicServiceConnectionUtils.serviceBinder != null) {
            return MusicServiceConnectionUtils.serviceBinder.getService();
        }
        return null;
    }

    public static void playAll(List<Song> songs, @IntRange(from = 0) int position, boolean canClearShuffle, UnsafeConsumer<String> onEmpty) {

        if (canClearShuffle && !MusicSettings.getInstance().getRememberShuffle()) {
            setShuffleMode(QueueManager.ShuffleMode.OFF);
        }

        if (songs.size() == 0 || getService() == null) {
            onEmpty.accept(ShuttleApplication.getInstance().getResources().getString(R.string.empty_playlist));
            return;
        }

        getService().open(songs, position);
        getService().play();
    }

    public static void playFile(final Uri uri) {
        if (uri == null || getService() == null) {
            return;
        }

        // If this is a file:// URI, just use the path directly instead
        // of going through the open-from-filedescriptor codepath.
        String filename;
        final String scheme = uri.getScheme();
        if ("file".equals(scheme)) {
            filename = uri.getPath();
        } else {
            filename = uri.toString();
        }

        getService().stop();
        getService().openFile(filename);
        getService().play();
    }

    @SuppressLint("CheckResult")
    public static void playAll(Single<List<Song>> songsSingle, UnsafeConsumer<String> onEmpty) {
        songsSingle
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> playAll(songs, 0, true, onEmpty));
    }

    @SuppressLint("CheckResult")
    public static void shuffleAll(Single<List<Song>> songsSingle, UnsafeConsumer<String> onEmpty) {
        setShuffleMode(QueueManager.ShuffleMode.ON);
        songsSingle
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        songs -> {
                            if (!songs.isEmpty()) {
                                playAll(songs, new Random().nextInt(songs.size()), false, onEmpty);
                            }
                        },
                        e -> LogUtils.logException(TAG, "Shuffle all error", e));
    }

    public static boolean isPlaying() {
        return getService() != null && getService().isPlaying();
    }

    public static void togglePlayback() {
        if (getService() != null) {
            if (getService().isPlaying()) {
                getService().pause();
            } else {
                getService().play();
            }
        }
    }

    @QueueManager.ShuffleMode
    public static String getShuffleMode() {
        if (getService() != null) {
            return getService().getShuffleMode();
        }
        return QueueManager.ShuffleMode.OFF;
    }

    public static void setShuffleMode(@QueueManager.ShuffleMode String mode) {
        if (getService() != null) {
            getService().setShuffleMode(mode);
        }
    }

    @QueueManager.RepeatMode
    public static String getRepeatMode() {
        if (getService() != null) {
            return getService().getRepeatMode();
        }
        return QueueManager.RepeatMode.OFF;
    }

    public static void skip() {
        if (getService() != null) {
            getService().skip(true);
        }
    }

    /**
     * Changes to the previous track
     *
     * @param allowTrackRestart if true, the track will restart if the track position is > 2 seconds
     */
    public static void previous(boolean allowTrackRestart) {
        if (allowTrackRestart && getPosition() > 2000) {
            seekTo(0);
            if (getService() != null) {
                getService().play();
            }
        } else {
            if (getService() != null) {
                getService().prev();
            }
        }
    }

    public static int getAudioSessionId() {
        if (getService() != null) {
            return getService().getAudioSessionId();
        }
        return 0;
    }

    public static long getPosition() {
        if (getService() != null) {
            return getService().getPosition();
        }
        return 0;
    }

    public static long getDuration() {
        if (getService() != null) {
            return getService().getDuration();
        }
        return 0;
    }

    public static void seekTo(final long position) {
        if (getService() != null) {
            getService().seekTo(position);
        }
    }

    public static void toggleShuffle() {
        if (getService() != null) {
            @QueueManager.ShuffleMode String shuffleMode = getShuffleMode();
            switch (shuffleMode) {
                case QueueManager.ShuffleMode.OFF:
                    setShuffleMode(QueueManager.ShuffleMode.ON);
                    break;
                case QueueManager.ShuffleMode.ON:
                    setShuffleMode(QueueManager.ShuffleMode.OFF);
                    break;
            }
        }
    }

    public static void toggleRepeat() {
        if (getService() != null) {
            @QueueManager.RepeatMode String repeatMode = getRepeatMode();
            switch (repeatMode) {
                case QueueManager.RepeatMode.OFF:
                    getService().setRepeatMode(QueueManager.RepeatMode.ALL);
                    break;
                case QueueManager.RepeatMode.ALL:
                    getService().setRepeatMode(QueueManager.RepeatMode.ONE);
                    break;
                case QueueManager.RepeatMode.ONE:
                    getService().setRepeatMode(QueueManager.RepeatMode.OFF);
                    break;
            }
        }
    }

    public static void toggleFavorite() {
        if (getService() != null) {
            getService().toggleFavorite();
        }
    }

    // Queue

    public static List<Song> getQueue() {
        if (getService() != null) {
            return getService().getQueue();
        }
        return new ArrayList<>();
    }

    public static void setQueuePosition(final int position) {
        if (getService() != null) {
            getService().setQueuePosition(position);
        }
    }

    public static int getQueuePosition() {
        if (getService() != null) {
            return getService().getQueuePosition();
        }
        return 0;
    }

    public static void addToQueue(List<Song> songs, UnsafeConsumer<String> onAdded) {
        if (getService() != null) {
            getService().addToQueue(songs);
            onAdded.accept(ShuttleApplication.getInstance().getResources().getQuantityString(R.plurals.NNNtrackstoqueue, songs.size(), songs.size()));
        }
    }

    public static void playNext(Single<List<Song>> songsSingle, UnsafeConsumer<String> onAdded) {
        if (MusicServiceConnectionUtils.serviceBinder.getService() == null) {
            return;
        }
        songsSingle
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> playNext(songs, onAdded));
    }

    public static void playNext(List<Song> songs, UnsafeConsumer<String> onAdded) {
        if (getService() != null) {
            getService().playNext(songs);
            onAdded.accept(ShuttleApplication.getInstance().getResources().getQuantityString(R.plurals.NNNtrackstoqueue, songs.size(), songs.size()));
        }
    }

    public static void moveQueueItem(final int from, final int to) {
        if (getService() != null) {
            getService().moveQueueItem(from, to);
        }
    }

    public static void clearQueue() {
        if (getService() != null) {
            getService().clearQueue();
        }
    }

    public static void removeFromQueue(int position) {
        if (getService() != null) {
            getService().removeSong(position);
        }
    }

    public static void removeFromQueue(final List<Song> songs) {
        if (getService() != null) {
            getService().removeSongs(songs);
        }
    }

    @Nullable
    public static Song getCurrentSong() {
        if (getService() != null) {
            return getService().getCurrentSong();
        }
        return null;
    }

    // Equalizer

    public static void closeEqualizerSessions(boolean internal, int audioSessionId) {
        if (getService() != null) {
            getService().closeEqualizerSessions(internal, audioSessionId);
        }
    }

    public static void openEqualizerSession(boolean internal, int audioSessionId) {
        if (getService() != null) {
            getService().openEqualizerSession(internal, audioSessionId);
        }
    }

    public static void updateEqualizer() {
        if (getService() != null) {
            getService().updateEqualizer();
        }
    }
}

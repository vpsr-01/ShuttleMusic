package com.simplecity.amp_library.playback;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.settings.MusicSettings;
import com.simplecity.amp_library.rx.UnsafeAction;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.TimeLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.reactivex.Completable;
import timber.log.Timber;

public class QueueManager {

    private static final String TAG = "QueueManager";

    public interface MetadataUpdateListener {
        void onSongChanged(@Nullable Song previousSong, @Nullable Song newSong);

        void onQueuePositionChanged(int queuePosition);

        void onQueueChanged(@NonNull List<Song> queue);
    }

    public @interface ShuffleMode {
        String OFF = "OFF";
        String ON = "ON";
    }

    public @interface RepeatMode {
        String OFF = "OFF";
        String ONE = "ONE";
        String ALL = "ALL";
    }

    private Context appContext;

    @NonNull
    public
    List<Song> playlist = new ArrayList<>();

    @NonNull
    public
    List<Song> shuffleList = new ArrayList<>();

    private int queuePosition = -1;

    private boolean queueIsSaveable = true;

    @NonNull
    private MetadataUpdateListener metadataUpdateListener;

    @ShuffleMode
    private String shuffleMode;

    @RepeatMode
    private String repeatMode;

    public QueueManager(
            @NonNull Context context,
            @NonNull MetadataUpdateListener metadataUpdateListener) {
        this.appContext = context.getApplicationContext();
        this.metadataUpdateListener = metadataUpdateListener;
        shuffleMode = MusicSettings.getInstance().getShuffleMode();
        repeatMode = MusicSettings.getInstance().getRepeatMode();
    }

    public void open(@NonNull List<Song> songs, @IntRange(from = 0) int position) {
        Timber.d("open() called, size: %d, position: %d", songs.size(), position);

        clearQueue();
        addToQueue(songs);
        makeShuffleList();
        setQueuePosition(position);

        saveQueue();
    }

    public void setShuffleMode(@ShuffleMode String shuffleMode) {

        if (this.shuffleMode.equals(shuffleMode)) {
            return;
        }

        if (shuffleMode.equals(ShuffleMode.ON)) {
            makeShuffleList();
        } else if (shuffleMode.equals(ShuffleMode.OFF)) {
            if (getQueuePosition() >= 0 && getQueuePosition() < shuffleList.size()) {
                int queuePosition = playlist.indexOf(shuffleList.get(getQueuePosition()));
                if (queuePosition != -1) {
                    setQueuePosition(queuePosition);
                }
            }
        }

        this.shuffleMode = shuffleMode;
        MusicSettings.getInstance().setShuffleMode(shuffleMode);

        metadataUpdateListener.onQueueChanged(getCurrentPlaylist());
        metadataUpdateListener.onQueuePositionChanged(getQueuePosition());
    }

    @ShuffleMode
    public String getShuffleMode() {
        return shuffleMode;
    }

    public void setRepeatMode(@RepeatMode String repeatMode) {
        Timber.d("setRepeatMode() called. repeat mode: %s", repeatMode);
        this.repeatMode = repeatMode;
        MusicSettings.getInstance().setRepeatMode(repeatMode);
    }

    @RepeatMode
    public String getRepeatMode() {
        return repeatMode;
    }

    @NonNull
    public List<Song> getCurrentPlaylist() {
        if (shuffleMode.equals(ShuffleMode.OFF)) {
            return playlist;
        } else {
            return shuffleList;
        }
    }

    @Nullable
    public Song getCurrentSong() {
        return getSong(getQueuePosition());
    }

    public void addToQueue(@NonNull List<Song> songs) {
        Timber.d("addToQueue() called for %d songs", songs.size());
        playlist.addAll(songs);
        shuffleList.addAll(songs);

        saveQueue();
        metadataUpdateListener.onQueueChanged(getCurrentPlaylist());
    }

    public void playNext(@NonNull List<Song> songs) {
        Timber.d("playNext() called for %d songs", songs.size());
        List<Song> otherList = getCurrentPlaylist() == playlist ? shuffleList : playlist;
        getCurrentPlaylist().addAll(getQueuePosition() + 1, songs);
        otherList.addAll(songs);

        metadataUpdateListener.onQueueChanged(getCurrentPlaylist());
    }

    public boolean skip() {
        return skip(false);
    }

    /**
     * Skip to the next song, accounting for the current repeat mode.
     *
     * @param wrapAround true if the skip action should still be regarded as successful, even after we've reached
     *                   the end of the queue. i.e. if repeat mode is off, and we're on the last track when skip is
     *                   called, the position will be reset to 0, but we still return true to indicate success.
     * @return whether the skip action was successful, i.e. the queue position changed to a valid value
     */
    public boolean skip(boolean wrapAround) {
        Timber.d("Skip called. Current position: %d, repeatMode: %s..", getQueuePosition(), repeatMode);
        boolean success = true;

        switch (repeatMode) {
            case RepeatMode.ALL:
                int queuePosition = getQueuePosition() + 1;
                if (queuePosition > getCurrentPlaylist().size()) {
                    setQueuePosition(0);
                } else {
                    setQueuePosition(queuePosition);
                }
                break;
            case RepeatMode.ONE:
                //Nothing to do
                break;
            case RepeatMode.OFF:
                queuePosition = getQueuePosition() + 1;

                if (queuePosition == getCurrentPlaylist().size()) {
                    if (wrapAround) {
                        setQueuePosition(0);
                    } else {
                        setQueuePosition(Math.max(0, getCurrentPlaylist().size() - 1));
                        success = false;
                    }
                } else {
                    setQueuePosition(queuePosition);
                }
                break;
        }

        Timber.d("..new queuePosition: %d, success: %s", getQueuePosition(), success);

        return success;
    }

    public boolean prev() {
        Timber.d("Prev called. Current queuePosition: %d..", getQueuePosition());
        boolean success = true;
        if (getQueuePosition() > 0) {
            setQueuePosition(getQueuePosition() - 1);
        } else {
            setQueuePosition(getCurrentPlaylist().size() - 1);
            if (getQueuePosition() < 0) {
                success = false;
                setQueuePosition(0);
            }
        }

        Timber.d("..new queuePosition: %d, returning %s", getQueuePosition(), getQueuePosition() >= 0);
        return success;
    }

    public void setQueueIsSaveable(boolean queueIsSaveable) {
        this.queueIsSaveable = queueIsSaveable;
    }

    public Completable reloadQueue() {
        Timber.d("reloadQueue() called..");
        long time = System.currentTimeMillis();

        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return Completable.error(new SecurityException("READ_EXTERNAL_STORAGE permission not granted"));
        }

        return DataManager.getInstance().getSongsRelay()
                .first(Collections.emptyList())
                .flatMapCompletable(songs -> {
                    Timber.d("Beginning to reload queue..");
                    int queuePosition = MusicSettings.getInstance().getQueuePosition();
                    Timber.d("..queue position: %d", queuePosition);

                    if (restorePlaylist(songs, queuePosition)) {
                        restoreShuffleList(songs, queuePosition);

                        if (queuePosition >= 0 && queuePosition < getCurrentPlaylist().size()) {
                            setQueuePosition(queuePosition);
                        } else {
                            setQueuePosition(0);
                        }
                    }

                    metadataUpdateListener.onQueuePositionChanged(queuePosition);
                    metadataUpdateListener.onSongChanged(null, getCurrentSong());
                    metadataUpdateListener.onQueueChanged(getCurrentPlaylist());

                    Timber.d("..reloadQueue() completed in %dms. Queue size: %d", System.currentTimeMillis() - time, playlist.size());
                    return Completable.complete();
                });
    }

    private boolean restorePlaylist(List<Song> songs, int pos) {
        String q = MusicSettings.getInstance().getPlaylist();
        if (!TextUtils.isEmpty(q)) {

            playlist = deserializePlaylist(q, songs);

            if (pos >= 0 && pos < playlist.size()) {
                return true;
            }

            // The saved playlist is bogus, discard it
            playlist.clear();
        }
        return false;
    }

    private void restoreShuffleList(List<Song> songs, int pos) {
        String q = MusicSettings.getInstance().getShuffleList();
        if (!TextUtils.isEmpty(q)) {
            shuffleList = deserializePlaylist(q, songs);

            if (pos >= 0 && pos < shuffleList.size()) {
                return;
            }

            // The saved playlist is bogus, discard it
            shuffleList.clear();
        }
    }

    /**
     * Removes the first instance of the Song the playlist & shuffleList.
     */
    public void removeSong(int position, UnsafeAction onPlaylistEmpty, UnsafeAction onCurrentSongRemoved) {
        Song currentSong = getCurrentSong();

        List<Song> otherPlaylist = getCurrentPlaylist().equals(playlist) ? shuffleList : playlist;
        Song song = getCurrentPlaylist().remove(position);
        otherPlaylist.remove(song);

        if (getQueuePosition() == position) {
            handleCurrentSongRemoval(currentSong, onPlaylistEmpty, onCurrentSongRemoved);
        } else {
            setQueuePosition(getCurrentPlaylist().indexOf(currentSong));
        }

        metadataUpdateListener.onQueueChanged(getCurrentPlaylist());
    }

    public void moveSong(int from, int to) {
        if (from >= getCurrentPlaylist().size()) {
            from = getCurrentPlaylist().size() - 1;
        }
        if (to >= getCurrentPlaylist().size()) {
            to = getCurrentPlaylist().size() - 1;
        }

        getCurrentPlaylist().add(to, getCurrentPlaylist().remove(from));

        if (from < to) {
            if (getQueuePosition() == from) {
                setQueuePosition(to);
            } else if (getQueuePosition() >= from && getQueuePosition() <= to) {
                setQueuePosition(getQueuePosition() - 1);
            }
        } else if (to < from) {
            if (getQueuePosition() == from) {
                setQueuePosition(to);
            } else if (getQueuePosition() >= to && getQueuePosition() <= from) {
                setQueuePosition(getQueuePosition() + 1);
            }
        }
        metadataUpdateListener.onQueueChanged(getCurrentPlaylist());
    }

    public void setQueuePosition(int position) {
        if (position >= getCurrentPlaylist().size() || position < 0) {
            throw new IllegalStateException(String.format("Invalid position: %d, playlist size: %d", position, getCurrentPlaylist().size()));
        }

        if (position != getQueuePosition()) {
            Song previousSong = getCurrentSong();
            MusicSettings.getInstance().setQueuePosition(position);
            this.queuePosition = position;

            metadataUpdateListener.onQueuePositionChanged(position);
            if (getCurrentSong() != null && !getCurrentSong().equals(previousSong)) {
                metadataUpdateListener.onSongChanged(previousSong, getCurrentSong());
            }
        }
    }

    public int getQueuePosition() {
        return queuePosition;
    }

    @Nullable
    public Song getSong(int index) {
        if (index < 0 || index >= getCurrentPlaylist().size()) {
            return null;
        } else {
            return getCurrentPlaylist().get(index);
        }
    }

    /**
     * Removes the range of Songs specified from the playlist & shuffleList. If a Song
     * within the range is the file currently being played, playback will move
     * to the next Song after the range.
     *
     * @param songsToRemove the Songs to remove
     */
    public void removeSongs(@NonNull List<Song> songsToRemove, UnsafeAction onPlaylistEmpty, UnsafeAction onCurrentSongRemoved) {
        Song currentSong = getCurrentSong();

        playlist.removeAll(songsToRemove);
        shuffleList.removeAll(songsToRemove);

        if (currentSong != null) {
            if (songsToRemove.contains(currentSong)) {
                /*
                 * If we remove a list of songs from the current queue, and that list contains our currently
                 * playing song, we need to figure out which song should play next. We'll play the first song
                 * that comes after the list of songs to be removed.
                 *
                 * In this example, let's say Song 7 is currently playing
                 *
                 * Playlist:                    [Song 3,    Song 4,     Song 5,     Song 6,     Song 7,     Song 8]
                 * Indices:                     [0,         1,          2,          3,          4,          5]
                 *
                 * Remove;                                              [Song 5,     Song 6,     Song 7]
                 *
                 * First removed song:                                  Song 5
                 * Index of first removed song:                         2
                 *
                 * Playlist after removal:      [Song 3,    Song 4,     Song 8]
                 * Indices:                     [0,         1,          2]
                 *
                 *
                 * So after the removal, we'll play index 2, which is Song 8.
                 */
                setQueuePosition(Collections.indexOfSubList(getCurrentPlaylist(), songsToRemove));
                handleCurrentSongRemoval(currentSong, onPlaylistEmpty, onCurrentSongRemoved);
            } else {
                setQueuePosition(getCurrentPlaylist().indexOf(currentSong));
            }
        }

        metadataUpdateListener.onQueueChanged(getCurrentPlaylist());
    }

    private void handleCurrentSongRemoval(Song removedSong, UnsafeAction onPlaylistEmpty, UnsafeAction onCurrentSongRemoved) {
        if (getCurrentPlaylist().isEmpty()) {
            setQueuePosition(-1);
            onPlaylistEmpty.run();
        } else {
            if (getQueuePosition() >= getCurrentPlaylist().size()) {
                setQueuePosition(0);
            }
            onCurrentSongRemoved.run();
        }
        metadataUpdateListener.onSongChanged(removedSong, getCurrentSong());
    }

    public void clearQueue() {
        playlist.clear();
        shuffleList.clear();

//        setQueuePosition(0);

        if (!MusicSettings.getInstance().getRememberShuffle()) {
            setShuffleMode(ShuffleMode.OFF);
        }
        metadataUpdateListener.onQueueChanged(getCurrentPlaylist());
    }

    public void saveQueue() {
        TimeLogger timeLogger = new TimeLogger();
        Timber.d("Saving queue..");

        if (!queueIsSaveable) {
            Timber.d(".. queue not saveable, returning.");
            return;
        }

        MusicSettings.getInstance().setPlaylist(serializePlaylist(playlist));
        MusicSettings.getInstance().setShuffleList(serializePlaylist(shuffleList));

        Timber.d("Queue save complete in %dms", timeLogger.getTotalTime());
    }

    public void makeShuffleList() {
        if (playlist.isEmpty()) {
            return;
        }

        shuffleList = new ArrayList<>(playlist);
        Song currentSong = null;
        if (getQueuePosition() >= 0 && getQueuePosition() < shuffleList.size()) {
            currentSong = shuffleList.remove(queuePosition);
        }

        Collections.shuffle(shuffleList);

        if (currentSong != null) {
            shuffleList.add(0, currentSong);
        }

        setQueuePosition(0);
    }

    private final char hexDigits[] = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * Converts a playlist to a String which can be saved to SharedPrefs
     */
    private String serializePlaylist(List<Song> list) {

        // The current playlist is saved as a list of "reverse hexadecimal"
        // numbers, which we can generate faster than normal decimal or
        // hexadecimal numbers, which in turn allows us to save the playlist
        // more often without worrying too much about performance.

        StringBuilder q = new StringBuilder();

        int len = list.size();
        for (int i = 0; i < len; i++) {
            long n = list.get(i).id;
            if (n >= 0) {
                if (n == 0) {
                    q.append("0;");
                } else {
                    while (n != 0) {
                        final int digit = (int) (n & 0xf);
                        n >>>= 4;
                        q.append(hexDigits[digit]);
                    }
                    q.append(";");
                }
            }
        }

        return q.toString();
    }

    /**
     * Converts a string representation of a playlist from SharedPrefs into a list of songs.
     */
    private List<Song> deserializePlaylist(String listString, List<Song> allSongs) {
        List<Long> ids = new ArrayList<>();
        int n = 0;
        int shift = 0;
        for (int i = 0; i < listString.length(); i++) {
            char c = listString.charAt(i);
            if (c == ';') {
                ids.add((long) n);
                n = 0;
                shift = 0;
            } else {
                if (c >= '0' && c <= '9') {
                    n += ((c - '0') << shift);
                } else if (c >= 'a' && c <= 'f') {
                    n += ((10 + c - 'a') << shift);
                } else {
                    // bogus playlist data
                    playlist.clear();
                    break;
                }
                shift += 4;
            }
        }

        Map<Integer, Song> map = new TreeMap<>();

        for (Song song : allSongs) {
            int index = ids.indexOf(song.id);
            if (index != -1) {
                map.put(index, song);
            }
        }
        return new ArrayList<>(map.values());
    }
}
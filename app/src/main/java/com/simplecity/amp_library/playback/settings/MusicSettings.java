package com.simplecity.amp_library.playback.settings;

import android.support.annotation.Nullable;

import com.simplecity.amp_library.playback.QueueManager;
import com.simplecity.amp_library.utils.SettingsManager;

import timber.log.Timber;

public class MusicSettings extends SettingsManager {

    private static MusicSettings instance;

    public static MusicSettings getInstance() {
        if (instance == null) {
            instance = new MusicSettings();
        }
        return instance;
    }


    private static String KEY_PREF_REMEMBER_SHUFFLE = "pref_remember_shuffle";

    public boolean getRememberShuffle() {
        return getBool(KEY_PREF_REMEMBER_SHUFFLE, false);
    }

    public void setRememberShuffle(boolean rememberShuffle) {
        setBool(KEY_PREF_REMEMBER_SHUFFLE, rememberShuffle);
    }


    private static final String KEY_SHUFFLE_MODE = "shuffle_mode";

    @QueueManager.ShuffleMode
    public String getShuffleMode() {
        return getString(KEY_SHUFFLE_MODE, QueueManager.ShuffleMode.OFF);
    }

    public void setShuffleMode(@QueueManager.ShuffleMode String shuffleMode) {
        setString(KEY_SHUFFLE_MODE, shuffleMode);
    }


    private static final String KEY_Repeat_Mode = "repeat_mode";

    @QueueManager.RepeatMode
    public String getRepeatMode() {
        return getString(KEY_Repeat_Mode, QueueManager.RepeatMode.OFF);
    }

    public void setRepeatMode(@QueueManager.RepeatMode String repeatMode) {
        setString(KEY_Repeat_Mode, repeatMode);
    }


    private static final String KEY_PLAYLIST = "playlist";

    public void setPlaylist(String playlist) {
        setString(KEY_PLAYLIST, playlist);
    }

    @Nullable
    public String getPlaylist() {
        return getString(KEY_PLAYLIST);
    }


    private static final String KEY_SHUFFLE_LIST = "shuffle_list";

    public void setShuffleList(String shuffleList) {
        setString(KEY_SHUFFLE_LIST, shuffleList);
    }

    @Nullable
    public String getShuffleList() {
        return getString(KEY_SHUFFLE_LIST);
    }


    private static final String KEY_QUEUE_POSITION = "queue_position";

    public void setQueuePosition(int queuePosition) {
        Timber.d("Saving queue position: %d", queuePosition);
        setInt(KEY_QUEUE_POSITION, queuePosition);
    }

    public int getQueuePosition() {
        return getInt(KEY_QUEUE_POSITION, 0);
    }


    private static final String KEY_PLAYBACK_POSITION = "playback_position";

    public long getPlaybackPosition() {
        return getLong(KEY_PLAYBACK_POSITION, 0L);
    }

    public void setPlaybackPosition(long playbackPosition) {
        setLong(KEY_PLAYBACK_POSITION, playbackPosition);
    }


}
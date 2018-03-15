package com.simplecity.amp_library.playback.playback

import com.simplecity.amp_library.model.Song

abstract class PlaybackQueueManagerAdapter : PlaybackQueueManager {

    override fun skipToNext() {

    }

    override fun skipToPrev() {

    }

    override fun setQueuePosition(position: Int) {

    }

    override fun clearQueue() {

    }

    override fun removeSong(position: Int) {

    }

    override fun removeSongs(songs: List<Song>) {

    }

    override fun moveSong(from: Int, to: Int) {

    }

    override fun setShuffleMode(shuffleMode: String) {

    }
}
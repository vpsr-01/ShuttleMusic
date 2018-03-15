package com.simplecity.amp_library.playback.playback

import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.QueueManager

interface PlaybackQueueManager {

    fun skipToNext()

    fun skipToPrev()

    fun setQueuePosition(position: Int)

    fun clearQueue()

    fun removeSong(position: Int)

    fun removeSongs(songs: List<Song>)

    fun moveSong(from: Int, to: Int)

    fun setShuffleMode(@QueueManager.ShuffleMode shuffleMode: String)

//    fun setCurrentAndNext(queuePosition: Int)
}
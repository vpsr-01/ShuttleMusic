package com.simplecity.amp_library.playback.playback

import android.support.v4.media.session.PlaybackStateCompat

import io.reactivex.Completable

interface Playback {

    val isPlaying: Boolean

    @PlaybackStateCompat.State
    val state: Int

    val position: Long

    val duration: Long

    val playbackQueueManager: PlaybackQueueManager

    fun prepare(): Completable

    fun start()

    fun pause()

    fun stop()

    fun seekTo(position: Long)

    fun setVolume(volume: Float)

    interface Callback {
        fun onPlaybackStateChanged(@PlaybackStateCompat.State state: Int)

        fun onError(errorMessage: String)
    }

    fun setCallback(callback: Callback?)

}
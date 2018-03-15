package com.simplecity.amp_library.playback.playback

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.QueueManager
import io.reactivex.Completable
import timber.log.Timber

//Todo: Make this work
class MediaPlayerPlayback(context: Context, private val queueManager: QueueManager) : LocalPlayback(context) {

    private var mediaPlayer: MediaPlayer = MediaPlayer()
    private var nextMediaPlayer: MediaPlayer = MediaPlayer()

    private val mediaSourceManager = MediaSourceManager()

    private var isInitialized = false

    private var isSupposedToBePlaying = false

    private var callback: Playback.Callback? = null

    override val isPlaying: Boolean
        get() = isSupposedToBePlaying

    override val state: Int
        get() = when {
            !isInitialized -> PlaybackStateCompat.STATE_NONE
            isPlaying -> PlaybackStateCompat.STATE_PLAYING
            else -> PlaybackStateCompat.STATE_PAUSED
        }

    override val position: Long
        get() = if (isInitialized) mediaPlayer.currentPosition.toLong() else 0

    override val duration: Long
        get() = if (isInitialized) mediaPlayer.duration.toLong() else 0

    override val playbackQueueManager: PlaybackQueueManager
        get() = mediaSourceManager

    override fun prepare(): Completable {
        return Completable.create { emitter ->

            mediaPlayer.setOnCompletionListener {
                nextMediaPlayer = mediaPlayer
            }

            isInitialized = false

            mediaSourceManager.prepare({
                emitter.onComplete()
            })
        }
    }

    override fun seekTo(position: Long) {
        mediaPlayer.seekTo(position.toInt())
    }

    override fun setVolume(volume: Float) {
        mediaPlayer.setVolume(volume, volume)
    }

    override fun setCallback(callback: Playback.Callback?) {
        this.callback = callback
    }

    override fun start() {
        super.start()
        Timber.d("start() called")
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
        isSupposedToBePlaying = true
        onPlayerStateChanged()
    }

    override fun pause() {
        super.pause()
        Timber.d("pause() called")
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
        isSupposedToBePlaying = false
        onPlayerStateChanged()
    }

    override fun stop() {
        super.stop()
        Timber.d("stop() called")
        mediaPlayer.reset()
        isInitialized = false
        isSupposedToBePlaying = false
        onPlayerStateChanged()
    }

    private fun onPlayerStateChanged() {
        callback?.onPlaybackStateChanged(state)
    }

    inner class MediaSourceManager : PlaybackQueueManager {

        private val queueChangeClassifier: QueueChangeClassifier = QueueChangeClassifier(queueManager)

        fun prepare(onComplete: () -> Unit) {
            setCurrentAndNext(queueManager.queuePosition, onComplete)
        }

        fun setCurrentAndNext(queuePosition: Int, onComplete: (() -> Unit)? = null) {
            val changeType = queueChangeClassifier.getChangeType(queuePosition)
            when (changeType) {
                is QueueChangeClassifier.ChangeType.Invalid,
                is QueueChangeClassifier.ChangeType.NoChange -> {
                    // Nothing to do
                }
                is QueueChangeClassifier.ChangeType.MovedToNext -> {
                    setDataSource(mediaPlayer, changeType.firstSong.path, MediaPlayer.OnPreparedListener {
                        isInitialized = true

                        changeType.nextSong?.let { nextSong ->
                            setNextdataSource(nextSong.path)
                        }
                    })
                }
                is QueueChangeClassifier.ChangeType.SecondSongChanged -> {
                    changeType.nextSong?.let { nextSong ->
                        setNextdataSource(nextSong.path)
                    }
                }
                is QueueChangeClassifier.ChangeType.BothSongsChanged -> {
                    setDataSource(mediaPlayer, changeType.firstSong.path, MediaPlayer.OnPreparedListener {
                        isInitialized = true

                        changeType.nextSong?.let { nextSong ->
                            setNextdataSource(nextSong.path)
                            onComplete?.invoke()
                        }
                    })
                }
            }
        }

        fun setDataSource(mediaPlayer: MediaPlayer, path: String, onPreparedListener: MediaPlayer.OnPreparedListener) {

            if (mediaPlayer == this@MediaPlayerPlayback.mediaPlayer) {
                isInitialized = false
            }

            mediaPlayer.reset()

            mediaPlayer.setOnPreparedListener(onPreparedListener)

            if (path.startsWith("content://")) {
                val uri = Uri.parse(path)
                mediaPlayer.setDataSource(context, uri)
            } else {
                mediaPlayer.setDataSource(path)
            }

            mediaPlayer.setOnCompletionListener { mp ->
                if (mp == mediaPlayer) {
                    mediaPlayer.release()
                    this@MediaPlayerPlayback.mediaPlayer = nextMediaPlayer
                } else {
                    callback?.onPlaybackStateChanged(PlaybackStateCompat.STATE_STOPPED)
                }
            }

            mediaPlayer.prepareAsync()
        }

        fun setNextdataSource(path: String) {
            mediaPlayer.setNextMediaPlayer(null)

            nextMediaPlayer.release()

            nextMediaPlayer = MediaPlayer()

            setDataSource(nextMediaPlayer, path, MediaPlayer.OnPreparedListener {
                mediaPlayer.setNextMediaPlayer(nextMediaPlayer)
            })
        }

        override fun skipToNext() {
            setCurrentAndNext(queueManager.queuePosition)
        }

        override fun skipToPrev() {
            setCurrentAndNext(queueManager.queuePosition)
        }

        override fun setQueuePosition(position: Int) {
            setCurrentAndNext(position)
        }

        override fun clearQueue() {

        }

        override fun removeSong(position: Int) {
            setCurrentAndNext(queueManager.queuePosition)
        }

        override fun removeSongs(songs: List<Song>) {
            setCurrentAndNext(queueManager.queuePosition)
        }

        override fun moveSong(from: Int, to: Int) {
            setCurrentAndNext(queueManager.queuePosition)
        }

        override fun setShuffleMode(shuffleMode: String) {
            setCurrentAndNext(queueManager.queuePosition)
        }
    }
}
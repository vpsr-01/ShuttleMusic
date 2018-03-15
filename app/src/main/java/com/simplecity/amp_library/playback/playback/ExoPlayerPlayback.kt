package com.simplecity.amp_library.playback.playback

import android.content.Context
import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.C.CONTENT_TYPE_MUSIC
import com.google.android.exoplayer2.C.USAGE_MEDIA
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.DynamicConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.QueueManager
import com.simplecity.amp_library.playback.playback.QueueChangeClassifier.ChangeType.*
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import timber.log.Timber

class ExoPlayerPlayback(context: Context, private val queueManager: QueueManager) : LocalPlayback(context) {

    private var exoPlayer: SimpleExoPlayer? = null

    private var callback: Playback.Callback? = null

    private var currentWindowIndex: Int = 0

    private val mediaSourceManager: MediaSourceManager = MediaSourceManager()

    private val eventListener = object : EventListener() {

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
            currentWindowIndex = exoPlayer?.currentWindowIndex ?: 0
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Timber.d("onPlayerStateChanged(), playWhenReady: %s, state: %s", playWhenReady, getPlaybackStateString(playbackState))

            when (playbackState) {
                Player.STATE_READY -> {
                    prepareEmitter?.onComplete()
                    callback?.onPlaybackStateChanged(state)
                }
                Player.STATE_IDLE, Player.STATE_BUFFERING -> {
                    callback?.onPlaybackStateChanged(state)
                }
                Player.STATE_ENDED -> {
                    pause()
                    callback?.onPlaybackStateChanged(state)
                }
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            val what: String = when (error.type) {
                ExoPlaybackException.TYPE_SOURCE -> error.sourceException.message!!
                ExoPlaybackException.TYPE_RENDERER -> error.rendererException.message!!
                ExoPlaybackException.TYPE_UNEXPECTED -> error.unexpectedException.message!!
                else -> "Unknown: " + error
            }
            Timber.d(error, "onPlayerError() %s", what)
            prepareEmitter?.onError(error)
            callback?.onError(String.format("onPlayerError: %s", what))
        }


        override fun onPositionDiscontinuity(reason: Int) {
            if (exoPlayer == null) return

            if (currentWindowIndex != exoPlayer!!.currentWindowIndex) {

                Timber.i("onPositionDiscontinuity: Window index changed from %d to %d", currentWindowIndex, exoPlayer!!.currentWindowIndex)

                queueManager.setQueuePosition(queueManager.getQueuePosition() + exoPlayer!!.currentWindowIndex)

                mediaSourceManager.setCurrentAndNext(queueManager.getQueuePosition())

                currentWindowIndex = exoPlayer!!.currentWindowIndex
            }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {

        }

        override fun onSeekProcessed() {

        }

        override fun onRepeatModeChanged(repeatMode: Int) {

        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {

        }

        override fun onLoadingChanged(isLoading: Boolean) {

        }

        override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {

        }
    }

    override fun prepare(): Completable {
        Timber.d("prepare() called")

        return Completable.create { emitter ->
            eventListener.prepareEmitter = emitter

            if (exoPlayer == null) {
                exoPlayer = ExoPlayerFactory.newSimpleInstance(DefaultRenderersFactory(context), DefaultTrackSelector(), DefaultLoadControl())
                exoPlayer!!.addListener(eventListener)
                exoPlayer!!.audioAttributes = AudioAttributes.Builder()
                        .setUsage(USAGE_MEDIA)
                        .setContentType(CONTENT_TYPE_MUSIC)
                        .build()

                mediaSourceManager.setExoPlayer(exoPlayer!!)
            }

            mediaSourceManager.prepare()
            exoPlayer!!.prepare(mediaSourceManager.mediaSource)
        }.doOnTerminate { eventListener.prepareEmitter = null }
    }

    override fun start() {
        super.start()
        Timber.d("start() called")

        exoPlayer?.playWhenReady = true
    }

    override fun pause() {
        super.pause()
        Timber.d("pause() called")

        exoPlayer?.playWhenReady = false
    }

    override fun stop() {
        super.stop()
        Timber.d("stop() called")

        exoPlayer?.playWhenReady = false
        exoPlayer?.release()
        exoPlayer?.removeListener(eventListener)
        exoPlayer = null
    }

    override val isPlaying: Boolean
        get() = exoPlayer?.playWhenReady ?: false


    override fun setVolume(volume: Float) {
        exoPlayer?.volume = volume
    }

    override val playbackQueueManager: PlaybackQueueManager
        get() = mediaSourceManager

    override val state: Int
        get() = when (exoPlayer?.playbackState) {
            Player.STATE_IDLE -> PlaybackStateCompat.STATE_PAUSED
            Player.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
            Player.STATE_READY -> if (exoPlayer?.playWhenReady == true) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            Player.STATE_ENDED -> PlaybackStateCompat.STATE_PAUSED
            else -> PlaybackStateCompat.STATE_NONE
        }

    override fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
    }

    override val position: Long
        get() = exoPlayer?.contentPosition ?: 0

    override val duration: Long
        get() = exoPlayer?.duration ?: 0

    override fun setCallback(callback: Playback.Callback?) {
        this.callback = callback
    }

    private abstract class EventListener : Player.EventListener {
        internal var prepareEmitter: CompletableEmitter? = null
    }

    private fun getPlaybackStateString(playbackState: Int): String {
        when (playbackState) {
            Player.STATE_IDLE -> return "STATE_IDLE"
            Player.STATE_BUFFERING -> return "STATE_BUFFERING"
            Player.STATE_READY -> return "STATE_READY"
            Player.STATE_ENDED -> return "STATE_ENDED"
        }
        return "STATE_UNKNOWN"
    }


    inner class MediaSourceManager : PlaybackQueueManager {

        private val applicationContext: Context = context.applicationContext

        private var exoPlayer: ExoPlayer? = null

        var mediaSource = DynamicConcatenatingMediaSource()

        private val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
                applicationContext,
                Util.getUserAgent(applicationContext, "shuttle-music-player"),
                null
        )

        private val queueChangeClassifier: QueueChangeClassifier = QueueChangeClassifier(queueManager)

        private fun getExtractorMediaSource(song: Song): ExtractorMediaSource {
            return ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(song.path))
        }

        fun setExoPlayer(exoPlayer: ExoPlayer) {
            this.exoPlayer = exoPlayer
        }

        fun prepare() {
            mediaSource = DynamicConcatenatingMediaSource()

            setCurrentAndNext(queueManager.queuePosition)
        }

        fun setCurrentAndNext(queuePosition: Int) {

            if (exoPlayer == null) return

            val changeType = queueChangeClassifier.getChangeType(queuePosition)
            when (changeType) {
                is Invalid,
                is NoChange -> {
                    // Nothing to do
                }
                is MovedToNext -> {
                    // Remove the current song
                    if (mediaSource.size != 0) {
                        mediaSource.removeMediaSource(0)
                    }

                    // Add in our new second song
                    changeType.nextSong?.let { nextSong ->
                        mediaSource.addMediaSource(getExtractorMediaSource(nextSong))
                    }
                }
                is SecondSongChanged -> {
                    if (mediaSource.size > 1) {

                        // Remove our old second song
                        mediaSource.removeMediaSource(1)

                        // Add in our new second song
                        changeType.nextSong?.let { nextSong ->
                            mediaSource.addMediaSource(getExtractorMediaSource(nextSong))
                        }
                    }
                }
                is BothSongsChanged -> {
                    mediaSource.releaseSource()

                    mediaSource = DynamicConcatenatingMediaSource()

                    // Insert first song
                    mediaSource.addMediaSource(getExtractorMediaSource(changeType.firstSong))

                    // Insert second song
                    changeType.nextSong?.let { nextSong ->
                        mediaSource.addMediaSource(getExtractorMediaSource(nextSong))
                    }

                    exoPlayer!!.prepare(mediaSource)
                }
            }
        }

        override fun setQueuePosition(position: Int) {
            setCurrentAndNext(position)
        }

        override fun skipToNext() {
            Timber.d("skipToNext() called")
            setCurrentAndNext(queueManager.getQueuePosition())
        }

        override fun skipToPrev() {
            Timber.d("skipToPrev() called")
            setCurrentAndNext(queueManager.getQueuePosition())
        }

        override fun clearQueue() {
            mediaSource.releaseSource()
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
            Timber.d("setShuffleMode %s", shuffleMode)
            setCurrentAndNext(queueManager.getQueuePosition())
        }
    }
}
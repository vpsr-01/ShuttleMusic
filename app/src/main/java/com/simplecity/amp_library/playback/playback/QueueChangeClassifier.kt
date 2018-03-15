package com.simplecity.amp_library.playback.playback

import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.QueueManager
import com.simplecity.amp_library.playback.playback.QueueChangeClassifier.ChangeType
import timber.log.Timber

/**
 * A class for classifying changes to the queue, to help determine how an [Playback] implementation
 * should respond to the change.
 *
 * This class keeps track of the current and next song, and determines the [ChangeType] in response to
 * queue position changes.
 *
 * Particularly useful for gapless playback.
 */
internal class QueueChangeClassifier(private val queueManager: QueueManager) {

    private var currentSong: Song? = null

    private var nextSong: Song? = null

    open class ChangeType {
        class Invalid : ChangeType()
        class NoChange : ChangeType()
        class MovedToNext(val firstSong: Song, val nextSong: Song?) : ChangeType()
        class SecondSongChanged(val nextSong: Song?) : ChangeType()
        class BothSongsChanged(val firstSong: Song, val nextSong: Song?) : ChangeType()
    }

    /**
     * Determines the [ChangeType] associated with thr [QueueManager] changing position to [queuePosition]
     */
    fun getChangeType(queuePosition: Int): ChangeType {

        val newFirstSong = queueManager.getSong(queuePosition) ?: return ChangeType.Invalid()
        val newSecondSong = queueManager.getSong(queuePosition + 1)

        val changeType = when {
            currentSong === newFirstSong -> {
                when {
                    nextSong === newSecondSong -> ChangeType.NoChange()
                    else -> ChangeType.SecondSongChanged(newSecondSong)
                }
            }
            nextSong === newFirstSong -> ChangeType.MovedToNext(newFirstSong, newSecondSong)
            else -> ChangeType.BothSongsChanged(newFirstSong, newSecondSong)
        }

        currentSong = newFirstSong
        nextSong = newSecondSong

        Timber.i("ChangeType: ${changeType.javaClass.simpleName}")

        return changeType
    }
}

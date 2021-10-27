package com.dinaraparanid.prima.viewmodels.mvvm

import android.app.AlertDialog
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.View
import carbon.widget.Button
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.fragments.guess_the_melody.GtmGameFragment
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.extensions.getGTMTracks
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * MVVM [ViewModel] for
 * [com.dinaraparanid.prima.fragments.guess_the_melody.GTMPlaylistSelectFragment]
 */

class GtmGameViewModel(
    private val fragment: WeakReference<GtmGameFragment>,
    private var _trackNumber: Int,
    /** Tracks on buttons */
    @JvmField
    internal val tracks: AbstractPlaylist,
    /** All tracks */ private val _tracks: AbstractPlaylist,
    private val playbackStart: Int,
    private val playbackLength: Byte,
    private var _score: Int = 0,
    private val unsolvedTracks: AbstractPlaylist = DefaultPlaylist()
): ViewModel() {
    private lateinit var buttonWithCorrectTrack: Button

    internal fun setButtonWithCorrectTrack(button: Button) {
        buttonWithCorrectTrack = button
    }

    private val correctTrack = _tracks[_trackNumber - 1]

    internal inline val score
        @JvmName("getScore")
        get() = "${fragment.unchecked.resources.getString(R.string.score)} $_score"

    internal inline val trackNumber
        @JvmName("getTrackNumber")
        get() = "${fragment.unchecked.resources.getString(R.string.gtm_track_number)} $_trackNumber"

    internal inline val playButtonImage
        @JvmName("getPlayButtonImage")
        get() = ViewSetter.getPlayButtonImage(isPlaying)

    internal inline val nextOrFinishButtonTitle
        @JvmName("getNextOrFinishButtonTitle")
        get() = fragment.unchecked.resources.getString(
            if (_trackNumber == _tracks.size) R.string.finish else R.string.gtm_next_track
        )

    private var isNextButtonClickable = false
    private var isTracksButtonsClickable = true
    private var isPlaying = false
    private val playbackLock = ReentrantLock()
    private val playbackCondition = playbackLock.newCondition()
    private val playbackExecutor = Executors.newSingleThreadExecutor()
    private val uiHandler = Handler(Looper.getMainLooper())

    private inline var musicPlayer
        get() = fragment.unchecked.musicPlayer
        set(value) { fragment.unchecked.musicPlayer = value }

    @JvmName("onPlayButtonClicked")
    internal fun onPlayButtonClicked() = when {
        isPlaying -> {
            isPlaying = false
            fragment.unchecked.run {
                releaseMusicPlayer()
                setPlayButtonImage(isPlaying)
            }
        }

        else -> {
            isPlaying = true
            fragment.unchecked.setPlayButtonImage(isPlaying)

            musicPlayer = MediaPlayer().apply {
                setDataSource(correctTrack.path)

                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                setOnCompletionListener {
                    this@GtmGameViewModel.isPlaying = false
                    fragment.unchecked.run {
                        setPlayButtonImage(isPlaying)
                        releaseMusicPlayer()
                    }
                }

                prepare()
                isLooping = false
                seekTo(playbackStart)
                start()
            }

            playbackExecutor.execute {
                playbackLock.withLock {
                    playbackCondition.await(playbackLength.toLong(), TimeUnit.SECONDS)
                    isPlaying = false

                    fragment.unchecked.run {
                        uiHandler.post { setPlayButtonImage(isPlaying) }
                        releaseMusicPlayer()
                    }
                }
            }
        }
    }

    @JvmName("onTrackButtonClicked")
    internal fun onTrackButtonClicked(v: View) {
        if (isTracksButtonsClickable) {
            _trackNumber++
            isNextButtonClickable = true
            isTracksButtonsClickable = false
            buttonWithCorrectTrack.setBackgroundColor(Color.GREEN)

            fragment.unchecked.run {
                setNextButtonClickable(isNextButtonClickable)
                setTracksButtonsClickable(isTracksButtonsClickable)
            }

            when {
                v === buttonWithCorrectTrack -> {
                    _score++
                    fragment.unchecked.scoreButtonText = score
                }

                else -> {
                    v.setBackgroundColor(Color.RED)
                    unsolvedTracks.add(correctTrack)
                }
            }
        }
    }

    @JvmName("onNextOrFinishButtonClicked")
    internal fun onNextOrFinishButtonClicked() {
        when (_trackNumber) {
            _tracks.size + 1 -> {
                // TODO: finish dialog
                fragment.unchecked.requireActivity().finish()
            }

            else -> fragment.unchecked
                .requireActivity()
                .supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in,
                    R.anim.slide_out,
                    R.anim.slide_in,
                    R.anim.slide_out
                )
                .replace(
                    R.id.gtm_fragment_container,
                    GtmGameFragment.newInstance(
                        _tracks,
                        _tracks.getGTMTracks(_trackNumber - 1),
                        _tracks[_trackNumber - 1]
                            .getGTMRandomPlaybackStartPosition(playbackLength),
                        playbackLength,
                        _trackNumber,
                        _score,
                        unsolvedTracks
                    )
                )
                .commit()
        }
    }

    @JvmName("onExitButtonClicked")
    internal fun onExitButtonClicked() = AlertDialog
        .Builder(fragment.unchecked.requireContext())
        .setCancelable(true)
        .setMessage(R.string.exit_request)
        .setPositiveButton(R.string.ok) { dialog, _ ->
            dialog.dismiss()
            fragment.unchecked.requireActivity().finish()
        }
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()
}
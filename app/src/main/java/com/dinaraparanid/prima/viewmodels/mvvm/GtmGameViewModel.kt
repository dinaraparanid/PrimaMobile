package com.dinaraparanid.prima.viewmodels.mvvm

import android.app.AlertDialog
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.ConditionVariable
import android.view.View
import carbon.widget.Button
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.fragments.guess_the_melody.GTMGameFragment
import com.dinaraparanid.prima.utils.Statistics
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.extensions.getGTMTracks
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.utils.polymorphism.StatisticsUpdatable
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * MVVM [ViewModel] for
 * [com.dinaraparanid.prima.fragments.guess_the_melody.GTMPlaylistSelectFragment]
 */

class GtmGameViewModel(
    private val fragment: WeakReference<GTMGameFragment>,
    private var _trackNumber: Int,
    /** Tracks on buttons */
    @JvmField
    internal val tracks: AbstractPlaylist,
    /** All tracks */
    private val _tracks: AbstractPlaylist,
    private val playbackStart: Int,
    private val playbackLength: Byte,
    private var _score: Int = 0,
    private val unsolvedTracks: AbstractPlaylist = DefaultPlaylist()
) : ViewModel(), StatisticsUpdatable, CoroutineScope by MainScope() {
    private lateinit var buttonWithCorrectTrack: Button
    override val coroutineScope = this
    override val updateStyle = Statistics::withIncrementedNumberOfGuessedTracksInGTM

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
    private val playbackCondition = ConditionVariable()
    private val playbackExecutor = Executors.newSingleThreadExecutor()

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
                playbackCondition.block(playbackLength * 1000L)
                isPlaying = false

                fragment.unchecked.run {
                    launch(Dispatchers.Main) { setPlayButtonImage(isPlaying) }
                    releaseMusicPlayer()
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
                    runOnIOThread { updateStatisticsAsync() }
                    _score++
                    fragment.unchecked.scoreText = score
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
                val context = fragment.unchecked.requireContext()

                AlertDialog.Builder(context)
                    .setCancelable(false)
                    .setTitle(
                        "${context.getString(R.string.guessing_percent)}: " +
                                "${((_tracks.size - unsolvedTracks.size) / (_tracks.size / 100.0F)).roundToInt()}%"
                    )
                    .setSingleChoiceItems(
                        unsolvedTracks.map(AbstractTrack::gtmFormat).toTypedArray(), -1
                    ) { _, ind -> playUnsolvedTrack(unsolvedTracks[ind]) }
                    .setPositiveButton(R.string.finish) { d, _ ->
                        d.dismiss()
                        fragment.unchecked.requireActivity().finishAndRemoveTask()
                    }
                    .create()
                    .show()
            }

            else -> if (isNextButtonClickable)
                fragment.unchecked
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
                        GTMGameFragment.newInstance(
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
    internal fun onExitButtonClicked() {
        AlertDialog
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

    /**
     * Plays selected unsolved track.
     * Playback duration equals to the game's playback duration
     * and start position is chosen randomly
     *
     * @param track chosen unsolved track to play
     */

    private fun playUnsolvedTrack(track: AbstractTrack) = runOnIOThread {
        if (musicPlayer != null)
            fragment.unchecked.releaseMusicPlayer()

        musicPlayer = MediaPlayer().apply {
            setDataSource(track.path)

            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            setOnCompletionListener {
                fragment.unchecked.releaseMusicPlayer()
            }

            prepare()
            isLooping = false
            seekTo(track.getGTMRandomPlaybackStartPosition(playbackLength))
            start()
        }

        playbackExecutor.execute {
            playbackCondition.block(playbackLength * 1000L)
            fragment.unchecked.releaseMusicPlayer()
        }
    }
}
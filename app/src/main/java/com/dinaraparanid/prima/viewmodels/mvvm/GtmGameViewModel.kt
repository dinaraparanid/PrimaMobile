package com.dinaraparanid.prima.viewmodels.mvvm

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.databinding.Bindable
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.AbstractTrack
import com.dinaraparanid.prima.fragments.guess_the_melody.GtmGameFragment
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.extensions.unchecked
import java.lang.ref.WeakReference

/**
 * MVVM [ViewModel] for
 * [com.dinaraparanid.prima.fragments.guess_the_melody.GTMPlaylistSelectFragment]
 */

class GtmGameViewModel(
    private val fragment: WeakReference<GtmGameFragment>,
    private val _trackNumber: Int,
    private val _tracks: List<AbstractTrack>,
    private val correctTrack: AbstractTrack,
    private val playbackLength: Byte,
    private val _score: Int = 0,
): ViewModel() {
    @JvmField
    internal val tracks = (_tracks.shuffled().take(3) + correctTrack).shuffled()

    internal inline val score
        @JvmName("getScore")
        get() = "${fragment.unchecked.resources.getString(R.string.score)} $_score"

    internal inline val trackNumber
        @JvmName("getTrackNumber")
        get() = "${fragment.unchecked.resources.getString(R.string.gtm_track_number)} $_trackNumber"

    internal inline val playButtonImage
        @Bindable
        @JvmName("getPlayButtonImage")
        get() = ViewSetter.getPlayButtonImage(musicPlayer?.isPlaying == true)

    @Bindable
    @JvmField
    internal var isNextButtonClickable = false

    private val startPosition = maxOf(correctTrack.duration - playbackLength, 0).toInt()

    private inline var musicPlayer
        get() = fragment.unchecked.musicPlayer
        set(value) { fragment.unchecked.musicPlayer = value }

    @Synchronized
    @JvmName("onPlayButtonClicked")
    internal fun onPlayButtonClicked() {
        musicPlayer = MediaPlayer().apply {
            setDataSource(correctTrack.path)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            setOnCompletionListener {
                fragment.unchecked.releaseMusicPlayer()
            }

            prepareAsync()
            isLooping = false
            seekTo(startPosition)
            start()
        }
    }
}
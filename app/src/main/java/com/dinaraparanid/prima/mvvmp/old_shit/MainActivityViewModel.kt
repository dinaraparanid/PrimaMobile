package com.dinaraparanid.prima.mvvmp.old_shit

import android.view.View
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import java.lang.ref.WeakReference

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.MainActivity]
 */

class MainActivityViewModel(private val _activity: WeakReference<MainActivity>) : BasePresenter() {

    private inline val activity
        get() = _activity.unchecked

    @JvmName("liftPlayingMenu")
    internal fun liftPlayingMenu() = activity.liftPlayingMenu()

    @JvmName("onPlayingPrevTrackClicked")
    internal fun onPlayingPrevTrackClicked() = activity.onPlayingPrevTrackClicked()

    @JvmName("onPlayingNextTrackClicked")
    internal fun onPlayingNextTrackClicked() = activity.onPlayingNextTrackClicked()

    @JvmName("onPlayingAlbumImageClicked")
    internal fun onPlayingAlbumImageClicked() = activity.liftPlayingMenu()

    @JvmName("onNextTrackButtonClicked")
    internal fun onNextTrackButtonClicked() = activity.run {
        runOnUIThread { playNextAndUpdUI(isLocking = true) }
    }

    @JvmName("onPrevTrackButtonClicked")
    internal fun onPrevTrackButtonClicked() = activity.run {
        runOnUIThread { playPrevAndUpdUI(isLocking = true) }
    }

    @JvmName("onRecordButtonClicked")
    internal fun onRecordButtonClicked() = activity.onRecordButtonClicked()

    @JvmName("onRepeatButtonClicked")
    internal fun onRepeatButtonClicked() = activity.updateLooping(isOnlyUIUpdate = false)

    @JvmName("onPlaylistButtonClicked")
    internal fun onPlaylistButtonClicked() = activity.onPlaylistButtonClicked()

    @JvmName("onSleepTimerClicked")
    internal fun onSleepTimerClicked() = activity.onSleepTimerClicked()

    @JvmName("onReturnButtonClicked")
    internal fun onReturnButtonClicked() = activity.onReturnButtonClicked()

    @JvmName("onTrackSettingsButtonClicked")
    internal fun onTrackSettingsButtonClicked(view: View) =
        activity.onTrackSettingsButtonClicked(view)

    @JvmName("onPlayButtonClicked")
    internal fun onPlayButtonClicked() = activity.onPlayButtonClicked()

    @JvmName("onPlayingPlayButtonClicked")
    internal fun onPlayingPlayButtonClicked() = activity.onPlayingPlayButtonClicked()

    @JvmName("onEqualizerButtonClicked")
    internal fun onEqualizerButtonClicked() = activity.onEqualizerButtonClicked()

    @JvmName("onTrimButtonClicked")
    internal fun onTrimButtonClicked() = activity.onTrimButtonClicked()

    internal val appBarColor
        @JvmName("getAppBarColor")
        get() = when (Params.instance.backgroundImage) {
            null -> Params.instance.primaryColor
            else -> android.R.color.transparent
        }
}
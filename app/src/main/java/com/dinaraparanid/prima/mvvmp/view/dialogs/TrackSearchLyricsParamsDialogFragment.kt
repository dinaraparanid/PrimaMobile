package com.dinaraparanid.prima.mvvmp.view.dialogs

import com.dinaraparanid.prima.fragments.track_lists.TrackListFoundFragment
import kotlinx.coroutines.channels.Channel

/**
 * Dialog to input title and artist
 * for track of which lyrics should be found
 */

class TrackSearchLyricsParamsDialogFragment(
    trackTitle: String,
    artistName: String,
    setSheetBehaviourFromExpandedToCollapsedChannel: Channel<Unit>
) : TrackSearchParamsDialogFragment(
    trackTitle,
    artistName,
    target = TrackListFoundFragment.Target.LYRICS,
    setSheetBehaviourFromExpandedToCollapsedChannel
)
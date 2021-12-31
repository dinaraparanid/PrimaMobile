package com.dinaraparanid.prima.utils

import java.io.Serializable

/** User's statistics in some period of the time */

class Statistics(
    _musicInMinutes: Long,
    _numberOfTracks: Long,
    _numberOfConverted: Long,
    _numberOfRecorded: Long,
    _numberOfTrimmed: Long,
    _numberOfChanged: Long,
    _numberOfLyricsShown: Long,
    _numberOfCreatedPlaylists: Long,
    _numberOfGuessedTracksInGTM: Long
) : Serializable {
    internal var musicInMinutes = _musicInMinutes
        private set

    internal var numberOfTracks = _numberOfTracks
        private set

    internal var numberOfConverted = _numberOfConverted
        private set

    internal var numberOfRecorded = _numberOfRecorded
        private set

    internal var numberOfTrimmed = _numberOfTrimmed
        private set

    internal var numberOfChanged = _numberOfChanged
        private set

    internal var numberOfLyricsShown = _numberOfLyricsShown
        private set

    internal var numberOfCreatedPlaylists = _numberOfCreatedPlaylists
        private set

    internal var numberOfGuessedTracksInGTM = _numberOfGuessedTracksInGTM
        private set

    internal inline val withIncrementedMinutes
        get() = apply { musicInMinutes++ }

    internal inline val withIncrementedNumberOfTracks
        get() = apply { numberOfTracks++ }

    internal inline val withIncrementedNumberOfConverted
        get() = apply { numberOfConverted++ }

    internal inline val withIncrementedNumberOfRecorded
        get() = apply { numberOfRecorded++ }

    internal inline val withIncrementedNumberOfTrimmed
        get() = apply { numberOfTrimmed++ }

    internal inline val withIncrementedNumberOfChanged
        get() = apply { numberOfChanged++ }

    internal inline val withIncrementedNumberOfLyricsShown
        get() = apply { numberOfLyricsShown++ }

    internal inline val withIncrementedNumberOfCreatedPlaylists
        get() = apply { numberOfCreatedPlaylists++ }

    internal inline val withIncrementedNumberOfGuessedTracksInGTM
        get() = apply { numberOfGuessedTracksInGTM++ }

    internal companion object {
        /** Empty statistics without any progress */
        internal inline val empty
            @JvmStatic
            get() = Statistics(
                _musicInMinutes = 0,
                _numberOfTracks = 0,
                _numberOfConverted = 0,
                _numberOfRecorded = 0,
                _numberOfTrimmed = 0,
                _numberOfChanged = 0,
                _numberOfLyricsShown = 0,
                _numberOfCreatedPlaylists = 0,
                _numberOfGuessedTracksInGTM = 0
            )
    }
}

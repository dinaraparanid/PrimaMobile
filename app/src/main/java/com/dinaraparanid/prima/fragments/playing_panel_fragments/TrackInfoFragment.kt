package com.dinaraparanid.prima.fragments.playing_panel_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentTrackInfoBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.MainActivitySimpleFragment
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.utils.web.genius.songs_response.Song
import com.dinaraparanid.prima.viewmodels.mvvm.TrackInfoViewModel
import java.lang.ref.WeakReference

/**
 * Fragment that shows info about track from Genius website
 */

class TrackInfoFragment : MainActivitySimpleFragment<FragmentTrackInfoBinding>(), Rising {
    override var binding: FragmentTrackInfoBinding? = null
    private lateinit var track: Song

    internal companion object {
        private const val TRACK_KEY = "track"

        /**
         * Creates new instance of [TrackInfoFragment] with given params
         * @param track [Song] which info will be shown
         */

        @JvmStatic
        internal fun newInstance(track: Song) = TrackInfoFragment().apply {
            arguments = Bundle().apply { putSerializable(TRACK_KEY, track) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        track = requireArguments().getSerializable(TRACK_KEY)!! as Song
        mainLabelCurText = resources.getString(R.string.track_info)
        setMainLabelInitialized()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentTrackInfoBinding>(
            inflater,
            R.layout.fragment_track_info,
            container,
            false
        ).apply {
            viewModel = TrackInfoViewModel(
                WeakReference(requireActivity()),
                this@TrackInfoFragment.track.youTubeUrl?.url
            )

            track = this@TrackInfoFragment.track

            Glide.with(this@TrackInfoFragment)
                .load(this@TrackInfoFragment.track.songArtImageUrl)
                .placeholder(R.drawable.album_default)
                .skipMemoryCache(true)
                .override(trackInfoImage.width, trackInfoImage.height)
                .into(trackInfoImage)
        }

        if (application.playingBarIsVisible) up()
        return binding!!.root
    }

    override fun up() {
        if (!fragmentActivity.isUpped)
            binding!!.trackInfoMainLayout.layoutParams =
                (binding!!.trackInfoMainLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT
                }
    }
}
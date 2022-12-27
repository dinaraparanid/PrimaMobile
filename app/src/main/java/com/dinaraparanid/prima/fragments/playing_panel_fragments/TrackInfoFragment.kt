package com.dinaraparanid.prima.fragments.playing_panel_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentTrackInfoBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.mvvmp.view.fragments.MainActivitySimpleFragment
import com.dinaraparanid.prima.utils.web.genius.songs_response.Song
import com.dinaraparanid.prima.mvvmp.old_shit.TrackInfoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/** Fragment that shows info about track from Genius website */

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
        mainLabelText.set(resources.getString(R.string.track_info))
        setMainLabelInitializedSync()
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
            setImage()
        }

        if (application.playingBarIsVisible) up()
        return binding!!.root
    }

    /** Frees UI */
    override fun onStop() {
        super.onStop()
        binding?.trackInfoImage?.let(Glide.with(this)::clear)

        Glide.get(requireContext()).run {
            lifecycleScope.launch(Dispatchers.IO) { clearDiskCache() }
            bitmapPool.clearMemory()
            clearMemory()
        }
    }

    override fun onResume() {
        super.onResume()
        setImage()
    }

    /**
     * Rise fragment if playing bar is active.
     * It handles GUI error when playing bar was hiding some content
     */

    override fun up() {
        if (!fragmentActivity.isUpped)
            binding!!.trackInfoMainLayout.layoutParams =
                (binding!!.trackInfoMainLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT
                }
    }

    /** Sets track's cover */

    private fun setImage() {
        Glide.with(this@TrackInfoFragment)
            .load(this@TrackInfoFragment.track.songArtImageUrl)
            .placeholder(R.drawable.album_default)
            .error(R.drawable.album_default)
            .fallback(R.drawable.album_default)
            .skipMemoryCache(true)
            .override(binding!!.trackInfoImage.width, binding!!.trackInfoImage.height)
            .into(binding!!.trackInfoImage)
    }
}
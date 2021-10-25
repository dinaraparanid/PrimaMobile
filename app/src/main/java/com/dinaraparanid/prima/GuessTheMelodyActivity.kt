package com.dinaraparanid.prima

import android.os.Bundle
import androidx.core.graphics.drawable.toDrawable
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.dinaraparanid.prima.core.AbstractTrack
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.databinding.ActivityGtmBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.polymorphism.AbstractActivity
import com.dinaraparanid.prima.viewmodels.androidx.GuessTheMelodyActivityViewModel

/** Activity for the "Guess the Melody" game */

class GuessTheMelodyActivity : AbstractActivity() {
    private var binding: ActivityGtmBinding? = null

    override val viewModel: GuessTheMelodyActivityViewModel by lazy {
        ViewModelProvider(this)[GuessTheMelodyActivityViewModel::class.java]
    }

    internal companion object {
        internal const val PLAYLIST_KEY = "playlist"
        internal const val MAX_PLAYBACK_LENGTH_KEY = "max_playback_length"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme()
        super.onCreate(savedInstanceState)
        initView(savedInstanceState)

        viewModel.load(
            DefaultPlaylist(tracks = intent.getSerializableExtra(PLAYLIST_KEY) as Array<AbstractTrack>),
            intent.getByteExtra(MAX_PLAYBACK_LENGTH_KEY, 5)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding = DataBindingUtil
            .setContentView<ActivityGtmBinding>(this, R.layout.activity_gtm)
            .apply {
                Params.instance.backgroundImage?.run {
                    gtmMainLayout.background = toBitmap().toDrawable(resources)
                }
            }
    }
}
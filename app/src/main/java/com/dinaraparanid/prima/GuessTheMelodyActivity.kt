package com.dinaraparanid.prima

import android.os.Bundle
import androidx.core.graphics.drawable.toDrawable
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.dinaraparanid.prima.core.AbstractTrack
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.databinding.GtmActivityBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.polymorphism.AbstractActivity
import com.dinaraparanid.prima.viewmodels.androidx.GuessTheMelodyActivityViewModel

class GuessTheMelodyActivity : AbstractActivity() {
    private var binding: GtmActivityBinding? = null

    override val viewModel: GuessTheMelodyActivityViewModel by lazy {
        ViewModelProvider(this)[GuessTheMelodyActivityViewModel::class.java]
    }

    internal companion object {
        internal const val PLAYLIST_KEY = "playlist"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme()
        super.onCreate(savedInstanceState)
        viewModel.load(DefaultPlaylist(tracks = intent.getSerializableExtra(PLAYLIST_KEY) as Array<AbstractTrack>))
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding = DataBindingUtil
            .setContentView<GtmActivityBinding>(this, R.layout.gtm_activity)
            .apply {
                Params.instance.backgroundImage?.run {
                    gtmMainLayout.background = toBitmap().toDrawable(resources)
                }
            }
    }
}
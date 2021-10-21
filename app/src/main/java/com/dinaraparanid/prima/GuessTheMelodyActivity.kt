package com.dinaraparanid.prima

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.dinaraparanid.prima.core.AbstractTrack
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.databinding.GtmActivityBinding
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

    override fun initView(savedInstanceState: Bundle?) {
        binding = DataBindingUtil.setContentView(this, R.layout.gtm_activity)
    }
}
package com.dinaraparanid.prima

import android.app.AlertDialog
import android.os.Bundle
import androidx.core.graphics.drawable.toDrawable
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.databinding.ActivityGtmBinding
import com.dinaraparanid.prima.fragments.guess_the_melody.GTMGameFragment
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.getGTMTracks
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractActivity
import com.dinaraparanid.prima.viewmodels.androidx.GuessTheMelodyActivityViewModel
import java.lang.ref.WeakReference

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

        viewModel.load(
            intent.getSerializableExtra(PLAYLIST_KEY) as Array<AbstractTrack>,
            intent.getByteExtra(MAX_PLAYBACK_LENGTH_KEY, 5)
        )

        initView(savedInstanceState)
    }

    override fun onBackPressed() {
        AlertDialog
            .Builder(this)
            .setCancelable(true)
            .setMessage(R.string.exit_request)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .show()
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

        initFirstFragment()
    }

    override fun initFirstFragment() {
        currentFragment = WeakReference(
            supportFragmentManager.findFragmentById(R.id.gtm_fragment_container)
        )

        if (currentFragment.get() == null) {
            val tracks = viewModel.playlistFlow.value.shuffled().toPlaylist()

            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.gtm_fragment_container,
                    GTMGameFragment.newInstance(
                        tracks,
                        tracks.getGTMTracks(),
                        tracks[0].getGTMRandomPlaybackStartPosition(
                            viewModel.maxPlaybackLengthFlow.value
                        ),
                        viewModel.maxPlaybackLengthFlow.value
                    )
                )
                .commit()
        }
    }
}
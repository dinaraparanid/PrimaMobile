package com.dinaraparanid.prima.fragments.main_menu.settings

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentThemesBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.utils.polymorphism.fragments.ChangeImageFragment
import com.dinaraparanid.prima.utils.polymorphism.fragments.MainActivitySimpleFragment
import com.dinaraparanid.prima.viewmodels.mvvm.ThemesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/** Fragment for customizing themes */

class ThemesFragment : MainActivitySimpleFragment<FragmentThemesBinding>(),
    Rising, ChangeImageFragment {
    override var binding: FragmentThemesBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelCurText.set(requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY)!!)
        setMainLabelInitializedSync()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate<FragmentThemesBinding>(inflater, R.layout.fragment_themes, container, false)
            .apply { viewModel = ThemesViewModel(WeakReference(fragmentActivity)) }

        val binding = binding!!

        arrayOf(
            binding.purple to 0,
            binding.purpleNight to 1,
            binding.red to 2,
            binding.redNight to 3,
            binding.blue to 4,
            binding.blueNight to 5,
            binding.green to 6,
            binding.greenNight to 7,
            binding.orange to 8,
            binding.orangeNight to 9,
            binding.lemon to 10,
            binding.lemonNight to 11,
            binding.turquoise to 12,
            binding.turquoiseNight to 13,
            binding.greenTurquoise to 14,
            binding.greenTurquoiseNight to 15,
            binding.sea to 16,
            binding.seaNight to 17,
            binding.pink to 18,
            binding.pinkNight to 19
        ).forEach { (b, t) ->
            b.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    StorageUtil.getInstanceSynchronized().run {
                        clearPrimaryColor()
                        clearSecondaryColor()
                    }
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    Params.getInstanceSynchronized().changeTheme(requireActivity(), t)
                }
            }
        }

        if (application.playingBarIsVisible) up()
        return binding.root
    }

    /**
     * Rise fragment if playing bar is active.
     * It handles GUI error when playing bar was hiding some content
     */

    override fun up() {
        if (!fragmentActivity.isUpped)
            binding!!.themesLayout.layoutParams =
                (binding!!.themesLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT
                }
    }

    override suspend fun setUserImageAsync(image: Uri) =
        fragmentActivity.updateViewOnUserImageSelectedAsync(image)
}
package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentThemesBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.viewmodels.mvvm.ThemesViewModel

/**
 * Fragment for customizing themes
 */

class ThemesFragment : AbstractFragment(), Rising {
    private lateinit var binding: FragmentThemesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil
            .inflate<FragmentThemesBinding>(inflater, R.layout.fragment_themes, container, false)
            .apply { viewModel = ThemesViewModel(binding.themesLayout) }

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
                Params.instance.changeTheme(requireContext(), t)
            }

            StorageUtil(requireContext()).clearCustomThemeColors()
        }

        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        return view
    }

    override fun up() {
        val act = requireActivity() as MainActivity

        if (!act.upped)
            binding.themesLayout.layoutParams =
                (binding.themesLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = act.playingToolbarHeight
                }
    }
}
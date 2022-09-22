package com.dinaraparanid.prima.fragments.main_menu.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentLanguagesBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.utils.polymorphism.fragments.MainActivitySimpleFragment
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fragment for choosing languages */
@Deprecated("There are not so many languages, so there is no reasons why fragment should be used")
class LanguagesFragment : MainActivitySimpleFragment<FragmentLanguagesBinding>(), Rising {
    override var binding: FragmentLanguagesBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelCurText = resources.getString(R.string.language)
        setMainLabelInitializedSync()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_languages, container, false)
        val binding = binding!!
        binding.viewModel = ViewModel()

        arrayOf(
            binding.english to Params.Companion.Language.EN,
            binding.belarusian to Params.Companion.Language.BE,
            binding.russian to Params.Companion.Language.RU,
            binding.chinese to Params.Companion.Language.ZH
        ).forEach { (b, lang) ->
            b.setOnClickListener {
                lifecycleScope.launch(Dispatchers.Main) {
                    Params.getInstanceSynchronized().changeLang(requireActivity(), lang)
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
            binding!!.languagesLayout.layoutParams =
                (binding!!.languagesLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT
                }
    }
}
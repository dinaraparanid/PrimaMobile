package com.dinaraparanid.prima.utils.polymorphism

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentViewPagerBinding
import com.google.android.material.tabs.TabLayoutMediator
import kotlin.reflect.KClass

/**
 * Ancestor for all View Pager Fragments
 */

abstract class ViewPagerFragment : MainActivitySimpleFragment<FragmentViewPagerBinding>() {
    final override var binding: FragmentViewPagerBinding? = null

    protected abstract val firstFragment: Fragment
    protected abstract val secondFragment: Fragment

    protected abstract val firstFragmentTitle: Int
    protected abstract val secondFragmentTitle: Int

    private var startSelectedType = 0

    internal companion object {
        private const val ARG_SELECTED_TYPE = "selected_type"

        @JvmStatic
        internal fun <T> newInstance(
            mainLabelOldText: String,
            clazz: KClass<T>,
            selectedType: Int
        )  where T: Fragment, T: MainActivityFragment
        = clazz.constructors.first().call().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putInt(ARG_SELECTED_TYPE, selectedType)
            }
        }
    }

    final override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelCurText = ""
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        startSelectedType = requireArguments().getInt(ARG_SELECTED_TYPE)
        setMainLabelInitialized()
        super.onCreate(savedInstanceState)
    }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_view_pager,
            container,
            false
        )

        return binding!!.root
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.pager.adapter = FavouritesAdapter(this)

        TabLayoutMediator(binding!!.tabLayout, binding!!.pager) { tab, pos ->
            tab.text = when (pos) {
                0 -> resources.getString(firstFragmentTitle)
                else -> resources.getString(secondFragmentTitle)
            }
        }.attach()
    }

    private inner class FavouritesAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount() = 2

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> firstFragment
            else -> secondFragment
        }
    }
}
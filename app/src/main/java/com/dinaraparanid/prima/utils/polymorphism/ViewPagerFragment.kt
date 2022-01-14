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
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.delay
import kotlin.reflect.KClass

/** Ancestor for all View Pager Fragments */

abstract class ViewPagerFragment : MainActivitySimpleFragment<FragmentViewPagerBinding>() {
    final override var binding: FragmentViewPagerBinding? = null

    protected abstract val fragments: Array<() -> Fragment>
    protected abstract val fragmentsTitles: IntArray
    protected abstract val isTabShown: Boolean

    private var startSelectedType = 0

    internal companion object {
        private const val ARG_SELECTED_TYPE = "selected_type"

        @JvmStatic
        internal fun <T : ViewPagerFragment> newInstance(
            mainLabelOldText: String,
            selectedType: Int,
            clazz: KClass<T>,
        ) = clazz.constructors.first().call().apply {
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
        binding = DataBindingUtil.inflate<FragmentViewPagerBinding>(
            inflater,
            R.layout.fragment_view_pager,
            container,
            false
        ).apply {
            viewModel = ViewModel()
            tabVisibility = if (this@ViewPagerFragment.isTabShown) View.VISIBLE else View.GONE
        }

        return binding!!.root
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.pager.adapter = FavouritesAdapter()

        fragmentActivity.runOnUIThread {
            delay(100)
            binding!!.pager.setCurrentItem(startSelectedType, false)
        }

        if (isTabShown) TabLayoutMediator(binding!!.tabLayout, binding!!.pager) { tab, pos ->
            tab.text = resources.getString(fragmentsTitles[pos])
        }.attach()
    }

    private inner class FavouritesAdapter : FragmentStateAdapter(
        childFragmentManager,
        viewLifecycleOwner.lifecycle
    ) {
        override fun getItemCount() = fragments.size
        override fun createFragment(position: Int) = fragments[position]()
    }
}
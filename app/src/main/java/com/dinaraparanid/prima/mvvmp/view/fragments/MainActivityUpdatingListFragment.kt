package com.dinaraparanid.prima.mvvmp.view.fragments

import android.os.Parcelable
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.mvvmp.presenters.MainActivityListPresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.UIHandler
import com.dinaraparanid.prima.mvvmp.view_models.ObservableViewModel
import com.dinaraparanid.prima.utils.polymorphism.AsyncListDifferAdapter
import com.dinaraparanid.prima.utils.polymorphism.Rising

/** [UpdatingListFragment] for MainActivity's list fragments */

abstract class MainActivityUpdatingListFragment<P, VM, H, B, T, A, VH> :
    UpdatingListFragment<P, VM, H, B, MainActivity, T, A, VH>(),
    MainActivityFragment,
    MenuProviderFragment,
    Rising
        where P : MainActivityListPresenter,
              VM : ObservableViewModel<P>,
              H : UIHandler,
              B : ViewDataBinding,
              T : Parcelable,
              VH : RecyclerView.ViewHolder,
              A : AsyncListDifferAdapter<T, VH> {
    final override val menuProvider = defaultMenuProvider

    override fun onResume() {
        super.onResume()
        requireActivity().addMenuProvider(menuProvider)
    }

    final override fun onPause() {
        super.onPause()
        requireActivity().removeMenuProvider(menuProvider)
    }

    final override fun onDestroy() {
        super.onDestroy()
        Glide.get(requireContext()).clearMemory()
    }
}
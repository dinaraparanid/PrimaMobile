package com.dinaraparanid.prima.utils.polymorphism

import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.io.Serializable

/** [RecyclerView.Adapter] with [AsyncListDiffer] */

abstract class AsyncListDifferAdapter<T: Serializable, VH: RecyclerView.ViewHolder> :
    RecyclerView.Adapter<VH>() {
    protected val differ by lazy {
        AsyncListDiffer(self, object : DiffUtil.ItemCallback<T>() {
            override fun areItemsTheSame(oldItem: T, newItem: T) = areItemsEqual(oldItem, newItem)
            override fun areContentsTheSame(oldItem: T, newItem: T) = areItemsEqual(oldItem, newItem)
        })
    }

    abstract fun areItemsEqual(first: T, second: T): Boolean
    abstract val self: AsyncListDifferAdapter<T, VH>

    internal inline var currentList
        get() = differ.currentList
        set(value) = differ.submitList(value)

    final override fun getItemCount() = differ.currentList.size
}
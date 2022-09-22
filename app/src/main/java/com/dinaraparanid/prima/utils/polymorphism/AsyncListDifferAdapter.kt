package com.dinaraparanid.prima.utils.polymorphism

import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Serializable

/** [RecyclerView.Adapter] with [AsyncListDiffer] */

abstract class AsyncListDifferAdapter<T : Serializable, VH : RecyclerView.ViewHolder> :
    RecyclerView.Adapter<VH>() {
    protected val differ: AsyncListDiffer<T> by lazy {
        AsyncListDiffer(this, object : DiffUtil.ItemCallback<T>() {
            override fun areItemsTheSame(oldItem: T, newItem: T) = areItemsEqual(oldItem, newItem)
            override fun areContentsTheSame(oldItem: T, newItem: T) =
                areItemsEqual(oldItem, newItem)
        })
    }

    private val mutex = Mutex()

    abstract fun areItemsEqual(first: T, second: T): Boolean

    internal inline val currentList
        get() = differ.currentList

    internal suspend fun setCurrentList(list: List<T>) =
        mutex.withLock { differ.submitList(list.toList()) }

    internal suspend inline fun setCurrentList(
        list: List<T>,
        noinline onFinishCallback: () -> Unit
    ) = mutex.withLock { differ.submitList(list.toList(), onFinishCallback) }

    final override fun getItemCount() = currentList.size
}
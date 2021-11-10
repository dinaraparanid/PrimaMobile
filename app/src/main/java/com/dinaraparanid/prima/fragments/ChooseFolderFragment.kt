package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.FoldersActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Folder
import com.dinaraparanid.prima.databinding.FragmentChooseFolderBinding
import com.dinaraparanid.prima.databinding.ListItemFolderBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.dialogs.NewFolderDialog
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.viewmodels.androidx.DefaultViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.ChooseFolderViewModel
import kotlinx.coroutines.Job
import java.lang.ref.WeakReference

class ChooseFolderFragment :
    UpdatingListFragment<FoldersActivity,
            Folder,
            ChooseFolderFragment.FolderAdapter,
            ChooseFolderFragment.FolderAdapter.FolderHolder,
            FragmentChooseFolderBinding>() {
    interface Callbacks : CallbacksFragment.Callbacks {
        /**
         * Saves [folder]'s [Folder.path] as path of converted mp3 tracks
         * @param folder folder which [Folder.path] will be saved
         */

        fun onFolderSelected(folder: Folder)
    }

    private lateinit var folder: Folder

    override val viewModel: ViewModel by lazy {
        ViewModelProvider(this)[DefaultViewModel::class.java]
    }

    override var adapter: FolderAdapter? = FolderAdapter(emptyList())
    override var binding: FragmentChooseFolderBinding? = null
    override var emptyTextView: TextView? = null
    override var updater: SwipeRefreshLayout? = null

    internal companion object {
        private const val FOLDER_KEY = "folder"

        /**
         * Creates new instance of [ChooseFolderFragment] with given param
         * @param folder [Folder] which sub folders will be shown
         * @return created fragment
         */

        @JvmStatic
        internal fun newInstance(folder: Folder) = ChooseFolderFragment().apply {
            arguments = Bundle().apply {
                putSerializable(FOLDER_KEY, folder)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        folder = requireArguments().getSerializable(FOLDER_KEY) as Folder
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate<FragmentChooseFolderBinding>(
                inflater,
                R.layout.fragment_choose_folder,
                container,
                false
            )
            .apply {
                viewModel = ChooseFolderViewModel(folder.path, WeakReference(fragmentActivity))
                emptyTextView = foldersEmpty

                updater = foldersSwipeRefreshLayout.apply {
                    setColorSchemeColors(Params.instance.primaryColor)
                    setOnRefreshListener {
                        runOnUIThread {
                            loadAsync().join()
                            updateUIAsync()
                            isRefreshing = false
                        }
                    }
                }
            }

        runOnUIThread {
            val task = loadAsync()
            val progress = createAndShowAwaitDialog(requireContext(), false)

            task.join()
            progress.dismiss()

            itemListSearch.addAll(itemList)
            adapter = FolderAdapter(itemList).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            setEmptyTextViewVisibility(itemList)

            recyclerView = binding!!.foldersRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)

                adapter = this@ChooseFolderFragment.adapter?.apply {
                    stateRestorationPolicy =
                        RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                }

                addItemDecoration(VerticalSpaceItemDecoration(30))
            }
        }

        return binding!!.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_choose_folder, menu)
        (menu.findItem(R.id.folder_find).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.create_folder)
            NewFolderDialog(WeakReference(this), folder.path, coroutineScope)
                .show(requireActivity().supportFragmentManager, null)

        return super.onOptionsItemSelected(item)
    }

    override suspend fun loadAsync(): Job = runOnWorkerThread {
        itemList.clear()
        itemList.addAll(folder.folders)
    }

    override fun filter(models: Collection<Folder>?, query: String): List<Folder> =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.title.lowercase() } ?: listOf()
        }

    override suspend fun updateUIAsync(src: List<Folder>): Job = runOnUIThread {
        adapter = FolderAdapter(src).apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        recyclerView!!.adapter = adapter
        setEmptyTextViewVisibility(src)
    }

    /**
     * [RecyclerView.Adapter] for [ChooseFolderFragment]
     * @param folders folders to bind and use in adapter
     */

    inner class FolderAdapter(private val folders: List<Folder>) :
        RecyclerView.Adapter<FolderAdapter.FolderHolder>() {
        /**
         * [RecyclerView.ViewHolder] for folders of [FolderAdapter]
         */

        inner class FolderHolder(private val folderBinding: ListItemFolderBinding) :
            RecyclerView.ViewHolder(folderBinding.root),
            View.OnClickListener {
            private lateinit var folder: Folder

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) = (callbacker as Callbacks).onFolderSelected(folder)

            /**
             * Constructs GUI for folder item
             * @param _folder folder to bind
             */

            fun bind(_folder: Folder) {
                folderBinding.viewModel = binding!!.viewModel!!
                folderBinding.folder = _folder
                folderBinding.executePendingBindings()
                folder = _folder
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderHolder =
            FolderHolder(
                ListItemFolderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun getItemCount(): Int = folders.size

        override fun onBindViewHolder(holder: FolderHolder, position: Int): Unit =
            holder.bind(folders[position])
    }
}
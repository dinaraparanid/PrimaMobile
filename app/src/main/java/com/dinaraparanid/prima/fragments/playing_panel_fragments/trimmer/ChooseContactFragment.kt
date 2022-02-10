package com.dinaraparanid.prima.fragments.playing_panel_fragments.trimmer

import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Contact
import com.dinaraparanid.prima.databinding.FragmentChooseContactBinding
import com.dinaraparanid.prima.databinding.ListItemContactBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.viewmodels.androidx.DefaultViewModel
import com.kaopiz.kprogresshud.KProgressHUD
import kotlinx.coroutines.Job

/** Fragment to set ringtone for chosen contact */

class ChooseContactFragment : MainActivityUpdatingListFragment<
        Contact,
        ChooseContactFragment.ContactAdapter,
        ChooseContactFragment.ContactAdapter.ContactHolder,
        FragmentChooseContactBinding>() {
    interface Callbacks : CallbacksFragment.Callbacks {
        /**
         * Sets ringtone to contact
         * @param contact contact himself
         */

        fun onContactSelected(contact: Contact, ringtoneUri: Uri)
    }

    internal companion object {
        private const val RINGTONE_URI_KEY = "ringtone_uri"

        /**
         * Creates new instance of [ChooseContactFragment] with given params
         * @param ringtoneUri [Uri] of ringtone to set for [Contact]
         */

        @JvmStatic
        internal fun newInstance(ringtoneUri: Uri) = ChooseContactFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
                putParcelable(RINGTONE_URI_KEY, ringtoneUri)
            }
        }
    }

    override val viewModel by lazy {
        ViewModelProvider(this)[DefaultViewModel::class.java]
    }

    override var _adapter: ContactAdapter? = null
    override var updater: SwipeRefreshLayout? = null
    override var binding: FragmentChooseContactBinding? = null
    override var emptyTextView: TextView? = null

    private lateinit var ringtoneUri: Uri
    private var awaitDialog: KProgressHUD? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelCurText = resources.getString(R.string.choose_contact_title)
        ringtoneUri = requireArguments().getParcelable(RINGTONE_URI_KEY)!!

        setMainLabelInitialized()
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate<FragmentChooseContactBinding>(
                inflater,
                R.layout.fragment_choose_contact,
                container,
                false
            )
            .apply {
                viewModel = com.dinaraparanid.prima.viewmodels.mvvm.ViewModel()
                emptyTextView = contactEmpty

                updater = contactSwipeRefreshLayout.apply {
                    setOnRefreshListener {
                        runOnUIThread {
                            setColorSchemeColors(Params.getInstanceSynchronized().primaryColor)
                            loadAsync().join()
                            updateUIAsync(isLocking = true)
                            isRefreshing = false
                        }
                    }
                }
            }

        runOnUIThread {
            val task = loadAsync()
            awaitDialog = createAndShowAwaitDialog(requireContext(), false)

            task.join()
            awaitDialog?.dismiss()
            initAdapter()

            itemListSearch.addAll(itemList)
            adapter.setCurrentList(itemList)
            setEmptyTextViewVisibility(itemList)

            recyclerView = binding!!.contactRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@ChooseContactFragment.adapter
                addItemDecoration(VerticalSpaceItemDecoration(30))
            }

            if (application.playingBarIsVisible) up()
        }

        fragmentActivity.mainLabelCurText = mainLabelCurText
        return binding!!.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        awaitDialog?.dismiss()
        awaitDialog = null
    }

    override suspend fun updateUIAsyncNoLock(src: List<Contact>) {
        adapter.setCurrentList(src)
        recyclerView!!.adapter = adapter
        setEmptyTextViewVisibility(src)
    }

    override fun filter(models: Collection<Contact>?, query: String): List<Contact> =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.displayName.lowercase() } ?: listOf()
        }

    override suspend fun loadAsync(): Job = runOnIOThread {
        try {
            requireActivity().contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.CUSTOM_RINGTONE,
                    ContactsContract.Contacts.DISPLAY_NAME
                ),
                null,
                null,
                "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
            ).use { cursor ->
                itemList.clear()

                if (cursor != null) {
                    val contactList = mutableListOf<Contact>()

                    while (cursor.moveToNext()) {
                        contactList.add(
                            Contact(
                                cursor.getLong(0),
                                cursor.getString(1) ?: "",
                                cursor.getString(2)
                            )
                        )
                    }

                    itemList.addAll(contactList.distinctBy(Contact::id))
                }
            }
        } catch (e: Exception) {
            // Permission to storage not given
            e.printStackTrace()
        }
    }

    override fun initAdapter() {
        _adapter = ContactAdapter().apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    /** [RecyclerView.Adapter] for [ChooseContactFragment] */

    inner class ContactAdapter : AsyncListDifferAdapter<Contact, ContactAdapter.ContactHolder>() {
        override fun areItemsEqual(first: Contact, second: Contact) = first == second

        /** [RecyclerView.ViewHolder] for contacts of [ContactAdapter] */

        inner class ContactHolder(private val contactBinding: ListItemContactBinding) :
            RecyclerView.ViewHolder(contactBinding.root),
            View.OnClickListener {
            private lateinit var contact: Contact

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) =
                (callbacker as Callbacks).onContactSelected(contact, ringtoneUri)

            /**
             * Constructs GUI for contact item
             * @param _contact contact to bind
             */

            fun bind(_contact: Contact) {
                contactBinding.viewModel = binding!!.viewModel!!
                contactBinding.contact = _contact
                contactBinding.executePendingBindings()
                contact = _contact
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactHolder =
            ContactHolder(
                ListItemContactBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: ContactHolder, position: Int) =
            holder.bind(differ.currentList[position])
    }
}
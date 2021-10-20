package com.dinaraparanid.prima.fragments

import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Contact
import com.dinaraparanid.prima.databinding.FragmentChooseContactBinding
import com.dinaraparanid.prima.databinding.ListItemContactBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.CallbacksFragment
import com.dinaraparanid.prima.utils.polymorphism.UpdatingListFragment
import com.dinaraparanid.prima.viewmodels.androidx.DefaultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Fragment to set ringtone for chosen contact
 */

class ChooseContactFragment :
    UpdatingListFragment<Contact,
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
         * @param mainLabelCurText main label's text when fragment was created
         * @param mainLabelOldText main label's text when fragment was started to create
         * @param ringtoneUri [Uri] of ringtone to set for [Contact]
         */

        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            mainLabelCurText: String,
            ringtoneUri: Uri
        ) = ChooseContactFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
                putParcelable(RINGTONE_URI_KEY, ringtoneUri)
            }
        }
    }

    override val viewModel: ViewModel by lazy {
        ViewModelProvider(this)[DefaultViewModel::class.java]
    }

    override var updater: SwipeRefreshLayout? = null
    override var binding: FragmentChooseContactBinding? = null
    override var adapter: ContactAdapter? = ContactAdapter(listOf())
    override var emptyTextView: TextView? = null

    private lateinit var ringtoneUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mainLabelOldText =
            requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText =
            requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY)!!
        ringtoneUri = requireArguments().getParcelable(RINGTONE_URI_KEY)!!
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
                    setColorSchemeColors(Params.instance.primaryColor)
                    setOnRefreshListener {
                        this@ChooseContactFragment.viewModel.viewModelScope.launch(Dispatchers.Main) {
                            loadAsync().join()
                            updateUI()
                            isRefreshing = false
                        }
                    }
                }
            }

        viewModel.viewModelScope.launch(Dispatchers.Main) {
            val task = loadAsync()
            val progress = createAndShowAwaitDialog(requireContext(), false)

            task.join()
            progress.dismiss()

            itemListSearch.addAll(itemList)
            adapter = ContactAdapter(itemList).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            setEmptyTextViewVisibility(itemList)

            recyclerView = binding!!.contactRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)

                adapter = this@ChooseContactFragment.adapter?.apply {
                    stateRestorationPolicy =
                        RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                }

                addItemDecoration(VerticalSpaceItemDecoration(30))
            }

            if (application.playingBarIsVisible) up()
        }

        mainActivity.mainLabelCurText = mainLabelCurText
        return binding!!.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun updateUI(src: List<Contact>) {
        viewModel.viewModelScope.launch(Dispatchers.Main) {
            adapter = ContactAdapter(src).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            recyclerView!!.adapter = adapter
            setEmptyTextViewVisibility(src)
        }
    }

    override fun filter(models: Collection<Contact>?, query: String): List<Contact> =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.displayName.lowercase() } ?: listOf()
        }

    override suspend fun loadAsync(): Job = coroutineScope {
        launch(Dispatchers.IO) {
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
    }

    /**
     * [RecyclerView.Adapter] for [ChooseContactFragment]
     * @param contacts contacts to bind and use in adapter
     */

    inner class ContactAdapter(private val contacts: List<Contact>) :
        RecyclerView.Adapter<ContactAdapter.ContactHolder>() {
        /**
         * [RecyclerView.ViewHolder] for artists of [ContactAdapter]
         */

        inner class ContactHolder(private val contactBinding: ListItemContactBinding) :
            RecyclerView.ViewHolder(contactBinding.root),
            View.OnClickListener {
            private lateinit var contact: Contact

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                (callbacker as Callbacks).onContactSelected(contact, ringtoneUri)
            }

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

        override fun getItemCount(): Int = contacts.size

        override fun onBindViewHolder(holder: ContactHolder, position: Int): Unit =
            holder.bind(contacts[position])
    }
}
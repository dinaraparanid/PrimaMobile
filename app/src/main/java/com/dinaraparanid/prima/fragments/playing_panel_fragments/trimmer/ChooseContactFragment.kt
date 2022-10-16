package com.dinaraparanid.prima.fragments.playing_panel_fragments.trimmer

import android.Manifest
import android.net.Uri
import android.os.Build
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
import com.dinaraparanid.prima.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.decorations.DividerItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.drawables.Divider
import com.dinaraparanid.prima.utils.polymorphism.AsyncListDifferAdapter
import com.dinaraparanid.prima.utils.polymorphism.fragments.CallbacksFragment
import com.dinaraparanid.prima.utils.polymorphism.fragments.MainActivityUpdatingListFragment
import com.dinaraparanid.prima.utils.polymorphism.fragments.setMainLabelInitializedSync
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.viewmodels.androidx.DefaultViewModel
import com.kaopiz.kprogresshud.KProgressHUD
import com.vmadalin.easypermissions.EasyPermissions

/** Fragment to set ringtone for chosen contact */

class ChooseContactFragment : MainActivityUpdatingListFragment<
        Contact,
        ChooseContactFragment.ContactAdapter,
        ChooseContactFragment.ContactAdapter.ContactHolder,
        FragmentChooseContactBinding>(),
    EasyPermissions.PermissionCallbacks {
    interface Callbacks : CallbacksFragment.Callbacks {

        /**
         * Sets ringtone to contact
         * @param contact contact himself
         */

        fun onContactSelected(contact: Contact, ringtoneUri: Uri)
    }

    internal companion object {
        private const val RINGTONE_URI_KEY = "ringtone_uri"
        private const val CONTACTS_PERMISSIONS_REQUEST_CODE = 0

        /**
         * Creates new instance of [ChooseContactFragment] with given params
         * @param ringtoneUri [Uri] of ringtone to set for [Contact]
         */

        @JvmStatic
        internal fun newInstance(ringtoneUri: Uri) = ChooseContactFragment().apply {
            arguments = Bundle().apply {
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
        mainLabelCurText.set(resources.getString(R.string.choose_contact_title))

        ringtoneUri = requireArguments().let { args ->
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                    args.getParcelable(RINGTONE_URI_KEY, Uri::class.java)

                else -> args.getParcelable(RINGTONE_URI_KEY)
            }!!
        }

        setMainLabelInitializedSync()
        super.onCreate(savedInstanceState)

        if (!areContactsPermissionsGranted)
            requestContactsPermissions()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateMenu(menu, menuInflater)
        menuInflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView)
            .setOnQueryTextListener(this@ChooseContactFragment)
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

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N &&
                    Params.getInstanceSynchronized().areDividersShown
                ) addItemDecoration(DividerItemDecoration(requireContext(), Divider.instance))
            }

            if (application.playingBarIsVisible) up()
        }

        fragmentActivity.mainLabelCurText = mainLabelCurText.get()
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        awaitDialog?.dismiss()
        awaitDialog = null
    }

    /** Reloads contacts and updates adapter with empty text view */
    override suspend fun updateUIAsyncNoLock(src: List<Contact>) {
        adapter.setCurrentList(src)
        recyclerView!!.adapter = adapter
        setEmptyTextViewVisibility(src)
    }

    /**
     * Filters contacts with query (contact's name must contains query)
     * @param models contacts to filter
     * @param query searched name
     */

    override fun filter(models: Collection<Contact>?, query: String) =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.displayName.lowercase() } ?: listOf()
        }

    /** Loads contacts from [ContactsContract] */
    override suspend fun loadAsync() = runOnIOThread {
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

    /** Initializes adapter */
    override fun initAdapter() {
        _adapter = ContactAdapter().apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    /** Requests permission to load contacts */
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) =
        requestContactsPermissions()

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) = Unit

    private inline val areContactsPermissionsGranted
        get() = EasyPermissions.hasPermissions(
            requireContext().applicationContext,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )

    /** Requests permission to load contacts */
    private fun requestContactsPermissions() = EasyPermissions.requestPermissions(
        this,
        resources.getString(R.string.contacts_permission_why),
        CONTACTS_PERMISSIONS_REQUEST_CODE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS
    )

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

            internal fun bind(_contact: Contact) {
                contactBinding.viewModel = binding!!.viewModel!!
                contactBinding.contact = _contact
                contactBinding.executePendingBindings()
                contact = _contact
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ContactHolder(
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
package com.dinaraparanid.prima.trimmer

import android.app.ListActivity
import android.app.LoaderManager
import android.content.ContentValues
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.SimpleCursorAdapter
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.ActivityChooseContactBinding
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel

/**
 * After a ringtone has been saved, this activity lets you pick a contact
 * and assign the ringtone to that contact.
 */

class ChooseContactActivity : ListActivity(), TextWatcher,
    LoaderManager.LoaderCallbacks<Cursor> {
    private lateinit var adapter: SimpleCursorAdapter
    private lateinit var ringtoneUri: Uri
    private lateinit var binding: ActivityChooseContactBinding

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = Unit
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = Unit

    override fun afterTextChanged(s: Editable) {
        val args = Bundle()
        args.putString("filter", binding.searchFilter.text.toString())
        loaderManager.restartLoader(0, args, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor> {
        var selection: String? = null
        val filter = args.getString("filter")

        if (filter != null && filter.isNotEmpty())
            selection = "(DISPLAY_NAME LIKE \"%$filter%\")"

        return CursorLoader(
            this,
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.CUSTOM_RINGTONE,
                ContactsContract.Contacts.DISPLAY_NAME,
            ),
            selection,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        adapter.swapCursor(data)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter.swapCursor(null)
    }

    /**
     * Called when the activity is first created.
     */

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setTitle(R.string.choose_contact_title)
        val intent = intent
        ringtoneUri = intent.data!!

        binding = DataBindingUtil.setContentView<ActivityChooseContactBinding>(
            this,
            R.layout.activity_choose_contact
        ).apply {
            viewModel = ViewModel()
        }

        try {
            adapter = SimpleCursorAdapter(
                this,           // Use a template that displays a text view
                R.layout.contact_row,   // Set an empty cursor right now. Will be set in onLoadFinished()
                null,
                arrayOf(
                    ContactsContract.Contacts.CUSTOM_RINGTONE,
                    ContactsContract.Contacts.DISPLAY_NAME
                ),
                intArrayOf(
                    R.id.row_ringtone,
                    R.id.row_display_name
                ),
                0
            ).apply {
                viewBinder = SimpleCursorAdapter.ViewBinder { view, cursor, columnIndex ->
                    val name = cursor.getColumnName(columnIndex)
                    val value = cursor.getString(columnIndex)

                    when (name) {
                        ContactsContract.Contacts.CUSTOM_RINGTONE -> {
                            view.visibility = when {
                                value != null && value.isNotEmpty() -> View.VISIBLE
                                else -> View.INVISIBLE
                            }

                            true
                        }

                        else -> false
                    }
                }
            }

            listAdapter = adapter

            listView.onItemClickListener = OnItemClickListener { _, _, _, _ ->
                assignRingtoneToContact()
            }

            loaderManager.initLoader(0, null, this)
        } catch (e: SecurityException) {
            // No permission to retrieve contacts
        }

        binding.searchFilter.addTextChangedListener(this)
    }

    private fun assignRingtoneToContact() {
        val c = adapter.cursor
        var dataIndex = c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
        val contactId = c.getString(dataIndex)

        dataIndex = c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)

        val displayName = c.getString(dataIndex)
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
        val values = ContentValues()

        values.put(ContactsContract.Contacts.CUSTOM_RINGTONE, ringtoneUri.toString())
        contentResolver.update(uri, values, null, null)

        val message = resources.getText(R.string.success_contact_ringtone).toString() +
                " " +
                displayName

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }
}
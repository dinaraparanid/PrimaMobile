package com.dinaraparanid.prima.core

import java.io.Serializable

/** Contact entity for [android.provider.ContactsContract.Contacts] */

data class Contact(
    /** android id of entity */
    val id: Long,

    /** URI for the custom ringtone (or default ringtone) */
    val customRingtone: String,

    /** Name of contact */
    val displayName: String
) : Serializable, Comparable<Contact> {
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = 3253018524961782400L
    }

    /** Compares [Contact] by his [displayName] */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return displayName == (other as Contact).displayName
    }

    /** Hashes [Contact] by his [displayName] */
    override fun hashCode() = displayName.hashCode()

    /** Compares [Contact] by his [displayName] */
    override fun compareTo(other: Contact) = displayName.compareTo(other.displayName)
}
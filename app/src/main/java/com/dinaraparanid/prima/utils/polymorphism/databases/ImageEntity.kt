package com.dinaraparanid.prima.utils.polymorphism.databases

/** Ancestor for every image entity */
interface ImageEntity : Entity {
    /** Image in bytes */
    val image: ByteArray
}
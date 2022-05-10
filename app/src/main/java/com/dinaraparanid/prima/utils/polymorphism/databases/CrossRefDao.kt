package com.dinaraparanid.prima.utils.polymorphism.databases

import androidx.room.Dao
import androidx.room.Transaction

/**
 * Ancestor for all entities
 * that represent cross reference relationships
 */

interface CrossRefDao<T : CrossRefEntity>
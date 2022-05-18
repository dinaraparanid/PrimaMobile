package com.dinaraparanid.prima.utils.extensions

import android.os.ConditionVariable

/** Calls [ConditionVariable.close] so [ConditionVariable.block] will really block thread  */
internal fun ConditionVariable.openClose() {
    open(); close()
}
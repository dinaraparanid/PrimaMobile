package com.dinaraparanid.prima.utils.polymorphism

import android.os.Bundle
import androidx.databinding.ViewDataBinding
import com.dinaraparanid.prima.MainActivity

/**
 * Fragment of [MainActivity].
 * It is needed to set main label's text
 */

abstract class MainActivityFragment<B: ViewDataBinding> : AbstractFragment<B, MainActivity>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentActivity.mainLabelCurText = mainLabelCurText
    }
}
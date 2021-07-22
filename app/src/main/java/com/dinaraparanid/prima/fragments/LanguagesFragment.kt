package com.dinaraparanid.prima.fragments

import android.content.Intent
import android.os.Bundle
import android.os.LocaleList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.utils.polymorphism.Rising
import java.util.*


class LanguagesFragment : AbstractFragment(), Rising {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_languages, container, false)

        val layout: LinearLayout = view
            .findViewById<LinearLayout>(R.id.languages_big_layout)
            .findViewById<NestedScrollView>(R.id.languages_scroll)
            .findViewById(R.id.languages_layout)

        arrayOf<Button>(
            layout.findViewById(R.id.english),
            layout.findViewById(R.id.arabian),
            layout.findViewById(R.id.belarusian),
            layout.findViewById(R.id.bulgarian),
            layout.findViewById(R.id.german),
            layout.findViewById(R.id.greek),
            layout.findViewById(R.id.spanish),
            layout.findViewById(R.id.french),
            layout.findViewById(R.id.italian),
            layout.findViewById(R.id.japanese),
            layout.findViewById(R.id.korean),
            layout.findViewById(R.id.mongolian),
            layout.findViewById(R.id.norwegian),
            layout.findViewById(R.id.polish),
            layout.findViewById(R.id.portuguese),
            layout.findViewById(R.id.russian),
            layout.findViewById(R.id.swedish),
            layout.findViewById(R.id.turkish),
            layout.findViewById(R.id.ukrainian),
            layout.findViewById(R.id.chinese)
        ).forEachIndexed { ind, b ->

        }

        return view
    }

    override fun up() {

    }
}
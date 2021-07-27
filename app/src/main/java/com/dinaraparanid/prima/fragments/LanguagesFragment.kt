package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.utils.polymorphism.Rising

/**
 * Fragment for choosing languages
 */

class LanguagesFragment : AbstractFragment(), Rising {
    private lateinit var mainLayout: LinearLayout

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

        mainLayout = view
            .findViewById<LinearLayout>(R.id.languages_big_layout)
            .findViewById<NestedScrollView>(R.id.languages_scroll)
            .findViewById(R.id.languages_layout)

        arrayOf<Button>(
            mainLayout.findViewById(R.id.english),
            mainLayout.findViewById(R.id.arabian),
            mainLayout.findViewById(R.id.belarusian),
            mainLayout.findViewById(R.id.bulgarian),
            mainLayout.findViewById(R.id.german),
            mainLayout.findViewById(R.id.greek),
            mainLayout.findViewById(R.id.spanish),
            mainLayout.findViewById(R.id.french),
            mainLayout.findViewById(R.id.italian),
            mainLayout.findViewById(R.id.japanese),
            mainLayout.findViewById(R.id.korean),
            mainLayout.findViewById(R.id.mongolian),
            mainLayout.findViewById(R.id.norwegian),
            mainLayout.findViewById(R.id.polish),
            mainLayout.findViewById(R.id.portuguese),
            mainLayout.findViewById(R.id.russian),
            mainLayout.findViewById(R.id.swedish),
            mainLayout.findViewById(R.id.turkish),
            mainLayout.findViewById(R.id.ukrainian),
            mainLayout.findViewById(R.id.chinese)
        ).forEachIndexed { ind, b ->
            b.typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)

            b.setOnClickListener {
                Params.instance.changeLang(requireContext(), ind)
            }
        }

        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        return view
    }

    override fun up() {
        if (!(requireActivity() as MainActivity).upped)
            mainLayout.layoutParams = (mainLayout.layoutParams as FrameLayout.LayoutParams).apply {
                bottomMargin = 200
            }
    }
}
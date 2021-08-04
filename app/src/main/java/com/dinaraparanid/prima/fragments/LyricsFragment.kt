package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment

class LyricsFragment : AbstractFragment() {
    private lateinit var lyrics: String
    private lateinit var lyricsTextView: TextView

    internal companion object {
        private const val LYRICS_KEY = "lyrics"

        /**
         * Creates new instance of fragment with params
         * @param mainLabelOldText old main label text (to return)
         * @param mainLabelCurText main label text for current fragment
         * @param lyrics track's lyrics to show
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            mainLabelCurText: String,
            lyrics: String
        ) = LyricsFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
                putString(LYRICS_KEY, lyrics)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY)!!
        lyrics = requireArguments().getString(LYRICS_KEY)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_lyrics, container, false)

        lyricsTextView = view
            .findViewById<NestedScrollView>(R.id.lyrics_nested_scroll)
            .findViewById<ConstraintLayout>(R.id.lyrics_layout)
            .findViewById<TextView>(R.id.lyrics_text).apply {
                text = lyrics
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

        return view
    }
}
package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment

class SettingsFragment : AbstractFragment() {
    private lateinit var languageButton: Button
    private lateinit var themesButton: Button
    private lateinit var playlistsPerRowButton: Button
    private lateinit var playlistImageCirclingButton: Button
    private lateinit var saveProgressButton: Button

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
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val layout: LinearLayout = view.findViewById(R.id.settings_layout)

        languageButton = layout
            .findViewById<Button>(R.id.language_button)
            .apply { setTextColor(ViewSetter.textColor) }

        themesButton = layout
            .findViewById<Button>(R.id.themes)
            .apply { setTextColor(ViewSetter.textColor) }

        playlistsPerRowButton = layout
            .findViewById<Button>(R.id.playlists_per_row)
            .apply { setTextColor(ViewSetter.textColor) }

        playlistImageCirclingButton = layout
            .findViewById<Button>(R.id.playlist_image_circling)
            .apply { setTextColor(ViewSetter.textColor) }

        saveProgressButton = layout
            .findViewById<Button>(R.id.save_progress)
            .apply { setTextColor(ViewSetter.textColor) }

        return view
    }
}
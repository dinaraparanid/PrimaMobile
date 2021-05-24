package com.app.musicplayer.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import com.app.musicplayer.MainActivity
import com.app.musicplayer.R
import com.app.musicplayer.utils.Params

class MainMenuFragment private constructor() : Fragment() {
    private lateinit var tracksButton: Button
    private lateinit var playlistsButtons: Button
    private lateinit var artistsButton: Button
    private lateinit var favouritesButton: Button
    private lateinit var recommendationsButton: Button
    private lateinit var compilationButton: Button
    private lateinit var settingsButton: Button
    private lateinit var aboutAppButton: Button

    companion object {
        fun newInstance() = MainMenuFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main_menu, container, false)
        val menuLayoutContainer = view
            .findViewById<ConstraintLayout>(R.id.menu_layout)
            .findViewById<LinearLayout>(R.id.menu_layout_container)
            .apply { setBackgroundColor(if (Params.getInstance().theme.isNight) Color.BLACK else Color.WHITE) }

        tracksButton = menuLayoutContainer.findViewById<Button>(R.id.tracks_menu)
            .apply { setTextColor(if (!Params.getInstance().theme.isNight) Color.BLACK else Color.WHITE) }

        playlistsButtons = menuLayoutContainer.findViewById<Button>(R.id.playlists_menu)
            .apply { setTextColor(if (!Params.getInstance().theme.isNight) Color.BLACK else Color.WHITE) }

        artistsButton = menuLayoutContainer.findViewById<Button>(R.id.artists_menu)
            .apply { setTextColor(if (!Params.getInstance().theme.isNight) Color.BLACK else Color.WHITE) }

        favouritesButton = menuLayoutContainer.findViewById<Button>(R.id.favourites_menu)
            .apply { setTextColor(if (!Params.getInstance().theme.isNight) Color.BLACK else Color.WHITE) }

        recommendationsButton = menuLayoutContainer.findViewById<Button>(R.id.recommendations_menu)
            .apply { setTextColor(if (!Params.getInstance().theme.isNight) Color.BLACK else Color.WHITE) }

        compilationButton = menuLayoutContainer.findViewById<Button>(R.id.compilation_menu)
            .apply { setTextColor(if (!Params.getInstance().theme.isNight) Color.BLACK else Color.WHITE) }

        settingsButton = menuLayoutContainer.findViewById<Button>(R.id.settings_menu)
            .apply { setTextColor(if (!Params.getInstance().theme.isNight) Color.BLACK else Color.WHITE) }

        aboutAppButton = menuLayoutContainer.findViewById<Button>(R.id.about_app_menu)
            .apply { setTextColor(if (!Params.getInstance().theme.isNight) Color.BLACK else Color.WHITE) }

        return view
    }

    override fun onStop() = (requireActivity() as MainActivity).run {
        super.onStop()
        supportActionBar!!.show()
        Params.getInstance().menuPressed = false
        (fragmentContainer.layoutParams as CoordinatorLayout.LayoutParams)
            .setMargins(0, actionBarSize, 0, 0)
    }
}
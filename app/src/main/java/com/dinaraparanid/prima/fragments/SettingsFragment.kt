package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Switch
import androidx.core.widget.NestedScrollView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.utils.polymorphism.Rising

/**
 * Fragment for settings.
 * 1. Change Language
 * 2. Change Theme
 * 3. Playlists circle images (switch)
 * 4. Save playing progress (current track and playlist, time, looping)
 */

class SettingsFragment : AbstractFragment(), Rising {
    private lateinit var mainLayout: LinearLayout
    private lateinit var fontButton: Button
    private lateinit var languageButton: Button
    private lateinit var themesButton: Button
    private lateinit var playlistImageCirclingButton: Switch
    private lateinit var saveProgressButton: Switch

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

        mainLayout = view
            .findViewById<LinearLayout>(R.id.settings_big_layout)
            .findViewById<NestedScrollView>(R.id.scroll_settings)
            .findViewById(R.id.settings_layout)

        languageButton = mainLayout
            .findViewById<Button>(R.id.language_button)
            .apply {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)

                setTextColor(ViewSetter.textColor)
                setOnClickListener {
                    requireActivity().supportFragmentManager
                        .beginTransaction()
                        .setCustomAnimations(
                            R.anim.fade_in,
                            R.anim.fade_out,
                            R.anim.fade_in,
                            R.anim.fade_out
                        )
                        .replace(
                            R.id.fragment_container,
                            defaultInstance(
                                mainLabelCurText,
                                resources.getString(R.string.language),
                                LanguagesFragment::class
                            )
                        )
                        .addToBackStack(null)
                        .commit()
                }
            }

        fontButton = mainLayout
            .findViewById<Button>(R.id.font_button)
            .apply {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)

                setTextColor(ViewSetter.textColor)
                setOnClickListener {
                    requireActivity().supportFragmentManager
                        .beginTransaction()
                        .setCustomAnimations(
                            R.anim.fade_in,
                            R.anim.fade_out,
                            R.anim.fade_in,
                            R.anim.fade_out
                        )
                        .replace(
                            R.id.fragment_container,
                            defaultInstance(
                                mainLabelCurText,
                                resources.getString(R.string.font),
                                FontsFragment::class
                            )
                        )
                        .addToBackStack(null)
                        .commit()
                }
            }

        themesButton = mainLayout
            .findViewById<Button>(R.id.themes)
            .apply {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)

                setTextColor(ViewSetter.textColor)
                setOnClickListener {
                    requireActivity().supportFragmentManager
                        .beginTransaction()
                        .setCustomAnimations(
                            R.anim.fade_in,
                            R.anim.fade_out,
                            R.anim.fade_in,
                            R.anim.fade_out
                        )
                        .replace(
                            R.id.fragment_container,
                            defaultInstance(
                                mainLabelCurText,
                                resources.getString(R.string.themes),
                                ThemesFragment::class
                            )
                        )
                        .addToBackStack(null)
                        .commit()
                }
            }

        playlistImageCirclingButton = mainLayout
            .findViewById<Switch>(R.id.playlist_image_circling)
            .apply {
                isChecked = Params.instance.isRoundingPlaylistImage
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)

                setTextColor(ViewSetter.textColor)
                setOnCheckedChangeListener { _, isChecked ->
                    StorageUtil(context).storeRounded(isChecked)
                    Params.instance.isRoundingPlaylistImage = isChecked
                    (requireActivity() as MainActivity).setRoundingOfPlaylistImage()
                }
            }

        saveProgressButton = mainLayout
            .findViewById<Switch>(R.id.save_progress)
            .apply {
                isChecked = Params.instance.isSavingProgress
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)

                setTextColor(ViewSetter.textColor)
                setOnCheckedChangeListener { _, isChecked ->
                    StorageUtil(context).storeSaveProgress(isChecked)
                    Params.instance.isSavingProgress = isChecked
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
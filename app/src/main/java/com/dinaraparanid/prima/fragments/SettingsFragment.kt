package com.dinaraparanid.prima.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.NestedScrollView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.utils.polymorphism.Rising

/**
 * Fragment for settings.
 */

class SettingsFragment : AbstractFragment(), Rising {
    private lateinit var mainLayout: LinearLayout
    private lateinit var fontButton: Button
    private lateinit var languageButton: Button
    private lateinit var themesButton: Button

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var showPlaylistImages: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var playlistImageCirclingButton: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var showVisualizerButton: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var saveCurrentTrackAndPlaylist: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var saveLooping: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var saveEqualizer: Switch

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

        val appearanceLayout = mainLayout.findViewById<LinearLayout>(R.id.appearance_layout)

        appearanceLayout.findViewById<TextView>(R.id.appearance_title).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }

        val appearanceButtonsLayout =
            appearanceLayout.findViewById<LinearLayout>(R.id.appearance_buttons_layout)

        languageButton = appearanceButtonsLayout
            .findViewById<Button>(R.id.language_button)
            .apply {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)

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

        fontButton = appearanceButtonsLayout
            .findViewById<Button>(R.id.font_button)
            .apply {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)

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

        themesButton = appearanceButtonsLayout
            .findViewById<Button>(R.id.themes)
            .apply {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)

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

        showPlaylistImages = appearanceButtonsLayout
            .findViewById<Switch>(R.id.show_playlist_images)
            .apply {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

        playlistImageCirclingButton = appearanceButtonsLayout
            .findViewById<Switch>(R.id.playlist_image_circling)
            .apply {
                isChecked = Params.instance.isRoundingPlaylistImage
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)

                setOnCheckedChangeListener { _, isChecked ->
                    StorageUtil(context).storeRounded(isChecked)
                    Params.instance.isRoundingPlaylistImage = isChecked
                    (requireActivity() as MainActivity).setRoundingOfPlaylistImage()
                }
            }

        showVisualizerButton = appearanceButtonsLayout
            .findViewById<Switch>(R.id.show_visualizer)
            .apply {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

        val progressLayout = mainLayout.findViewById<LinearLayout>(R.id.progress_layout)

        progressLayout.findViewById<TextView>(R.id.progress_title).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }

        val progressButtonsLayout =
            progressLayout.findViewById<LinearLayout>(R.id.progress_buttons_layout)

        saveCurrentTrackAndPlaylist = progressButtonsLayout
            .findViewById<Switch>(R.id.progress_cur_track_playlist)
            .apply {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

        saveLooping = progressButtonsLayout
            .findViewById<Switch>(R.id.progress_looping)
            .apply {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

        saveEqualizer = progressButtonsLayout
            .findViewById<Switch>(R.id.progress_equalizer)
            .apply {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

        /*saveProgressButton = mainLayout
            .findViewById<Switch>(R.id.save_progress)
            .apply {
                isChecked = Params.instance.isSavingProgress
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)

                setOnCheckedChangeListener { _, isChecked ->
                    StorageUtil(context).storeSaveProgress(isChecked)
                    Params.instance.isSavingProgress = isChecked
                }
            }*/

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
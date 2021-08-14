package com.dinaraparanid.prima.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.core.widget.NestedScrollView
import carbon.widget.Button
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.utils.polymorphism.Rising
import top.defaults.colorpicker.ColorPickerPopup
import top.defaults.colorpicker.ColorPickerPopup.ColorPickerObserver


/**
 * Fragment for customizing themes
 */

class ThemesFragment : AbstractFragment(), Rising {
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
        val view = inflater.inflate(R.layout.fragment_themes, container, false)

        mainLayout = view
            .findViewById<LinearLayout>(R.id.themes_big_layout)
            .findViewById<NestedScrollView>(R.id.themes_scroll)
            .findViewById(R.id.themes_layout)

        mainLayout.findViewById<Button>(R.id.custom_theme).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)

            setOnClickListener {
                ColorPickerPopup.Builder(requireContext())
                    .initialColor(Params.instance.theme.rgb)
                    .enableBrightness(true)
                    .enableAlpha(true)
                    .okTitle("Choose")
                    .cancelTitle("Cancel")
                    .showIndicator(true)
                    .showValue(true)
                    .build()
                    .show(object : ColorPickerObserver() {
                        override fun onColorPicked(color: Int) {
                            PopupMenu(requireContext(), mainLayout).run {
                                menuInflater.inflate(R.menu.fragment_night_or_day, menu)
                                setOnMenuItemClickListener {
                                    StorageUtil(requireContext())
                                        .storeCustomThemeColors(
                                            color to if (it.itemId == R.id.night_theme) 0 else 1
                                        )

                                    startActivity(
                                        Intent(
                                            requireContext(),
                                            MainActivity::class.java
                                        )
                                    )

                                    false
                                }
                            }
                        }

                        fun onColor(color: Int, fromUser: Boolean) = Unit
                    })
            }
        }

        arrayOf<Pair<Button, Int>>(
            mainLayout.findViewById<Button>(R.id.purple) to 0,
            mainLayout.findViewById<Button>(R.id.purple_night) to 1,
            mainLayout.findViewById<Button>(R.id.red) to 2,
            mainLayout.findViewById<Button>(R.id.red_night) to 3,
            mainLayout.findViewById<Button>(R.id.blue) to 4,
            mainLayout.findViewById<Button>(R.id.blue_night) to 5,
            mainLayout.findViewById<Button>(R.id.green) to 6,
            mainLayout.findViewById<Button>(R.id.green_night) to 7,
            mainLayout.findViewById<Button>(R.id.orange) to 8,
            mainLayout.findViewById<Button>(R.id.orange_night) to 9,
            mainLayout.findViewById<Button>(R.id.lemon) to 10,
            mainLayout.findViewById<Button>(R.id.lemon_night) to 11,
            mainLayout.findViewById<Button>(R.id.turquoise) to 12,
            mainLayout.findViewById<Button>(R.id.turquoise_night) to 13,
            mainLayout.findViewById<Button>(R.id.green_turquoise) to 14,
            mainLayout.findViewById<Button>(R.id.green_turquoise_night) to 15,
            mainLayout.findViewById<Button>(R.id.sea) to 16,
            mainLayout.findViewById<Button>(R.id.sea_night) to 17,
            mainLayout.findViewById<Button>(R.id.pink) to 18,
            mainLayout.findViewById<Button>(R.id.pink_night) to 19
        ).forEach { (b, t) ->
            b.typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)

            b.setOnClickListener {
                Params.instance.changeTheme(requireContext(), t)
            }

            StorageUtil(requireContext()).clearCustomThemeColors()
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
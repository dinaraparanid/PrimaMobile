package com.dinaraparanid.prima.fragments

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.decorations.DividerItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.ListFragment
import com.dinaraparanid.prima.viewmodels.FontsViewModel
import kotlinx.coroutines.Deferred

class FontsFragment : ListFragment<String, FontsFragment.FontsAdapter.FontsHolder>() {
    interface Callbacks : ListFragment.Callbacks {
        /**
         * Changes font of app
         * @param font font itself
         */

        fun onFontSelected(font: String)
    }

    private companion object {
        private val FONT_NAMES = listOf(
            "Abeezee",
            "Abel",
            "Abril Fatface",
            "Aclonica",
            "Adamina",
            "Advent Pro",
            "Aguafina Script",
            "Akronim",
            "Aladin",
            "Aldrich",
            "Alegreya Sc",
            "Alex Brush",
            "Alfa Slab One",
            "Allan",
            "Allerta",
            "Almendra",
            "Almendra Sc",
            "Amarante",
            "Amiko",
            "Amita",
            "Anarchy",
            "Annie Use Your Telescope",
            "Anton",
            "Architects Daughter",
            "Archivo Black",
            "Arima Madurai Medium",
            "Arizonia",
            "Artifika",
            "Atma",
            "Atomic Age",
            "Audiowide",
            "Bastong",
            "Berkshire Swash",
            "Bilbo Swash Caps",
            "Black Ops One",
            "Bonbon",
            "Boogaloo",
            "Caesar Dressing",
            "Calligraffitti",
            "Carter One",
            "Caveat Bold",
            "Changa One",
            "Cherry Cream Soda",
            "Cherry Swash",
            "Chewy",
            "Cinzel Decorative",
            "Coming Soon",
            "Condiment",
            "Dancing Script Bold",
            "Delius Unicase",
            "Droid Sans Mono",
            "Droid Serif",
            "Faster One",
            "Fira Sans Thin",
            "Gruppo",
            "Jim Nightshade",
            "Mako",
            "Mclaren",
            "Megrim",
            "Metal Mania",
            "Modern Antiqua",
            "Monospace",
            "Mountains Of Christmas",
            "Nova Flat",
            "Orbitron",
            "Oxygen",
            "Paprika",
            "Permanent Marker",
            "Press Start 2p",
            "Pristina",
            "Pt Sans",
            "Puritan",
            "Rusthack",
            "Sans Serif",
            "Serif",
            "Shadows Into Light Two",
            "Sniglet",
            "Special Elite",
            "Thejulayna",
            "Trade Winds",
            "Tropical Summer Signature",
            "Ubuntu"
        )
    }

    private val fonts = listOf(
        R.font.abeezee,
        R.font.abel,
        R.font.abril_fatface,
        R.font.aclonica,
        R.font.adamina,
        R.font.advent_pro,
        R.font.aguafina_script,
        R.font.akronim,
        R.font.aladin,
        R.font.aldrich,
        R.font.alegreya_sc,
        R.font.alex_brush,
        R.font.alfa_slab_one,
        R.font.allan,
        R.font.allerta,
        R.font.almendra,
        R.font.almendra_sc,
        R.font.amarante,
        R.font.amiko,
        R.font.amita,
        R.font.anarchy,
        R.font.andika,
        R.font.android,
        R.font.android_hollow,
        R.font.android_italic,
        R.font.android_scratch,
        R.font.annie_use_your_telescope,
        R.font.anton,
        R.font.architects_daughter,
        R.font.archivo_black,
        R.font.arima_madurai_medium,
        R.font.arizonia,
        R.font.artifika,
        R.font.atma,
        R.font.atomic_age,
        R.font.audiowide,
        R.font.bad_script,
        R.font.bangers,
        R.font.bastong,
        R.font.berkshire_swash,
        R.font.bilbo_swash_caps,
        R.font.black_ops_one,
        R.font.bonbon,
        R.font.boogaloo,
        R.font.bracknell_f,
        R.font.bungee_inline,
        R.font.bungee_shade,
        R.font.caesar_dressing,
        R.font.calligraffitti,
        R.font.carter_one,
        R.font.caveat_bold,
        R.font.cedarville_cursive,
        R.font.changa_one,
        R.font.cherry_cream_soda,
        R.font.cherry_swash,
        R.font.chewy,
        R.font.cinzel_decorative,
        R.font.coming_soon,
        R.font.condiment,
        R.font.dancing_script_bold,
        R.font.delius_unicase,
        R.font.droid_sans_mono,
        R.font.droid_serif,
        R.font.extendo_italic,
        R.font.faster_one,
        R.font.fira_sans_thin,
        R.font.gruppo,
        R.font.homemade_apple,
        R.font.jim_nightshade,
        R.font.magretta,
        R.font.mako,
        R.font.mclaren,
        R.font.megrim,
        R.font.metal_mania,
        R.font.modern_antiqua,
        Typeface.MONOSPACE,
        R.font.morning_vintage,
        R.font.mountains_of_christmas,
        R.font.naylime,
        R.font.nova_flat,
        R.font.orbitron,
        R.font.oxygen,
        R.font.pacifico,
        R.font.paprika,
        R.font.permanent_marker,
        R.font.press_start_2p,
        R.font.pristina,
        R.font.pt_sans,
        R.font.puritan,
        R.font.rock_salt,
        R.font.rusthack,
        Typeface.SANS_SERIF,
        Typeface.SERIF,
        R.font.shadows_into_light_two,
        R.font.sniglet,
        R.font.special_elite,
        R.font.thejulayna,
        R.font.trade_winds,
        R.font.tropical_summer_signature,
        R.font.ubuntu
    )

    override var adapter: RecyclerView.Adapter<FontsAdapter.FontsHolder>? = FontsAdapter(FONT_NAMES)

    override val viewModel: ViewModel by lazy {
        ViewModelProvider(this)[FontsViewModel::class.java]
    }

    /** Should not be used */

    @Deprecated("There are always some fonts")
    override lateinit var emptyTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        itemList.addAll(FONT_NAMES)
        itemListSearch.addAll(FONT_NAMES)

        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fonts, container, false)

        recyclerView = view
            .findViewById<ConstraintLayout>(R.id.fonts_constraint_layout)
            .findViewById<RecyclerView>(R.id.fonts_recycler_view)
            .apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@FontsFragment.adapter?.apply {
                    stateRestorationPolicy =
                        RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                }
                addItemDecoration(VerticalSpaceItemDecoration(30))
                addItemDecoration(DividerItemDecoration(requireActivity()))
            }

        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        (requireActivity() as MainActivity).mainLabel.text = mainLabelCurText
        return view
    }

    override fun updateUI(src: List<String>): Unit = throw Exception("Should not be used")

    override fun filter(models: Collection<String>?, query: String): List<String> =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.lowercase() } ?: listOf()
        }

    override suspend fun loadAsync(): Deferred<Unit> = throw Exception("Should not be used")

    /**
     * [RecyclerView.Adapter] for [FontsFragment]
     */

    inner class FontsAdapter(private val fonts: List<String>) :
        RecyclerView.Adapter<FontsAdapter.FontsHolder>() {
        /**
         * [RecyclerView.ViewHolder] for artists of [FontsAdapter]
         */

        inner class FontsHolder(view: View) :
            RecyclerView.ViewHolder(view),
            View.OnClickListener {
            private lateinit var font: String

            private val fontTitleTextView = itemView
                .findViewById<TextView>(R.id.font_title)
                .apply { setTextColor(ViewSetter.textColor) }

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                (callbacks as Callbacks?)?.onFontSelected(font)
            }

            /**
             * Constructs GUI for artist item
             * @param _font artist to bind
             */

            fun bind(_font: String) {
                font = _font

                fontTitleTextView.run {
                    text = font
                    typeface = (requireActivity().application as MainApplication)
                        .getFontFromName(font)

                    setTextColor(
                        when (font) {
                            Params.instance.font -> Params.instance.theme.rgb
                            else -> ViewSetter.textColor
                        }
                    )
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontsHolder =
            FontsHolder(layoutInflater.inflate(R.layout.list_item_font, parent, false))

        override fun getItemCount(): Int = fonts.size

        override fun onBindViewHolder(holder: FontsHolder, position: Int): Unit =
            holder.bind(fonts[position])
    }
}
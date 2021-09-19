package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentFontsBinding
import com.dinaraparanid.prima.databinding.ListItemFontBinding
import com.dinaraparanid.prima.utils.decorations.DividerItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.CallbacksFragment
import com.dinaraparanid.prima.utils.polymorphism.ListFragment
import com.dinaraparanid.prima.viewmodels.androidx.FontsViewModel

class FontsFragment : ListFragment<String,
        FontsFragment.FontsAdapter,
        FontsFragment.FontsAdapter.FontsHolder,
        FragmentFontsBinding>() {
    interface Callbacks : CallbacksFragment.Callbacks {
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

    override var adapter: FontsAdapter? = FontsAdapter(FONT_NAMES)

    override val viewModel: ViewModel by lazy {
        ViewModelProvider(this)[FontsViewModel::class.java]
    }

    /** Should not be used */

    @Deprecated("There are always some fonts")
    override lateinit var emptyTextView: TextView

    override var binding: FragmentFontsBinding? = null
    private lateinit var mvvmViewModel: com.dinaraparanid.prima.viewmodels.mvvm.ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate<FragmentFontsBinding>(inflater, R.layout.fragment_fonts, container, false)
            .apply {
                viewModel = com.dinaraparanid.prima.viewmodels.mvvm.ViewModel()
                mvvmViewModel = viewModel!!

                recyclerView = fontsRecyclerView.apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = this@FontsFragment.adapter?.apply {
                        stateRestorationPolicy =
                            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                    }
                    addItemDecoration(VerticalSpaceItemDecoration(30))
                    addItemDecoration(DividerItemDecoration(requireActivity()))
                }
            }

        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        (requireActivity() as MainActivity).binding!!.mainLabel.text = mainLabelCurText
        return binding!!.root
    }

    /**
     * [RecyclerView.Adapter] for [FontsFragment]
     */

    inner class FontsAdapter(private val fonts: List<String>) :
        RecyclerView.Adapter<FontsAdapter.FontsHolder>() {
        /**
         * [RecyclerView.ViewHolder] for artists of [FontsAdapter]
         */

        inner class FontsHolder(private val fontBinding: ListItemFontBinding) :
            RecyclerView.ViewHolder(fontBinding.root),
            View.OnClickListener {
            private lateinit var font: String

            init {
                fontBinding.viewModel = mvvmViewModel
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                (callbacker as Callbacks?)?.onFontSelected(font)
            }

            /**
             * Constructs GUI for artist item
             * @param _font artist to bind
             */

            fun bind(_font: String) {
                fontBinding.fontStr = _font
                fontBinding.executePendingBindings()
                font = _font
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontsHolder =
            FontsHolder(
                ListItemFontBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun getItemCount(): Int = fonts.size

        override fun onBindViewHolder(holder: FontsHolder, position: Int): Unit =
            holder.bind(fonts[position])
    }
}
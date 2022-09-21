package com.dinaraparanid.prima.fragments.main_menu.settings

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentFontsBinding
import com.dinaraparanid.prima.databinding.ListItemFontBinding
import com.dinaraparanid.prima.utils.AsyncCondVar
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.decorations.DividerItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.utils.polymorphism.fragments.*
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.utils.polymorphism.runOnWorkerThread
import com.dinaraparanid.prima.viewmodels.androidx.DefaultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FontsFragment : ListFragment<MainActivity,
        String,
        FontsFragment.FontsAdapter,
        FontsFragment.FontsAdapter.FontsHolder,
        FragmentFontsBinding>(),
    MainActivityFragment,
    MenuProviderFragment,
    Rising {
    interface Callbacks : CallbacksFragment.Callbacks {
        /**
         * Changes font of app
         * @param font font itself
         */

        fun onFontSelected(font: String)
    }

    override var isMainLabelInitialized = false
    override val awaitMainLabelInitCondition = AsyncCondVar()
    override lateinit var mainLabelCurText: String
    override val menuProvider = defaultMenuProvider

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
            "Anders",
            "Annie Use Your Telescope",
            "Anton",
            "Architects Daughter",
            "Archivo Black",
            "Arial",
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
            "Calibri",
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
            "Times New Roman",
            "Trade Winds",
            "Tropical Summer Signature",
            "Ubuntu",
        )
    }

    override var _adapter: FontsAdapter? = FontsAdapter(FONT_NAMES)
    override fun initAdapter() = Unit

    override val viewModel by lazy {
        ViewModelProvider(this)[DefaultViewModel::class.java]
    }

    /** Should not be used */

    @Deprecated("There are always some fonts")
    override var emptyTextView: TextView? = null

    override var binding: FragmentFontsBinding? = null
    private lateinit var mvvmViewModel: com.dinaraparanid.prima.viewmodels.mvvm.ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelCurText = requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY)!!
        runOnUIThread { setMainLabelInitializedAsync() }
        super.onCreate(savedInstanceState)
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
                    adapter = this@FontsFragment.adapter.apply {
                        stateRestorationPolicy =
                            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                    }
                    addItemDecoration(VerticalSpaceItemDecoration(30))
                    addItemDecoration(DividerItemDecoration(requireActivity()))
                }
            }

        if (application.playingBarIsVisible) up()
        return binding!!.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        fragmentActivity.run {
            runOnWorkerThread {
                while (!isMainLabelInitialized)
                    awaitMainLabelInitCondition.blockAsync()

                launch(Dispatchers.Main) {
                    mainLabelCurText = this@FontsFragment.mainLabelCurText
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(menuProvider)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().removeMenuProvider(menuProvider)
    }

    /**
     * Rise fragment if playing bar is active.
     * It handles GUI error when playing bar was hiding some content
     */

    override fun up() {
        if (!fragmentActivity.isUpped)
            recyclerView!!.layoutParams =
                (recyclerView!!.layoutParams as ConstraintLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT
                }
    }

    /** [RecyclerView.Adapter] for [FontsFragment] */

    inner class FontsAdapter(private val fonts: List<String>) :
        RecyclerView.Adapter<FontsAdapter.FontsHolder>() {
        /** [RecyclerView.ViewHolder] for artists of [FontsAdapter] */

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

            internal fun bind(_font: String) {
                fontBinding.fontStr = _font
                fontBinding.executePendingBindings()
                font = _font
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FontsHolder(
            ListItemFontBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun getItemCount() = fonts.size

        override fun onBindViewHolder(holder: FontsHolder, position: Int) =
            holder.bind(fonts[position])
    }
}
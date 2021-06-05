package com.dinaraparanid.prima.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.utils.ArtistList
import com.dinaraparanid.prima.utils.Colors
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.viewmodels.ArtistsViewModel
import de.hdodenhof.circleimageview.CircleImageView

class ArtistListFragment : Fragment(), SearchView.OnQueryTextListener {
    interface Callbacks {
        fun onArtistSelected(artist: Artist)
    }

    private lateinit var artistRecyclerView: RecyclerView
    private var adapter: ArtistAdapter? = ArtistAdapter(mutableListOf())
    private var callbacks: Callbacks? = null
    private val artistViewModel: ArtistsViewModel by lazy {
        ViewModelProvider(this)[ArtistsViewModel::class.java]
    }

    companion object {
        @JvmStatic
        fun newInstance(): ArtistListFragment = ArtistListFragment()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        artistViewModel.load(
            arguments?.getSerializable("artists") as ArtistList?,
            arguments?.getString("main_label_old_text")
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_artists, container, false)

        artistRecyclerView = view.findViewById(R.id.artists_recycler_view)
        artistRecyclerView.layoutManager = LinearLayoutManager(context)
        artistRecyclerView.adapter = adapter
        artistRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(30))

        (requireActivity() as MainActivity).mainLabel.setText(R.string.artists)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI(artistViewModel.artists.value!!.data)
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onStop() {
        super.onStop()
        (requireActivity() as MainActivity).mainLabel.text =
            artistViewModel.mainLabelOldText.value!!
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(
            "main_label_old_text",
            (requireActivity() as MainActivity).mainLabel.text.toString()
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun onQueryTextChange(query: String?): Boolean {
        val filteredModelList = filter(
            artistViewModel.artists.value!!.data,
            query ?: ""
        )

        adapter?.replaceAll(filteredModelList)
        artistRecyclerView.scrollToPosition(0)
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    private fun updateUI(artists: List<Artist>) {
        adapter = ArtistAdapter(artists.toMutableList())
        artistRecyclerView.adapter = adapter
    }

    private fun filter(models: Collection<Artist>?, query: String) =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.name.lowercase() } ?: listOf()
        }

    private inner class ArtistHolder(view: View) :
        RecyclerView.ViewHolder(view),
        View.OnClickListener {
        private lateinit var artist: Artist

        private val artistNameTextView =
            itemView.findViewById<TextView>(R.id.artist_name).apply {
                setTextColor(if (Params.getInstance().theme.isNight) Color.WHITE else Color.BLACK)
            }

        private val artistImage = itemView.findViewById<CircleImageView>(R.id.artist_image)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(_artist: Artist) {
            artist = _artist
            artistNameTextView.text = artist.name

            artistImage.setImageResource(
                when (Params.getInstance().theme) {
                    is Colors.Blue -> R.drawable.human_blue
                    is Colors.BlueNight -> R.drawable.human_blue
                    is Colors.Green -> R.drawable.human_green
                    is Colors.GreenNight -> R.drawable.human_green
                    is Colors.GreenTurquoise -> R.drawable.human_green_turquoise
                    is Colors.GreenTurquoiseNight -> R.drawable.human_green_turquoise
                    is Colors.Lemon -> R.drawable.human_lemon
                    is Colors.LemonNight -> R.drawable.human_lemon
                    is Colors.Orange -> R.drawable.human_orange
                    is Colors.OrangeNight -> R.drawable.human_orange
                    is Colors.Pink -> R.drawable.human_pink
                    is Colors.PinkNight -> R.drawable.human_pink
                    is Colors.Purple -> R.drawable.human_purple
                    is Colors.PurpleNight -> R.drawable.human_purple
                    is Colors.Red -> R.drawable.human_red
                    is Colors.RedNight -> R.drawable.human_red
                    is Colors.Sea -> R.drawable.human_sea
                    is Colors.SeaNight -> R.drawable.human_sea
                    is Colors.Turquoise -> R.drawable.human_turquoise
                    is Colors.TurquoiseNight -> R.drawable.human_turquoise
                    else -> R.drawable.human
                }
            )
        }

        override fun onClick(v: View?) {
            callbacks?.onArtistSelected(artist)
        }
    }

    private inner class ArtistAdapter(private val artists: List<Artist>) :
        RecyclerView.Adapter<ArtistListFragment.ArtistHolder>() {
        val artistList = SortedList(
            Artist::class.java,
            object : SortedList.Callback<Artist>() {
                override fun compare(o1: Artist, o2: Artist) = o1.name.compareTo(o2.name)

                override fun onInserted(position: Int, count: Int) =
                    notifyItemRangeInserted(position, count)

                override fun onRemoved(position: Int, count: Int) =
                    notifyItemRangeRemoved(position, count)

                override fun onMoved(fromPosition: Int, toPosition: Int) =
                    notifyItemMoved(fromPosition, toPosition)

                override fun onChanged(position: Int, count: Int) =
                    notifyItemRangeChanged(position, count)

                override fun areContentsTheSame(oldItem: Artist, newItem: Artist) =
                    oldItem == newItem

                override fun areItemsTheSame(item1: Artist, item2: Artist) =
                    item1.id == item2.id
            }
        ).apply { addAll(artists) }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ArtistHolder(layoutInflater.inflate(R.layout.list_item_artist, parent, false))

        override fun getItemCount() = artistList.size()

        override fun onBindViewHolder(holder: ArtistHolder, position: Int) =
            holder.bind(artistList[position])

        fun replaceAll(models: Collection<Artist>) = artistList.run {
            beginBatchedUpdates()
            (size() - 1 downTo 0).forEach {
                get(it).let { artist -> if (artist !in models) remove(artist) }
            }

            addAll(models)
            endBatchedUpdates()
        }
    }
}
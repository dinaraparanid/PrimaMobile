package com.dinaraparanid.prima.fragments

import android.content.ContentValues
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.entities.FavouriteTrack
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.viewmodels.TrackChangeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TrackChangeFragment : AbstractFragment() {
    private lateinit var track: Track
    private lateinit var titleInput: EditText
    private lateinit var artistInput: EditText
    private lateinit var albumInput: EditText

    private val viewModel: TrackChangeViewModel by lazy {
        ViewModelProvider(this)[TrackChangeViewModel::class.java]
    }

    companion object {
        private const val TRACK_KEY = "track"

        @JvmStatic
        fun newInstance(
            track: Track,
            mainLabelOldText: String,
            mainLabelCurText: String
        ): TrackChangeFragment = TrackChangeFragment().apply {
            arguments = Bundle().apply {
                putSerializable(TRACK_KEY, track)
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        track = requireArguments().getSerializable(TRACK_KEY) as Track
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_change_track_info, container, false)
        val tableLayout: TableLayout = view.findViewById(R.id.track_change_table_layout)

        val titleRow = tableLayout.findViewById<TableRow>(R.id.title_change_row).apply {
            findViewById<TextView>(R.id.track_title_change).setTextColor(ViewSetter.textColor)
        }

        titleInput = titleRow.findViewById<EditText>(R.id.track_title_change_input).apply {
            setTextColor(ViewSetter.textColor)
            setHintTextColor(Color.GRAY)
            setText(track.title, TextView.BufferType.EDITABLE)
        }

        val artistRow = tableLayout.findViewById<TableRow>(R.id.artist_change_row).apply {
            findViewById<TextView>(R.id.track_artist_change).setTextColor(ViewSetter.textColor)
        }

        artistInput = artistRow.findViewById<EditText>(R.id.track_artist_change_input).apply {
            setTextColor(ViewSetter.textColor)
            setHintTextColor(Color.GRAY)
            setText(track.artist, TextView.BufferType.EDITABLE)
        }

        val albumRow = tableLayout.findViewById<TableRow>(R.id.album_change_row).apply {
            findViewById<TextView>(R.id.track_album_change).setTextColor(ViewSetter.textColor)
        }

        albumInput = albumRow.findViewById<EditText>(R.id.track_album_change_input).apply {
            setTextColor(ViewSetter.textColor)
            setHintTextColor(Color.GRAY)
            setText(track.playlist, TextView.BufferType.EDITABLE)
        }

        (requireActivity() as MainActivity).mainLabel.text = mainLabelCurText
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_accept, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.accept) {
            viewModel.viewModelScope.launch {
                launch(Dispatchers.IO) {
                    requireActivity().contentResolver.update(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        ContentValues().apply {
                            put(MediaStore.Audio.Media.TITLE, titleInput.text.toString())
                            put(MediaStore.Audio.Media.ARTIST, artistInput.text.toString())
                            put(MediaStore.Audio.Media.ALBUM, albumInput.text.toString())
                        },
                        "${MediaStore.Audio.Media.DATA} = ?",
                        arrayOf(track.path)
                    )
                }

                launch(Dispatchers.IO) {
                    CustomPlaylistsRepository.instance
                        .getTrackAsync(track.path).await()
                        ?.let { (id, _, _, _, playlistId, path, duration) ->
                            CustomPlaylistsRepository.instance.updateTrack(
                                CustomPlaylistTrack(
                                    id,
                                    titleInput.text.toString(),
                                    artistInput.text.toString(),
                                    albumInput.text.toString(),
                                    playlistId,
                                    path,
                                    duration
                                )
                            )
                        }
                }

                launch(Dispatchers.IO) {
                    FavouriteRepository.instance.updateTrack(FavouriteTrack(track))
                }
            }

            requireActivity().supportFragmentManager.popBackStack()
        }

        return super.onOptionsItemSelected(item)
    }
}
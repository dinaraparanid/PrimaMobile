package com.dinaraparanid.prima.fragments

import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.entities.FavouriteTrack
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.viewmodels.TrackChangeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fragment to change track's metadata.
 * @since Android 11 it only changes entities in app,
 * but not metadata of track itself
 */

class TrackChangeFragment : AbstractFragment() {
    private lateinit var track: Track
    private lateinit var titleInput: EditText
    private lateinit var artistInput: EditText
    private lateinit var albumInput: EditText

    private val viewModel: TrackChangeViewModel by lazy {
        ViewModelProvider(this)[TrackChangeViewModel::class.java]
    }

    internal companion object {
        private const val TRACK_KEY = "track"

        /**
         * Creates new instance of fragment with params
         * @param mainLabelOldText old main label text (to return)
         * @param mainLabelCurText main label text for current fragment
         * @param track track to change
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            mainLabelCurText: String,
            track: Track
        ) = TrackChangeFragment().apply {
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
            findViewById<TextView>(R.id.track_title_change).run {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }
        }

        titleInput = titleRow.findViewById<EditText>(R.id.track_title_change_input).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)

            setHintTextColor(Color.GRAY)
            setText(track.title, TextView.BufferType.EDITABLE)
        }

        val artistRow = tableLayout.findViewById<TableRow>(R.id.artist_change_row).apply {
            findViewById<TextView>(R.id.track_artist_change).run {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }
        }

        artistInput = artistRow.findViewById<EditText>(R.id.track_artist_change_input).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)

            setHintTextColor(Color.GRAY)
            setText(track.artist, TextView.BufferType.EDITABLE)
        }

        val albumRow = tableLayout.findViewById<TableRow>(R.id.album_change_row).apply {
            findViewById<TextView>(R.id.track_album_change).run {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }
        }

        albumInput = albumRow.findViewById<EditText>(R.id.track_album_change_input).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)

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
            val act = requireActivity() as MainActivity
            act.needToUpdate = true

            viewModel.viewModelScope.launch {
                launch(Dispatchers.IO) {
                    val track = Track(
                        track.androidId,
                        titleInput.text.toString(),
                        artistInput.text.toString(),
                        albumInput.text.toString(),
                        track.path,
                        track.duration,
                        track.relativePath,
                        track.displayName,
                        track.addDate
                    )

                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                            (act.application as MainApplication)
                                .changedTracks[track.path] = track
                        }

                        else -> {
                            val content = ContentValues().apply {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                                    put(MediaStore.Audio.Media.IS_PENDING, 0)

                                put(MediaStore.Audio.Media.TITLE, titleInput.text.toString())
                                put(MediaStore.Audio.Media.ARTIST, artistInput.text.toString())
                                put(MediaStore.Audio.Media.ALBUM, albumInput.text.toString())
                            }

                            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                                val uri = ContentUris.withAppendedId(
                                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    track.androidId
                                )

                                act.contentResolver.update(
                                    uri, ContentValues().apply {
                                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                                    }, null, null
                                )

                                act.contentResolver.update(
                                    ContentUris.withAppendedId(
                                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                        track.androidId
                                    ),
                                    content,
                                    "${MediaStore.Audio.Media.RELATIVE_PATH} = ?" +
                                            " AND ${MediaStore.Audio.Media.DISPLAY_NAME} = ?",
                                    arrayOf(track.relativePath, track.displayName)
                                )
                            } else act.contentResolver.update(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                content,
                                "${MediaStore.Audio.Media.DATA} = ?",
                                arrayOf(track.path)
                            )
                        }
                    }

                    (act.application as MainApplication).curPlaylist.replace(
                        this@TrackChangeFragment.track,
                        track
                    )

                    launch(Dispatchers.Main) {
                        if ((act.application as MainApplication).curPath == track.path)
                            act.updateUI(track to false)
                    }
                }

                launch(Dispatchers.IO) {
                    CustomPlaylistsRepository.instance
                        .getTrackAsync(track.path).await()
                        ?.let { (androidId,
                                    id,
                                    _, _, _,
                                    playlistId,
                                    path,
                                    duration,
                                    relativePath,
                                    displayName,
                                    addDate) ->
                            CustomPlaylistsRepository.instance.updateTrack(
                                CustomPlaylistTrack(
                                    androidId,
                                    id,
                                    titleInput.text.toString(),
                                    artistInput.text.toString(),
                                    albumInput.text.toString(),
                                    playlistId,
                                    path,
                                    duration,
                                    relativePath,
                                    displayName,
                                    addDate
                                )
                            )
                        }
                }

                launch(Dispatchers.IO) {
                    FavouriteRepository.instance.updateTrack(FavouriteTrack(track))
                }
            }

            act.supportFragmentManager.popBackStack()
        }

        return super.onOptionsItemSelected(item)
    }
}
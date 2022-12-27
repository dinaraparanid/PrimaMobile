package com.dinaraparanid.prima.mvvmp.old_shit

import android.content.Intent
import android.provider.MediaStore
import androidx.databinding.ViewDataBinding
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.favourites.FavouritePlaylist
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.fragments.AbstractTrackListFragment
import com.dinaraparanid.prima.mvvmp.view.fragments.ChangeImageFragment
import com.dinaraparanid.prima.utils.polymorphism.fragments.TrackCollectionTrackListFragment

open class PlaylistTrackListViewModel<B, F>(
    private val playlistTitle: String,
    private val playlistType: Int,
    fragment: F,
) : TrackListViewModel<AbstractTrackListFragment<B>.TrackAdapter,
        AbstractTrackListFragment<B>.TrackAdapter.TrackHolder, B, F>(fragment)
        where B : ViewDataBinding,
              F : TrackCollectionTrackListFragment<B> {

    /** Sends intent to pick image from gallery*/

    @JvmName("onPlaylistTrackImageButtonPressed")
    internal fun onPlaylistTrackImageButtonClicked() =
        fragment.unchecked.requireActivity().startActivityForResult(
            Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            ), ChangeImageFragment.PICK_IMAGE
        )

    internal fun isPlaylistLikedAsync() = fragment.unchecked.getFromIOThreadAsync {
        FavouriteRepository
            .getInstanceSynchronized()
            .getPlaylistAsync(playlistTitle, playlistType)
            .await() != null
    }

    private fun setLikeButtonImage(isLiked: Boolean) = fragment.unchecked.runOnUIThread {
        fragment.unchecked.addPlaylistToFavouritesButton.setImageResource(
            when {
                isLiked -> R.drawable.heart_like_white
                else -> R.drawable.heart_white
            }
        )
    }

    @JvmName("onAddPlaylistToFavouritesButtonClicked")
    internal fun onAddPlaylistToFavouritesButtonClicked() = fragment.unchecked.runOnIOThread {
        FavouriteRepository
            .getInstanceSynchronized()
            .getPlaylistAsync(playlistTitle, playlistType)
            .await()
            ?.let {
                setLikeButtonImage(isLiked = false)
                FavouriteRepository
                    .getInstanceSynchronized()
                    .removePlaylistsAsync(it)
            } ?: kotlin.run {
            setLikeButtonImage(isLiked = true)
            FavouriteRepository
                .getInstanceSynchronized()
                .addPlaylistsAsync(
                    FavouritePlaylist.Entity(
                        id = 0,
                        title = playlistTitle,
                        type = playlistType
                    )
                )
        }
    }
}
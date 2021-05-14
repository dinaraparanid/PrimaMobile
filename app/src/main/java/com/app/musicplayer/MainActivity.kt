package com.app.musicplayer

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import java.util.UUID

class MainActivity :
    AppCompatActivity(),
    TrackListFragment.Callbacks,
    TrackDetailFragment.Callbacks
{
    private var player: MediaPlayer? = MediaPlayer()
    private val trackDetailedViewModel: TrackDetailedViewModel by lazy {
        ViewModelProvider(this)[TrackDetailedViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null)
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, TrackListFragment.newInstance())
                .commit()
    }

    override fun onTrackSelected(trackId: UUID, isPlaying: Boolean) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, TrackDetailFragment.newInstance(trackId, isPlaying))
            .addToBackStack(null)
            .commit()
    }

    override fun onReturnSelected() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, TrackListFragment.newInstance())
            .addToBackStack(null)
            .commit()
    }

    /* TODO: Not yet ready for usage

    private fun playTrack(rootToTrack: String) {
        if (player != null && player!!.isPlaying) {
            clearMediaPlayer()
            trackBar.progress = 0
            isPlaying = true
            playButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pause
                )
            )
        }

        if (!isPlaying) {
            playButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.play
                )
            )

            val descriptor = assets.openFd(rootToTrack)

            player!!.setDataSource(
                descriptor.fileDescriptor,
                descriptor.startOffset,
                descriptor.length
            )

            descriptor.close()

            player!!.apply {
                setVolume(0.5f, 0.5f)
                isLooping = false
            }

            player!!.prepare()
            trackBar.max = player!!.duration
            player!!.start()
            Thread(this).start()
        }

        isPlaying = false
    }

    override fun run() {
        var currentPosition = player!!.currentPosition
        val total = player!!.duration

        while (player != null && player!!.isPlaying && currentPosition < total) {
            currentPosition = try {
                Thread.sleep(1000)
                player!!.currentPosition
            } catch (e: Exception) {
                return
            }

            trackBar.progress = currentPosition
        }
    }

    private fun clearMediaPlayer() {
        player!!.stop()
        player!!.release()
        player = null
    }*/
}
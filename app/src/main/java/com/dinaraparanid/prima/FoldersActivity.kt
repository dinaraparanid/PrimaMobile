package com.dinaraparanid.prima

import android.os.Bundle
import androidx.core.graphics.drawable.toDrawable
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.dinaraparanid.prima.core.Folder
import com.dinaraparanid.prima.databinding.ActivityFoldersBinding
import com.dinaraparanid.prima.fragments.ChooseFolderFragment
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.rootFile
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.polymorphism.AbstractActivity
import com.dinaraparanid.prima.mvvmp.androidx.FoldersActivityViewModel
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import java.lang.ref.WeakReference

class FoldersActivity : AbstractActivity(),
    ChooseFolderFragment.Callbacks {
    internal companion object {
        @Deprecated("Switched to registerForActivityResult")
        internal const val PICK_FOLDER = 212
        internal const val FOLDER_KEY = "folder"
    }

    private var binding: ActivityFoldersBinding? = null

    override val viewModel: FoldersActivityViewModel by lazy {
        ViewModelProvider(this)[FoldersActivityViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme()
        super.onCreate(savedInstanceState)
        initView(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    override fun initFirstFragment() {
        currentFragment = WeakReference(
            supportFragmentManager.findFragmentById(R.id.gtm_fragment_container)
        )

        if (currentFragment.get() == null) {
            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.folders_fragment_container,
                    ChooseFolderFragment.newInstance(Folder.fromFile(this.rootFile)!!)
                )
                .commit()
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding = DataBindingUtil
            .setContentView<ActivityFoldersBinding>(this, R.layout.activity_folders)
            .apply {
                viewModel = BasePresenter()
                Params.instance.backgroundImage?.run {
                    foldersMainLayout.background = toBitmap().toDrawable(resources)
                }
            }

        initFirstFragment()
        setSupportActionBar(binding!!.foldersSwitchToolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
    }

    override fun onFolderSelected(folder: Folder) {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out,
                R.anim.slide_in,
                R.anim.slide_out
            )
            .replace(
                R.id.folders_fragment_container,
                ChooseFolderFragment.newInstance(folder)
            )
            .addToBackStack(null)
            .commit()
    }
}
package com.dinaraparanid.prima.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment

/**
 * Fragment with app info.
 * It shows current version, how to contact with developer and FAQ
 */

class AboutAppFragment : AbstractFragment() {
    private lateinit var versionButton: carbon.widget.Button
    private lateinit var githubButton: Button
    private lateinit var vkButton: Button
    private lateinit var emailButton: Button
    private lateinit var faqButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = resources.getString(R.string.about_app)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_about_app, container, false)

        versionButton = view.findViewById<carbon.widget.Button>(R.id.version_button).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)
        }

        githubButton = view.findViewById<Button>(R.id.github_button).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)

            setOnClickListener {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/dinaraparanid")
                    )
                )
            }
        }

        vkButton = view.findViewById<Button>(R.id.vk_button).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)

            setOnClickListener {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://vk.com/paranid5")
                    )
                )
            }
        }

        emailButton = view.findViewById<Button>(R.id.email_button).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)

            setOnClickListener {
                startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND)
                            .setType("plain/text")
                            .putExtra(Intent.EXTRA_EMAIL, arrayOf("arseny_magnitogorsk@live.ru")),
                        resources.getString(R.string.send_email)
                    )
                )
            }
        }

        faqButton = view.findViewById<Button>(R.id.FAQ_button).apply {
            typeface = (requireActivity().application as MainApplication)
                .getFontFromName(Params.instance.font)

            setOnClickListener {
                requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.fade_in,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.fade_out
                    )
                    .replace(
                        R.id.fragment_container,
                        defaultInstance(
                            mainLabelCurText,
                            resources.getString(R.string.faq),
                            FAQFragment::class
                        )
                    )
                    .addToBackStack(null)
                    .commit()
            }
        }

        return view
    }
}
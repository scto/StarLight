/*
 * MainActivity.kt created by Minki Moon(mooner1022)
 * Copyright (c) mooner1022. all rights reserved.
 * This code is licensed under the GNU General Public License v3.0.
 */

package dev.mooner.starlight

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import dev.mooner.starlight.databinding.ActivityMainBinding
import dev.mooner.starlight.logging.bindLogNotifier
import dev.mooner.starlight.plugincore.Session
import dev.mooner.starlight.plugincore.Session.pluginManager
import dev.mooner.starlight.plugincore.logger.LogData
import dev.mooner.starlight.plugincore.utils.hasFlag
import dev.mooner.starlight.ui.ViewPagerAdapter
import dev.mooner.starlight.utils.LAYOUT_TABLET
import dev.mooner.starlight.utils.layoutMode
import dev.mooner.starlight.utils.showChangelogDialog
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private var currentTabId: Int = R.id.nav_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        bindLogNotifier { log ->
            !log.message.startsWith("ECOMF: ") && !(log.flags hasFlag LogData.FLAG_COMPILE_RESULT)
        }

        binding.apply {
            viewPager.adapter = ViewPagerAdapter(this@MainActivity)
            viewPager.registerOnPageChangeCallback(getOnPageChangeCallback())

            bottomMenu.setItemSelected(R.id.nav_home, isSelected = true)
            bottomMenu.setOnItemSelectedListener(::handleMenuItemSelection)
            if (layoutMode == LAYOUT_TABLET) {
                buttonExpandMenu?.setOnClickListener {
                    bottomMenu.let { menu ->
                        TransitionManager.beginDelayedTransition(root as ViewGroup, ChangeBounds())
                        if (menu.isExpanded())
                            menu.collapse()
                        else
                            menu.expand()
                    }
                }
            }

            appBarLayout.addOnOffsetChangedListener(::handleOffsetUpdate)
        }

        val prefs = getSharedPreferences("general", 0)
        val lastVersionCode = prefs.getInt("lastVersionCode", 0)
        if (lastVersionCode < BuildConfig.VERSION_CODE) {
            showChangelogDialog("StarLight 업데이트 완료!")
            prefs.edit {
                putInt("lastVersionCode", BuildConfig.VERSION_CODE)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (currentTabId == R.id.nav_home)
            super.onBackPressed()
        else
            setCurrentTab(R.id.nav_home)
    }

    private fun setCurrentTab(id: Int) {
        val index = when(id) {
            R.id.nav_home     -> 0
            R.id.nav_projects -> 1
            R.id.nav_plugins  -> 2
            R.id.nav_logs     -> 3
            R.id.nav_settings -> 4
            else              -> 0
        }
        binding.apply {
            viewPager.setCurrentItem(index, true)
            bottomMenu.setItemSelected(id, true)
        }
        onPageChanged(id)
    }

    private fun getOnPageChangeCallback() =
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val id = when(position) {
                    0 -> R.id.nav_home
                    1 -> R.id.nav_projects
                    2 -> R.id.nav_plugins
                    3 -> R.id.nav_logs
                    4 -> R.id.nav_settings
                    else -> R.id.nav_home
                }
                binding.bottomMenu.setItemSelected(id, true)
                onPageChanged(id)
            }
        }

    private fun handleMenuItemSelection(id: Int) {
        val index = when(id) {
            R.id.nav_home     -> 0
            R.id.nav_projects -> 1
            R.id.nav_plugins  -> 2
            R.id.nav_logs     -> 3
            R.id.nav_settings -> 4
            else              -> 0
        }
        binding.viewPager.setCurrentItem(index, true)
        onPageChanged(id)
    }

    private fun handleOffsetUpdate(appBarLayout: AppBarLayout, verticalOffset: Int) {
        val alpha = 1.0f - abs(
            verticalOffset / appBarLayout.totalScrollRange.toFloat()
        )
        binding.apply {
            statusText.alpha = alpha
            titleText.alpha = alpha
            imageViewLogo.alpha = alpha
        }
    }

    fun onPageChanged(id: Int) {
        val (title, status) = with(applicationContext) {
            when(id) {
                R.id.nav_home ->
                    getText(R.string.app_name) to
                            getText(R.string.app_version)

                R.id.nav_projects ->
                    getText(R.string.title_projects) to
                            getString(R.string.subtitle_projects)
                                .format(Session.projectManager.getEnabledProjects().size)

                R.id.nav_plugins ->
                    getText(R.string.title_plugins) to
                            getString(R.string.subtitle_plugins)
                                .format(pluginManager.plugins.size)

                R.id.nav_logs ->
                    getString(R.string.title_logs) to ""

                R.id.nav_settings ->
                    applicationContext.getText(R.string.title_settings) to ""

                else ->
                    null to null
            }
        }

        binding.titleText.text  = title
        binding.statusText.text = status
        currentTabId = id
    }
}
/*
 * HomeFragment.kt created by Minki Moon(mooner1022)
 * Copyright (c) mooner1022. all rights reserved.
 * This code is licensed under the GNU General Public License v3.0.
 */


package dev.mooner.starlight.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import dev.mooner.starlight.CA_WIDGETS
import dev.mooner.starlight.CF_IDS
import dev.mooner.starlight.WIDGET_DEF_STRING
import dev.mooner.starlight.databinding.FragmentHomeBinding
import dev.mooner.starlight.plugincore.Session
import dev.mooner.starlight.plugincore.config.GlobalConfig
import dev.mooner.starlight.plugincore.logger.LoggerFactory
import dev.mooner.starlight.plugincore.translation.Locale
import dev.mooner.starlight.plugincore.translation.translate
import dev.mooner.starlight.plugincore.utils.infoTranslated
import dev.mooner.starlight.plugincore.widget.Widget
import dev.mooner.starlight.ui.widget.config.WidgetConfigActivity
import dev.mooner.starlight.ui.widget.config.WidgetsAdapter
import dev.mooner.starlight.utils.LAYOUT_DEFAULT
import dev.mooner.starlight.utils.LAYOUT_TABLET
import dev.mooner.starlight.utils.layoutMode
import jp.wasabeef.recyclerview.animators.FadeInUpAnimator

class HomeFragment : Fragment() {

    private val logger = LoggerFactory.logger {  }

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var widgetsAdapter: WidgetsAdapter? = null

    /*
    private var uptimeTimer: Timer? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val updateUpTimeTask: TimerTask
        get() = object: TimerTask() {
            override fun run() {
                val diffMillis = System.currentTimeMillis() - ApplicationSession.initMillis
                val formatStr = Utils.formatTime(diffMillis)
                mainScope.launch {
                    binding.uptimeText.setText(formatStr)
                }
            }
        }
    private lateinit var mAdapter: LogsRecyclerViewAdapter
     */

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val context = requireContext()

        widgetsAdapter = WidgetsAdapter(context).apply {
            data = getWidgets()
            notifyAllItemInserted()
        }

        val mLayoutManager = when(context.layoutMode) {
            LAYOUT_DEFAULT -> LinearLayoutManager(context)
            LAYOUT_TABLET -> StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            else -> LinearLayoutManager(context)
        }

        with(binding.widgets) {
            itemAnimator = FadeInUpAnimator()
            layoutManager = mLayoutManager
            adapter = widgetsAdapter
        }

        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == WidgetConfigActivity.RESULT_EDITED) {
                widgetsAdapter!!.apply {
                    onDestroy()
                    notifyItemRangeRemoved(0, data.size)
                    data = getWidgets()
                    notifyItemRangeInserted(0, data.size)
                }
                logger.verbose { "Widget list updated" }
            }
        }
        binding.cardViewConfigWidget.setOnClickListener {
            val intent = Intent(requireActivity(), WidgetConfigActivity::class.java)
            resultLauncher.launch(intent)
        }

        return binding.root
    }

    private fun getWidgets(): List<Widget> {
        val widgetIds: List<String> =
            GlobalConfig
                .category(CA_WIDGETS)
                .getString(CF_IDS, WIDGET_DEF_STRING)
                .let(Session.json::decodeFromString)
        val widgets: MutableList<Widget> = mutableListOf()

        for (id in widgetIds) {
            try {
                Session.widgetManager
                    .getWidgetById(id)
                    ?.newInstance()
                    ?.also(viewLifecycleOwner.lifecycle::addObserver)
                    ?.let(widgets::add)
                    ?: logger.infoTranslated {
                        Locale.ENGLISH { "Skipping unknown widget: $id" }
                        Locale.KOREAN  { "알 수 없는 위젯 $id 의 초기화 건너뜀" }
                    }
            } catch (e: Exception) {
                logger.error(e) {
                    translate {
                        Locale.ENGLISH { "Failed to initialize widget '$id': " }
                        Locale.KOREAN  { "위젯 '$id' 초기화 실패: " }
                    }
                }
            }
        }
        return widgets
    }

    override fun onPause() {
        try {
            widgetsAdapter?.onPause()
        } catch (e: Exception) {
            logger.verbose { "Failed to call onPause() on widget: $e" }
        }
        super.onPause()
    }

    override fun onResume() {
        try {
            widgetsAdapter?.onResume()
        } catch (e: Exception) {
            logger.verbose { "Failed to call onResume() on widget: $e" }
        }
        super.onResume()
    }

    override fun onDestroyView() {
        try {
            widgetsAdapter?.onDestroy()
        } catch (e: Exception) {
            logger.verbose { "Failed to destroy widget: $e" }
        }
        widgetsAdapter = null
        _binding = null
        super.onDestroyView()
    }
}
package com.mooner.starlight.ui.plugins

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BasicGridItem
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.bottomsheets.gridItems
import com.afollestad.materialdialogs.customview.customView
import com.mooner.starlight.R
import com.mooner.starlight.databinding.FragmentPluginsBinding
import com.mooner.starlight.models.Align
import com.mooner.starlight.plugincore.core.Session.globalConfig
import com.mooner.starlight.plugincore.core.Session.pluginManager
import com.mooner.starlight.plugincore.plugin.Plugin
import com.mooner.starlight.plugincore.plugin.StarlightPlugin
import com.mooner.starlight.utils.Utils.Companion.formatStringRes
import jp.wasabeef.recyclerview.animators.FadeInUpAnimator

class PluginsFragment : Fragment() {

    companion object {
        @JvmStatic
        private val ALIGN_GANADA: Align<Plugin> = Align(
            name = "가나다 순",
            reversedName = "가나다 역순",
            icon = R.drawable.ic_round_sort_by_alpha_24,
            sort = { list, _ ->
                list.sortedBy { it.name }
            }
        )

        @JvmStatic
        private val ALIGN_FILE_SIZE: Align<Plugin> = Align(
            name = "파일 크기 순",
            reversedName = "파일 크기 역순",
            icon = R.drawable.ic_round_plugins_24,
            sort = { list, _ ->
                list.sortedByDescending { (it as StarlightPlugin).fileSize }
            }
        )

        @JvmStatic
        private val DEFAULT_ALIGN = ALIGN_GANADA

        const val CONFIG_PLUGINS_ALIGN = "plugins_align_state"
        const val CONFIG_PLUGINS_REVERSED = "plugins_align_reversed"
    }

    private var _binding: FragmentPluginsBinding? = null
    private val binding get() = _binding!!

    private var listAdapter: PluginsListAdapter? = null
    private val plugins: List<Plugin> by lazy {
        pluginManager.getPlugins().toList()
    }

    private val aligns = arrayOf(
        ALIGN_GANADA,
        ALIGN_FILE_SIZE,
    )
    private var alignState: Align<Plugin> = getAlignByName(
        globalConfig[CONFIG_PLUGINS_ALIGN, DEFAULT_ALIGN.name]
    )?: DEFAULT_ALIGN
    private var isReversed: Boolean = globalConfig[CONFIG_PLUGINS_REVERSED, "false"].toBoolean()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPluginsBinding.inflate(inflater, container, false)

        if (plugins.isEmpty()) {
            with(binding.textViewNoPluginYet) {
                visibility = View.VISIBLE
                text = if (globalConfig.getCategory("plugin").getBoolean("safe_mode", false)) {
                    "플러그인 안전 모드가 켜져있어요."
                } else {
                    requireContext().formatStringRes(
                        R.string.nothing_yet,
                        mapOf(
                            "name" to "플러그인이",
                            "emoji" to "(>_<｡)\uD83D\uDCA6"
                        )
                    )
                }
            }
        }

        binding.cardViewPluginAlign.setOnClickListener {
            MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                cornerRadius(25f)
                gridItems(aligns.toGridItems()) { dialog, _, item ->
                    alignState = getAlignByName(item.title)?: DEFAULT_ALIGN
                    isReversed = dialog.findViewById<CheckBox>(R.id.checkBoxAlignReversed).isChecked
                    update()
                }
                //customView(R.layout.dialog_align_state)
                customView(R.layout.dialog_align_plugins)
                findViewById<CheckBox>(R.id.checkBoxAlignReversed).isChecked = isReversed
            }
        }

        binding.alignState.text = if (isReversed) alignState.reversedName else alignState.name
        binding.alignStateIcon.setImageResource(alignState.icon)

        listAdapter = PluginsListAdapter(requireContext())
        listAdapter!!.data = sortData()
        listAdapter!!.notifyItemRangeInserted(0, plugins.size)

        with(binding.recyclerViewProjectList) {
            adapter = listAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = FadeInUpAnimator()
        }

        return binding.root
    }

    private fun getAlignByName(name: String): Align<Plugin>? = aligns.find { it.name == name }

    private fun Array<Align<Plugin>>.toGridItems(): List<BasicGridItem> = this.map { item ->
        BasicGridItem(
            iconRes = item.icon,
            title = item.name
        )
    }

    private fun sortData(): List<Plugin> {
        val aligned = alignState.sort(
            plugins,
            mapOf()
        )
        return if (isReversed) aligned.asReversed() else aligned
    }

    private fun reloadList(list: List<Plugin>) {
        listAdapter?.apply {
            val orgDataSize = data.size
            data = listOf()
            notifyItemRangeRemoved(0, orgDataSize)
            data = list
            notifyItemRangeInserted(0, data.size)
        }
    }

    private fun update() {
        binding.alignState.text = if (isReversed) alignState.reversedName else alignState.name
        binding.alignStateIcon.setImageResource(alignState.icon)
        reloadList(sortData())
        globalConfig.apply {
            set(CONFIG_PLUGINS_ALIGN, alignState.name)
            set(CONFIG_PLUGINS_REVERSED, isReversed.toString())
            push()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listAdapter = null
        _binding = null
    }
}
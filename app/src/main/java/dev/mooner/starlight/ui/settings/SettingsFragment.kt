package dev.mooner.starlight.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.mooner.configdsl.ConfigStructure
import dev.mooner.configdsl.adapters.ConfigAdapter
import dev.mooner.starlight.databinding.FragmentSettingsBinding
import dev.mooner.starlight.plugincore.config.GlobalConfig
import dev.mooner.starlight.plugincore.utils.onSaveConfigAdapter
import dev.mooner.starlight.utils.isNoobMode
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import dev.mooner.starlight.ui.settings.getSettingsStruct as actualSettingStruct

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var configAdapter: ConfigAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        // Transfer legacy data
        val updatedData: MutableMap<String, MutableMap<String, JsonElement>> = hashMapOf()
        val category = GlobalConfig.category("notifications")
        for ((key, value) in category.data.toMap()) { // Clone to avoid Concurrent Modification
            if (value !is JsonObject) {
                val destKey = when(key) {
                    "read_noti_perm",
                    "use_legacy_event",
                    "log_received_message" ->
                        "event"
                    "set_package_rules",
                    "use_on_notification_posted",
                    "magic" ->
                        "noti"
                    else -> continue
                }
                updatedData.computeIfAbsent(destKey) { hashMapOf() }[key] = value
                category.remove(key)
            }
        }
        updatedData.mapValues { (_, v) -> JsonObject(v) }.forEach(category::setRaw)

        configAdapter = ConfigAdapter.Builder(requireActivity()) {
            bind(binding.configRecyclerView)
            structure(::getSettingStruct)
            configData(GlobalConfig.getMutableData())
            onValueUpdated(GlobalConfig::onSaveConfigAdapter)
        }.build()

        return binding.root
    }

    private fun getSettingStruct(): ConfigStructure =
        if (isNoobMode) getNoobSettingStruct() else actualSettingStruct()

    override fun onDestroyView() {
        configAdapter?.destroy()
        super.onDestroyView()
    }
}
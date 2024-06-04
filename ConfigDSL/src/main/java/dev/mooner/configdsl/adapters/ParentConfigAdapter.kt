/*
 * ParentConfigAdapter.kt created by Minki Moon(mooner1022) on 5/3/24, 5:48 PM
 * Copyright (c) mooner1022. all rights reserved.
 * This code is licensed under the GNU General Public License v3.0.
 */

package dev.mooner.configdsl.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.mooner.configdsl.*
import dev.mooner.configdsl.ConfigDSL.getIsDevMode
import dev.mooner.configdsl.options.CategoryConfigOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

private typealias HashCode = Int

class ParentConfigAdapter(
    private var configStructure  : ConfigStructure,
    private val configData       : MutableDataMap,
    coroutineContext : CoroutineContext,
): RecyclerView.Adapter<BaseViewHolder>() {

    //private var descCache: MutableMap<String, String> = hashMapOf()
    private val eventScope = CoroutineScope(coroutineContext + SupervisorJob())

    //private var childAdapters : MutableList<CategoryRecyclerAdapter> = arrayListOf()
    private val viewTypes     : MutableList<HashCode> = arrayListOf()
    private val viewInstances : MutableList<RootConfigOption<*, *>> = arrayListOf()

    private val eventPublisher = MutableSharedFlow<ConfigOption.EventData>(
        extraBufferCapacity = Channel.UNLIMITED
    )
    val eventFlow
        get() = eventPublisher.asSharedFlow()

    val isHavingError: Boolean
        get() = configStructure.any(RootConfigOption<*, *>::hasError)

    override fun getItemViewType(position: Int): Int {
        val viewData = configStructure[position]

        return viewTypes.indexOf(viewData::class.hashCode())
    }

    override fun getItemCount(): Int =
        if (getIsDevMode())
            configStructure.size
        else
            configStructure.count { it !is CategoryConfigOption || !it.isDevModeOnly }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val instance = viewInstances[viewType]
        return instance.onCreateViewHolder(parent)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val posData = configStructure[position]
        val viewData = if (posData is CategoryConfigOption && posData.isDevModeOnly) {
            if (getIsDevMode()) posData
            else configStructure[position + 1]
        } else posData

        val childData = configData[viewData.id] ?: emptyMap()
        @Suppress("UNCHECKED_CAST")
        (viewData as RootConfigOption<BaseViewHolder, Any>).onDraw(holder, childData)
    }

    fun updateStruct(structure: ConfigStructure) {
        configStructure = structure
    }

    fun updateData(data: MutableDataMap) {
        configData.clear()
        data.forEach(configData::put)
    }

    fun notifyAllItemInserted() {
        notifyItemRangeInserted(0, configStructure.size)
    }

    fun destroy() {
        eventScope.cancel()

        configStructure.forEach(RootConfigOption<*, *>::onDestroyed)
    }

    init {
        for (option in configStructure) {
            val hash = option::class.hashCode()
            if (hash !in viewTypes) {
                viewTypes     += hash
                viewInstances += option
            }
            option.init(eventPublisher)
        }

        eventFlow
            .filterIsInstance<ConfigOption.RootUpdateData>()
            .onEach { data ->
                configData[data.rootId]?.also { ent ->
                    ent[data.provider] = data.jsonData
                }
            }
            .launchIn(eventScope)
    }
}
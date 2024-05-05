/*
 * CategoryConfigOption.kt created by Minki Moon(mooner1022) on 2/17/24, 2:49 AM
 * Copyright (c) mooner1022. all rights reserved.
 * This code is licensed under the GNU General Public License v3.0.
 */

package dev.mooner.configdsl.options

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.mooner.configdsl.*
import dev.mooner.configdsl.adapters.CategoryRecyclerAdapter
import dev.mooner.configdsl.utils.hasFlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class CategoryConfigOption(
    override val id          : String,
    override val title       : String,
             val flags       : Int = FLAG_NONE,
             @ColorInt
             val textColor   : Int?,
    override val childOptions: List<ConfigOption<*, *>>
): RootConfigOption<CategoryConfigOption.CategoryViewHolder, MutableMap<String, JsonElement>>() {

    val isDevModeOnly get() = flags hasFlag FLAG_DEV_MODE_ONLY

    override val icon        : IconInfo = IconInfo.none()
    override val default     : MutableMap<String, JsonElement> = hashMapOf()
    override val description : String? = null
    override val dependency  : String? = null

    private val childAdapters: MutableList<CategoryRecyclerAdapter> = arrayListOf()
    private val eventScope: CoroutineScope =
        CoroutineScope(Dispatchers.Default)

    override fun onCreateViewHolder(parent: ViewGroup): CategoryViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.config_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun dataFromJson(jsonElement: JsonElement): MutableMap<String, JsonElement> =
        jsonElement.jsonObject.toMutableMap()

    override fun dataToJson(value: MutableMap<String, JsonElement>): JsonElement =
        JsonObject(value)

    override fun onDraw(holder: CategoryViewHolder, data: MutableMap<String, JsonElement>) {
        // Implemented in ParentConfigAdapter.kt
        val childAdapter = CategoryRecyclerAdapter(
            options    = childOptions,
            optionData = data,
            eventScope = eventScope,
        )
        val mLayoutManager = LinearLayoutManager(holder.itemView.context)
        holder.itemList.apply {
            adapter = childAdapter
            layoutManager = mLayoutManager
        }
        childAdapter.eventFlow
            .filterNot { it.provider.startsWith("dep:") && it.provider.startsWith("redraw:") }
            .onEach {
                //configData.computeIfAbsent(id) { hashMapOf() }[it.provider] = it.jsonData
                tryEmitData(EventData(
                    id + ":" + it.provider,
                    it.data,
                    it.jsonData
                ))
            }
            .launchIn(eventScope)
        childAdapter.notifyItemRangeInserted(0, childOptions.size)

        childAdapters += childAdapter

        title.nullIfBlank()?.let {
            holder.title.apply {
                text = it
                visibility = View.VISIBLE
                setTextColor(textColor ?: context.getColor(R.color.text))
            }
        } ?: let {
            holder.title.visibility = View.INVISIBLE
        }
    }

    override fun onDestroyed() {
        eventScope.cancel()

        childAdapters.forEach(CategoryRecyclerAdapter::destroy)
        childAdapters.clear()
    }

    private fun String.nullIfBlank(): String? =
        this.ifBlank { null }

    companion object {

        const val FLAG_NONE: Int = 0x0

        const val FLAG_DEV_MODE_ONLY: Int = 0x1
    }

    class CategoryViewHolder(itemView: View): BaseViewHolder(itemView) {

        val title   : TextView     = itemView.findViewById(R.id.title)
        val itemList: RecyclerView = itemView.findViewById(R.id.recyclerViewCategory)

        override fun setEnabled(enabled: Boolean) {}
    }
}
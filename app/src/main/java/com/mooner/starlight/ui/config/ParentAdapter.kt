package com.mooner.starlight.ui.config

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Scale
import com.mooner.starlight.R
import com.mooner.starlight.plugincore.config.CategoryConfigObject
import com.mooner.starlight.plugincore.config.ConfigObjectType
import com.mooner.starlight.plugincore.config.TypedString
import com.mooner.starlight.plugincore.config.config
import com.mooner.starlight.plugincore.logger.Logger
import com.mooner.starlight.plugincore.utils.Icon
import com.mooner.starlight.utils.isDevMode
import com.mooner.starlight.utils.startConfigActivity

class ParentAdapter(
    private val context: Context,
    private val onConfigChanged: (parentId: String, id: String, view: View?, data: Any) -> Unit
): RecyclerView.Adapter<ParentAdapter.ViewHolder>() {

    private var recyclerAdapter: ConfigAdapter? = null
    private var descCache: MutableMap<String, String> = hashMapOf()

    var data: List<CategoryConfigObject> = mutableListOf()
    var saved: Map<String, Map<String, TypedString>> = mutableMapOf()
    val isHavingError get() = recyclerAdapter?.isHavingError?: false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = when(viewType) {
            ConfigObjectType.CATEGORY_NESTED.viewType -> R.layout.config_category_nested
            ConfigObjectType.CATEGORY.viewType        -> R.layout.config_category
            else -> {
                Logger.w("Rendering config category with unknown viewType: $viewType, using default layout...")
                R.layout.config_category
            }
        }

        val view = LayoutInflater.from(context).inflate(layout, parent, false)
        return ViewHolder(view, viewType)
    }

    override fun getItemCount(): Int = if (isDevMode) data.size else data.count { !it.isDevModeOnly }

    override fun getItemViewType(position: Int): Int {
        val viewData = data[position]
        return if (viewData.isNested) ConfigObjectType.CATEGORY_NESTED.viewType
        else ConfigObjectType.CATEGORY.viewType
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val posData = data[position]
        val viewData = if (posData.isDevModeOnly) {
            if (isDevMode) posData
            else data[position + 1]
        } else posData

        if (viewData.isNested) {
            require(viewData.title.isNotBlank()) {
                "Title is required for nested category."
            }

            holder.categoryTitle.apply {
                text = viewData.title
                if (viewData.textColor == null)
                    setTextColor(context.getColor(R.color.text))
                else
                    setTextColor(viewData.textColor!!)
            }

            holder.categoryDescription.text = viewData.getDescription()

            holder.categoryIcon.apply {
                when {
                    viewData.icon != null -> when(viewData.icon) {
                        Icon.NONE -> setImageDrawable(null)
                        else -> load(viewData.icon!!.drawableRes)
                    }
                    viewData.iconFile != null -> load(viewData.iconFile!!) {
                        scale(Scale.FIT)
                    }
                    viewData.iconResId != null -> load(viewData.iconResId!!)
                }

                if (viewData.iconTintColor != null)
                    ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(viewData.iconTintColor!!))
                else
                    ImageViewCompat.setImageTintList(this, null)
                //setColorFilter(viewData.iconTintColor, android.graphics.PorterDuff.Mode.SRC_IN)
            }

            holder.categoryButton.setOnClickListener {
                val saved = this@ParentAdapter.saved[viewData.id]?.toMutableMap()?: mutableMapOf()
                context.startConfigActivity(
                    title = viewData.title,
                    subTitle = "설정",
                    items = config {
                        category {
                            id = "nested_category"
                            items = viewData.items
                        }
                    },
                    saved = mapOf("nested_category" to saved),
                    onConfigChanged = { _, id, view, data ->
                        onConfigChanged(viewData.id, id, view, data)
                    }
                )
            }
        } else {
            if (viewData.title.isNotBlank()) {
                holder.categoryTitle.apply {
                    visibility = View.VISIBLE
                    text = viewData.title
                    if (viewData.textColor == null)
                        setTextColor(context.getColor(R.color.text))
                    else
                        setTextColor(viewData.textColor!!)
                }
            } else {
                holder.categoryTitle.visibility = View.INVISIBLE
            }

            val children = viewData.items
            recyclerAdapter = ConfigAdapter(context) { id, view, data ->
                onConfigChanged(viewData.id, id, view, data)
            }.apply {
                this.data = children
                saved = this@ParentAdapter.saved[viewData.id]?.toMutableMap()?: mutableMapOf()
                notifyItemRangeInserted(0, data.size)
            }
            val mLayoutManager = LinearLayoutManager(context)
            holder.categoryItems.apply {
                adapter = recyclerAdapter
                layoutManager = mLayoutManager
            }
        }
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    inner class ViewHolder(itemView: View, viewType: Int) : RecyclerView.ViewHolder(itemView) {
        val categoryTitle: TextView = itemView.findViewById(R.id.title)

        lateinit var categoryItems: RecyclerView

        lateinit var categoryIcon: ImageView
        lateinit var categoryDescription: TextView
        lateinit var categoryButton: ConstraintLayout

        init {
            when(viewType) {
                ConfigObjectType.CATEGORY.viewType -> {
                    categoryItems = itemView.findViewById(R.id.recyclerViewCategory)
                }
                ConfigObjectType.CATEGORY_NESTED.viewType -> {
                    categoryIcon = itemView.findViewById(R.id.icon)
                    categoryDescription = itemView.findViewById(R.id.description)
                    categoryButton = itemView.findViewById(R.id.layout_configNestedCategory)
                }
            }
        }
    }

    fun notifyAllItemInserted() {
        notifyItemRangeInserted(0, data.size)
    }

    fun destroy() {
        if (recyclerAdapter != null) {
            recyclerAdapter!!.destroy()
            recyclerAdapter = null
        }
    }

    private fun CategoryConfigObject.getDescription(): String {
        if (id !in descCache)
            descCache[id] = items.joinToString { it.title }
        return descCache[id]!!
    }
}
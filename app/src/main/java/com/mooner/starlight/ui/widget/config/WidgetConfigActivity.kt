package com.mooner.starlight.ui.widget.config

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mooner.starlight.R
import com.mooner.starlight.databinding.ActivityWidgetConfigBinding
import com.mooner.starlight.plugincore.Session
import com.mooner.starlight.plugincore.Session.json
import com.mooner.starlight.plugincore.logger.Logger
import com.mooner.starlight.plugincore.widget.Widget
import com.mooner.starlight.utils.formatStringRes
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WidgetConfigActivity : AppCompatActivity() {

    companion object {
        const val RESULT_EDITED = 1
    }

    private lateinit var binding: ActivityWidgetConfigBinding

    var recyclerAdapter: WidgetsThumbnailAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWidgetConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val widgetIds: List<String> = json.decodeFromString(Session.globalConfig.getCategory("widgets").getString("ids", "[]"))
        Logger.v("ids= $widgetIds")
        val widgets: MutableList<Widget> = mutableListOf()
        for (id in widgetIds) {
            with(Session.widgetManager.getWidgetById(id)) {
                if (this != null)
                    widgets += this
            }
        }

        binding.subTitle.text = formatStringRes(R.string.subtitle_widgets, mapOf(
            "count" to widgets.size.toString()
        ))
        binding.scroll.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val alpha = if (scrollY in 0..200) {
                1f - (scrollY / 200.0f)
            } else {
                0f
            }
            binding.imageViewLogo.alpha = alpha
            binding.title.alpha = alpha
            binding.subTitle.alpha = alpha
        }

        binding.leave.setOnClickListener { finish() }

        binding.cardViewAddWidget.setOnClickListener {
            recyclerAdapter!!.apply {
                data += Session.widgetManager.getWidgetById("widget_uptime_default")!!
                notifyItemInserted(data.size)

                notifyDataEdited(data)
            }
        }

        recyclerAdapter = WidgetsThumbnailAdapter(binding.root.context) { data ->
            notifyDataEdited(data)
        }.apply {
            data = widgets
            notifyAllItemInserted()
        }

        val itemTouchCallback = object : ItemTouchHelper.SimpleCallback (
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.RIGHT
        ){
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos: Int = viewHolder.bindingAdapterPosition
                val toPos: Int = target.bindingAdapterPosition
                recyclerAdapter!!.swapData(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                recyclerAdapter!!.removeData(viewHolder.layoutPosition)
            }
        }

        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)

        val mLayoutManager = LinearLayoutManager(applicationContext)
        binding.recyclerView.apply {
            layoutManager = mLayoutManager
            adapter = recyclerAdapter
        }
    }

    private fun notifyDataEdited(data: List<Widget>) {
        Logger.v("edited")
        Session.globalConfig.edit {
            getCategory("widgets")["ids"] = Json.encodeToString(data.map { it.id })
        }
        setResult(RESULT_EDITED)
    }

    override fun onDestroy() {
        super.onDestroy()
        recyclerAdapter = null
    }
}
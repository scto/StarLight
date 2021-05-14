package com.mooner.starlight.ui.projects.config

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.mooner.starlight.R
import com.mooner.starlight.plugincore.TypedString
import com.mooner.starlight.plugincore.language.*
import org.angmarch.views.NiceSpinner

class ProjectConfigAdapter(
    private val context: Context,
    private val onConfigChanged: (id: String, data: Any) -> Unit
): RecyclerView.Adapter<ProjectConfigAdapter.ProjectConfigViewHolder>() {
    var data = mutableListOf<ConfigObject>()
    var saved: MutableMap<String, TypedString> = mutableMapOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectConfigViewHolder {
        val view = when(viewType) {
            ConfigObjectType.TOGGLE.viewType -> LayoutInflater.from(context).inflate(R.layout.config_toggle, parent, false)
            ConfigObjectType.SLIDER.viewType -> LayoutInflater.from(context).inflate(R.layout.config_slider, parent, false)
            ConfigObjectType.STRING.viewType -> LayoutInflater.from(context).inflate(R.layout.config_string, parent, false)
            ConfigObjectType.SPINNER.viewType -> LayoutInflater.from(context).inflate(R.layout.config_spinner, parent, false)
            ConfigObjectType.BUTTON.viewType -> LayoutInflater.from(context).inflate(R.layout.config_button, parent, false)
            ConfigObjectType.CUSTOM.viewType -> LayoutInflater.from(context).inflate(R.layout.config_custom, parent, false)
            else -> LayoutInflater.from(context).inflate(R.layout.config_toggle, parent, false)
        }
        return ProjectConfigViewHolder(view, viewType)
    }

    override fun getItemCount(): Int = data.size

    override fun getItemViewType(position: Int): Int {
        return data[position].viewType
    }

    override fun onBindViewHolder(holder: ProjectConfigViewHolder, position: Int) {
        val viewData = data[position]
        fun getDefault(): Any {
            return if (saved.containsKey(viewData.id)) saved[viewData.id]!!.cast()!! else viewData.default
        }

        when(viewData.viewType) {
            ConfigObjectType.TOGGLE.viewType -> {
                holder.textToggle.text = viewData.name
                holder.toggle.isChecked = getDefault() as Boolean
                holder.toggle.setOnCheckedChangeListener { _, isChecked ->
                    onConfigChanged(viewData.id, isChecked)
                }
            }
            ConfigObjectType.SLIDER.viewType -> {
                holder.textSlider.text = viewData.name
                holder.seekBar.progress = getDefault() as Int
                holder.seekBar.max = (viewData as SliderConfigObject).max
                holder.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        holder.seekBarIndex.text = progress.toString()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        onConfigChanged(viewData.id, seekBar!!.progress)
                    }
                })
                holder.seekBarIndex.text = holder.seekBar.progress.toString()
            }
            ConfigObjectType.STRING.viewType -> {
                holder.textString.text = viewData.name
                holder.editTextString.hint = viewData.default as String
                holder.editTextString.setText(getDefault() as String)
                holder.editTextString.addTextChangedListener(object: TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                    override fun afterTextChanged(s: Editable?) {
                        onConfigChanged(viewData.id, s!!.toString())
                    }
                })
            }
            ConfigObjectType.SPINNER.viewType -> {
                holder.textSpinner.text = viewData.name
                holder.spinner.apply {
                    setBackgroundColor(context.getColor(R.color.transparent))
                    attachDataSource((viewData as SpinnerConfigObject).dataList)
                    setOnSpinnerItemSelectedListener { _, _, position, _ ->
                        onConfigChanged(viewData.id, position)
                    }
                    selectedIndex = getDefault() as Int
                }
            }
            ConfigObjectType.BUTTON.viewType -> {
                holder.textButton.text = viewData.name
                val langConf = viewData as ButtonConfigObject
                holder.layoutButton.setOnClickListener {
                    langConf.onClickListener()
                }
                if (langConf.iconRes != null) {
                    holder.imageViewButton.load(AppCompatResources.getDrawable(context, langConf.iconRes!!))
                } else if (langConf.iconDrawable != null) {
                    holder.imageViewButton.load(langConf.iconDrawable!!)
                }
            }
            ConfigObjectType.CUSTOM.viewType -> {
                val data = viewData as CustomConfigObject
                val inflater = context.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val child: View = inflater.inflate(data.layoutID, holder.customLayout)
                //holder.customLayout.addView(child)
                data.onInflate(child)
            }
        }
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    inner class ProjectConfigViewHolder(itemView: View, viewType: Int) : RecyclerView.ViewHolder(itemView) {
        lateinit var textToggle: TextView
        lateinit var toggle: Switch

        lateinit var textSlider: TextView
        lateinit var seekBarIndex: TextView
        lateinit var seekBar: SeekBar

        lateinit var textString: TextView
        lateinit var editTextString: EditText

        lateinit var textSpinner: TextView
        lateinit var spinner: NiceSpinner

        lateinit var textButton: TextView
        lateinit var layoutButton: ConstraintLayout
        lateinit var imageViewButton: ImageView

        lateinit var customLayout: LinearLayout

        var context: Context = itemView.context

        init {
            when(viewType) {
                ConfigObjectType.TOGGLE.viewType -> {
                    textToggle = itemView.findViewById(R.id.textView_configToggle)
                    toggle = itemView.findViewById(R.id.switch_configToggle)
                }
                ConfigObjectType.SLIDER.viewType -> {
                    textSlider = itemView.findViewById(R.id.textView_configSlider)
                    seekBarIndex = itemView.findViewById(R.id.index_configSlider)
                    seekBar = itemView.findViewById(R.id.seekBar_configSlider)
                }
                ConfigObjectType.STRING.viewType -> {
                    textString = itemView.findViewById(R.id.textView_configString)
                    editTextString = itemView.findViewById(R.id.editText_configString)
                }
                ConfigObjectType.SPINNER.viewType -> {
                    textSpinner = itemView.findViewById(R.id.textView_configSpinner)
                    spinner = itemView.findViewById(R.id.spinner_configSpinner)
                }
                ConfigObjectType.BUTTON.viewType -> {
                    textButton = itemView.findViewById(R.id.textView_configButton)
                    layoutButton = itemView.findViewById(R.id.layout_configButton)
                    imageViewButton = itemView.findViewById(R.id.imageView_configButton)
                }
                ConfigObjectType.CUSTOM.viewType -> {
                    customLayout = itemView.findViewById(R.id.layout_configCustom)
                }
            }
        }
    }
}
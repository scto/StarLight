package dev.mooner.starlight.utils

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.UiContext
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.callbacks.onShow
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import dev.mooner.configdsl.ConfigBuilder
import dev.mooner.configdsl.ConfigStructure
import dev.mooner.configdsl.MutableDataMap
import dev.mooner.configdsl.adapters.ConfigAdapter
import dev.mooner.configdsl.adapters.OnValueUpdatedListener
import dev.mooner.peekalert.PeekAlert
import dev.mooner.peekalert.PeekAlertBuilder
import dev.mooner.peekalert.createPeekAlert
import dev.mooner.starlight.R
import dev.mooner.starlight.databinding.DialogConfigLayoutBinding
import dev.mooner.starlight.databinding.DialogLogsBinding
import dev.mooner.starlight.logging.LogCollector
import dev.mooner.starlight.plugincore.config.GlobalConfig
import dev.mooner.starlight.plugincore.event.EventHandler
import dev.mooner.starlight.plugincore.event.Events
import dev.mooner.starlight.plugincore.event.on
import dev.mooner.starlight.plugincore.logger.LogType
import dev.mooner.starlight.ui.logs.LogsRecyclerViewAdapter
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonVisitor
import jp.wasabeef.recyclerview.animators.FadeInUpAnimator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.commonmark.node.SoftLineBreak
import java.io.Reader
import kotlin.properties.Delegates.notNull

typealias ConfirmCallback = (confirm: Boolean) -> Unit

@UiContext
fun Context.showLogsDialog() {
    val logUpdateScope = CoroutineScope(Dispatchers.Default)

    MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
        setCommonAttrs()
        cancelOnTouchOutside(true)
        title(res = R.string.title_logs)

        val activity = this@showLogsDialog

        val binding = DialogLogsBinding.inflate(LayoutInflater.from(activity))
        customView(view = binding.root)

        onShow { _ ->
            val showInternalLog = GlobalConfig
                .category("dev_mode_config")
                .getBoolean("show_internal_log", false)
            val logs = LogCollector.logs
            val mAdapter = LogsRecyclerViewAdapter().withData(logs)
            val mLayoutManager = LinearLayoutManager(activity).apply {
                reverseLayout = true
                stackFromEnd = true
            }

            binding.rvLog.apply {
                itemAnimator = FadeInUpAnimator()
                layoutManager = mLayoutManager
                adapter = mAdapter
            }
            mAdapter.notifyItemRangeInserted(0, logs.size)

            EventHandler.on<Events.Log.Create>(logUpdateScope) {
                if (log.type == LogType.VERBOSE && !showInternalLog) return@on
                binding.rvLog.post {
                    mAdapter.push(log)
                    binding.rvLog.smoothScrollToPosition(mAdapter.getItems().size - 1)
                }
            }
        }

        onDismiss {
            logUpdateScope.cancel()
        }

        negativeButton(res = R.string.close) {
            dismiss()
        }
    }
}

fun showAlertDialog(context: Context, title: String, message: String, onDismiss: ConfirmCallback? = null): MaterialDialog {
    return MaterialDialog(context, BottomSheet(LayoutMode.WRAP_CONTENT)).noAutoDismiss().show {
        setCommonAttrs()
        cancelOnTouchOutside(true)
        title(text = title)
        message(text = message)
        positiveButton(res = R.string.ok) {
            onDismiss?.invoke(true)
            dismiss()
        }
    }
}

fun showConfirmDialog(context: Context, title: String, message: String, onDismiss: ConfirmCallback? = null) {
    MaterialDialog(context, BottomSheet(LayoutMode.WRAP_CONTENT)).noAutoDismiss().show {
        setCommonAttrs()
        cancelOnTouchOutside(false)
        title(text = title)
        message(text = message)
        positiveButton(res = R.string.ok) {
            onDismiss?.invoke(true)
            dismiss()
        }
        negativeButton(res = R.string.cancel) {
            onDismiss?.invoke(false)
            dismiss()
        }
    }
}

fun Context.showErrorLogDialog(title: String, e: Throwable) {
    MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).noAutoDismiss().show {
        setCommonAttrs()
        cancelOnTouchOutside(false)
        title(text = title)
        message(text = e.toString() + "\n" + e.stackTraceToString())
        positiveButton(res = R.string.close, click = MaterialDialog::dismiss)
    }
}

fun Context.showChangelogDialog(title: String) {
    val changelog = this.assets
        .open("changes.md")
        .bufferedReader()
        .use(Reader::readText)
    val markdown = Markwon
        .builder(this)
        .usePlugin(
            object : AbstractMarkwonPlugin() {
                override fun configureVisitor(builder: MarkwonVisitor.Builder) {
                    builder.on(SoftLineBreak::class.java) { visitor, _ ->
                        visitor.forceNewLine()
                    }
                }
            }
        )
        .build()
        .toMarkdown(changelog)

    MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).noAutoDismiss().show {
        setCommonAttrs()
        cancelOnTouchOutside(false)
        title(text = title)
        message(text = markdown)
        positiveButton(res = R.string.close, click = MaterialDialog::dismiss)
    }
}

fun MaterialDialog.setCommonAttrs() {
    cornerRadius(res = R.dimen.card_radius)
}

context(LifecycleOwner)
fun MaterialDialog.setCommonAttrs() {
    cornerRadius(res = R.dimen.card_radius)
    lifecycleOwner(this@LifecycleOwner)
}

context(LifecycleOwner)
fun MaterialDialog.configStruct(context: Context, block: DialogConfigStructBuilder.() -> Unit) {
    val binding = DialogConfigLayoutBinding.inflate(layoutInflater, null, false)
    var adapter: ConfigAdapter? = ConfigAdapter.Builder(context) {
        bind(binding.configRecyclerView)
        lifecycleOwner(this@LifecycleOwner)

        DialogConfigStructBuilder().apply(block).build(this)
    }.build()

    onDismiss {
        adapter?.destroy()
        adapter = null
    }

    customView(view = binding.root)
}

class DialogConfigStructBuilder {

    private var structure: ConfigStructure by notNull()
    private var dataMap  : MutableDataMap = hashMapOf()
    private var listener : OnValueUpdatedListener = { _, _, _, _ -> }

    fun struct(structure: ConfigStructure) {
        this.structure = structure
    }

    fun struct(block: ConfigBuilder.() -> Unit) {
        structure = ConfigBuilder()
            .apply(block)
            .build(flush = true)
    }

    fun data(block: () -> MutableDataMap) {
        dataMap = block()
    }

    fun onUpdate(listener: OnValueUpdatedListener) {
        this.listener = listener
    }

    internal fun build(builder: ConfigAdapter.Builder) {
        builder.apply {
            structure { structure }
            configData(dataMap)
            onValueUpdated(listener)
        }
    }
}

fun Fragment.createSimplePeek(title: String? = null, text: String, builder: PeekAlertBuilder.() -> Unit): PeekAlert {
    return createPeekAlert(this) {
        setup(requireContext(), title, text)
        this.apply(builder)
    }
}

fun Activity.createSimplePeek(title: String? = null, text: String, builder: PeekAlertBuilder.() -> Unit): PeekAlert {
    return createPeekAlert(this) {
        setup(this@createSimplePeek, title, text)
        this.apply(builder)
    }
}

fun Fragment.createSuccessPeek(title: String, position: PeekAlert.Position): PeekAlert {
    return createSimplePeek(text = title) {
        this.position = position
        iconRes = R.drawable.ic_round_check_24
        iconTint(res = R.color.noctis_green)
        backgroundColor(res = R.color.background_popup)
    }
}

fun Activity.createSuccessPeek(title: String, position: PeekAlert.Position): PeekAlert {
    return createSimplePeek(text = title) {
        this.position = position
        iconRes = R.drawable.ic_round_check_24
        iconTint(res = R.color.noctis_green)
        backgroundColor(res = R.color.background_popup)
    }
}

fun Fragment.createFailurePeek(title: String, position: PeekAlert.Position): PeekAlert {
    return createSimplePeek(text = title) {
        this.position = position
        iconRes = R.drawable.ic_round_close_24
        iconTint(res = R.color.code_error)
        backgroundColor(res = R.color.background_popup)
    }
}

fun Activity.createFailurePeek(title: String, position: PeekAlert.Position): PeekAlert {
    return createSimplePeek(text = title) {
        this.position = position
        iconRes = R.drawable.ic_round_close_24
        iconTint(res = R.color.code_error)
        backgroundColor(res = R.color.background_popup)
    }
}

private fun PeekAlertBuilder.setup(context: Context, title: String?, text: String) {
    position = PeekAlert.Position.Top
    width = ViewGroup.LayoutParams.WRAP_CONTENT
    cornerRadius = dp(14).toFloat()
    autoHideMillis = 3000L
    draggable = true
    iconTint(res = R.color.white)

    if (title == null) {
        text(text) {
            textColor(R.color.text)
            textSize = 14f
            typeface = getTypeface(context, R.font.nanumsquare_round_bold)
        }
    } else {
        //paddingDp = 17
        title(title) {
            textColor(R.color.text)
            textSize = 10f
            typeface = getTypeface(context, R.font.nanumsquare_neo_regular)
        }
        text(text) {
            textColor(R.color.text)
            textSize = 12f
            typeface = getTypeface(context, R.font.nanumsquare_neo_bold)
        }
    }
}
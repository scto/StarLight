package com.mooner.starlight.listener

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Base64
import android.util.Log
import com.mooner.starlight.core.ApplicationSession
import com.mooner.starlight.plugincore.core.Session
import com.mooner.starlight.plugincore.core.GeneralConfig
import com.mooner.starlight.plugincore.core.Session.Companion.projectLoader
import com.mooner.starlight.plugincore.logger.Logger
import com.mooner.starlight.plugincore.project.Replier
import java.io.ByteArrayOutputStream
import java.util.*

class NotificationListener: NotificationListenerService() {

    companion object {
        fun send(message: String, session: Notification.Action, context: Context = ApplicationSession.context) {
            val sendIntent = Intent()
            val msg = Bundle()
            for (input in session.remoteInputs) msg.putCharSequence(
                input.resultKey,
                message
            )
            RemoteInput.addResultsToIntent(session.remoteInputs, sendIntent, msg)
            try {
                session.actionIntent.send(context, 0, sendIntent)
                Logger.d("NotificationListenerService", "send() success: $message")
            } catch (e: PendingIntent.CanceledException) {
                e.printStackTrace()
            }
        }
    }

    private val sessions: HashMap<String, Notification.Action> = hashMapOf()
    private var lastRoom: String? = null
    private val replier = object : Replier {
        override fun reply(msg: String) {
            if (lastRoom != null) {
                if (!sessions.containsKey(lastRoom)) {
                    Log.w("NotificationListener", "No session for room $lastRoom found")
                }
                send(msg, sessions[lastRoom]!!)
            }
        }

        override fun reply(room: String, msg: String) {
            if (!sessions.containsKey(room)) {
                Log.w("NotificationListener", "No session for room $room found")
            }
            send(msg, sessions[room]!!)
        }
    }
    private val isAllPowerOn: Boolean
    get() = Session.getGeneralConfig()[GeneralConfig.CONFIG_ALL_PROJECTS_POWER, "true"].toBoolean()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        if (sbn.packageName == "com.kakao.talk") {
            val wearableExtender = Notification.WearableExtender(sbn.notification)
            for (act in wearableExtender.actions) {
                if (act.remoteInputs!=null && act.remoteInputs.isNotEmpty()) {
                    if (!isAllPowerOn) {
                        return
                    }
                    val notification = sbn.notification
                    val message = notification.extras["android.text"].toString()
                    val sender = notification.extras.getString("android.title").toString()
                    val room = act.title.toString().replaceFirst("답장 (", "").replace(")", "")
                    lastRoom = room
                    val imageHash = encodeIcon(
                            notification.getLargeIcon().loadDrawable(
                                    applicationContext
                            )
                    )
                    if (!sessions.containsKey(room)) {
                        sessions[room] = act
                    }

                    Logger.d("NotificationListenerService", "message : $message sender : $sender nRoom : $room nSession : $act")

                    projectLoader.callEvent("default", "response", arrayOf(room, message, sender, imageHash, replier))
                }
            }
        }
    }

    private fun encodeIcon(icon: Drawable?): Long {
        if (icon != null) {
            val bitDw = icon as BitmapDrawable
            val bitmap = bitDw.bitmap
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            var bitmapByte = stream.toByteArray()
            bitmapByte = Base64.encode(bitmapByte, Base64.DEFAULT)
            return Arrays.hashCode(bitmapByte).toLong()
        }
        return 0
    }
}
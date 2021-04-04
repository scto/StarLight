package com.mooner.starlight.plugincore.plugin

interface Plugin {
    val pluginManager: PluginManager

    val name: String

    fun isEnabled(): Boolean

    fun onEnable() {}

    fun onError(e: Exception) {}

    fun onDisable() {}

    fun onFinish() {}
}
/*
 * ConfigUtils.kt created by Minki Moon(mooner1022) on 2/18/24, 1:50 PM
 * Copyright (c) mooner1022. all rights reserved.
 * This code is licensed under the GNU General Public License v3.0.
 */

package dev.mooner.starlight.plugincore.utils

import dev.mooner.configdsl.DataMap
import dev.mooner.configdsl.MutableDataMap
import dev.mooner.configdsl.utils.toJsonElement
import dev.mooner.starlight.plugincore.config.data.MutableConfig
import dev.mooner.starlight.plugincore.config.data.MutableLegacyDataMap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

private fun transformLegacyData(legacyDataMap: MutableLegacyDataMap): MutableDataMap {
    val transformed: MutableDataMap = hashMapOf()
    for ((categoryId, categoryData) in legacyDataMap) {
        transformed[categoryId] = hashMapOf()
        val catTData = transformed[categoryId]!!
        for ((key, value) in categoryData) {
            val tf = value.toJsonElement()
            catTData[key] = tf
        }
    }
    return transformed
}

fun Json.decodeLegacyData(string: String): MutableDataMap {
    val legacyData = decodeFromString<MutableLegacyDataMap>(string)
    return transformLegacyData(legacyData)
}

fun MutableConfig.onSaveConfigAdapter(parentId: String, id: String, data: Any, jsonData: JsonElement) {

    edit {
        category(parentId).setRaw(id, jsonData)
    }
}

fun DataMap.dumpAllData() {
    for ((catId, entry) in this) {
        println("$catId:\\")
        for ((key, value) in entry)
            println("\t $key: $value")
    }
}
package com.github.unclepomedev.houdinivexassist.services

import com.github.unclepomedev.houdinivexassist.types.VexType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger

@Service(Service.Level.PROJECT)
class VexBuiltinVariableProvider {
    private val logger = Logger.getInstance(VexBuiltinVariableProvider::class.java)
    private val builtinTypes: Map<String, VexType> by lazy { loadBuiltinTypes() }

    private fun loadBuiltinTypes(): Map<String, VexType> {
        val resourceStream = javaClass.classLoader.getResourceAsStream("type_inference_data.json")
            ?: run {
                logger.info("type_inference_data.json not found in resources; builtin attribute types unavailable")
                return emptyMap()
            }

        return try {
            resourceStream.reader().use { reader ->
                val mapType = object : TypeToken<Map<String, Map<String, List<String>>>>() {}.type
                val data: Map<String, Map<String, List<String>>> = Gson().fromJson(reader, mapType)

                data.mapNotNull { (name, typeMap) ->
                    if (typeMap.size == 1) {
                        val typeName = typeMap.keys.first()
                        val vexType = VexType.fromString(typeName)
                        if (vexType != VexType.UnknownType) name to vexType else null
                    } else {
                        null // conflicting types -> skip
                    }
                }.toMap()
            }
        } catch (e: Exception) {
            logger.warn("Failed to load type_inference_data.json", e)
            emptyMap()
        }
    }

    fun getBuiltinType(name: String): VexType? {
        return builtinTypes[name]
    }
}

package com.github.unclepomedev.houdinivexassist.services

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

data class VexFunction(val name: String, val args: List<String>, val returnType: String)

@Service(Service.Level.PROJECT)
class VexApiProvider {
    private val logger = Logger.getInstance(VexApiProvider::class.java)
    val functions: List<VexFunction> by lazy { loadApiDump() }
    private val functionNames: Set<String> by lazy { functions.map { it.name }.toSet() }

    private fun loadApiDump(): List<VexFunction> {
        val resourceStream = javaClass.classLoader.getResourceAsStream("vex_api_dump.json")
            ?: return emptyList()

        return try {
            InputStreamReader(resourceStream, StandardCharsets.UTF_8).use { reader ->
                val jsonObject = Gson().fromJson(reader, JsonObject::class.java)

                val funcsObj = jsonObject.getAsJsonObject("Vex")
                    ?.getAsJsonObject("cvex")
                    ?.getAsJsonObject("functions")
                    ?: return emptyList()

                funcsObj.entrySet().mapNotNull { (funcName, element) ->
                    val funcArray = element.takeIf { it.isJsonArray }?.asJsonArray
                    val firstOverload = funcArray?.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
                        ?: return@mapNotNull null

                    val returnType = firstOverload.get("return")?.takeIf { it.isJsonPrimitive }?.asString ?: ""

                    val argsList = firstOverload.get("args")?.takeIf { it.isJsonArray }?.asJsonArray
                        ?.mapNotNull { it.takeIf { arg -> arg.isJsonPrimitive }?.asString }
                        ?: emptyList()

                    VexFunction(funcName, argsList, returnType)
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to load VEX API dump", e)
            emptyList()
        }
    }

    fun hasFunction(functionName: String): Boolean {
        return functionName in functionNames
    }
}

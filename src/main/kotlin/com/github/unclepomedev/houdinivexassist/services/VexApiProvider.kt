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

                funcsObj.entrySet().flatMap { (funcName, element) ->
                    val funcArray = element.takeIf { it.isJsonArray }?.asJsonArray
                        ?: return@flatMap emptyList()

                    funcArray.mapNotNull { overload ->
                        val obj = overload.takeIf { it.isJsonObject }?.asJsonObject
                            ?: return@mapNotNull null

                        val returnType = obj.get("return")?.takeIf { it.isJsonPrimitive }?.asString ?: ""

                        val argsList = obj.get("args")?.takeIf { it.isJsonArray }?.asJsonArray
                            ?.mapNotNull { it.takeIf { arg -> arg.isJsonPrimitive }?.asString }
                            ?: emptyList()

                        VexFunction(funcName, argsList, returnType)
                    }
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

    fun getOverloads(functionName: String): List<VexFunction> {
        return functions.filter { it.name == functionName }
    }
}

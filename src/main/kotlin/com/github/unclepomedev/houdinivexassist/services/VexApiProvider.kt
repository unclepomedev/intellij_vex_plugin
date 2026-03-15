package com.github.unclepomedev.houdinivexassist.services

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.openapi.components.Service
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

data class VexFunction(val name: String, val args: List<String>, val returnType: String)

@Service(Service.Level.PROJECT)
class VexApiProvider {

    val functions: List<VexFunction> by lazy { loadApiDump() }

    private fun loadApiDump(): List<VexFunction> {
        val resultList = mutableListOf<VexFunction>()
        val resourceStream = javaClass.classLoader.getResourceAsStream("vex_api_dump.json") ?: return resultList
        try {
            InputStreamReader(resourceStream, StandardCharsets.UTF_8).use { reader ->
                val gson = Gson()
                val jsonObject = gson.fromJson(reader, JsonObject::class.java)

                val vexObj = jsonObject.getAsJsonObject("Vex")
                val cvexObj = vexObj?.getAsJsonObject("cvex")
                val funcsObj = cvexObj?.getAsJsonObject("functions")

                funcsObj?.entrySet()?.forEach { entry ->
                    val funcName = entry.key
                    val funcArray = entry.value.asJsonArray
                    if (funcArray.size() > 0) {
                        val firstOverload = funcArray[0].asJsonObject
                        val returnType =
                            if (firstOverload.has("return")) firstOverload.get("return").asString else ""

                        val argsList = mutableListOf<String>()
                        if (firstOverload.has("args")) {
                            firstOverload.getAsJsonArray("args").forEach { arg ->
                                argsList.add(arg.asString)
                            }
                        }

                        resultList.add(VexFunction(funcName, argsList, returnType))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return resultList
    }
}

package com.github.unclepomedev.houdinivexassist.services

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

data class VexFunction(val name: String, val args: List<String>, val returnType: String)

private data class ApiDumpDto(
    @SerializedName("Vex") val vex: VexContextDto?
)

private data class VexContextDto(val cvex: CvexContextDto?)
private data class CvexContextDto(val functions: Map<String, List<FunctionOverloadDto>>?)
private data class FunctionOverloadDto(
    @SerializedName("return") val returnType: String?, // avoid reserved word with annotation
    val args: List<String>?
)

@Service(Service.Level.PROJECT)
class VexApiProvider {
    private val logger = Logger.getInstance(VexApiProvider::class.java)
    val functions: List<VexFunction> by lazy { loadApiDump() }
    private val overloadsByName: Map<String, List<VexFunction>> by lazy { functions.groupBy(VexFunction::name) }

    private fun loadApiDump(): List<VexFunction> {
        val resourceStream = javaClass.classLoader.getResourceAsStream("vex_api_dump.json")
            ?: return emptyList()

        return try {
            InputStreamReader(resourceStream, StandardCharsets.UTF_8).use { reader ->
                val dump = Gson().fromJson(reader, ApiDumpDto::class.java)
                val functionsMap = dump?.vex?.cvex?.functions ?: return emptyList()

                functionsMap.flatMap { (funcName, overloads) ->
                    overloads.mapNotNull { overload ->
                        val retType = overload.returnType ?: return@mapNotNull null
                        val argsList = overload.args ?: return@mapNotNull null

                        VexFunction(funcName, argsList, retType)
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to load VEX API dump", e)
            emptyList()
        }
    }

    fun hasFunction(functionName: String): Boolean {
        return overloadsByName.containsKey(functionName)
    }

    fun getOverloads(functionName: String): List<VexFunction> {
        return overloadsByName[functionName].orEmpty()
    }
}

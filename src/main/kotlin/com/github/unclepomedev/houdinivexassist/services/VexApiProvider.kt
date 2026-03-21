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
    private val helpArgNamesCache = java.util.concurrent.ConcurrentHashMap<String, List<List<String>>>()
    private val usageRegex = Regex(":usage:\\s*`.*?\\((.*?)\\)`")

    private fun loadApiDump(): List<VexFunction> {
        val resourceStream = javaClass.classLoader.getResourceAsStream("vex_api_dump.json")
            ?: return emptyList()

        return try {
            resourceStream.reader().use { reader ->
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

    fun getParameterNamesFromHelp(functionName: String, arity: Int): List<String>? {
        val overloads = helpArgNamesCache.computeIfAbsent(functionName) { name ->
            val path = "vex_help/functions/$name.txt"
            val helpText = javaClass.classLoader.getResourceAsStream(path)?.use { stream ->
                InputStreamReader(stream, StandardCharsets.UTF_8).readText()
            } ?: return@computeIfAbsent emptyList()

            val matches = usageRegex.findAll(helpText)

            val result = mutableListOf<List<String>>()
            for (match in matches) {
                val paramsStr = match.groupValues[1].trim()
                if (paramsStr.isEmpty()) {
                    result.add(emptyList())
                    continue
                }
                val params = paramsStr.split(",").map { it.trim() }

                // variadic function like `printf(string format, ...)`
                if (params.last() == "...") {
                    // Exclude type information and get only variable names (excluding modifiers such as & and *)
                    val names = params.dropLast(1).map { param ->
                        param.split("\\s+".toRegex()).last().trimStart('&', '*')
                    }.toMutableList()
                    result.add(names)
                } else {
                    val names = params.map { param ->
                        param.split("\\s+".toRegex()).last().trimStart('&', '*')
                    }
                    result.add(names)
                }
            }
            result
        }

        // Exact match takes priority
        overloads.find { it.size == arity }?.let { return it }
        // For variadic functions, prefer overloads where arity > size
        // This requires tracking variadic status during parsing
        return overloads.filter { arity >= it.size }.maxByOrNull { it.size }
    }
}

package com.github.unclepomedev.houdinivexassist.psi

import com.github.unclepomedev.houdinivexassist.services.VexApiProvider
import com.github.unclepomedev.houdinivexassist.services.VexFunction
import com.github.unclepomedev.houdinivexassist.types.VexType
import com.github.unclepomedev.houdinivexassist.types.VexTypeExtractor
import com.github.unclepomedev.houdinivexassist.types.VexTypeInference
import com.github.unclepomedev.houdinivexassist.types.VexTypePromotion
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

object VexFunctionResolver {
    private const val EXACT_MATCH_WEIGHT = 1000

    /**
     * Finds and returns the VexFunctionDef of the specified function name.
     * When argTypes is provided, resolves by matching type signatures.
     * When only arity is provided, resolves by argument count.
     * Returns null if not found.
     */
    fun resolveFunction(
        element: PsiElement,
        functionName: String,
        arity: Int? = null,
        argTypes: List<VexType>? = null
    ): PsiElement? {
        val file = element.containingFile as? VexFile ?: return null
        val localFunctions = PsiTreeUtil.findChildrenOfType(file, VexFunctionDef::class.java)
        val candidates = localFunctions.filter { it.identifier.text == functionName }

        if (argTypes != null) {
            // Resolve by type signature matching
            return resolveByTypeSignature(candidates, argTypes)
        }

        if (arity == null) return candidates.firstOrNull()
        return candidates.firstOrNull { def ->
            val paramCount = def.parameterListDef?.parameterDefList?.size ?: 0
            paramCount == arity
        } ?: candidates.firstOrNull()
    }

    /**
     * Resolves the parameter types for a function call expression.
     * Checks local functions first, then falls back to API functions.
     * Returns null if the function cannot be resolved.
     */
    fun resolveParameterTypes(element: VexCallExpr): List<VexType>? {
        val funcName = element.identifier.text
        val args = element.argumentList?.exprList ?: return null
        val argTypes = args.map { VexTypeInference.inferType(it) }

        // Try local function first (resolve by type signature)
        val localFunc = resolveFunction(element, funcName, argTypes = argTypes)
        if (localFunc is VexFunctionDef) {
            val params = localFunc.parameterListDef?.parameterDefList ?: return null
            return params.map { VexTypeExtractor.extractType(it) }
        }

        // Try API functions
        val file = element.containingFile as? VexFile ?: return null
        val apiProvider = file.project.getService(VexApiProvider::class.java) ?: return null
        val overloads = apiProvider.getOverloads(funcName)
        if (overloads.isEmpty()) return null

        return findBestApiOverload(overloads, argTypes)
    }

    /**
     * Check if the specified function name actually exists as a standard function or a local function.
     */
    fun isKnownFunction(functionName: String, file: VexFile): Boolean {
        val apiProvider = file.project.getService(VexApiProvider::class.java)
        if (apiProvider?.hasFunction(functionName) == true) return true

        val localFunctions = VexScopeAnalyzer.getLocalFunctionNames(file)
        return functionName in localFunctions
    }

    /**
     * Parses the argument strings of the standard API and converts them to VexType.
     */
    fun parseApiArgType(argString: String): VexType {
        val tokens = argString
            .replace("&", " ")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() && it !in setOf("const", "export") }

        if (tokens.isEmpty()) return VexType.UnknownType

        val (rawType, rawName) = when (tokens.first()) {
            "struct" -> (tokens.getOrNull(1) ?: return VexType.UnknownType) to tokens.getOrNull(2).orEmpty()
            else -> tokens.first() to tokens.getOrNull(1).orEmpty()
        }

        val isArray = rawType.endsWith("[]") || rawName.endsWith("[]")

        val normalizedType = when (val base = rawType.removeSuffix("[]")) {
            "vector3" -> "vector"
            "matrix4" -> "matrix"
            else -> base
        }

        val baseType = VexType.fromString(normalizedType)
        return if (isArray && baseType != VexType.UnknownType) VexType.ArrayType(baseType) else baseType
    }

    /**
     * Calculates the matching score between expected parameter types and actual argument types.
     * Returns null if any argument is completely unassignable.
     * Otherwise, returns a weighted score prioritizing exact matches.
     */
    private fun calculateMatchScore(expectedTypes: List<VexType>, actualTypes: List<VexType>): Int? {
        if (expectedTypes.size != actualTypes.size) return null

        var exactMatches = 0
        var assignableMatches = 0

        for ((expected, actual) in expectedTypes.zip(actualTypes)) {
            if (expected == actual) {
                exactMatches++
                assignableMatches++
            } else if (expected == VexType.UnknownType || actual == VexType.UnknownType || VexTypePromotion.isAssignable(
                    expected,
                    actual
                )
            ) {
                assignableMatches++
            } else {
                return null
            }
        }

        // use exactMatches to tie-break between multiple valid overloads.
        return if (assignableMatches == actualTypes.size) {
            Int.MAX_VALUE
        } else {
            exactMatches * EXACT_MATCH_WEIGHT + assignableMatches
        }
    }

    /**
     * Calculates a partial match score for error reporting when no overload is perfectly assignable.
     */
    private fun calculatePartialMatchScore(expectedTypes: List<VexType>, actualTypes: List<VexType>): Int {
        var exactMatches = 0
        var assignableMatches = 0

        for ((expected, actual) in expectedTypes.zip(actualTypes)) {
            if (expected == actual) {
                exactMatches++
            }
            if (expected == VexType.UnknownType || actual == VexType.UnknownType || VexTypePromotion.isAssignable(
                    expected,
                    actual
                )
            ) {
                assignableMatches++
            }
        }
        return exactMatches * EXACT_MATCH_WEIGHT + assignableMatches
    }

    private fun resolveByTypeSignature(
        candidates: Collection<VexFunctionDef>,
        argTypes: List<VexType>
    ): VexFunctionDef? {
        val sameArity = candidates.filter {
            (it.parameterListDef?.parameterDefList?.size ?: 0) == argTypes.size
        }
        if (sameArity.isEmpty()) return null

        val fullyAssignable = sameArity.filter { candidate ->
            val paramTypes =
                candidate.parameterListDef?.parameterDefList?.map { VexTypeExtractor.extractType(it) } ?: emptyList()
            calculateMatchScore(paramTypes, argTypes) == Int.MAX_VALUE
        }

        if (fullyAssignable.isNotEmpty()) {
            return fullyAssignable.maxByOrNull { candidate ->
                val paramTypes = candidate.parameterListDef?.parameterDefList?.map { VexTypeExtractor.extractType(it) }
                    ?: emptyList()
                calculatePartialMatchScore(paramTypes, argTypes)
            }
        }

        return sameArity.maxByOrNull { candidate ->
            val paramTypes =
                candidate.parameterListDef?.parameterDefList?.map { VexTypeExtractor.extractType(it) } ?: emptyList()
            calculatePartialMatchScore(paramTypes, argTypes)
        }
    }

    private fun findBestApiOverload(overloads: List<VexFunction>, argTypes: List<VexType>): List<VexType>? {
        val sameArityOverloads = overloads.filter { it.args.size == argTypes.size }
        if (sameArityOverloads.isEmpty()) return null

        val parsedOverloads = sameArityOverloads.associateWith { it.args.map { arg -> parseApiArgType(arg) } }

        // look for an overload where all arguments are assignable.
        // If multiple are completely assignable (due to implicit casting), tie-break using exact matches.
        val fullyAssignable = sameArityOverloads.filter { overload ->
            val paramTypes = parsedOverloads[overload]!!
            calculateMatchScore(paramTypes, argTypes) == Int.MAX_VALUE
        }

        if (fullyAssignable.isNotEmpty()) {
            val best = fullyAssignable.maxByOrNull { overload ->
                val paramTypes = parsedOverloads[overload]!!
                calculatePartialMatchScore(
                    paramTypes,
                    argTypes
                )
            } ?: fullyAssignable.first()
            return parsedOverloads[best]!!
        }

        val bestPartial = sameArityOverloads.maxByOrNull { overload ->
            val paramTypes = parsedOverloads[overload]!!
            calculatePartialMatchScore(paramTypes, argTypes)
        } ?: return null

        return parsedOverloads[bestPartial]!!
    }
}

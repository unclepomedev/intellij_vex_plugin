package com.github.unclepomedev.houdinivexassist.psi

import com.github.unclepomedev.houdinivexassist.services.VexApiProvider
import com.github.unclepomedev.houdinivexassist.services.VexFunction
import com.github.unclepomedev.houdinivexassist.types.VexType
import com.github.unclepomedev.houdinivexassist.types.VexTypeExtractor
import com.github.unclepomedev.houdinivexassist.types.VexTypeInference
import com.github.unclepomedev.houdinivexassist.types.VexTypePromotion
import com.intellij.psi.PsiElement

object VexFunctionResolver {
    private const val EXACT_MATCH_WEIGHT = 1000

    /**
     * Finds and returns the VexFunctionDef of the specified function name. When argTypes is
     * provided, resolves by matching type signatures. When only arity is provided, resolves by
     * argument count. Returns null if not found.
     */
    fun resolveFunction(
        element: PsiElement,
        functionName: String,
        arity: Int? = null,
        argTypes: List<VexType>? = null,
    ): PsiElement? {
        val candidates =
            VexScopeAnalyzer.getVisibleFunctions(element).filter {
                it.identifier.text == functionName
            }
        if (candidates.isEmpty()) return null

        return when {
            argTypes != null -> resolveByTypeSignature(candidates, argTypes)
            arity != null ->
                candidates.firstOrNull { it.parameterCount == arity } ?: candidates.firstOrNull()
            else -> candidates.firstOrNull()
        }
    }

    /**
     * Resolves the parameter types for a function call expression. Checks local functions first,
     * then falls back to API functions. Returns null if the function cannot be resolved.
     */
    fun resolveParameterTypes(element: VexCallExpr): List<VexType>? {
        val funcName = element.identifier.text
        val args = element.argumentList?.exprList ?: return null
        val argTypes = args.map(VexTypeInference::inferType)

        // Try local function first (resolve by type signature)
        val localFunc = resolveFunction(element, funcName, argTypes = argTypes)
        if (localFunc is VexFunctionDef) {
            return localFunc.parameterTypes
        }

        // Try API functions
        val file = element.containingFile as? VexFile ?: return null
        val apiProvider = file.project.getService(VexApiProvider::class.java) ?: return null
        val overloads = apiProvider.getOverloads(funcName)
        if (overloads.isEmpty()) return null

        return findBestApiOverload(overloads, argTypes)
    }

    /**
     * Resolves the parameter names for a function call expression. Checks local functions first,
     * then falls back to API functions. Returns an empty list if no match is found.
     */
    fun resolveParameterNames(element: VexCallExpr): List<String> {
        val funcName = element.identifier.text
        val args = element.argumentList?.exprList ?: return emptyList()
        val argTypes = args.map(VexTypeInference::inferType)

        // Try to resolve to local function
        val resolved = resolveFunction(element, funcName, args.size, argTypes)
        if (resolved is VexFunctionDef) {
            return resolved.parameterNames
        }

        // Try to resolve to standard function
        val apiProvider =
            element.project.getService(VexApiProvider::class.java) ?: return emptyList()
        return apiProvider.getParameterNamesFromHelp(funcName, args.size).orEmpty()
    }

    /**
     * Check if the specified function name actually exists as a standard function or a local
     * function.
     */
    fun isKnownFunction(functionName: String, file: VexFile): Boolean {
        val apiProvider = file.project.getService(VexApiProvider::class.java)
        return apiProvider?.hasFunction(functionName) == true ||
            functionName in VexScopeAnalyzer.getLocalFunctionNames(file)
    }

    /** Parses the argument strings of the standard API and converts them to VexType. */
    fun parseApiArgType(argString: String): VexType {
        val tokens =
            argString.replace("&", " ").split("\\s+".toRegex()).filter {
                it.isNotBlank() && it !in setOf("const", "export")
            }

        if (tokens.isEmpty()) return VexType.UnknownType

        val (rawType, rawName) =
            when (tokens.first()) {
                "struct" ->
                    (tokens.getOrNull(1) ?: return VexType.UnknownType) to
                        tokens.getOrNull(2).orEmpty()
                else -> tokens.first() to tokens.getOrNull(1).orEmpty()
            }

        val isArray = rawType.endsWith("[]") || rawName.endsWith("[]")
        val normalizedType =
            when (val base = rawType.removeSuffix("[]")) {
                "vector3" -> "vector"
                "matrix4" -> "matrix"
                else -> base
            }

        val baseType = VexType.fromString(normalizedType)
        return if (isArray && baseType != VexType.UnknownType) VexType.ArrayType(baseType)
        else baseType
    }

    private data class MatchResult(
        val exactMatches: Int,
        val assignableMatches: Int,
        val isAllAssignable: Boolean,
    )

    private fun evaluateMatch(
        expectedTypes: List<VexType>,
        actualTypes: List<VexType>,
    ): MatchResult? {
        if (expectedTypes.size != actualTypes.size) return null

        var exactMatches = 0
        var assignableMatches = 0

        for ((expected, actual) in expectedTypes.zip(actualTypes)) {
            val isExact = expected == actual
            val isAssignable =
                isExact ||
                    expected == VexType.UnknownType ||
                    actual == VexType.UnknownType ||
                    VexTypePromotion.isAssignable(expected, actual)

            if (isExact) exactMatches++
            if (isAssignable) assignableMatches++ else return null
        }

        return MatchResult(
            exactMatches,
            assignableMatches,
            isAllAssignable = assignableMatches == actualTypes.size,
        )
    }

    /** Calculates a partial match score for error reporting or tie-breaking. */
    private fun calculatePartialMatchScore(
        expectedTypes: List<VexType>,
        actualTypes: List<VexType>,
    ): Int {
        var exactMatches = 0
        var assignableMatches = 0

        for ((expected, actual) in expectedTypes.zip(actualTypes)) {
            if (expected == actual) exactMatches++
            if (
                expected == VexType.UnknownType ||
                    actual == VexType.UnknownType ||
                    VexTypePromotion.isAssignable(expected, actual)
            ) {
                assignableMatches++
            }
        }
        return exactMatches * EXACT_MATCH_WEIGHT + assignableMatches
    }

    private fun resolveByTypeSignature(
        candidates: Collection<VexFunctionDef>,
        argTypes: List<VexType>,
    ): VexFunctionDef? {
        val sameArity = candidates.filter { it.parameterCount == argTypes.size }
        if (sameArity.isEmpty()) return null

        val fullyAssignable = sameArity.filter { candidate ->
            val match = evaluateMatch(candidate.parameterTypes, argTypes)
            match?.isAllAssignable == true
        }

        val pool = fullyAssignable.ifEmpty { sameArity }
        return pool.maxByOrNull { calculatePartialMatchScore(it.parameterTypes, argTypes) }
    }

    private fun findBestApiOverload(
        overloads: List<VexFunction>,
        argTypes: List<VexType>,
    ): List<VexType>? {
        val candidateOverloads = overloads.filter {
            it.args.size == argTypes.size || (it.isVariadic && argTypes.size >= it.args.size)
        }
        if (candidateOverloads.isEmpty()) return null

        val parsedOverloads = candidateOverloads.associateWith {
            it.resolveExpectedArgTypes(argTypes.size)
        }

        val fullyAssignable = candidateOverloads.filter { overload ->
            val match = evaluateMatch(parsedOverloads.getValue(overload), argTypes)
            match?.isAllAssignable == true
        }

        if (fullyAssignable.isNotEmpty()) {
            val best =
                fullyAssignable.maxByOrNull {
                    calculatePartialMatchScore(parsedOverloads.getValue(it), argTypes)
                } ?: fullyAssignable.first()
            return parsedOverloads[best]
        }

        val bestPartial =
            candidateOverloads.maxByOrNull {
                calculatePartialMatchScore(parsedOverloads.getValue(it), argTypes)
            } ?: return null

        return parsedOverloads[bestPartial]
    }

    private fun VexFunction.resolveExpectedArgTypes(targetArity: Int): List<VexType> {
        val baseArgs = args.map(::parseApiArgType)
        return if (isVariadic && targetArity > baseArgs.size) {
            baseArgs + List(targetArity - baseArgs.size) { VexType.UnknownType }
        } else {
            baseArgs
        }
    }

    private val VexFunctionDef.parameterCount: Int
        get() = parameterListDef?.parameterDefList?.size ?: 0

    private val VexFunctionDef.parameterTypes: List<VexType>
        get() = parameterListDef?.parameterDefList?.map(VexTypeExtractor::extractType).orEmpty()

    private val VexFunctionDef.parameterNames: List<String>
        get() = parameterListDef?.parameterDefList?.map { it.identifier.text }.orEmpty()
}

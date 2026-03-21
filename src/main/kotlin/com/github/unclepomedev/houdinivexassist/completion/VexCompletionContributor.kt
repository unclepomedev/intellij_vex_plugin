package com.github.unclepomedev.houdinivexassist.completion

import com.github.unclepomedev.houdinivexassist.lang.VexLanguage
import com.github.unclepomedev.houdinivexassist.psi.*
import com.github.unclepomedev.houdinivexassist.services.VexApiProvider
import com.github.unclepomedev.houdinivexassist.types.VexType
import com.github.unclepomedev.houdinivexassist.types.VexTypeInference
import com.intellij.codeInsight.completion.*
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class VexCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(VexLanguage.INSTANCE),
            VexCompletionProvider()
        )
    }
}

private class VexCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val element = parameters.position
        val memberExpr = PsiTreeUtil.getParentOfType(element, VexMemberExpr::class.java)
        if (memberExpr != null) {
            VexDotAccessCompletionHandler.handle(memberExpr.expr, result)
        } else {
            VexStandardCompletionHandler.handle(parameters, result)
        }
    }
}

private object VexDotAccessCompletionHandler {
    fun handle(baseExpr: VexExpr, result: CompletionResultSet) {
        when (val baseType = VexTypeInference.inferType(baseExpr)) {
            is VexType.StructType -> {
                addStructMemberCompletions(baseExpr, baseType.name, result)
            }

            VexType.Vector2Type, VexType.VectorType, VexType.Vector4Type,
            VexType.Matrix3Type, VexType.MatrixType -> {
                addSwizzleCompletions(baseType, result)
            }

            else -> {
                // Primitive types (int, float, string) or unknown types have no members
            }
        }
    }

    private fun addStructMemberCompletions(context: PsiElement, structName: String, result: CompletionResultSet) {
        val file = context.containingFile

        val targetStruct = CachedValuesManager.getCachedValue(file) {
            CachedValueProvider.Result.create(
                PsiTreeUtil.findChildrenOfType(file, VexStructDef::class.java),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }.find { it.identifier?.text == structName } ?: return

        targetStruct.structMemberList.forEach { member ->
            val typeString = member.typeRef.text ?: "unknown"
            member.declarationItemList.forEach { declItem ->
                val memberName = declItem.identifier.text
                if (memberName.isNotEmpty()) {
                    result.addElement(VexLookupElementFactory.createStructMember(memberName, typeString))
                }
            }
        }
    }

    private fun addSwizzleCompletions(baseType: VexType, result: CompletionResultSet) {
        val maxDim = getDimension(baseType) ?: return
        val isMatrix = baseType is VexType.Matrix3Type || baseType is VexType.MatrixType

        val candidates = mutableListOf("x", "y", "z", "w", "xy", "xyz", "xyzw")
        if (!isMatrix) {
            candidates.addAll(listOf("r", "g", "b", "a", "u", "v", "uv", "rg", "rgb", "rgba"))
        }

        val componentIndices = mapOf(
            'x' to 1, 'r' to 1, 'u' to 1,
            'y' to 2, 'g' to 2, 'v' to 2,
            'z' to 3, 'b' to 3,
            'w' to 4, 'a' to 4
        )

        candidates.forEach { swizzle ->
            val isValid = swizzle.all { char ->
                val requiredDim = componentIndices[char] ?: 5
                requiredDim <= maxDim
            }

            if (isValid) {
                val typeText = getSwizzleTypeText(swizzle.length)
                result.addElement(VexLookupElementFactory.createSwizzle(swizzle, typeText))
            }
        }
    }

    private fun getDimension(type: VexType): Int? {
        return when (type) {
            VexType.Vector2Type -> 2
            VexType.VectorType, VexType.Matrix3Type -> 3
            VexType.Vector4Type, VexType.MatrixType -> 4
            else -> null
        }
    }

    private fun getSwizzleTypeText(length: Int): String {
        return when (length) {
            1 -> "float"
            2 -> "vector2"
            3 -> "vector"
            4 -> "vector4"
            else -> "unknown"
        }
    }
}

private object VexStandardCompletionHandler {
    fun handle(parameters: CompletionParameters, result: CompletionResultSet) {
        addPrimitiveTypes(result)
        val localFunctionNames = addLocalFunctions(parameters, result)
        addStandardFunctions(parameters, result, localFunctionNames)
        addLocalVariablesAndParameters(parameters, result)
    }

    private fun addPrimitiveTypes(result: CompletionResultSet) {
        val primitiveTypes = listOf(
            "int", "float", "vector", "vector2", "vector4",
            "matrix", "matrix2", "matrix3", "string", "void",
            "bsdf", "dict", "struct", "function"
        )

        primitiveTypes.forEach { typeName ->
            result.addElement(VexLookupElementFactory.createKeyword(typeName))
        }
    }

    private fun addLocalVariablesAndParameters(parameters: CompletionParameters, result: CompletionResultSet) {
        val element = parameters.position
        val currentOffset = element.textOffset
        var currentScope = VexScopeAnalyzer.findDeclarationScope(element)
        val seenNames = mutableSetOf<String>()

        // Traverse scopes upwards and delegate the extraction
        while (currentScope != null) {
            addVariablesFromScope(currentScope, currentOffset, result, seenNames)
            addParametersFromScope(currentScope, result, seenNames)
            currentScope = VexScopeAnalyzer.findDeclarationScope(currentScope.parent)
        }
    }

    private fun addVariablesFromScope(
        scope: PsiElement,
        currentOffset: Int,
        result: CompletionResultSet,
        seenNames: MutableSet<String>
    ) {
        VexScopeAnalyzer.getDeclarationsInScope(scope)
            .filter { it.textOffset < currentOffset }
            .map { it.identifier.text }
            .filter { it.isNotEmpty() && seenNames.add(it) }
            .forEach { name -> result.addElement(VexLookupElementFactory.createVariable(name, isParameter = false)) }
    }

    private fun addParametersFromScope(
        scope: PsiElement,
        result: CompletionResultSet,
        seenNames: MutableSet<String>
    ) {
        VexScopeAnalyzer.getParametersForScope(scope)
            .map { it.identifier.text }
            .filter { it.isNotEmpty() && seenNames.add(it) }
            .forEach { name -> result.addElement(VexLookupElementFactory.createVariable(name, isParameter = true)) }
    }

    private fun addLocalFunctions(parameters: CompletionParameters, result: CompletionResultSet): Set<String> {
        val file = parameters.originalFile

        val localFunctions = CachedValuesManager.getCachedValue(file) {
            CachedValueProvider.Result.create(
                PsiTreeUtil.findChildrenOfType(file, VexFunctionDef::class.java),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }

        val addedNames = mutableSetOf<String>()

        localFunctions.forEach { localFunc ->
            val funcName = localFunc.identifier.text ?: return@forEach
            val hasArgs = localFunc.parameterListDef?.parameterDefList?.isNotEmpty() == true

            result.addElement(VexLookupElementFactory.createLocalFunction(funcName, hasArgs))
            addedNames.add(funcName)
        }
        return addedNames
    }

    private fun addStandardFunctions(
        parameters: CompletionParameters,
        result: CompletionResultSet,
        skipNames: Set<String>
    ) {
        val vexApiProvider = parameters.originalFile.project.getService(VexApiProvider::class.java) ?: return

        vexApiProvider.functions.forEach { func ->
            if (func.name in skipNames) return@forEach
            result.addElement(VexLookupElementFactory.createStandardFunction(func))
        }
    }
}

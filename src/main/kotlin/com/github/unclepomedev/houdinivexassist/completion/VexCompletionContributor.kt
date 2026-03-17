package com.github.unclepomedev.houdinivexassist.completion

import com.github.unclepomedev.houdinivexassist.lang.VexLanguage
import com.github.unclepomedev.houdinivexassist.psi.*
import com.github.unclepomedev.houdinivexassist.services.VexApiProvider
import com.github.unclepomedev.houdinivexassist.types.VexType
import com.github.unclepomedev.houdinivexassist.types.VexTypeInference
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
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
            handleDotAccessCompletion(memberExpr.expr, result)
            return
        }

        val localFunctionNames = addLocalFunctions(parameters, result)
        addStandardFunctions(parameters, result, localFunctionNames)
        addLocalVariablesAndParameters(parameters, result)
    }

    private fun handleDotAccessCompletion(baseExpr: VexExpr, result: CompletionResultSet) {
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
        val targetStruct = PsiTreeUtil.findChildrenOfType(file, VexStructDef::class.java)
            .find { it.identifier?.text == structName } ?: return

        targetStruct.structMemberList.forEach { member ->
            val typeString = member.firstChild?.text ?: "unknown"
            member.declarationItemList.forEach { declItem ->
                val memberName = declItem.identifier.text
                if (memberName.isNotEmpty()) {
                    result.addElement(
                        LookupElementBuilder.create(memberName)
                            .withIcon(AllIcons.Nodes.Field)
                            .withTypeText(typeString)
                    )
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
                result.addElement(
                    LookupElementBuilder.create(swizzle)
                        .withIcon(AllIcons.Nodes.Property)
                        .withTypeText(getSwizzleTypeText(swizzle.length))
                )
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
            .forEach { name -> result.addElement(createVariableLookup(name)) }
    }

    private fun addParametersFromScope(scope: PsiElement, result: CompletionResultSet, seenNames: MutableSet<String>) {
        VexScopeAnalyzer.getParametersForScope(scope)
            .map { it.identifier.text }
            .filter { it.isNotEmpty() && seenNames.add(it) }
            .forEach { name -> result.addElement(createParameterLookup(name)) }
    }

    private fun createVariableLookup(name: String): LookupElementBuilder {
        return LookupElementBuilder.create(name)
            .withIcon(AllIcons.Nodes.Variable)
            .withTypeText("local variable")
    }

    private fun createParameterLookup(name: String): LookupElementBuilder {
        return LookupElementBuilder.create(name)
            .withIcon(AllIcons.Nodes.Parameter)
            .withTypeText("parameter")
    }

    private fun addLocalFunctions(parameters: CompletionParameters, result: CompletionResultSet): Set<String> {
        val containingFile = parameters.originalFile
        val localFunctions = PsiTreeUtil.findChildrenOfType(containingFile, VexFunctionDef::class.java)
        val addedNames = mutableSetOf<String>()

        localFunctions.forEach { localFunc ->
            val funcName = localFunc.identifier.text ?: return@forEach
            val hasArgs = localFunc.parameterListDef?.parameterDefList?.isNotEmpty() == true
            val tailText = if (hasArgs) "(...)" else "()"

            val lookupElement = LookupElementBuilder.create(funcName)
                .withTypeText("local")
                .withTailText(tailText, true)
                .withIcon(AllIcons.Nodes.Method)
                .withInsertHandler(FunctionInsertHandler(hasArgs = hasArgs))

            result.addElement(lookupElement)
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

            val lookupElement = LookupElementBuilder.create(func.name)
                .withTypeText(func.returnType)
                .withTailText("(${func.args.joinToString(", ")})", true)
                .withIcon(AllIcons.Nodes.Function)
                .withInsertHandler(FunctionInsertHandler(hasArgs = func.args.isNotEmpty()))

            result.addElement(lookupElement)
        }
    }
}

/**
 * Handler that inserts `()` and moves the cursor to the appropriate position immediately after completion is confirmed.
 */
private class FunctionInsertHandler(private val hasArgs: Boolean) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val editor = context.editor
        val document = context.document
        val offset = context.tailOffset

        if (context.completionChar == '(') {
            context.setAddCompletionChar(false)
        }

        val hasParen = offset < document.textLength && document.charsSequence[offset] == '('
        val hasClosingParen = hasParen && offset + 1 < document.textLength && document.charsSequence[offset + 1] == ')'

        if (!hasParen) {
            document.insertString(offset, "()")
        }

        // Calculate cursor movement:
        // Move outside (+2) ONLY IF the function has NO arguments AND it has cleanly closed empty parentheses `()`.
        // Otherwise, move inside (+1).
        val moveOffset = if (!hasArgs && (!hasParen || hasClosingParen)) offset + 2 else offset + 1
        editor.caretModel.moveToOffset(moveOffset)
    }
}

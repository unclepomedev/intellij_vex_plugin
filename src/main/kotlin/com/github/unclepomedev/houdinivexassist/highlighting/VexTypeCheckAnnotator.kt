package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.psi.*
import com.github.unclepomedev.houdinivexassist.services.VexApiProvider
import com.github.unclepomedev.houdinivexassist.services.VexFunction
import com.github.unclepomedev.houdinivexassist.types.*
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

class VexTypeCheckAnnotator : Annotator {
    companion object {
        private const val EXACT_MATCH_WEIGHT = 1000
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is VexDeclarationItem -> checkDeclarationInitialization(element, holder)
            is VexAssignExpr -> checkAssignmentExpression(element, holder)
            is VexCallExpr -> checkFunctionArguments(element, holder)
        }
    }

    private fun checkDeclarationInitialization(element: VexDeclarationItem, holder: AnnotationHolder) {
        val expr = element.expr ?: return
        val declaredType = VexTypeExtractor.extractType(element)

        if (declaredType is VexType.ArrayType && isLiteralInitializerList(expr)) {
            return
        }

        val inferredType = VexTypeInference.inferType(expr)

        if (!VexTypePromotion.isAssignable(declaredType, inferredType)) {
            holder.newAnnotation(
                HighlightSeverity.ERROR,
                "Incompatible types: cannot assign '${inferredType.displayName}' to '${declaredType.displayName}'"
            )
                .range(expr.textRange)
                .create()
        }
    }

    private fun checkAssignmentExpression(element: VexAssignExpr, holder: AnnotationHolder) {
        val exprs = element.exprList
        if (exprs.size < 2) return

        val lhsExpr = exprs[0]
        val rhsExpr = exprs[1]

        val lhsType = VexTypeInference.inferType(lhsExpr)
        val rhsType = VexTypeInference.inferType(rhsExpr)
        val operatorKind = element.operatorKind

        if (operatorKind == null) {
            if (lhsType is VexType.ArrayType && isLiteralInitializerList(rhsExpr)) {
                return
            }
            if (!VexTypePromotion.isAssignable(lhsType, rhsType)) {
                holder.newAnnotation(
                    HighlightSeverity.ERROR,
                    "Incompatible types: cannot assign '${rhsType.displayName}' to '${lhsType.displayName}'"
                )
                    .range(rhsExpr.textRange)
                    .create()
            }
            return
        }

        val promotedType = VexTypePromotion.promote(lhsType, rhsType, operatorKind)

        if (promotedType == VexType.UnknownType) {
            holder.newAnnotation(
                HighlightSeverity.ERROR,
                "Invalid operation: cannot apply operator to '${lhsType.displayName}' and '${rhsType.displayName}'"
            )
                .range(element.textRange)
                .create()
            return
        }

        if (!VexTypePromotion.isAssignable(lhsType, promotedType)) {
            holder.newAnnotation(
                HighlightSeverity.ERROR,
                "Incompatible types: cannot assign result of type '${promotedType.displayName}' to '${lhsType.displayName}'"
            )
                .range(element.textRange)
                .create()
        }
    }

    private fun checkFunctionArguments(element: VexCallExpr, holder: AnnotationHolder) {
        val funcName = element.identifier.text
        val args = element.argumentList?.exprList ?: return
        val argTypes = args.map { VexTypeInference.inferType(it) }

        val paramTypes = resolveParameterTypes(element, funcName, args.size)
            ?: return // unknown function or can't resolve — skip

        for (i in args.indices) {
            if (i >= paramTypes.size) break
            val expected = paramTypes[i]
            val actual = argTypes[i]
            if (expected == VexType.UnknownType || actual == VexType.UnknownType) continue
            if (!VexTypePromotion.isAssignable(expected, actual)) {
                holder.newAnnotation(
                    HighlightSeverity.ERROR,
                    "Type mismatch in argument ${i + 1}: expected '${expected.displayName}', got '${actual.displayName}'"
                )
                    .range(args[i].textRange)
                    .create()
            }
        }
    }

    private fun resolveParameterTypes(element: VexCallExpr, funcName: String, arity: Int): List<VexType>? {
        // local function
        val localFunc = VexFunctionResolver.resolveFunction(element, funcName, arity)
        if (localFunc is VexFunctionDef) {
            val params = localFunc.parameterListDef?.parameterDefList ?: return null
            return params.map { VexTypeExtractor.extractType(it) }
        }

        // API functions
        val file = element.containingFile as? VexFile ?: return null
        val apiProvider = file.project.getService(VexApiProvider::class.java) ?: return null
        val overloads = apiProvider.getOverloads(funcName)
        if (overloads.isEmpty()) return null

        return findBestApiOverload(overloads, element)
    }

    private fun findBestApiOverload(overloads: List<VexFunction>, element: VexCallExpr): List<VexType>? {
        val args = element.argumentList?.exprList ?: return null
        val argTypes = args.map { VexTypeInference.inferType(it) }
        // Cache parsed parameter types per overload
        val parsedOverloads = overloads.associateWith { it.args.map { arg -> parseApiArgType(arg) } }

        // Find an overload where all arguments are assignable
        for (overload in overloads) {
            if (overload.args.size != args.size) continue
            val paramTypes = parsedOverloads[overload]!!
            val allMatch = paramTypes.zip(argTypes).all { (expected, actual) ->
                expected == VexType.UnknownType || actual == VexType.UnknownType ||
                        VexTypePromotion.isAssignable(expected, actual)
            }
            if (allMatch) return paramTypes
        }

        // No perfect match — pick the overload with the most matching arguments for best error reporting
        val sameArityOverloads = overloads.filter { it.args.size == args.size }
        if (sameArityOverloads.isEmpty()) return null

        val best = sameArityOverloads.maxByOrNull { overload ->
            val paramTypes = parsedOverloads[overload]!!
            val exactMatches = paramTypes.zip(argTypes).count { (expected, actual) ->
                expected == actual
            }
            val assignableMatches = paramTypes.zip(argTypes).count { (expected, actual) ->
                expected == VexType.UnknownType || actual == VexType.UnknownType ||
                        VexTypePromotion.isAssignable(expected, actual)
            }
            exactMatches * EXACT_MATCH_WEIGHT + assignableMatches
        } ?: return null
        return parsedOverloads[best]!!
    }

    private fun parseApiArgType(argString: String): VexType {
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
     * Determines whether the expression is a pure {...} literal (initializer list).
     */
    private fun isLiteralInitializerList(expr: PsiElement?): Boolean {
        return expr is VexPrimaryExpr && expr.vectorLiteral != null
    }
}

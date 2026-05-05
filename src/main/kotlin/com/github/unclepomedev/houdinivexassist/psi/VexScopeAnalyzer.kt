package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil

object VexScopeAnalyzer {

    /**
     * Inactive ranges for the root file and every transitively-included file, computed
     * with parent-file macro context propagated across includes. Required because
     * [VexPreprocessorEvaluator.isActive] caches per-file ranges with no parent context,
     * which mis-evaluates `#ifdef PARENT_MACRO` inside included headers.
     */
    private fun activityMap(rootFile: PsiFile): Map<String, List<TextRange>> {
        return CachedValuesManager.getCachedValue(rootFile) {
            val map = VexInactiveRangeAnalyzer.analyzeWithIncludes(rootFile)
            CachedValueProvider.Result.create(
                map,
                PsiModificationTracker.MODIFICATION_COUNT,
                VexIncludeResolver.includePathTracker
            )
        }
    }

    private fun isActiveIn(element: PsiElement, map: Map<String, List<TextRange>>): Boolean {
        val file = element.containingFile ?: return true
        val ranges = map[VexFile.getFileKey(file)] ?: return VexPreprocessorEvaluator.isActive(element)
        val offset = element.textOffset
        return ranges.none { offset in it.startOffset until it.endOffset }
    }

    /**
     * Recursively retrieves the specified VexFile and all files it includes.
     */
    fun getIncludedFiles(file: PsiFile): List<VexFile> {
        return CachedValuesManager.getCachedValue(file) {
            val result = mutableListOf<VexFile>()
            val visited = mutableSetOf<String>()

            fun visit(current: PsiFile) {
                val path = VexFile.getFileKey(current)
                if (!visited.add(path)) return

                val vexFile = VexSyntheticFileProvider.getAsVexFile(current) ?: return

                result.add(vexFile)

                val includes = PsiTreeUtil.findChildrenOfType(vexFile, VexIncludeDirective::class.java)
                for (include in includes) {
                    if (!VexPreprocessorEvaluator.isActive(include)) continue
                    val resolved = VexIncludeResolver.resolveIncludeFile(include, vexFile)
                    if (resolved != null) visit(resolved)
                }
            }

            visit(file)

            CachedValueProvider.Result.create(
                result,
                PsiModificationTracker.MODIFICATION_COUNT,
                VexIncludeResolver.includePathTracker
            )
        }
    }

    fun findDeclarationScope(element: PsiElement?): PsiElement? {
        if (element == null) return null
        return PsiTreeUtil.getParentOfType(
            element,
            VexBlock::class.java,
            VexStructDef::class.java,
            VexFile::class.java
        )
    }

    fun getDeclarationsInScope(scope: PsiElement): List<VexDeclarationItem> {
        return CachedValuesManager.getCachedValue(scope) {
            val decls = PsiTreeUtil.findChildrenOfType(scope, VexDeclarationItem::class.java)
                .filter { findDeclarationScope(it) == scope && VexPreprocessorEvaluator.isActive(it) }
            CachedValueProvider.Result.create(decls, scope, PsiModificationTracker.MODIFICATION_COUNT)
        }
    }

    fun getParametersForScope(scope: PsiElement): List<VexParameterDef> {
        if (scope !is VexBlock || scope.parent !is VexFunctionDef) return emptyList()
        val funcDef = scope.parent as VexFunctionDef
        val paramList = funcDef.parameterListDef ?: return emptyList()

        return CachedValuesManager.getCachedValue(paramList) {
            val params = PsiTreeUtil.findChildrenOfType(paramList, VexParameterDef::class.java)
                .filter { VexPreprocessorEvaluator.isActive(it) }
            CachedValueProvider.Result.create(params, paramList)
        }
    }

    fun getVisibleFunctions(element: PsiElement): List<VexFunctionDef> {
        val file = element.containingFile as? VexFile ?: return emptyList()
        return CachedValuesManager.getCachedValue(file) {
            val map = activityMap(file)
            val funcs = getIncludedFiles(file).flatMap { f ->
                PsiTreeUtil.findChildrenOfType(f, VexFunctionDef::class.java)
            }.filter { isActiveIn(it, map) }
            CachedValueProvider.Result.create(
                funcs,
                PsiModificationTracker.MODIFICATION_COUNT,
                VexIncludeResolver.includePathTracker
            )
        }
    }

    fun getVisibleFunctionsGrouped(element: PsiElement): Map<String, List<VexFunctionDef>> {
        val file = element.containingFile as? VexFile ?: return emptyMap()
        return CachedValuesManager.getCachedValue(file) {
            val map = activityMap(file)
            val funcs = getIncludedFiles(file).flatMap { f ->
                PsiTreeUtil.findChildrenOfType(f, VexFunctionDef::class.java)
            }.filter { isActiveIn(it, map) }
                .groupBy { it.identifier.text }

            CachedValueProvider.Result.create(
                funcs,
                PsiModificationTracker.MODIFICATION_COUNT,
                VexIncludeResolver.includePathTracker
            )
        }
    }

    fun getVisibleStructsGrouped(element: PsiElement): Map<String, List<VexStructDef>> {
        val file = element.containingFile as? VexFile ?: return emptyMap()
        return CachedValuesManager.getCachedValue(file) {
            val map = activityMap(file)
            val structs = getIncludedFiles(file).flatMap { f ->
                PsiTreeUtil.findChildrenOfType(f, VexStructDef::class.java)
            }.filter { isActiveIn(it, map) }
                .groupBy { it.identifier?.text ?: "" }

            CachedValueProvider.Result.create(
                structs,
                PsiModificationTracker.MODIFICATION_COUNT,
                VexIncludeResolver.includePathTracker
            )
        }
    }

    fun getVisibleStructs(element: PsiElement): List<VexStructDef> {
        val file = element.containingFile as? VexFile ?: return emptyList()
        return CachedValuesManager.getCachedValue(file) {
            val map = activityMap(file)
            val structs = getIncludedFiles(file).flatMap { f ->
                PsiTreeUtil.findChildrenOfType(f, VexStructDef::class.java)
            }.filter { isActiveIn(it, map) }
            CachedValueProvider.Result.create(
                structs,
                PsiModificationTracker.MODIFICATION_COUNT,
                VexIncludeResolver.includePathTracker
            )
        }
    }

    fun getVisibleVariables(element: PsiElement): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        var currentScope = findDeclarationScope(element)
        while (currentScope != null) {
            if (currentScope is VexFile) {
                val decls = getDeclarationsInScope(currentScope)
                result.addAll(decls.filter { it.textOffset < element.textOffset })

                val includedFiles = getIncludedFiles(currentScope)
                for (incFile in includedFiles) {
                    if (incFile != currentScope) {
                        result.addAll(getDeclarationsInScope(incFile))
                    }
                }
            } else {
                val decls = getDeclarationsInScope(currentScope)
                result.addAll(decls.filter { it.textOffset < element.textOffset })

                val params = getParametersForScope(currentScope)
                result.addAll(params)
            }
            currentScope = findDeclarationScope(currentScope.parent)
        }
        return result
    }

    fun getLocalFunctionNames(file: VexFile): Set<String> {
        return CachedValuesManager.getCachedValue(file) {
            val map = activityMap(file)
            val names = getIncludedFiles(file).flatMap { f ->
                PsiTreeUtil.findChildrenOfType(f, VexFunctionDef::class.java)
            }.filter { isActiveIn(it, map) }.mapNotNull { it.identifier.text }.toSet()
            CachedValueProvider.Result.create(
                names,
                PsiModificationTracker.MODIFICATION_COUNT,
                VexIncludeResolver.includePathTracker
            )
        }
    }
}

package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil

object VexScopeAnalyzer {
    /**
     * Helper to fetch and filter active elements of type [T] across all included files.
     */
    private inline fun <reified T : PsiElement> collectActiveElements(
        file: VexFile,
        activityMap: Map<String, List<TextRange>>
    ): List<T> {
        return getIncludedFiles(file)
            .flatMap { PsiTreeUtil.findChildrenOfType(it, T::class.java) }
            .filter { isActiveIn(it, activityMap) }
    }

    /**
     * Standardized caching provider for file-level scope queries.
     */
    private fun <T> createCacheResult(value: T): CachedValueProvider.Result<T> {
        return CachedValueProvider.Result.create(
            value,
            PsiModificationTracker.MODIFICATION_COUNT,
            VexIncludeResolver.includePathTracker
        )
    }

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
                VexIncludeResolver.includePathTracker,
                ProjectRootModificationTracker.getInstance(rootFile.project)
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
     * Earliest `#include` directive offset (in [rootFile]) that transitively brings in each
     * included file. The root file itself maps to 0. Used to order cross-file declarations
     * by their include site rather than treating any other-file symbol as "prior".
     */
    fun getIncludeSiteOffsets(rootFile: PsiFile): Map<String, Int> {
        return CachedValuesManager.getCachedValue(rootFile) {
            val result = mutableMapOf<String, Int>()
            val rootKey = VexFile.getFileKey(rootFile)
            result[rootKey] = 0

            fun visit(current: PsiFile, siteOffset: Int) {
                val vexFile = VexSyntheticFileProvider.getAsVexFile(current) ?: return
                val includes = PsiTreeUtil.findChildrenOfType(vexFile, VexIncludeDirective::class.java)
                for (include in includes) {
                    if (!VexPreprocessorEvaluator.isActive(include)) continue
                    val resolved = VexIncludeResolver.resolveIncludeFile(include, vexFile) ?: continue
                    val key = VexFile.getFileKey(resolved)
                    // For top-level includes, siteOffset is the include directive offset in the root file.
                    // For transitive includes, propagate the original root-level site offset.
                    val effectiveOffset = if (current === rootFile) include.textOffset else siteOffset
                    val prev = result[key]
                    if (prev == null || effectiveOffset < prev) {
                        result[key] = effectiveOffset
                        visit(resolved, effectiveOffset)
                    }
                }
            }

            visit(rootFile, 0)

            CachedValueProvider.Result.create(
                result,
                PsiModificationTracker.MODIFICATION_COUNT,
                VexIncludeResolver.includePathTracker,
                ProjectRootModificationTracker.getInstance(rootFile.project)
            )
        }
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
            createCacheResult(result)
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

    fun getDeclarationsInScope(scope: PsiElement, activityMap: Map<String, List<TextRange>>): List<VexDeclarationItem> {
        return PsiTreeUtil.findChildrenOfType(scope, VexDeclarationItem::class.java)
            .filter { findDeclarationScope(it) == scope && isActiveIn(it, activityMap) }
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

    fun getParametersForScope(scope: PsiElement, activityMap: Map<String, List<TextRange>>): List<VexParameterDef> {
        if (scope !is VexBlock || scope.parent !is VexFunctionDef) return emptyList()
        val funcDef = scope.parent as VexFunctionDef
        val paramList = funcDef.parameterListDef ?: return emptyList()
        return PsiTreeUtil.findChildrenOfType(paramList, VexParameterDef::class.java)
            .filter { isActiveIn(it, activityMap) }
    }

    fun getVisibleFunctions(element: PsiElement): List<VexFunctionDef> {
        val file = element.containingFile as? VexFile ?: return emptyList()
        return CachedValuesManager.getCachedValue(file) {
            createCacheResult(collectActiveElements<VexFunctionDef>(file, activityMap(file)))
        }
    }

    fun getVisibleFunctionsGrouped(element: PsiElement): Map<String, List<VexFunctionDef>> {
        val file = element.containingFile as? VexFile ?: return emptyMap()
        return CachedValuesManager.getCachedValue(file) {
            val grouped = collectActiveElements<VexFunctionDef>(file, activityMap(file)).groupBy { it.identifier.text }
            createCacheResult(grouped)
        }
    }

    fun getVisibleStructsGrouped(element: PsiElement): Map<String, List<VexStructDef>> {
        val file = element.containingFile as? VexFile ?: return emptyMap()
        return CachedValuesManager.getCachedValue(file) {
            val grouped =
                collectActiveElements<VexStructDef>(file, activityMap(file)).groupBy { it.identifier?.text ?: "" }
            createCacheResult(grouped)
        }
    }

    fun getVisibleStructs(element: PsiElement): List<VexStructDef> {
        val file = element.containingFile as? VexFile ?: return emptyList()
        return CachedValuesManager.getCachedValue(file) {
            createCacheResult(collectActiveElements<VexStructDef>(file, activityMap(file)))
        }
    }

    fun getLocalFunctionNames(file: VexFile): Set<String> {
        return CachedValuesManager.getCachedValue(file) {
            val names =
                collectActiveElements<VexFunctionDef>(file, activityMap(file)).mapNotNull { it.identifier.text }.toSet()
            createCacheResult(names)
        }
    }

    fun getVisibleVariables(element: PsiElement): List<PsiElement> {
        val rootFile = element.containingFile as? VexFile
        val map = rootFile?.let { activityMap(it) }
        val result = mutableListOf<PsiElement>()

        fun fetchDeclarations(scope: PsiElement) =
            if (map != null) getDeclarationsInScope(scope, map) else getDeclarationsInScope(scope)

        fun fetchParameters(scope: PsiElement) =
            if (map != null) getParametersForScope(scope, map) else getParametersForScope(scope)

        var currentScope = findDeclarationScope(element)
        while (currentScope != null) {
            result.addAll(fetchDeclarations(currentScope).filter { it.textOffset < element.textOffset })
            if (currentScope is VexFile) {
                getIncludedFiles(currentScope).forEach { incFile ->
                    if (incFile != currentScope) {
                        result.addAll(fetchDeclarations(incFile))
                    }
                }
            } else {
                result.addAll(fetchParameters(currentScope))
            }
            currentScope = findDeclarationScope(currentScope.parent)
        }
        return result
    }
}

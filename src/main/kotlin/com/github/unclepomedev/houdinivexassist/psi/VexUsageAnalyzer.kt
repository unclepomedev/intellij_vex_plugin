package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil

object VexUsageAnalyzer {
    fun getAllProjectVexFiles(project: com.intellij.openapi.project.Project): List<VexFile> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            val virtualFiles = com.intellij.psi.search.FileTypeIndex.getFiles(
                com.github.unclepomedev.houdinivexassist.lang.VexFileType,
                com.intellij.psi.search.GlobalSearchScope.projectScope(project)
            )
            val files = virtualFiles.mapNotNull { 
                com.intellij.psi.PsiManager.getInstance(project).findFile(it) as? VexFile 
            }
            CachedValueProvider.Result.create(files, PsiModificationTracker.MODIFICATION_COUNT)
        }
    }

    fun getFunctionCalls(file: VexFile, funcName: String): List<VexCallExpr> {
        val cache = CachedValuesManager.getCachedValue(file) {
            val calls = PsiTreeUtil.findChildrenOfType(file, VexCallExpr::class.java)
            val grouped = calls.groupBy { it.identifier.text }
            CachedValueProvider.Result.create(grouped, PsiModificationTracker.MODIFICATION_COUNT)
        }
        return cache[funcName] ?: emptyList()
    }

    fun getVariableUsages(scope: PsiElement, varName: String): List<VexPrimaryExpr> {
        val cache = CachedValuesManager.getCachedValue(scope) {
            val usages = PsiTreeUtil.findChildrenOfType(scope, VexPrimaryExpr::class.java)
            val grouped = usages
                .mapNotNull { expr -> expr.identifier?.text?.let { name -> name to expr } }
                .groupBy({ it.first }, { it.second })
            CachedValueProvider.Result.create(grouped, PsiModificationTracker.MODIFICATION_COUNT)
        }
        return cache[varName] ?: emptyList()
    }

    fun getMemberAccesses(file: VexFile, memberName: String): List<VexMemberExpr> {
        val cache = CachedValuesManager.getCachedValue(file) {
            val accesses = PsiTreeUtil.findChildrenOfType(file, VexMemberExpr::class.java)
            val grouped = accesses
                .mapNotNull { expr -> expr.identifier?.text?.let { name -> name to expr } }
                .groupBy({ it.first }, { it.second })
            CachedValueProvider.Result.create(grouped, PsiModificationTracker.MODIFICATION_COUNT)
        }
        return cache[memberName] ?: emptyList()
    }
}

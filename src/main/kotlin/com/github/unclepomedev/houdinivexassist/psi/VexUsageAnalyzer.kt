package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil

object VexUsageAnalyzer {
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
            val grouped = usages.groupBy { it.identifier?.text ?: "" }
            CachedValueProvider.Result.create(grouped, PsiModificationTracker.MODIFICATION_COUNT)
        }
        return cache[varName] ?: emptyList()
    }

    fun getMemberAccesses(file: VexFile, memberName: String): List<VexMemberExpr> {
        val cache = CachedValuesManager.getCachedValue(file) {
            val accesses = PsiTreeUtil.findChildrenOfType(file, VexMemberExpr::class.java)
            val grouped = accesses.groupBy { it.identifier?.text ?: "" }
            CachedValueProvider.Result.create(grouped, PsiModificationTracker.MODIFICATION_COUNT)
        }
        return cache[memberName] ?: emptyList()
    }
}

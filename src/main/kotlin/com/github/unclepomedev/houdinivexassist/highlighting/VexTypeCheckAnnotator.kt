package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.psi.*
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement

class VexTypeCheckAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val reporter = VexTypeCheckReporter(holder)

        when (element) {
            is VexDeclarationItem -> VexDeclarationChecker(reporter).check(element)
            is VexAssignExpr -> VexAssignmentChecker(reporter).check(element)
            is VexCallExpr -> VexCallChecker(reporter).check(element)
            is VexReturnStatement -> VexReturnChecker(reporter).check(element)
        }
    }
}

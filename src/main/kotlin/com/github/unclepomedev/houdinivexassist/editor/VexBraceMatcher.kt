package com.github.unclepomedev.houdinivexassist.editor

import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

class VexBraceMatcher : PairedBraceMatcher {

    override fun getPairs(): Array<BracePair> {
        return arrayOf(
            BracePair(VexTypes.LBRACE, VexTypes.RBRACE, true),
            BracePair(VexTypes.LPAREN, VexTypes.RPAREN, false),
            BracePair(VexTypes.LBRACK, VexTypes.RBRACK, false)
        )
    }

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean {
        return true
    }

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int {
        return openingBraceOffset
    }
}

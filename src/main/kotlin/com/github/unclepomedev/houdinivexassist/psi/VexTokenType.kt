package com.github.unclepomedev.houdinivexassist.psi

import com.github.unclepomedev.houdinivexassist.lang.VexLanguage
import com.intellij.psi.tree.IElementType

class VexTokenType(debugName: String) : IElementType(debugName, VexLanguage.INSTANCE) {
    override fun toString(): String = "VexTokenType." + super.toString()
}

package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.psi.tree.IElementType
import com.github.unclepomedev.houdinivexassist.VexLanguage

class VexTokenType(debugName: String) : IElementType(debugName, VexLanguage.INSTANCE) {
    override fun toString(): String = "VexTokenType." + super.toString()
}

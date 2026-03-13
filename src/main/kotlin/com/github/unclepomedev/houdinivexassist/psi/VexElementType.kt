package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.psi.tree.IElementType
import com.github.unclepomedev.houdinivexassist.VexLanguage

class VexElementType(debugName: String) : IElementType(debugName, VexLanguage.INSTANCE)

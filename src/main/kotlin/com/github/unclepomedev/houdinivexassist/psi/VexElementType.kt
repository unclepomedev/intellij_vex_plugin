package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.psi.tree.IElementType
import com.github.unclepomedev.houdinivexassist.lang.VexLanguage

class VexElementType(debugName: String) : IElementType(debugName, VexLanguage.INSTANCE)

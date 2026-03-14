package com.github.unclepomedev.houdinivexassist

import com.intellij.lang.Language

class VexLanguage private constructor() : Language("VEX") {
    companion object {
        val INSTANCE: VexLanguage = VexLanguage()
    }
}

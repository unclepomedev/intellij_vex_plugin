package com.github.unclepomedev.houdinivexassist.psi.impl

import com.github.unclepomedev.houdinivexassist.psi.VexAttributeExpr
import com.intellij.lang.ASTNode

abstract class VexAttributeExprMixin(node: ASTNode) : VexExprImpl(node), VexAttributeExpr

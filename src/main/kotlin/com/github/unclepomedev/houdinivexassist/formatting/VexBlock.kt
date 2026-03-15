package com.github.unclepomedev.houdinivexassist.formatting

import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock

class VexBlock(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    private val spacingBuilder: SpacingBuilder
) : AbstractBlock(node, wrap, alignment) {

    override fun buildChildren(): List<Block> {
        val blocks = mutableListOf<Block>()
        var child = myNode.firstChildNode
        while (child != null) {
            if (child.elementType != TokenType.WHITE_SPACE && child.textRange.length > 0) {
                blocks.add(VexBlock(child, null, null, spacingBuilder))
            }
            child = child.treeNext
        }
        return blocks
    }

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        val type = myNode.elementType
        if (
            type == VexTypes.BLOCK ||
            type == VexTypes.STRUCT_DEF ||
            type == VexTypes.PARAMETER_LIST_DEF ||
            type == VexTypes.PARAMETER_LIST_SIG ||
            type == VexTypes.ARGUMENT_LIST
        ) {
            return ChildAttributes(Indent.getNormalIndent(), null)
        }
        return ChildAttributes(Indent.getNoneIndent(), null)
    }

    override fun getIndent(): Indent? {
        val parentType = myNode.treeParent?.elementType
        val type = myNode.elementType

        return when (parentType) {
            VexTypes.STRUCT_DEF -> {
                if (type == VexTypes.STRUCT || type == VexTypes.IDENTIFIER ||
                    type == VexTypes.LBRACE || type == VexTypes.RBRACE
                ) {
                    Indent.getNoneIndent()
                } else {
                    Indent.getNormalIndent()
                }
            }

            VexTypes.BLOCK -> {
                if (type == VexTypes.LBRACE || type == VexTypes.RBRACE) {
                    Indent.getNoneIndent()
                } else {
                    Indent.getNormalIndent()
                }
            }

            VexTypes.PARAMETER_LIST_DEF, // function parameter
            VexTypes.PARAMETER_LIST_SIG,  // struct method signature
            VexTypes.ARGUMENT_LIST -> {  // function argument
                Indent.getNormalIndent()
            }

            else -> Indent.getNoneIndent()
        }
    }

    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        return spacingBuilder.getSpacing(this, child1, child2)
    }

    override fun isLeaf(): Boolean {
        return myNode.firstChildNode == null
    }
}

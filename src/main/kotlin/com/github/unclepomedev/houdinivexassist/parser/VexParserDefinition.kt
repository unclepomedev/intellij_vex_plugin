package com.github.unclepomedev.houdinivexassist.parser

import com.github.unclepomedev.houdinivexassist.lang.VexLanguage
import com.github.unclepomedev.houdinivexassist.lexer.VexLexerAdapter
import com.github.unclepomedev.houdinivexassist.psi.VexFile
import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

private val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)
private val COMMENTS = TokenSet.create(VexTypes.LINE_COMMENT, VexTypes.BLOCK_COMMENT)
private val STRINGS = TokenSet.create(VexTypes.STRING)
private val FILE = IFileElementType(VexLanguage.INSTANCE)

class VexParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = VexLexerAdapter()
    override fun createParser(project: Project?): PsiParser = VexParser()
    override fun getFileNodeType(): IFileElementType = FILE
    override fun getWhitespaceTokens(): TokenSet = WHITE_SPACES
    override fun getCommentTokens(): TokenSet = COMMENTS
    override fun getStringLiteralElements(): TokenSet = STRINGS

    override fun createElement(node: ASTNode): PsiElement = VexTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = VexFile(viewProvider)
}

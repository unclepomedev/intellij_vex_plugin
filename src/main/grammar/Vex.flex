package com.github.unclepomedev.houdinivexassist.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.github.unclepomedev.houdinivexassist.psi.VexTypes;
import com.intellij.psi.TokenType;

%%

%class VexLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

WHITE_SPACE=[\ \t\n\r\f]+
COMMENT="/"\*([^*]|\*+[^*/])*\*"/"
NUMBER=[0-9]+(\.[0-9]*)?
STRING=(\"[^\"]*\")|(\'[^\']*\')
ATTRIBUTE=[fiuvsp]?@[a-zA-Z0-9_]+
IDENTIFIER=[a-zA-Z_][a-zA-Z0-9_]*
MACRO="#".*

%%

<YYINITIAL> {
  {WHITE_SPACE}       { return TokenType.WHITE_SPACE; }
  {COMMENT}           { return VexTypes.COMMENT; }
  {NUMBER}            { return VexTypes.NUMBER; }
  {STRING}            { return VexTypes.STRING; }
  {ATTRIBUTE}         { return VexTypes.ATTRIBUTE; }
  {MACRO}             { return VexTypes.MACRO; }

  "int"|"float"|"vector"|"vector2"|"vector4"|"matrix"|"matrix3"|"string"|"void" { return VexTypes.TYPE;}

  "{"                 { return VexTypes.LBRACE; }
  "}"                 { return VexTypes.RBRACE; }
  "("                 { return VexTypes.LPAREN; }
  ")"                 { return VexTypes.RPAREN; }
  ";"                 { return VexTypes.SEMICOLON; }
  ","                 { return VexTypes.COMMA; }
  "="                 { return VexTypes.EQUALS; }

  {IDENTIFIER}        { return VexTypes.IDENTIFIER; }

  [^]                 { return TokenType.BAD_CHARACTER; }
}

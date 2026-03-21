package com.github.unclepomedev.houdinivexassist.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.github.unclepomedev.houdinivexassist.psi.VexTypes;
import com.intellij.psi.TokenType;

%%

%public
%class VexLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

WHITE_SPACE=[\ \t\n\r\f]+
COMMENT=("//"[^\r\n]*)|("/"\*([^*]|\*+[^*/])*\*"/")
NUMBER=([0-9]+(\.[0-9]*)?|\.[0-9]+)([eE][+-]?[0-9]+)?|(0[xX][0-9a-fA-F]+)
STRING=(\"[^\r\n\"]*\")|(\'[^\r\n\']*\')
UNCLOSED_STRING=(\"[^\r\n\"]*)|(\'[^\r\n\']*)
ATTRIBUTE=[fiuvsp]?(\[\])?\@[a-zA-Z0-9_]+
IDENTIFIER=[a-zA-Z_][a-zA-Z0-9_]*
MACRO="#".*

%%

<YYINITIAL> {
  {WHITE_SPACE}       { return TokenType.WHITE_SPACE; }
  {COMMENT}           { return VexTypes.COMMENT; }
  {NUMBER}            { return VexTypes.NUMBER; }
  {STRING}            { return VexTypes.STRING; }
  {UNCLOSED_STRING}   { return VexTypes.UNCLOSED_STRING; }
  {ATTRIBUTE}         { return VexTypes.ATTRIBUTE; }
  {MACRO}             { return VexTypes.MACRO; }

  "int"               { return VexTypes.INT_KW; }
  "float"             { return VexTypes.FLOAT_KW; }
  "vector"            { return VexTypes.VECTOR_KW; }
  "vector2"           { return VexTypes.VECTOR2_KW; }
  "vector4"           { return VexTypes.VECTOR4_KW; }
  "matrix"            { return VexTypes.MATRIX_KW; }
  "matrix3"           { return VexTypes.MATRIX3_KW; }
  "string"            { return VexTypes.STRING_KW; }
  "void"              { return VexTypes.VOID_KW; }
  "bsdf"              { return VexTypes.BSDF_KW; }
  "dict"              { return VexTypes.DICT_KW; }

  "{"                 { return VexTypes.LBRACE; }
  "}"                 { return VexTypes.RBRACE; }
  "("                 { return VexTypes.LPAREN; }
  ")"                 { return VexTypes.RPAREN; }
  "["                 { return VexTypes.LBRACK; }
  "]"                 { return VexTypes.RBRACK; }
  "->"                { return VexTypes.ARROW; }
  ";"                 { return VexTypes.SEMICOLON; }
  ","                 { return VexTypes.COMMA; }
  "."                 { return VexTypes.DOT; }
  "+="                { return VexTypes.PLUSEQ; }
  "-="                { return VexTypes.MINUSEQ; }
  "*="                { return VexTypes.MULEQ; }
  "/="                { return VexTypes.DIVEQ; }
  "="                 { return VexTypes.EQUALS; }
  "=="                { return VexTypes.EQEQ; }
  "!="                { return VexTypes.NEQ; }
  "<"                 { return VexTypes.LT; }
  ">"                 { return VexTypes.GT; }
  "<="                { return VexTypes.LE; }
  ">="                { return VexTypes.GE; }
  "++"                { return VexTypes.PLUSPLUS; }
  "--"                { return VexTypes.MINUSMINUS; }
  "+"                 { return VexTypes.PLUS; }
  "-"                 { return VexTypes.MINUS; }
  "*"                 { return VexTypes.MUL; }
  "/"                 { return VexTypes.DIV; }
  "%"                 { return VexTypes.MOD; }
  "&&"                { return VexTypes.ANDAND; }
  "||"                { return VexTypes.OROR; }
  "!"                 { return VexTypes.NOT; }
  "?"                 { return VexTypes.QMARK; }
  ":"                 { return VexTypes.COLON; }
  "%="                { return VexTypes.MODEQ; }
  "&="                { return VexTypes.ANDEQ; }
  "|="                { return VexTypes.OREQ; }
  "^="                { return VexTypes.XOREQ; }
  "<<="               { return VexTypes.LSHIFTEQ; }
  ">>="               { return VexTypes.RSHIFTEQ; }
  "<<"                { return VexTypes.LSHIFT; }
  ">>"                { return VexTypes.RSHIFT; }
  "&"                 { return VexTypes.BITAND; }
  "|"                 { return VexTypes.BITOR; }
  "^"                 { return VexTypes.BITXOR; }
  "~"                 { return VexTypes.BITNOT; }

  "if"                { return VexTypes.IF; }
  "else"              { return VexTypes.ELSE; }
  "for"               { return VexTypes.FOR; }
  "foreach"           { return VexTypes.FOREACH; }
  "while"             { return VexTypes.WHILE; }
  "do"                { return VexTypes.DO; }
  "break"             { return VexTypes.BREAK; }
  "continue"          { return VexTypes.CONTINUE; }
  "return"            { return VexTypes.RETURN; }
  "struct"            { return VexTypes.STRUCT; }
  "export"            { return VexTypes.EXPORT; }
  "function"          { return VexTypes.FUNCTION; }

  {IDENTIFIER}        { return VexTypes.IDENTIFIER; }

  [^]                 { return TokenType.BAD_CHARACTER; }
}

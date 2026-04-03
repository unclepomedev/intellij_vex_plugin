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

%state IN_INCLUDE
%state IN_DEFINE
%state IN_DEFINE_AFTER_NAME
%state IN_DEFINE_PARAMS
%state IN_DEFINE_BODY
%state IN_PP_IDENTIFIER

WHITE_SPACE=[\ \t\n\r\f]+
COMMENT=("//"[^\r\n]*)|("/"\*([^*]|\*+[^*/])*\*"/")
NUMBER=([0-9]+(\.[0-9]*)?|\.[0-9]+)([eE][+-]?[0-9]+)?|(0[xX][0-9a-fA-F]+)
STRING=(\"[^\r\n\"]*\")|(\'[^\r\n\']*\')
UNCLOSED_STRING=(\"[^\r\n\"]*)|(\'[^\r\n\']*)
ATTRIBUTE=[fiuvsp34md]?(\[\])?\@[a-zA-Z0-9_]+
IDENTIFIER=[a-zA-Z_][a-zA-Z0-9_]*
INCLUDE_KW="#"[ \t]*"include"

%%

<YYINITIAL> {
  {WHITE_SPACE}       { return TokenType.WHITE_SPACE; }
  {COMMENT}           { return VexTypes.COMMENT; }
  {NUMBER}            { return VexTypes.NUMBER; }
  {STRING}            { return VexTypes.STRING; }
  {UNCLOSED_STRING}   { return VexTypes.UNCLOSED_STRING; }
  {ATTRIBUTE}         { return VexTypes.ATTRIBUTE; }
  {INCLUDE_KW}        { yybegin(IN_INCLUDE); return VexTypes.INCLUDE_KW; }
  "#" [^\r\n]*        { 
                          String text = yytext().toString();
                          if (text.matches("^#[ \\t]*include([ \\t]+.*|)$")) {
                              int includeIdx = text.indexOf("include");
                              yypushback(yylength() - (includeIdx + 7));
                              yybegin(IN_INCLUDE);
                              return VexTypes.INCLUDE_KW;
                          }
                          if (text.matches("^#[ \\t]*define[ \\t]+[a-zA-Z_]\\w*.*$")) {
                              int defineIdx = text.indexOf("define");
                              yypushback(yylength() - (defineIdx + 6));
                              yybegin(IN_DEFINE);
                              return VexTypes.DEFINE_KW;
                          }
                          if (text.matches("^#[ \\t]*ifdef[ \\t]+[a-zA-Z_]\\w*.*$")) {
                              int idx = text.indexOf("ifdef");
                              yypushback(yylength() - (idx + 5));
                              yybegin(IN_PP_IDENTIFIER);
                              return VexTypes.PP_IFDEF_KW;
                          }
                          if (text.matches("^#[ \\t]*ifndef[ \\t]+[a-zA-Z_]\\w*.*$")) {
                              int idx = text.indexOf("ifndef");
                              yypushback(yylength() - (idx + 6));
                              yybegin(IN_PP_IDENTIFIER);
                              return VexTypes.PP_IFNDEF_KW;
                          }
                          if (text.matches("^#[ \\t]*undef[ \\t]+[a-zA-Z_]\\w*.*$")) {
                              int idx = text.indexOf("undef");
                              yypushback(yylength() - (idx + 5));
                              yybegin(IN_PP_IDENTIFIER);
                              return VexTypes.PP_UNDEF_KW;
                          }
                          if (text.matches("^#[ \\t]*if[ \\t]+.*$")) {
                              int idx = text.indexOf("if");
                              yypushback(yylength() - (idx + 2));
                              yybegin(IN_DEFINE_BODY);
                              return VexTypes.PP_IF_KW;
                          }
                          if (text.matches("^#[ \\t]*elif[ \\t]+.*$")) {
                              int idx = text.indexOf("elif");
                              yypushback(yylength() - (idx + 4));
                              yybegin(IN_DEFINE_BODY);
                              return VexTypes.PP_ELIF_KW;
                          }
                          if (text.matches("^#[ \\t]*else[ \\t]*$")) {
                              return VexTypes.PP_ELSE_KW;
                          }
                          if (text.matches("^#[ \\t]*endif[ \\t]*$")) {
                              return VexTypes.PP_ENDIF_KW;
                          }
                          return VexTypes.MACRO; 
                      }

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
  "chop"              { return VexTypes.CHOP_KW; }
  "cop2"              { return VexTypes.COP2_KW; }
  "cvex"              { return VexTypes.CVEX_KW; }
  "displace"          { return VexTypes.DISPLACE_KW; }
  "fog"               { return VexTypes.FOG_KW; }
  "image3d"           { return VexTypes.IMAGE3D_KW; }
  "light"             { return VexTypes.LIGHT_KW; }
  "shadow"            { return VexTypes.SHADOW_KW; }
  "sop"               { return VexTypes.SOP_KW; }
  "surface"           { return VexTypes.SURFACE_KW; }

  {IDENTIFIER}        { return VexTypes.IDENTIFIER; }

  [^]                 { return TokenType.BAD_CHARACTER; }
}

<IN_INCLUDE> {
  [ \t]+              { return TokenType.WHITE_SPACE; }
  "<" [^\r\n>]* ">"   { yybegin(YYINITIAL); return VexTypes.SYS_STRING; }
  "<" [^\r\n>]*       { yybegin(YYINITIAL); return VexTypes.UNCLOSED_SYS_STRING; }
  {STRING}            { yybegin(YYINITIAL); return VexTypes.STRING; }
  {UNCLOSED_STRING}   { yybegin(YYINITIAL); return VexTypes.UNCLOSED_STRING; }
  [^]                 { yybegin(YYINITIAL); yypushback(1); }
}

<IN_DEFINE> {
  [ \t]+              { return TokenType.WHITE_SPACE; }
  {IDENTIFIER}        { yybegin(IN_DEFINE_AFTER_NAME); return VexTypes.IDENTIFIER; }
  [^]                 { yybegin(YYINITIAL); yypushback(1); }
}

<IN_DEFINE_AFTER_NAME> {
  "("                 { yybegin(IN_DEFINE_PARAMS); return VexTypes.LPAREN; }
  [ \t]+              { yybegin(IN_DEFINE_BODY); return TokenType.WHITE_SPACE; }
  [\r\n]              { yybegin(YYINITIAL); yypushback(1); }
  [^]                 { yybegin(IN_DEFINE_BODY); yypushback(1); }
}

<IN_DEFINE_PARAMS> {
  [ \t]+              { return TokenType.WHITE_SPACE; }
  {IDENTIFIER}        { return VexTypes.IDENTIFIER; }
  ","                 { return VexTypes.COMMA; }
  ")"                 { yybegin(IN_DEFINE_BODY); return VexTypes.RPAREN; }
  [^]                 { yybegin(YYINITIAL); yypushback(1); }
}

<IN_PP_IDENTIFIER> {
  [ \t]+              { return TokenType.WHITE_SPACE; }
  {IDENTIFIER}        { yybegin(YYINITIAL); return VexTypes.IDENTIFIER; }
  [^]                 { yybegin(YYINITIAL); yypushback(1); }
}

<IN_DEFINE_BODY> {
  [ \t]+              { return TokenType.WHITE_SPACE; }
  [^\r\n \t][^\r\n]*  { yybegin(YYINITIAL); return VexTypes.MACRO_BODY; }
  [\r\n]              { yybegin(YYINITIAL); yypushback(1); }
  [^]                 { yybegin(YYINITIAL); yypushback(1); }
}

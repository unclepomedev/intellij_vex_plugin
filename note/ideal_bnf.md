regret grouping the types under the single term "TYPE"

```bnf
{
  parserClass="com.github.unclepomedev.houdinivexassist.parser.VexParser"
  extends="com.intellij.extapi.psi.ASTWrapperPsiElement"

  psiClassPrefix="Vex"
  psiImplClassSuffix="Impl"
  psiPackage="com.github.unclepomedev.houdinivexassist.psi"
  psiImplPackage="com.github.unclepomedev.houdinivexassist.psi.impl"

  elementTypeHolderClass="com.github.unclepomedev.houdinivexassist.psi.VexTypes"
  elementTypeClass="com.github.unclepomedev.houdinivexassist.psi.VexElementType"
  tokenTypeClass="com.github.unclepomedev.houdinivexassist.psi.VexTokenType"

  generateTokenAccessors=true

  tokens=[
    SPACE='regexp:\s+'
    COMMENT='regexp://.*|/\*([^*]|\*+[^*/])*\*+/'
    NUMBER='regexp:([0-9]+(\.[0-9]*)?|\.[0-9]+)([eE][+-]?[0-9]+)?|(0[xX][0-9a-fA-F]+)'
    STRING="regexp:\"[^\"]*\"|'[^']*'"
    UNCLOSED_STRING="regexp:\"[^\r\n\"]*|'[^\r\n']*"
    ATTRIBUTE="regexp:[fiuvsp]?(\\[\\])?@\\w+"
    MACRO='regexp:#.*'

    KW_INT="int"
    KW_FLOAT="float"
    KW_VECTOR2="vector2"
    KW_VECTOR="vector"
    KW_VECTOR4="vector4"
    KW_MATRIX3="matrix3"
    KW_MATRIX="matrix"
    KW_STRING="string"
    KW_VOID="void"
    KW_BSDF="bsdf"
    KW_DICT="dict"

    IDENTIFIER="regexp:[a-zA-Z_]\w*"

    LBRACE="{"
    RBRACE="}"
    LPAREN="("
    RPAREN=")"
    LBRACK="["
    RBRACK="]"
    ARROW="->"
    SEMICOLON=";"
    COMMA=","
    DOT="."
    PLUSEQ="+="
    MINUSEQ="-="
    MULEQ="*="
    DIVEQ="/="
    EQUALS="="
    EQEQ="=="
    NEQ="!="
    LT="<"
    GT=">"
    LE="<="
    GE=">="
    PLUSPLUS="++"
    MINUSMINUS="--"
    PLUS="+"
    MINUS="-"
    MUL="*"
    DIV="/"
    MOD="%"
    ANDAND="&&"
    OROR="||"
    NOT="!"
    QMARK="?"
    COLON=":"
    MODEQ="%="
    ANDEQ="&="
    OREQ="|="
    XOREQ="^="
    LSHIFTEQ="<<="
    RSHIFTEQ=">>="
    LSHIFT="<<"
    RSHIFT=">>"
    BITAND="&"
    BITOR="|"
    BITXOR="^"
    BITNOT="~"
    IF="if"
    ELSE="else"
    FOR="for"
    FOREACH="foreach"
    WHILE="while"
    DO="do"
    BREAK="break"
    CONTINUE="continue"
    RETURN="return"
    STRUCT="struct"
    EXPORT="export"
    FUNCTION="function"
  ]
  extends(".*_expr")=expr
  extends(".*_statement")=statement
}

vexFile ::= item_*

private builtin_type ::= KW_INT | KW_FLOAT | KW_VECTOR2 | KW_VECTOR | KW_VECTOR4
                       | KW_MATRIX3 | KW_MATRIX | KW_STRING | KW_VOID | KW_BSDF | KW_DICT

type_ref ::= builtin_type | IDENTIFIER

private name_identifier ::= IDENTIFIER

private item_ ::= MACRO | struct_def | function_def | statement | COMMENT | fallback_item

private fallback_item ::= IDENTIFIER | NUMBER | STRING | ATTRIBUTE | builtin_type
                        | LBRACK | RBRACK | ARROW
                        | SEMICOLON | COMMA | DOT
                        | PLUSEQ | MINUSEQ | MULEQ | DIVEQ | MODEQ | ANDEQ | OREQ | XOREQ | LSHIFTEQ | RSHIFTEQ
                        | EQUALS | EQEQ | NEQ | LT | GT | LE | GE
                        | PLUSPLUS | MINUSMINUS | PLUS | MINUS | MUL | DIV | MOD
                        | LSHIFT | RSHIFT | BITAND | BITOR | BITXOR | BITNOT
                        | ANDAND | OROR | NOT | QMARK | COLON
                        | IF | ELSE | FOR | FOREACH | WHILE | DO | BREAK | CONTINUE | RETURN | STRUCT

struct_def ::= STRUCT name_identifier LBRACE struct_member* RBRACE { pin=1 }
struct_member ::= type_ref ( name_identifier ARROW function_signature | declaration_item (COMMA declaration_item)* ) SEMICOLON { pin=1 recoverWhile=struct_member_recover }
private struct_member_recover ::= !(builtin_type | IDENTIFIER | RBRACE)

function_signature ::= name_identifier LPAREN parameter_list_sig? RPAREN
function_def ::= EXPORT? FUNCTION? type_ref name_identifier LPAREN parameter_list_def? RPAREN block { pin="LPAREN" }

parameter_list_def ::= parameter_def ((COMMA | SEMICOLON) parameter_def)*
parameter_def ::= type_ref name_identifier

parameter_list_sig ::= parameter_sig ((COMMA | SEMICOLON) parameter_sig)*
parameter_sig ::= type_ref name_identifier?

block ::= LBRACE item_* RBRACE

statement ::= block
            | if_statement
            | for_statement
            | foreach_statement
            | while_statement
            | do_while_statement
            | return_statement
            | control_statement
            | declaration_statement
            | expr_statement

if_statement ::= IF LPAREN expr RPAREN statement (ELSE statement)? { pin=1 }

for_statement ::= FOR LPAREN for_init expr_statement expr? RPAREN statement { pin=1 }
private for_init ::= declaration_statement | expr_statement

foreach_statement ::= FOREACH LPAREN foreach_body RPAREN statement { pin=1 }
private foreach_body ::= foreach_var SEMICOLON foreach_var SEMICOLON expr
                       | foreach_var COMMA foreach_var SEMICOLON expr
                       | foreach_var SEMICOLON expr
private foreach_var ::= type_ref? name_identifier

while_statement ::= WHILE LPAREN expr RPAREN statement { pin=1 }
do_while_statement ::= DO statement WHILE LPAREN expr RPAREN SEMICOLON { pin=1 }

return_statement ::= RETURN expr? SEMICOLON { pin=1 }
control_statement ::= (BREAK | CONTINUE) SEMICOLON { pin=1 }

declaration_statement ::= type_ref declaration_item (COMMA declaration_item)* SEMICOLON { pin=1 }
declaration_item ::= name_identifier (LBRACK RBRACK)? (EQUALS expr)? { pin=1 }

expr_statement ::= expr? SEMICOLON

expr ::= assign_expr
       | ternary_expr
       | logical_or_expr
       | logical_and_expr
       | bitwise_or_expr
       | bitwise_xor_expr
       | bitwise_and_expr
       | equality_expr
       | relational_expr
       | shift_expr
       | add_expr
       | mul_expr
       | prefix_expr
       | postfix_expr
       | call_expr
       | member_expr
       | primary_expr

assign_expr ::= expr (EQUALS | PLUSEQ | MINUSEQ | MULEQ | DIVEQ | MODEQ | ANDEQ | OREQ | XOREQ | LSHIFTEQ | RSHIFTEQ) expr { rightAssociative=true }
ternary_expr ::= expr QMARK expr COLON expr { rightAssociative=true }
logical_or_expr ::= expr OROR expr
logical_and_expr ::= expr ANDAND expr
bitwise_or_expr ::= expr BITOR expr
bitwise_xor_expr ::= expr BITXOR expr
bitwise_and_expr ::= expr BITAND expr
equality_expr ::= expr equality_op expr
private equality_op ::= EQEQ | NEQ
relational_expr ::= expr relational_op expr
private relational_op ::= LT | GT | LE | GE
shift_expr ::= expr shift_op expr
private shift_op ::= LSHIFT | RSHIFT
add_expr ::= expr add_op expr
private add_op ::= PLUS | MINUS
mul_expr ::= expr mul_op expr
private mul_op ::= MUL | DIV | MOD
prefix_expr ::= prefix_op expr
private prefix_op ::= PLUSPLUS | MINUSMINUS | PLUS | MINUS | NOT | BITNOT
postfix_expr ::= expr postfix_op
private postfix_op ::= PLUSPLUS | MINUSMINUS

call_expr ::= name_identifier LPAREN argument_list? RPAREN {
  mixin="com.github.unclepomedev.houdinivexassist.psi.impl.VexCallExprMixin"
}
member_expr ::= expr DOT name_identifier
primary_expr ::= name_identifier | NUMBER | STRING | ATTRIBUTE | LPAREN expr RPAREN | vector_literal {
  mixin="com.github.unclepomedev.houdinivexassist.psi.impl.VexPrimaryExprMixin"
}
vector_literal ::= LBRACE argument_list? RBRACE
argument_list ::= expr (COMMA expr)*
```

```jflex
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

  "int"               { return VexTypes.KW_INT; }
  "float"             { return VexTypes.KW_FLOAT; }
  "vector"            { return VexTypes.KW_VECTOR; }
  "vector2"           { return VexTypes.KW_VECTOR2; }
  "vector4"           { return VexTypes.KW_VECTOR4; }
  "matrix"            { return VexTypes.KW_MATRIX; }
  "matrix3"           { return VexTypes.KW_MATRIX3; }
  "string"            { return VexTypes.KW_STRING; }
  "void"              { return VexTypes.KW_VOID; }
  "bsdf"              { return VexTypes.KW_BSDF; }
  "dict"              { return VexTypes.KW_DICT; }

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
```
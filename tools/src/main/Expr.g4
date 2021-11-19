grammar Expr;
/** 起始规则 语法分析器起点 */
prog:	stat+ ;

stat:   expr NEWLINE        # printExpr
    |   ID '=' expr NEWLINE # assign
    |   NEWLINE             # blank
    ;

expr:
        ID '(' expr (',' expr)* ')'  # Method
    |   expr op=('*'|'/') expr       # MulDiv
    |	expr op=('+'|'-') expr       # AddSub
    |   '-' DOUBLE                   # Negative
    |	DOUBLE                       # double
    |   ID                           # id
    |	'(' expr ')'                 # paren
    ;

DOUBLE  : [0-9] + ('.'[0-9]+)? ;
ID  : ([a-zA-Z0-9] | '_' | '.' )+ ;
NEWLINE : '\r'? '\n' |';';
WS      : [ \t]+ -> skip ;
MUL     : '*' ;
DIV     : '/' ;
Add     : '+' ;
SUB     : '-' ;

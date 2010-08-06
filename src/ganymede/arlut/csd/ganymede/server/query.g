/*
   This is the ANTLR grammar for the Ganymede Query Language (GanyQL).

   It is converted into QueryParser.java, QueryLexer.java, and QueryParserTokenTypes.java by
   Terrence Parr's ANTLR tool, as follows:

   java -classpath antlr.jar antlr.Tool query.g

   Created: 23 August 2004

   Module By: Deepak Giridharagopal, deepak@brownman.org
   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2010
   The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

grammar query;

options {
  output=AST;
}

@header {
package arlut.csd.ganymede.server;
}

query :
       select_clause
       from_clause
       where_clause?
       EOF!
       ;

select_clause:
       SELECT^ (OBJECT | (STRING_VALUE (COMMA! STRING_VALUE)*))
       ;

from_clause:
       FROM^ EDITABLE? STRING_VALUE
       ;

where_clause:
       WHERE^
       expression;

expression:
       atom ((AND^ | OR^) atom)*
       ;

atom:
       LPAREN! expression RPAREN!
       | NOT^ atom
       | (STRING_VALUE (UNARY_OPERATOR | BINARY_OPERATOR)) => simple_expression
       | (STRING_VALUE DEREF) => deref_expression
       ;

simple_expression:
       STRING_VALUE (UNARY_OPERATOR^ | (BINARY_OPERATOR^ argument))
       ;

deref_expression:
       STRING_VALUE DEREF^ atom
       ;

argument:
       STRING_VALUE
       | INT_VALUE
       | DECIMAL_VALUE
       | BOOLEAN_VALUE
       ;

/* Lexer section */

BACKSLASH: '\\';

fragment ESC : BACKSLASH
  ( 'n'  { $setText("\n"); }
  | '"'  { $setText("\""); }
  | '\'' { $setText("'");  }
  | BACKSLASH
  );

LPAREN : '(';
RPAREN : ')';
COMMA  : ',';
AND    : 'and';
OR     : 'or';
NOT    : 'not';
SELECT : 'select';
FROM   : 'from';
WHERE  : 'where';
DEREF  : '->';
OBJECT : 'object';
EDITABLE : 'editable';

STRING_VALUE :
        '"' (options {greedy=false;}: (ESC)=> ESC | BACKSLASH | ~'"' )* '"'  |
        '\'' (options {greedy=false;}: (ESC)=> ESC | BACKSLASH | ~'\'' )* '\''
        ;

BOOLEAN_VALUE :
         'true'
         | 'false'
         ;

fragment DIGIT : ('0'..'9') ;
fragment INT_VALUE : '-'? DIGIT+;
fragment DECIMAL_VALUE : INT_VALUE ('.' DIGIT+)? ;

NUMERIC_ARG
        : ( INT_VALUE '.' ) => DECIMAL_VALUE { $type = DECIMAL_VALUE; }
        | INT_VALUE { $type =INT_VALUE; }
        ;

UNARY_OPERATOR : 'defined' ;

BINARY_OPERATOR
             : '=~'
             | '=~_ci'
             | '=='
             | '==_ci'
             | '<'
             | '<='
             | '>'
             | '>='
             | 'starts'
             | 'ends'
             | 'len<'
             | 'len<='
             | 'len>'
             | 'len>='
             | 'len=='
             ;

WS    : ( ' '
        | '\t'
        | '\r' '\n'
        | '\n'
        )
        {$channel = HIDDEN;}
      ;

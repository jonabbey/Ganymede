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

grammar Query;

options {
  language=Java;
  output=AST;
  ASTLabelType=CommonTree;
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
       SELECT^ (OBJECT? | (token (COMMA! token)*))
       ;

from_clause:
       FROM^ EDITABLE? token
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
       | (token (UNARY_OPERATOR | BINARY_OPERATOR)) => simple_expression
       | (token DEREF) => deref_expression
       ;

simple_expression:
       token (UNARY_OPERATOR^ | (BINARY_OPERATOR^ argument))
       ;

deref_expression:
       token DEREF^ atom
       ;

token
  : TOKEN
  | STRING_VALUE
  ;

argument
  : STRING_VALUE
  | INT_VALUE
  | DECIMAL_VALUE
  | BOOLEAN_VALUE
  ;


/* Lexer section */

BACKSLASH: '\\';
LPAREN : '(';
RPAREN : ')';
COMMA  : ',';
DEREF  : '->';
AND    : ('A'|'a')('N'|'n')('D'|'d');
OR     : ('O'|'o')('R'|'r');
NOT    : ('N'|'n')('O'|'o')('T'|'t');
SELECT : ('S'|'s')('E'|'e')('L'|'l')('E'|'e')('C'|'c')('T'|'t');
FROM   : ('F'|'f')('R'|'r')('O'|'o')('M'|'m');
WHERE  : ('W'|'w')('H'|'h')('E'|'e')('R'|'r')('E'|'e');
OBJECT : ('O'|'o')('B'|'b')('J'|'j')('E'|'e')('C'|'c')('T'|'t');
EDITABLE : ('E'|'e')('D'|'d')('I'|'i')('T'|'t')('A'|'a')('B'|'b')('L'|'l')('E'|'e');

BOOLEAN_VALUE
  : ('T'|'t')('R'|'r')('U'|'u')('E'|'e')
  | ('F'|'f')('A'|'a')('L'|'l')('S'|'s')('E'|'e')
  ;

UNARY_OPERATOR
  : ('D'|'d')('E'|'e')('F'|'f')('I'|'i')('N'|'n')('E'|'e')('D'|'d')
  ;

BINARY_OPERATOR
  : '=~'
  | '=~_'('C'|'c')('I'|'i')
  | '=='
  | '==_'('C'|'c')('I'|'i')
  | '<'
  | '<='
  | '>'
  | '>='
  | ('S'|'s')('T'|'t')('A'|'a')('R'|'r')('T'|'t')('S'|'s')
  | ('E'|'e')('N'|'n')('D'|'d')('S'|'s')
  | ('L'|'l')('E'|'e')('N'|'n')'<'
  | ('L'|'l')('E'|'e')('N'|'n')'<='
  | ('L'|'l')('E'|'e')('N'|'n')'>'
  | ('L'|'l')('E'|'e')('N'|'n')'>='
  | ('L'|'l')('E'|'e')('N'|'n')'=='
  ;

fragment TOKEN_START_CHAR
  : 'A'..'Z'
  | 'a'..'z'
  |'\u0080'..'\u009F'// NO NO_BREAK SPACE
  |'\u00A1'..'\u167F'// NO OGHAM SPACE MARK
  |'\u1681'..'\u180D'// NO MONGOLIAN VOWEL SEPARATOR
  |'\u180F'..'\u1FFF'// NO EN QUAD, EM QUAD, EN SPACE, THREE_PER_EM SPACE, FOUR_PER_EM SPACE, SIX_PER_EM SPACE
  |'\u2007' // NO PUNCTUATION SPACE, THIN SPACE, HAIR SPACE
  |'\u200B'..'\u202E'// NO NARROW NO_BREAK SPACE
  |'\u2030'..'\u205E'// NO MEDIUM MATHEMATICAL SPACE
  |'\u2060'..'\u2FFF'// NO IDEOGRAPHIC SPACE
  |'\u3001'..'\uD7FF'
  |'\uE000'..'\uFFFE'
  ;

TOKEN
  : TOKEN_START_CHAR (TOKEN_START_CHAR | DIGIT | '_')*
  ;

ESC : BACKSLASH
  ( 'n'  { this.setText("\n"); }
  | '"'  { this.setText("\""); }
  | '\'' { this.setText("'");  }
  | '\ ' { this.setText(" "); }
  | BACKSLASH
  );

STRING_VALUE
options {greedy=false;} 
  : '"' ((ESC)=> ESC | ~'"' )* '"' 
  | '\'' ((ESC)=> ESC | ~'\'' )* '\''
  ;

fragment DIGIT
  : '0'..'9'
  ;

INT_VALUE
  : '-'? DIGIT+
  ;

DECIMAL_VALUE
  : INT_VALUE '.' DIGIT+
  ;

WS
  : ( ' '
    | '\t'
    | '\r' '\n'
    | '\n'
    )+
    {$channel = HIDDEN;}
  ;

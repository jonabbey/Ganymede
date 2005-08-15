/*
   This is the ANTLR grammar for the Ganymede Query Language (GanyQL).

   It is converted into QueryParser.java, QueryLexer.java, and QueryParserTokenTypes.java by
   Terrence Parr's ANTLR tool, as follows:

   java -classpath antlr.jar antlr.Tool query.g

   Created: 23 August 2004

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Deepak Giridharagopal, deepak@brownman.org
   -----------------------------------------------------------------------
    
   Ganymede Directory Management System
   
   Copyright (C) 1996-2005
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA
*/

header {
  package arlut.csd.ganymede.server;
}

class QueryParser extends Parser;
options { buildAST=true; }

query : 
       select_clause
       from_clause
       (where_clause)?
       EOF!
       ;

select_clause: 
       SELECT^ (OBJECT | (select_field (COMMA! select_field)*))
       ;

select_field:
       STRING_VALUE
       | INVID
       | LABEL
       | ACTIVE
       | EDITABLE
       ;

from_clause:
       FROM^ STRING_VALUE
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

class QueryLexer extends Lexer;

options {
  k=10;                               // needed for some of our long op names
  charVocabulary='\u0000'..'\u007F';  // allow ascii
}

LPAREN : '(' ;
RPAREN : ')' ;
COMMA  : ',' ;
AND    : "and";
OR     : "or";
NOT    : "not";
SELECT : "select" ;
FROM   : "from" ;
WHERE  : "where" ;
DEREF  : "->" ;
OBJECT : "object";
INVID  : "invid";
EDITABLE : "editable";
LABEL  : "label";
ACTIVE : "active";

STRING_VALUE : 
         '"' (options {greedy=false;}:.)* '"' 
         | "'" (options {greedy=false;}:.)* "'" 
         ;

BOOLEAN_VALUE :
         "true"
         | "false"
         ;

protected DIGIT : ('0'..'9') ;
protected INT_VALUE : ('-')? (DIGIT)+ ;
protected DECIMAL_VALUE : INT_VALUE ('.' (DIGIT)+)? ;

NUMERIC_ARG 
        : ( INT_VALUE '.' ) => DECIMAL_VALUE { $setType(DECIMAL_VALUE); }
        | INT_VALUE { $setType(INT_VALUE); }
        ;

UNARY_OPERATOR : "defined" ;
         
BINARY_OPERATOR 
             : "=~"
             | "=~_ci"
             | "=="
             | "==_ci"
             | "<"
             | "<="
             | ">"
             | ">="
             | "starts"
             | "ends"
             | "len<"
             | "len<="
             | "len>"
             | "len>="
             | "len=="
             ;

WS    : ( ' '
        | '\t'
        | '\r' '\n'
        | '\n'
        ) 
        {$setType(Token.SKIP);}
      ;    

// $ANTLR 3.2 Sep 23, 2009 12:02:23 /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g 2010-08-10 21:07:35

package arlut.csd.ganymede.server; // had to be added by hand, ANTLR didn't insert this as expected

import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
public class QueryLexer extends Lexer {
    public static final int WHERE=9;
    public static final int TOKEN_START_CHAR=24;
    public static final int DECIMAL_VALUE=21;
    public static final int ESC=26;
    public static final int NUMERIC_ARG=27;
    public static final int DEREF=17;
    public static final int BINARY_OPERATOR=16;
    public static final int NOT=14;
    public static final int BOOLEAN_VALUE=22;
    public static final int AND=10;
    public static final int EOF=-1;
    public static final int LPAREN=12;
    public static final int TOKEN=18;
    public static final int RPAREN=13;
    public static final int INT_VALUE=20;
    public static final int EDITABLE=8;
    public static final int WS=28;
    public static final int OBJECT=5;
    public static final int COMMA=6;
    public static final int OR=11;
    public static final int DIGIT=25;
    public static final int FROM=7;
    public static final int UNARY_OPERATOR=15;
    public static final int SELECT=4;
    public static final int STRING_VALUE=19;
    public static final int BACKSLASH=23;

    // delegates
    // delegators

    public QueryLexer() {;} 
    public QueryLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public QueryLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "/home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g"; }

    // $ANTLR start "BACKSLASH"
    public final void mBACKSLASH() throws RecognitionException {
        try {
            int _type = BACKSLASH;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:114:10: ( '\\\\' )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:114:12: '\\\\'
            {
            match('\\'); if (state.failed) return ;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BACKSLASH"

    // $ANTLR start "LPAREN"
    public final void mLPAREN() throws RecognitionException {
        try {
            int _type = LPAREN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:115:8: ( '(' )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:115:10: '('
            {
            match('('); if (state.failed) return ;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "LPAREN"

    // $ANTLR start "RPAREN"
    public final void mRPAREN() throws RecognitionException {
        try {
            int _type = RPAREN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:116:8: ( ')' )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:116:10: ')'
            {
            match(')'); if (state.failed) return ;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "RPAREN"

    // $ANTLR start "COMMA"
    public final void mCOMMA() throws RecognitionException {
        try {
            int _type = COMMA;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:117:8: ( ',' )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:117:10: ','
            {
            match(','); if (state.failed) return ;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "COMMA"

    // $ANTLR start "DEREF"
    public final void mDEREF() throws RecognitionException {
        try {
            int _type = DEREF;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:118:8: ( '->' )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:118:10: '->'
            {
            match("->"); if (state.failed) return ;


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DEREF"

    // $ANTLR start "AND"
    public final void mAND() throws RecognitionException {
        try {
            int _type = AND;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:119:8: ( ( 'A' | 'a' ) ( 'N' | 'n' ) ( 'D' | 'd' ) )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:119:10: ( 'A' | 'a' ) ( 'N' | 'n' ) ( 'D' | 'd' )
            {
            if ( input.LA(1)=='A'||input.LA(1)=='a' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='D'||input.LA(1)=='d' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "AND"

    // $ANTLR start "OR"
    public final void mOR() throws RecognitionException {
        try {
            int _type = OR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:120:8: ( ( 'O' | 'o' ) ( 'R' | 'r' ) )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:120:10: ( 'O' | 'o' ) ( 'R' | 'r' )
            {
            if ( input.LA(1)=='O'||input.LA(1)=='o' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='R'||input.LA(1)=='r' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "OR"

    // $ANTLR start "NOT"
    public final void mNOT() throws RecognitionException {
        try {
            int _type = NOT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:121:8: ( ( 'N' | 'n' ) ( 'O' | 'o' ) ( 'T' | 't' ) )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:121:10: ( 'N' | 'n' ) ( 'O' | 'o' ) ( 'T' | 't' )
            {
            if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='O'||input.LA(1)=='o' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='T'||input.LA(1)=='t' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NOT"

    // $ANTLR start "SELECT"
    public final void mSELECT() throws RecognitionException {
        try {
            int _type = SELECT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:122:8: ( ( 'S' | 's' ) ( 'E' | 'e' ) ( 'L' | 'l' ) ( 'E' | 'e' ) ( 'C' | 'c' ) ( 'T' | 't' ) )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:122:10: ( 'S' | 's' ) ( 'E' | 'e' ) ( 'L' | 'l' ) ( 'E' | 'e' ) ( 'C' | 'c' ) ( 'T' | 't' )
            {
            if ( input.LA(1)=='S'||input.LA(1)=='s' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='L'||input.LA(1)=='l' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='C'||input.LA(1)=='c' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='T'||input.LA(1)=='t' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SELECT"

    // $ANTLR start "FROM"
    public final void mFROM() throws RecognitionException {
        try {
            int _type = FROM;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:123:8: ( ( 'F' | 'f' ) ( 'R' | 'r' ) ( 'O' | 'o' ) ( 'M' | 'm' ) )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:123:10: ( 'F' | 'f' ) ( 'R' | 'r' ) ( 'O' | 'o' ) ( 'M' | 'm' )
            {
            if ( input.LA(1)=='F'||input.LA(1)=='f' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='R'||input.LA(1)=='r' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='O'||input.LA(1)=='o' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='M'||input.LA(1)=='m' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "FROM"

    // $ANTLR start "WHERE"
    public final void mWHERE() throws RecognitionException {
        try {
            int _type = WHERE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:124:8: ( ( 'W' | 'w' ) ( 'H' | 'h' ) ( 'E' | 'e' ) ( 'R' | 'r' ) ( 'E' | 'e' ) )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:124:10: ( 'W' | 'w' ) ( 'H' | 'h' ) ( 'E' | 'e' ) ( 'R' | 'r' ) ( 'E' | 'e' )
            {
            if ( input.LA(1)=='W'||input.LA(1)=='w' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='H'||input.LA(1)=='h' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='R'||input.LA(1)=='r' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WHERE"

    // $ANTLR start "OBJECT"
    public final void mOBJECT() throws RecognitionException {
        try {
            int _type = OBJECT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:125:8: ( ( 'O' | 'o' ) ( 'B' | 'b' ) ( 'J' | 'j' ) ( 'E' | 'e' ) ( 'C' | 'c' ) ( 'T' | 't' ) )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:125:10: ( 'O' | 'o' ) ( 'B' | 'b' ) ( 'J' | 'j' ) ( 'E' | 'e' ) ( 'C' | 'c' ) ( 'T' | 't' )
            {
            if ( input.LA(1)=='O'||input.LA(1)=='o' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='B'||input.LA(1)=='b' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='J'||input.LA(1)=='j' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='C'||input.LA(1)=='c' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='T'||input.LA(1)=='t' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "OBJECT"

    // $ANTLR start "EDITABLE"
    public final void mEDITABLE() throws RecognitionException {
        try {
            int _type = EDITABLE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:126:10: ( ( 'E' | 'e' ) ( 'D' | 'd' ) ( 'I' | 'i' ) ( 'T' | 't' ) ( 'A' | 'a' ) ( 'B' | 'b' ) ( 'L' | 'l' ) ( 'E' | 'e' ) )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:126:12: ( 'E' | 'e' ) ( 'D' | 'd' ) ( 'I' | 'i' ) ( 'T' | 't' ) ( 'A' | 'a' ) ( 'B' | 'b' ) ( 'L' | 'l' ) ( 'E' | 'e' )
            {
            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='D'||input.LA(1)=='d' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='I'||input.LA(1)=='i' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='T'||input.LA(1)=='t' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='A'||input.LA(1)=='a' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='B'||input.LA(1)=='b' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='L'||input.LA(1)=='l' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "EDITABLE"

    // $ANTLR start "BOOLEAN_VALUE"
    public final void mBOOLEAN_VALUE() throws RecognitionException {
        try {
            int _type = BOOLEAN_VALUE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:129:3: ( ( 'T' | 't' ) ( 'R' | 'r' ) ( 'U' | 'u' ) ( 'E' | 'e' ) | ( 'F' | 'f' ) ( 'A' | 'a' ) ( 'L' | 'l' ) ( 'S' | 's' ) ( 'E' | 'e' ) )
            int alt1=2;
            int LA1_0 = input.LA(1);

            if ( (LA1_0=='T'||LA1_0=='t') ) {
                alt1=1;
            }
            else if ( (LA1_0=='F'||LA1_0=='f') ) {
                alt1=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 1, 0, input);

                throw nvae;
            }
            switch (alt1) {
                case 1 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:129:5: ( 'T' | 't' ) ( 'R' | 'r' ) ( 'U' | 'u' ) ( 'E' | 'e' )
                    {
                    if ( input.LA(1)=='T'||input.LA(1)=='t' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='R'||input.LA(1)=='r' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='U'||input.LA(1)=='u' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}


                    }
                    break;
                case 2 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:130:5: ( 'F' | 'f' ) ( 'A' | 'a' ) ( 'L' | 'l' ) ( 'S' | 's' ) ( 'E' | 'e' )
                    {
                    if ( input.LA(1)=='F'||input.LA(1)=='f' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='A'||input.LA(1)=='a' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='L'||input.LA(1)=='l' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='S'||input.LA(1)=='s' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}


                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BOOLEAN_VALUE"

    // $ANTLR start "UNARY_OPERATOR"
    public final void mUNARY_OPERATOR() throws RecognitionException {
        try {
            int _type = UNARY_OPERATOR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:134:3: ( ( 'D' | 'd' ) ( 'E' | 'e' ) ( 'F' | 'f' ) ( 'I' | 'i' ) ( 'N' | 'n' ) ( 'E' | 'e' ) ( 'D' | 'd' ) )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:134:5: ( 'D' | 'd' ) ( 'E' | 'e' ) ( 'F' | 'f' ) ( 'I' | 'i' ) ( 'N' | 'n' ) ( 'E' | 'e' ) ( 'D' | 'd' )
            {
            if ( input.LA(1)=='D'||input.LA(1)=='d' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='F'||input.LA(1)=='f' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='I'||input.LA(1)=='i' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            if ( input.LA(1)=='D'||input.LA(1)=='d' ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "UNARY_OPERATOR"

    // $ANTLR start "BINARY_OPERATOR"
    public final void mBINARY_OPERATOR() throws RecognitionException {
        try {
            int _type = BINARY_OPERATOR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:138:3: ( '=~' | '=~_' ( 'C' | 'c' ) ( 'I' | 'i' ) | '==' | '==_' ( 'C' | 'c' ) ( 'I' | 'i' ) | '<' | '<=' | '>' | '>=' | ( 'S' | 's' ) ( 'T' | 't' ) ( 'A' | 'a' ) ( 'R' | 'r' ) ( 'T' | 't' ) ( 'S' | 's' ) | ( 'E' | 'e' ) ( 'N' | 'n' ) ( 'D' | 'd' ) ( 'S' | 's' ) | ( 'L' | 'l' ) ( 'E' | 'e' ) ( 'N' | 'n' ) '<' | ( 'L' | 'l' ) ( 'E' | 'e' ) ( 'N' | 'n' ) '<=' | ( 'L' | 'l' ) ( 'E' | 'e' ) ( 'N' | 'n' ) '>' | ( 'L' | 'l' ) ( 'E' | 'e' ) ( 'N' | 'n' ) '>=' | ( 'L' | 'l' ) ( 'E' | 'e' ) ( 'N' | 'n' ) '==' )
            int alt2=15;
            alt2 = dfa2.predict(input);
            switch (alt2) {
                case 1 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:138:5: '=~'
                    {
                    match("=~"); if (state.failed) return ;


                    }
                    break;
                case 2 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:139:5: '=~_' ( 'C' | 'c' ) ( 'I' | 'i' )
                    {
                    match("=~_"); if (state.failed) return ;

                    if ( input.LA(1)=='C'||input.LA(1)=='c' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='I'||input.LA(1)=='i' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}


                    }
                    break;
                case 3 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:140:5: '=='
                    {
                    match("=="); if (state.failed) return ;


                    }
                    break;
                case 4 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:141:5: '==_' ( 'C' | 'c' ) ( 'I' | 'i' )
                    {
                    match("==_"); if (state.failed) return ;

                    if ( input.LA(1)=='C'||input.LA(1)=='c' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='I'||input.LA(1)=='i' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}


                    }
                    break;
                case 5 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:142:5: '<'
                    {
                    match('<'); if (state.failed) return ;

                    }
                    break;
                case 6 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:143:5: '<='
                    {
                    match("<="); if (state.failed) return ;


                    }
                    break;
                case 7 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:144:5: '>'
                    {
                    match('>'); if (state.failed) return ;

                    }
                    break;
                case 8 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:145:5: '>='
                    {
                    match(">="); if (state.failed) return ;


                    }
                    break;
                case 9 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:146:5: ( 'S' | 's' ) ( 'T' | 't' ) ( 'A' | 'a' ) ( 'R' | 'r' ) ( 'T' | 't' ) ( 'S' | 's' )
                    {
                    if ( input.LA(1)=='S'||input.LA(1)=='s' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='T'||input.LA(1)=='t' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='A'||input.LA(1)=='a' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='R'||input.LA(1)=='r' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='T'||input.LA(1)=='t' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='S'||input.LA(1)=='s' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}


                    }
                    break;
                case 10 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:147:5: ( 'E' | 'e' ) ( 'N' | 'n' ) ( 'D' | 'd' ) ( 'S' | 's' )
                    {
                    if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='D'||input.LA(1)=='d' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='S'||input.LA(1)=='s' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}


                    }
                    break;
                case 11 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:148:5: ( 'L' | 'l' ) ( 'E' | 'e' ) ( 'N' | 'n' ) '<'
                    {
                    if ( input.LA(1)=='L'||input.LA(1)=='l' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    match('<'); if (state.failed) return ;

                    }
                    break;
                case 12 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:149:5: ( 'L' | 'l' ) ( 'E' | 'e' ) ( 'N' | 'n' ) '<='
                    {
                    if ( input.LA(1)=='L'||input.LA(1)=='l' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    match("<="); if (state.failed) return ;


                    }
                    break;
                case 13 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:150:5: ( 'L' | 'l' ) ( 'E' | 'e' ) ( 'N' | 'n' ) '>'
                    {
                    if ( input.LA(1)=='L'||input.LA(1)=='l' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    match('>'); if (state.failed) return ;

                    }
                    break;
                case 14 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:151:5: ( 'L' | 'l' ) ( 'E' | 'e' ) ( 'N' | 'n' ) '>='
                    {
                    if ( input.LA(1)=='L'||input.LA(1)=='l' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    match(">="); if (state.failed) return ;


                    }
                    break;
                case 15 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:152:5: ( 'L' | 'l' ) ( 'E' | 'e' ) ( 'N' | 'n' ) '=='
                    {
                    if ( input.LA(1)=='L'||input.LA(1)=='l' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                        input.consume();
                    state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    match("=="); if (state.failed) return ;


                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BINARY_OPERATOR"

    // $ANTLR start "TOKEN_START_CHAR"
    public final void mTOKEN_START_CHAR() throws RecognitionException {
        try {
            int _type = TOKEN_START_CHAR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:156:3: ( 'A' .. 'Z' | 'a' .. 'z' | '\\u0080' .. '\\u009F' | '\\u00A1' .. '\\u167F' | '\\u1681' .. '\\u180D' | '\\u180F' .. '\\u1FFF' | '\\u2007' | '\\u200B' .. '\\u202E' | '\\u2030' .. '\\u205E' | '\\u2060' .. '\\u2FFF' | '\\u3001' .. '\\uD7FF' | '\\uE000' .. '\\uFFFE' )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:
            {
            if ( (input.LA(1)>='A' && input.LA(1)<='Z')||(input.LA(1)>='a' && input.LA(1)<='z')||(input.LA(1)>='\u0080' && input.LA(1)<='\u009F')||(input.LA(1)>='\u00A1' && input.LA(1)<='\u167F')||(input.LA(1)>='\u1681' && input.LA(1)<='\u180D')||(input.LA(1)>='\u180F' && input.LA(1)<='\u1FFF')||input.LA(1)=='\u2007'||(input.LA(1)>='\u200B' && input.LA(1)<='\u202E')||(input.LA(1)>='\u2030' && input.LA(1)<='\u205E')||(input.LA(1)>='\u2060' && input.LA(1)<='\u2FFF')||(input.LA(1)>='\u3001' && input.LA(1)<='\uD7FF')||(input.LA(1)>='\uE000' && input.LA(1)<='\uFFFE') ) {
                input.consume();
            state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "TOKEN_START_CHAR"

    // $ANTLR start "TOKEN"
    public final void mTOKEN() throws RecognitionException {
        try {
            int _type = TOKEN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:171:3: ( TOKEN_START_CHAR ( TOKEN_START_CHAR | DIGIT )* )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:171:5: TOKEN_START_CHAR ( TOKEN_START_CHAR | DIGIT )*
            {
            mTOKEN_START_CHAR(); if (state.failed) return ;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:171:22: ( TOKEN_START_CHAR | DIGIT )*
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( ((LA3_0>='0' && LA3_0<='9')||(LA3_0>='A' && LA3_0<='Z')||(LA3_0>='a' && LA3_0<='z')||(LA3_0>='\u0080' && LA3_0<='\u009F')||(LA3_0>='\u00A1' && LA3_0<='\u167F')||(LA3_0>='\u1681' && LA3_0<='\u180D')||(LA3_0>='\u180F' && LA3_0<='\u1FFF')||LA3_0=='\u2007'||(LA3_0>='\u200B' && LA3_0<='\u202E')||(LA3_0>='\u2030' && LA3_0<='\u205E')||(LA3_0>='\u2060' && LA3_0<='\u2FFF')||(LA3_0>='\u3001' && LA3_0<='\uD7FF')||(LA3_0>='\uE000' && LA3_0<='\uFFFE')) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:
            	    {
            	    if ( (input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='Z')||(input.LA(1)>='a' && input.LA(1)<='z')||(input.LA(1)>='\u0080' && input.LA(1)<='\u009F')||(input.LA(1)>='\u00A1' && input.LA(1)<='\u167F')||(input.LA(1)>='\u1681' && input.LA(1)<='\u180D')||(input.LA(1)>='\u180F' && input.LA(1)<='\u1FFF')||input.LA(1)=='\u2007'||(input.LA(1)>='\u200B' && input.LA(1)<='\u202E')||(input.LA(1)>='\u2030' && input.LA(1)<='\u205E')||(input.LA(1)>='\u2060' && input.LA(1)<='\u2FFF')||(input.LA(1)>='\u3001' && input.LA(1)<='\uD7FF')||(input.LA(1)>='\uE000' && input.LA(1)<='\uFFFE') ) {
            	        input.consume();
            	    state.failed=false;
            	    }
            	    else {
            	        if (state.backtracking>0) {state.failed=true; return ;}
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    break loop3;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "TOKEN"

    // $ANTLR start "ESC"
    public final void mESC() throws RecognitionException {
        try {
            int _type = ESC;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:174:5: ( BACKSLASH ( 'n' | '\"' | '\\'' | '\\ ' | BACKSLASH ) )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:174:7: BACKSLASH ( 'n' | '\"' | '\\'' | '\\ ' | BACKSLASH )
            {
            mBACKSLASH(); if (state.failed) return ;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:175:3: ( 'n' | '\"' | '\\'' | '\\ ' | BACKSLASH )
            int alt4=5;
            switch ( input.LA(1) ) {
            case 'n':
                {
                alt4=1;
                }
                break;
            case '\"':
                {
                alt4=2;
                }
                break;
            case '\'':
                {
                alt4=3;
                }
                break;
            case ' ':
                {
                alt4=4;
                }
                break;
            case '\\':
                {
                alt4=5;
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 4, 0, input);

                throw nvae;
            }

            switch (alt4) {
                case 1 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:175:5: 'n'
                    {
                    match('n'); if (state.failed) return ;
                    if ( state.backtracking==0 ) {
                       this.setText("\n"); 
                    }

                    }
                    break;
                case 2 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:176:5: '\"'
                    {
                    match('\"'); if (state.failed) return ;
                    if ( state.backtracking==0 ) {
                       this.setText("\""); 
                    }

                    }
                    break;
                case 3 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:177:5: '\\''
                    {
                    match('\''); if (state.failed) return ;
                    if ( state.backtracking==0 ) {
                       this.setText("'");  
                    }

                    }
                    break;
                case 4 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:178:5: '\\ '
                    {
                    match(' '); if (state.failed) return ;
                    if ( state.backtracking==0 ) {
                       this.setText(" "); 
                    }

                    }
                    break;
                case 5 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:179:5: BACKSLASH
                    {
                    mBACKSLASH(); if (state.failed) return ;

                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ESC"

    // $ANTLR start "STRING_VALUE"
    public final void mSTRING_VALUE() throws RecognitionException {
        try {
            int _type = STRING_VALUE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:184:3: ( '\"' ( ( ESC )=> ESC | ~ '\"' )* '\"' | '\\'' ( ( ESC )=> ESC | ~ '\\'' )* '\\'' )
            int alt7=2;
            int LA7_0 = input.LA(1);

            if ( (LA7_0=='\"') ) {
                alt7=1;
            }
            else if ( (LA7_0=='\'') ) {
                alt7=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 7, 0, input);

                throw nvae;
            }
            switch (alt7) {
                case 1 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:184:5: '\"' ( ( ESC )=> ESC | ~ '\"' )* '\"'
                    {
                    match('\"'); if (state.failed) return ;
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:184:9: ( ( ESC )=> ESC | ~ '\"' )*
                    loop5:
                    do {
                        int alt5=3;
                        alt5 = dfa5.predict(input);
                        switch (alt5) {
                    	case 1 :
                    	    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:184:10: ( ESC )=> ESC
                    	    {
                    	    mESC(); if (state.failed) return ;

                    	    }
                    	    break;
                    	case 2 :
                    	    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:184:24: ~ '\"'
                    	    {
                    	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='!')||(input.LA(1)>='#' && input.LA(1)<='\uFFFF') ) {
                    	        input.consume();
                    	    state.failed=false;
                    	    }
                    	    else {
                    	        if (state.backtracking>0) {state.failed=true; return ;}
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;}


                    	    }
                    	    break;

                    	default :
                    	    break loop5;
                        }
                    } while (true);

                    match('\"'); if (state.failed) return ;

                    }
                    break;
                case 2 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:185:5: '\\'' ( ( ESC )=> ESC | ~ '\\'' )* '\\''
                    {
                    match('\''); if (state.failed) return ;
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:185:10: ( ( ESC )=> ESC | ~ '\\'' )*
                    loop6:
                    do {
                        int alt6=3;
                        alt6 = dfa6.predict(input);
                        switch (alt6) {
                    	case 1 :
                    	    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:185:11: ( ESC )=> ESC
                    	    {
                    	    mESC(); if (state.failed) return ;

                    	    }
                    	    break;
                    	case 2 :
                    	    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:185:25: ~ '\\''
                    	    {
                    	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='&')||(input.LA(1)>='(' && input.LA(1)<='\uFFFF') ) {
                    	        input.consume();
                    	    state.failed=false;
                    	    }
                    	    else {
                    	        if (state.backtracking>0) {state.failed=true; return ;}
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;}


                    	    }
                    	    break;

                    	default :
                    	    break loop6;
                        }
                    } while (true);

                    match('\''); if (state.failed) return ;

                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "STRING_VALUE"

    // $ANTLR start "DIGIT"
    public final void mDIGIT() throws RecognitionException {
        try {
            int _type = DIGIT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:189:3: ( '0' .. '9' )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:189:5: '0' .. '9'
            {
            matchRange('0','9'); if (state.failed) return ;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DIGIT"

    // $ANTLR start "INT_VALUE"
    public final void mINT_VALUE() throws RecognitionException {
        try {
            int _type = INT_VALUE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:193:3: ( ( '-' )? ( DIGIT )+ )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:193:5: ( '-' )? ( DIGIT )+
            {
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:193:5: ( '-' )?
            int alt8=2;
            int LA8_0 = input.LA(1);

            if ( (LA8_0=='-') ) {
                alt8=1;
            }
            switch (alt8) {
                case 1 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:193:5: '-'
                    {
                    match('-'); if (state.failed) return ;

                    }
                    break;

            }

            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:193:10: ( DIGIT )+
            int cnt9=0;
            loop9:
            do {
                int alt9=2;
                int LA9_0 = input.LA(1);

                if ( ((LA9_0>='0' && LA9_0<='9')) ) {
                    alt9=1;
                }


                switch (alt9) {
            	case 1 :
            	    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:193:10: DIGIT
            	    {
            	    mDIGIT(); if (state.failed) return ;

            	    }
            	    break;

            	default :
            	    if ( cnt9 >= 1 ) break loop9;
            	    if (state.backtracking>0) {state.failed=true; return ;}
                        EarlyExitException eee =
                            new EarlyExitException(9, input);
                        throw eee;
                }
                cnt9++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "INT_VALUE"

    // $ANTLR start "DECIMAL_VALUE"
    public final void mDECIMAL_VALUE() throws RecognitionException {
        try {
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:197:3: ( INT_VALUE ( '.' ( DIGIT )+ )? )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:197:5: INT_VALUE ( '.' ( DIGIT )+ )?
            {
            mINT_VALUE(); if (state.failed) return ;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:197:15: ( '.' ( DIGIT )+ )?
            int alt11=2;
            int LA11_0 = input.LA(1);

            if ( (LA11_0=='.') ) {
                alt11=1;
            }
            switch (alt11) {
                case 1 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:197:16: '.' ( DIGIT )+
                    {
                    match('.'); if (state.failed) return ;
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:197:20: ( DIGIT )+
                    int cnt10=0;
                    loop10:
                    do {
                        int alt10=2;
                        int LA10_0 = input.LA(1);

                        if ( ((LA10_0>='0' && LA10_0<='9')) ) {
                            alt10=1;
                        }


                        switch (alt10) {
                    	case 1 :
                    	    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:197:20: DIGIT
                    	    {
                    	    mDIGIT(); if (state.failed) return ;

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt10 >= 1 ) break loop10;
                    	    if (state.backtracking>0) {state.failed=true; return ;}
                                EarlyExitException eee =
                                    new EarlyExitException(10, input);
                                throw eee;
                        }
                        cnt10++;
                    } while (true);


                    }
                    break;

            }


            }

        }
        finally {
        }
    }
    // $ANTLR end "DECIMAL_VALUE"

    // $ANTLR start "NUMERIC_ARG"
    public final void mNUMERIC_ARG() throws RecognitionException {
        try {
            int _type = NUMERIC_ARG;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:201:3: ( ( INT_VALUE '.' )=> DECIMAL_VALUE | INT_VALUE )
            int alt12=2;
            int LA12_0 = input.LA(1);

            if ( (LA12_0=='-') ) {
                int LA12_1 = input.LA(2);

                if ( ((LA12_1>='0' && LA12_1<='9')) ) {
                    int LA12_2 = input.LA(3);

                    if ( (synpred3_Query()) ) {
                        alt12=1;
                    }
                    else if ( (true) ) {
                        alt12=2;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        NoViableAltException nvae =
                            new NoViableAltException("", 12, 2, input);

                        throw nvae;
                    }
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return ;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 12, 1, input);

                    throw nvae;
                }
            }
            else if ( ((LA12_0>='0' && LA12_0<='9')) ) {
                int LA12_2 = input.LA(2);

                if ( (synpred3_Query()) ) {
                    alt12=1;
                }
                else if ( (true) ) {
                    alt12=2;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return ;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 12, 2, input);

                    throw nvae;
                }
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 12, 0, input);

                throw nvae;
            }
            switch (alt12) {
                case 1 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:201:5: ( INT_VALUE '.' )=> DECIMAL_VALUE
                    {
                    mDECIMAL_VALUE(); if (state.failed) return ;
                    if ( state.backtracking==0 ) {
                       _type = DECIMAL_VALUE; 
                    }

                    }
                    break;
                case 2 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:202:5: INT_VALUE
                    {
                    mINT_VALUE(); if (state.failed) return ;
                    if ( state.backtracking==0 ) {
                       _type =INT_VALUE; 
                    }

                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NUMERIC_ARG"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:206:3: ( ( ' ' | '\\t' | '\\r' '\\n' | '\\n' )+ )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:206:5: ( ' ' | '\\t' | '\\r' '\\n' | '\\n' )+
            {
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:206:5: ( ' ' | '\\t' | '\\r' '\\n' | '\\n' )+
            int cnt13=0;
            loop13:
            do {
                int alt13=5;
                switch ( input.LA(1) ) {
                case ' ':
                    {
                    alt13=1;
                    }
                    break;
                case '\t':
                    {
                    alt13=2;
                    }
                    break;
                case '\r':
                    {
                    alt13=3;
                    }
                    break;
                case '\n':
                    {
                    alt13=4;
                    }
                    break;

                }

                switch (alt13) {
            	case 1 :
            	    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:206:7: ' '
            	    {
            	    match(' '); if (state.failed) return ;

            	    }
            	    break;
            	case 2 :
            	    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:207:7: '\\t'
            	    {
            	    match('\t'); if (state.failed) return ;

            	    }
            	    break;
            	case 3 :
            	    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:208:7: '\\r' '\\n'
            	    {
            	    match('\r'); if (state.failed) return ;
            	    match('\n'); if (state.failed) return ;

            	    }
            	    break;
            	case 4 :
            	    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:209:7: '\\n'
            	    {
            	    match('\n'); if (state.failed) return ;

            	    }
            	    break;

            	default :
            	    if ( cnt13 >= 1 ) break loop13;
            	    if (state.backtracking>0) {state.failed=true; return ;}
                        EarlyExitException eee =
                            new EarlyExitException(13, input);
                        throw eee;
                }
                cnt13++;
            } while (true);

            if ( state.backtracking==0 ) {
              _channel = HIDDEN;
            }

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WS"

    public void mTokens() throws RecognitionException {
        // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:8: ( BACKSLASH | LPAREN | RPAREN | COMMA | DEREF | AND | OR | NOT | SELECT | FROM | WHERE | OBJECT | EDITABLE | BOOLEAN_VALUE | UNARY_OPERATOR | BINARY_OPERATOR | TOKEN_START_CHAR | TOKEN | ESC | STRING_VALUE | DIGIT | INT_VALUE | NUMERIC_ARG | WS )
        int alt14=24;
        alt14 = dfa14.predict(input);
        switch (alt14) {
            case 1 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:10: BACKSLASH
                {
                mBACKSLASH(); if (state.failed) return ;

                }
                break;
            case 2 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:20: LPAREN
                {
                mLPAREN(); if (state.failed) return ;

                }
                break;
            case 3 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:27: RPAREN
                {
                mRPAREN(); if (state.failed) return ;

                }
                break;
            case 4 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:34: COMMA
                {
                mCOMMA(); if (state.failed) return ;

                }
                break;
            case 5 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:40: DEREF
                {
                mDEREF(); if (state.failed) return ;

                }
                break;
            case 6 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:46: AND
                {
                mAND(); if (state.failed) return ;

                }
                break;
            case 7 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:50: OR
                {
                mOR(); if (state.failed) return ;

                }
                break;
            case 8 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:53: NOT
                {
                mNOT(); if (state.failed) return ;

                }
                break;
            case 9 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:57: SELECT
                {
                mSELECT(); if (state.failed) return ;

                }
                break;
            case 10 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:64: FROM
                {
                mFROM(); if (state.failed) return ;

                }
                break;
            case 11 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:69: WHERE
                {
                mWHERE(); if (state.failed) return ;

                }
                break;
            case 12 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:75: OBJECT
                {
                mOBJECT(); if (state.failed) return ;

                }
                break;
            case 13 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:82: EDITABLE
                {
                mEDITABLE(); if (state.failed) return ;

                }
                break;
            case 14 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:91: BOOLEAN_VALUE
                {
                mBOOLEAN_VALUE(); if (state.failed) return ;

                }
                break;
            case 15 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:105: UNARY_OPERATOR
                {
                mUNARY_OPERATOR(); if (state.failed) return ;

                }
                break;
            case 16 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:120: BINARY_OPERATOR
                {
                mBINARY_OPERATOR(); if (state.failed) return ;

                }
                break;
            case 17 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:136: TOKEN_START_CHAR
                {
                mTOKEN_START_CHAR(); if (state.failed) return ;

                }
                break;
            case 18 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:153: TOKEN
                {
                mTOKEN(); if (state.failed) return ;

                }
                break;
            case 19 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:159: ESC
                {
                mESC(); if (state.failed) return ;

                }
                break;
            case 20 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:163: STRING_VALUE
                {
                mSTRING_VALUE(); if (state.failed) return ;

                }
                break;
            case 21 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:176: DIGIT
                {
                mDIGIT(); if (state.failed) return ;

                }
                break;
            case 22 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:182: INT_VALUE
                {
                mINT_VALUE(); if (state.failed) return ;

                }
                break;
            case 23 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:192: NUMERIC_ARG
                {
                mNUMERIC_ARG(); if (state.failed) return ;

                }
                break;
            case 24 :
                // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:1:204: WS
                {
                mWS(); if (state.failed) return ;

                }
                break;

        }

    }

    // $ANTLR start synpred1_Query
    public final void synpred1_Query_fragment() throws RecognitionException {   
        // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:184:10: ( ESC )
        // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:184:11: ESC
        {
        mESC(); if (state.failed) return ;

        }
    }
    // $ANTLR end synpred1_Query

    // $ANTLR start synpred2_Query
    public final void synpred2_Query_fragment() throws RecognitionException {   
        // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:185:11: ( ESC )
        // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:185:12: ESC
        {
        mESC(); if (state.failed) return ;

        }
    }
    // $ANTLR end synpred2_Query

    // $ANTLR start synpred3_Query
    public final void synpred3_Query_fragment() throws RecognitionException {   
        // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:201:5: ( INT_VALUE '.' )
        // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:201:7: INT_VALUE '.'
        {
        mINT_VALUE(); if (state.failed) return ;
        match('.'); if (state.failed) return ;

        }
    }
    // $ANTLR end synpred3_Query

    public final boolean synpred3_Query() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred3_Query_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred1_Query() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred1_Query_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred2_Query() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred2_Query_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed=false;
        return success;
    }


    protected DFA2 dfa2 = new DFA2(this);
    protected DFA5 dfa5 = new DFA5(this);
    protected DFA6 dfa6 = new DFA6(this);
    protected DFA14 dfa14 = new DFA14(this);
    static final String DFA2_eotS =
        "\2\uffff\1\12\1\14\3\uffff\1\17\1\21\12\uffff\1\27\1\31\5\uffff";
    static final String DFA2_eofS =
        "\32\uffff";
    static final String DFA2_minS =
        "\1\74\3\75\2\uffff\1\105\2\137\4\uffff\1\116\4\uffff\1\74\2\75\5"+
        "\uffff";
    static final String DFA2_maxS =
        "\1\163\1\176\2\75\2\uffff\1\145\2\137\4\uffff\1\156\4\uffff\1\76"+
        "\2\75\5\uffff";
    static final String DFA2_acceptS =
        "\4\uffff\1\11\1\12\3\uffff\1\6\1\5\1\10\1\7\1\uffff\1\2\1\1\1\4"+
        "\1\3\3\uffff\1\17\1\14\1\13\1\16\1\15";
    static final String DFA2_specialS =
        "\32\uffff}>";
    static final String[] DFA2_transitionS = {
            "\1\2\1\1\1\3\6\uffff\1\5\6\uffff\1\6\6\uffff\1\4\21\uffff\1"+
            "\5\6\uffff\1\6\6\uffff\1\4",
            "\1\10\100\uffff\1\7",
            "\1\11",
            "\1\13",
            "",
            "",
            "\1\15\37\uffff\1\15",
            "\1\16",
            "\1\20",
            "",
            "",
            "",
            "",
            "\1\22\37\uffff\1\22",
            "",
            "",
            "",
            "",
            "\1\23\1\25\1\24",
            "\1\26",
            "\1\30",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA2_eot = DFA.unpackEncodedString(DFA2_eotS);
    static final short[] DFA2_eof = DFA.unpackEncodedString(DFA2_eofS);
    static final char[] DFA2_min = DFA.unpackEncodedStringToUnsignedChars(DFA2_minS);
    static final char[] DFA2_max = DFA.unpackEncodedStringToUnsignedChars(DFA2_maxS);
    static final short[] DFA2_accept = DFA.unpackEncodedString(DFA2_acceptS);
    static final short[] DFA2_special = DFA.unpackEncodedString(DFA2_specialS);
    static final short[][] DFA2_transition;

    static {
        int numStates = DFA2_transitionS.length;
        DFA2_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA2_transition[i] = DFA.unpackEncodedString(DFA2_transitionS[i]);
        }
    }

    class DFA2 extends DFA {

        public DFA2(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 2;
            this.eot = DFA2_eot;
            this.eof = DFA2_eof;
            this.min = DFA2_min;
            this.max = DFA2_max;
            this.accept = DFA2_accept;
            this.special = DFA2_special;
            this.transition = DFA2_transition;
        }
        public String getDescription() {
            return "137:1: BINARY_OPERATOR : ( '=~' | '=~_' ( 'C' | 'c' ) ( 'I' | 'i' ) | '==' | '==_' ( 'C' | 'c' ) ( 'I' | 'i' ) | '<' | '<=' | '>' | '>=' | ( 'S' | 's' ) ( 'T' | 't' ) ( 'A' | 'a' ) ( 'R' | 'r' ) ( 'T' | 't' ) ( 'S' | 's' ) | ( 'E' | 'e' ) ( 'N' | 'n' ) ( 'D' | 'd' ) ( 'S' | 's' ) | ( 'L' | 'l' ) ( 'E' | 'e' ) ( 'N' | 'n' ) '<' | ( 'L' | 'l' ) ( 'E' | 'e' ) ( 'N' | 'n' ) '<=' | ( 'L' | 'l' ) ( 'E' | 'e' ) ( 'N' | 'n' ) '>' | ( 'L' | 'l' ) ( 'E' | 'e' ) ( 'N' | 'n' ) '>=' | ( 'L' | 'l' ) ( 'E' | 'e' ) ( 'N' | 'n' ) '==' );";
        }
    }
    static final String DFA5_eotS =
        "\4\uffff\1\3\7\uffff";
    static final String DFA5_eofS =
        "\14\uffff";
    static final String DFA5_minS =
        "\1\0\1\uffff\1\0\1\uffff\5\0\3\uffff";
    static final String DFA5_maxS =
        "\1\uffff\1\uffff\1\uffff\1\uffff\1\uffff\4\0\3\uffff";
    static final String DFA5_acceptS =
        "\1\uffff\1\3\1\uffff\1\2\5\uffff\3\1";
    static final String DFA5_specialS =
        "\1\4\1\uffff\1\2\1\uffff\1\6\1\5\1\0\1\1\1\3\3\uffff}>";
    static final String[] DFA5_transitionS = {
            "\42\3\1\1\71\3\1\2\uffa3\3",
            "",
            "\40\3\1\10\1\3\1\4\4\3\1\7\64\3\1\5\21\3\1\6\uff91\3",
            "",
            "\42\13\1\11\71\13\1\12\uffa3\13",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "",
            "",
            ""
    };

    static final short[] DFA5_eot = DFA.unpackEncodedString(DFA5_eotS);
    static final short[] DFA5_eof = DFA.unpackEncodedString(DFA5_eofS);
    static final char[] DFA5_min = DFA.unpackEncodedStringToUnsignedChars(DFA5_minS);
    static final char[] DFA5_max = DFA.unpackEncodedStringToUnsignedChars(DFA5_maxS);
    static final short[] DFA5_accept = DFA.unpackEncodedString(DFA5_acceptS);
    static final short[] DFA5_special = DFA.unpackEncodedString(DFA5_specialS);
    static final short[][] DFA5_transition;

    static {
        int numStates = DFA5_transitionS.length;
        DFA5_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA5_transition[i] = DFA.unpackEncodedString(DFA5_transitionS[i]);
        }
    }

    class DFA5 extends DFA {

        public DFA5(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 5;
            this.eot = DFA5_eot;
            this.eof = DFA5_eof;
            this.min = DFA5_min;
            this.max = DFA5_max;
            this.accept = DFA5_accept;
            this.special = DFA5_special;
            this.transition = DFA5_transition;
        }
        public String getDescription() {
            return "()* loopback of 184:9: ( ( ESC )=> ESC | ~ '\"' )*";
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            IntStream input = _input;
        	int _s = s;
            switch ( s ) {
                    case 0 : 
                        int LA5_6 = input.LA(1);

                         
                        int index5_6 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred1_Query()) ) {s = 11;}

                        else if ( (true) ) {s = 3;}

                         
                        input.seek(index5_6);
                        if ( s>=0 ) return s;
                        break;
                    case 1 : 
                        int LA5_7 = input.LA(1);

                         
                        int index5_7 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred1_Query()) ) {s = 11;}

                        else if ( (true) ) {s = 3;}

                         
                        input.seek(index5_7);
                        if ( s>=0 ) return s;
                        break;
                    case 2 : 
                        int LA5_2 = input.LA(1);

                        s = -1;
                        if ( (LA5_2=='\"') ) {s = 4;}

                        else if ( (LA5_2=='\\') ) {s = 5;}

                        else if ( (LA5_2=='n') ) {s = 6;}

                        else if ( (LA5_2=='\'') ) {s = 7;}

                        else if ( (LA5_2==' ') ) {s = 8;}

                        else if ( ((LA5_2>='\u0000' && LA5_2<='\u001F')||LA5_2=='!'||(LA5_2>='#' && LA5_2<='&')||(LA5_2>='(' && LA5_2<='[')||(LA5_2>=']' && LA5_2<='m')||(LA5_2>='o' && LA5_2<='\uFFFF')) ) {s = 3;}

                        if ( s>=0 ) return s;
                        break;
                    case 3 : 
                        int LA5_8 = input.LA(1);

                         
                        int index5_8 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred1_Query()) ) {s = 11;}

                        else if ( (true) ) {s = 3;}

                         
                        input.seek(index5_8);
                        if ( s>=0 ) return s;
                        break;
                    case 4 : 
                        int LA5_0 = input.LA(1);

                        s = -1;
                        if ( (LA5_0=='\"') ) {s = 1;}

                        else if ( (LA5_0=='\\') ) {s = 2;}

                        else if ( ((LA5_0>='\u0000' && LA5_0<='!')||(LA5_0>='#' && LA5_0<='[')||(LA5_0>=']' && LA5_0<='\uFFFF')) ) {s = 3;}

                        if ( s>=0 ) return s;
                        break;
                    case 5 : 
                        int LA5_5 = input.LA(1);

                         
                        int index5_5 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred1_Query()) ) {s = 11;}

                        else if ( (true) ) {s = 3;}

                         
                        input.seek(index5_5);
                        if ( s>=0 ) return s;
                        break;
                    case 6 : 
                        int LA5_4 = input.LA(1);

                         
                        int index5_4 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (LA5_4=='\"') && (synpred1_Query())) {s = 9;}

                        else if ( (LA5_4=='\\') && (synpred1_Query())) {s = 10;}

                        else if ( ((LA5_4>='\u0000' && LA5_4<='!')||(LA5_4>='#' && LA5_4<='[')||(LA5_4>=']' && LA5_4<='\uFFFF')) && (synpred1_Query())) {s = 11;}

                        else s = 3;

                         
                        input.seek(index5_4);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 5, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA6_eotS =
        "\4\uffff\1\3\7\uffff";
    static final String DFA6_eofS =
        "\14\uffff";
    static final String DFA6_minS =
        "\1\0\1\uffff\1\0\1\uffff\5\0\3\uffff";
    static final String DFA6_maxS =
        "\1\uffff\1\uffff\1\uffff\1\uffff\1\uffff\4\0\3\uffff";
    static final String DFA6_acceptS =
        "\1\uffff\1\3\1\uffff\1\2\5\uffff\3\1";
    static final String DFA6_specialS =
        "\1\2\1\uffff\1\0\1\uffff\1\6\1\5\1\1\1\3\1\4\3\uffff}>";
    static final String[] DFA6_transitionS = {
            "\47\3\1\1\64\3\1\2\uffa3\3",
            "",
            "\40\3\1\10\1\3\1\7\4\3\1\4\64\3\1\5\21\3\1\6\uff91\3",
            "",
            "\47\13\1\11\64\13\1\12\uffa3\13",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "",
            "",
            ""
    };

    static final short[] DFA6_eot = DFA.unpackEncodedString(DFA6_eotS);
    static final short[] DFA6_eof = DFA.unpackEncodedString(DFA6_eofS);
    static final char[] DFA6_min = DFA.unpackEncodedStringToUnsignedChars(DFA6_minS);
    static final char[] DFA6_max = DFA.unpackEncodedStringToUnsignedChars(DFA6_maxS);
    static final short[] DFA6_accept = DFA.unpackEncodedString(DFA6_acceptS);
    static final short[] DFA6_special = DFA.unpackEncodedString(DFA6_specialS);
    static final short[][] DFA6_transition;

    static {
        int numStates = DFA6_transitionS.length;
        DFA6_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA6_transition[i] = DFA.unpackEncodedString(DFA6_transitionS[i]);
        }
    }

    class DFA6 extends DFA {

        public DFA6(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 6;
            this.eot = DFA6_eot;
            this.eof = DFA6_eof;
            this.min = DFA6_min;
            this.max = DFA6_max;
            this.accept = DFA6_accept;
            this.special = DFA6_special;
            this.transition = DFA6_transition;
        }
        public String getDescription() {
            return "()* loopback of 185:10: ( ( ESC )=> ESC | ~ '\\'' )*";
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            IntStream input = _input;
        	int _s = s;
            switch ( s ) {
                    case 0 : 
                        int LA6_2 = input.LA(1);

                        s = -1;
                        if ( (LA6_2=='\'') ) {s = 4;}

                        else if ( (LA6_2=='\\') ) {s = 5;}

                        else if ( (LA6_2=='n') ) {s = 6;}

                        else if ( (LA6_2=='\"') ) {s = 7;}

                        else if ( (LA6_2==' ') ) {s = 8;}

                        else if ( ((LA6_2>='\u0000' && LA6_2<='\u001F')||LA6_2=='!'||(LA6_2>='#' && LA6_2<='&')||(LA6_2>='(' && LA6_2<='[')||(LA6_2>=']' && LA6_2<='m')||(LA6_2>='o' && LA6_2<='\uFFFF')) ) {s = 3;}

                        if ( s>=0 ) return s;
                        break;
                    case 1 : 
                        int LA6_6 = input.LA(1);

                         
                        int index6_6 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred2_Query()) ) {s = 11;}

                        else if ( (true) ) {s = 3;}

                         
                        input.seek(index6_6);
                        if ( s>=0 ) return s;
                        break;
                    case 2 : 
                        int LA6_0 = input.LA(1);

                        s = -1;
                        if ( (LA6_0=='\'') ) {s = 1;}

                        else if ( (LA6_0=='\\') ) {s = 2;}

                        else if ( ((LA6_0>='\u0000' && LA6_0<='&')||(LA6_0>='(' && LA6_0<='[')||(LA6_0>=']' && LA6_0<='\uFFFF')) ) {s = 3;}

                        if ( s>=0 ) return s;
                        break;
                    case 3 : 
                        int LA6_7 = input.LA(1);

                         
                        int index6_7 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred2_Query()) ) {s = 11;}

                        else if ( (true) ) {s = 3;}

                         
                        input.seek(index6_7);
                        if ( s>=0 ) return s;
                        break;
                    case 4 : 
                        int LA6_8 = input.LA(1);

                         
                        int index6_8 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred2_Query()) ) {s = 11;}

                        else if ( (true) ) {s = 3;}

                         
                        input.seek(index6_8);
                        if ( s>=0 ) return s;
                        break;
                    case 5 : 
                        int LA6_5 = input.LA(1);

                         
                        int index6_5 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred2_Query()) ) {s = 11;}

                        else if ( (true) ) {s = 3;}

                         
                        input.seek(index6_5);
                        if ( s>=0 ) return s;
                        break;
                    case 6 : 
                        int LA6_4 = input.LA(1);

                         
                        int index6_4 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (LA6_4=='\'') && (synpred2_Query())) {s = 9;}

                        else if ( (LA6_4=='\\') && (synpred2_Query())) {s = 10;}

                        else if ( ((LA6_4>='\u0000' && LA6_4<='&')||(LA6_4>='(' && LA6_4<='[')||(LA6_4>=']' && LA6_4<='\uFFFF')) && (synpred2_Query())) {s = 11;}

                        else s = 3;

                         
                        input.seek(index6_4);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 6, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA14_eotS =
        "\1\uffff\1\25\4\uffff\11\32\1\uffff\2\32\1\uffff\1\51\4\uffff\1"+
        "\53\1\33\2\uffff\1\55\14\33\3\uffff\1\72\1\uffff\1\33\1\74\12\33"+
        "\1\uffff\1\33\1\uffff\2\33\1\111\3\33\1\17\1\115\4\33\1\uffff\1"+
        "\115\1\122\1\33\1\uffff\1\33\1\125\1\126\1\17\1\uffff\2\33\2\uffff"+
        "\1\33\1\132\1\133\2\uffff";
    static final String DFA14_eofS =
        "\134\uffff";
    static final String DFA14_minS =
        "\1\11\1\40\3\uffff\12\60\1\uffff\2\60\1\uffff\1\56\4\uffff\1\56"+
        "\1\104\2\uffff\1\60\1\112\1\124\1\114\1\101\1\117\1\114\1\105\1"+
        "\111\1\104\1\125\1\106\1\116\3\uffff\1\60\1\uffff\1\105\1\60\1\105"+
        "\1\122\1\115\1\123\1\122\1\124\1\123\1\105\1\111\1\74\1\uffff\1"+
        "\103\1\uffff\1\103\1\124\1\60\2\105\1\101\2\60\1\116\2\124\1\123"+
        "\1\uffff\2\60\1\102\1\uffff\1\105\3\60\1\uffff\1\114\1\104\2\uffff"+
        "\1\105\2\60\2\uffff";
    static final String DFA14_maxS =
        "\1\ufffe\1\156\3\uffff\1\76\11\ufffe\1\uffff\2\ufffe\1\uffff\1\71"+
        "\4\uffff\1\71\1\144\2\uffff\1\ufffe\1\152\1\164\1\154\1\141\1\157"+
        "\1\154\1\145\1\151\1\144\1\165\1\146\1\156\3\uffff\1\ufffe\1\uffff"+
        "\1\145\1\ufffe\1\145\1\162\1\155\1\163\1\162\1\164\1\163\1\145\1"+
        "\151\1\76\1\uffff\1\143\1\uffff\1\143\1\164\1\ufffe\2\145\1\141"+
        "\2\ufffe\1\156\2\164\1\163\1\uffff\2\ufffe\1\142\1\uffff\1\145\3"+
        "\ufffe\1\uffff\1\154\1\144\2\uffff\1\145\2\ufffe\2\uffff";
    static final String DFA14_acceptS =
        "\2\uffff\1\2\1\3\1\4\12\uffff\1\20\2\uffff\1\24\1\uffff\1\30\1\1"+
        "\1\23\1\5\2\uffff\1\21\1\22\15\uffff\1\25\1\27\1\26\1\uffff\1\7"+
        "\14\uffff\1\6\1\uffff\1\10\14\uffff\1\12\3\uffff\1\16\4\uffff\1"+
        "\13\2\uffff\1\14\1\11\3\uffff\1\17\1\15";
    static final String DFA14_specialS =
        "\134\uffff}>";
    static final String[] DFA14_transitionS = {
            "\2\24\2\uffff\1\24\22\uffff\1\24\1\uffff\1\22\4\uffff\1\22\1"+
            "\2\1\3\2\uffff\1\4\1\5\2\uffff\12\23\2\uffff\3\17\2\uffff\1"+
            "\6\2\21\1\16\1\14\1\12\5\21\1\20\1\21\1\10\1\7\3\21\1\11\1\15"+
            "\2\21\1\13\3\21\1\uffff\1\1\4\uffff\1\6\2\21\1\16\1\14\1\12"+
            "\5\21\1\20\1\21\1\10\1\7\3\21\1\11\1\15\2\21\1\13\3\21\5\uffff"+
            "\40\21\1\uffff\u15df\21\1\uffff\u018d\21\1\uffff\u07f1\21\7"+
            "\uffff\1\21\3\uffff\44\21\1\uffff\57\21\1\uffff\u0fa0\21\1\uffff"+
            "\ua7ff\21\u0800\uffff\u1fff\21",
            "\1\26\1\uffff\1\26\4\uffff\1\26\64\uffff\1\26\21\uffff\1\26",
            "",
            "",
            "",
            "\12\30\4\uffff\1\27",
            "\12\33\7\uffff\15\33\1\31\14\33\6\uffff\15\33\1\31\14\33\5"+
            "\uffff\40\33\1\uffff\u15df\33\1\uffff\u018d\33\1\uffff\u07f1"+
            "\33\7\uffff\1\33\3\uffff\44\33\1\uffff\57\33\1\uffff\u0fa0\33"+
            "\1\uffff\ua7ff\33\u0800\uffff\u1fff\33",
            "\12\33\7\uffff\1\33\1\35\17\33\1\34\10\33\6\uffff\1\33\1\35"+
            "\17\33\1\34\10\33\5\uffff\40\33\1\uffff\u15df\33\1\uffff\u018d"+
            "\33\1\uffff\u07f1\33\7\uffff\1\33\3\uffff\44\33\1\uffff\57\33"+
            "\1\uffff\u0fa0\33\1\uffff\ua7ff\33\u0800\uffff\u1fff\33",
            "\12\33\7\uffff\16\33\1\36\13\33\6\uffff\16\33\1\36\13\33\5"+
            "\uffff\40\33\1\uffff\u15df\33\1\uffff\u018d\33\1\uffff\u07f1"+
            "\33\7\uffff\1\33\3\uffff\44\33\1\uffff\57\33\1\uffff\u0fa0\33"+
            "\1\uffff\ua7ff\33\u0800\uffff\u1fff\33",
            "\12\33\7\uffff\4\33\1\37\16\33\1\40\6\33\6\uffff\4\33\1\37"+
            "\16\33\1\40\6\33\5\uffff\40\33\1\uffff\u15df\33\1\uffff\u018d"+
            "\33\1\uffff\u07f1\33\7\uffff\1\33\3\uffff\44\33\1\uffff\57\33"+
            "\1\uffff\u0fa0\33\1\uffff\ua7ff\33\u0800\uffff\u1fff\33",
            "\12\33\7\uffff\1\42\20\33\1\41\10\33\6\uffff\1\42\20\33\1\41"+
            "\10\33\5\uffff\40\33\1\uffff\u15df\33\1\uffff\u018d\33\1\uffff"+
            "\u07f1\33\7\uffff\1\33\3\uffff\44\33\1\uffff\57\33\1\uffff\u0fa0"+
            "\33\1\uffff\ua7ff\33\u0800\uffff\u1fff\33",
            "\12\33\7\uffff\7\33\1\43\22\33\6\uffff\7\33\1\43\22\33\5\uffff"+
            "\40\33\1\uffff\u15df\33\1\uffff\u018d\33\1\uffff\u07f1\33\7"+
            "\uffff\1\33\3\uffff\44\33\1\uffff\57\33\1\uffff\u0fa0\33\1\uffff"+
            "\ua7ff\33\u0800\uffff\u1fff\33",
            "\12\33\7\uffff\3\33\1\44\11\33\1\45\14\33\6\uffff\3\33\1\44"+
            "\11\33\1\45\14\33\5\uffff\40\33\1\uffff\u15df\33\1\uffff\u018d"+
            "\33\1\uffff\u07f1\33\7\uffff\1\33\3\uffff\44\33\1\uffff\57\33"+
            "\1\uffff\u0fa0\33\1\uffff\ua7ff\33\u0800\uffff\u1fff\33",
            "\12\33\7\uffff\21\33\1\46\10\33\6\uffff\21\33\1\46\10\33\5"+
            "\uffff\40\33\1\uffff\u15df\33\1\uffff\u018d\33\1\uffff\u07f1"+
            "\33\7\uffff\1\33\3\uffff\44\33\1\uffff\57\33\1\uffff\u0fa0\33"+
            "\1\uffff\ua7ff\33\u0800\uffff\u1fff\33",
            "\12\33\7\uffff\4\33\1\47\25\33\6\uffff\4\33\1\47\25\33\5\uffff"+
            "\40\33\1\uffff\u15df\33\1\uffff\u018d\33\1\uffff\u07f1\33\7"+
            "\uffff\1\33\3\uffff\44\33\1\uffff\57\33\1\uffff\u0fa0\33\1\uffff"+
            "\ua7ff\33\u0800\uffff\u1fff\33",
            "",
            "\12\33\7\uffff\4\33\1\50\25\33\6\uffff\4\33\1\50\25\33\5\uffff"+
            "\40\33\1\uffff\u15df\33\1\uffff\u018d\33\1\uffff\u07f1\33\7"+
            "\uffff\1\33\3\uffff\44\33\1\uffff\57\33\1\uffff\u0fa0\33\1\uffff"+
            "\ua7ff\33\u0800\uffff\u1fff\33",
            "\12\33\7\uffff\32\33\6\uffff\32\33\5\uffff\40\33\1\uffff\u15df"+
            "\33\1\uffff\u018d\33\1\uffff\u07f1\33\7\uffff\1\33\3\uffff\44"+
            "\33\1\uffff\57\33\1\uffff\u0fa0\33\1\uffff\ua7ff\33\u0800\uffff"+
            "\u1fff\33",
            "",
            "\1\52\1\uffff\12\30",
            "",
            "",
            "",
            "",
            "\1\52\1\uffff\12\30",
            "\1\54\37\uffff\1\54",
            "",
            "",
            "\12\33\7\uffff\32\33\6\uffff\32\33\5\uffff\40\33\1\uffff\u15df"+
            "\33\1\uffff\u018d\33\1\uffff\u07f1\33\7\uffff\1\33\3\uffff\44"+
            "\33\1\uffff\57\33\1\uffff\u0fa0\33\1\uffff\ua7ff\33\u0800\uffff"+
            "\u1fff\33",
            "\1\56\37\uffff\1\56",
            "\1\57\37\uffff\1\57",
            "\1\60\37\uffff\1\60",
            "\1\61\37\uffff\1\61",
            "\1\62\37\uffff\1\62",
            "\1\63\37\uffff\1\63",
            "\1\64\37\uffff\1\64",
            "\1\65\37\uffff\1\65",
            "\1\66\37\uffff\1\66",
            "\1\67\37\uffff\1\67",
            "\1\70\37\uffff\1\70",
            "\1\71\37\uffff\1\71",
            "",
            "",
            "",
            "\12\33\7\uffff\32\33\6\uffff\32\33\5\uffff\40\33\1\uffff\u15df"+
            "\33\1\uffff\u018d\33\1\uffff\u07f1\33\7\uffff\1\33\3\uffff\44"+
            "\33\1\uffff\57\33\1\uffff\u0fa0\33\1\uffff\ua7ff\33\u0800\uffff"+
            "\u1fff\33",
            "",
            "\1\73\37\uffff\1\73",
            "\12\33\7\uffff\32\33\6\uffff\32\33\5\uffff\40\33\1\uffff\u15df"+
            "\33\1\uffff\u018d\33\1\uffff\u07f1\33\7\uffff\1\33\3\uffff\44"+
            "\33\1\uffff\57\33\1\uffff\u0fa0\33\1\uffff\ua7ff\33\u0800\uffff"+
            "\u1fff\33",
            "\1\75\37\uffff\1\75",
            "\1\76\37\uffff\1\76",
            "\1\77\37\uffff\1\77",
            "\1\100\37\uffff\1\100",
            "\1\101\37\uffff\1\101",
            "\1\102\37\uffff\1\102",
            "\1\103\37\uffff\1\103",
            "\1\104\37\uffff\1\104",
            "\1\105\37\uffff\1\105",
            "\3\17",
            "",
            "\1\106\37\uffff\1\106",
            "",
            "\1\107\37\uffff\1\107",
            "\1\110\37\uffff\1\110",
            "\12\33\7\uffff\32\33\6\uffff\32\33\5\uffff\40\33\1\uffff\u15df"+
            "\33\1\uffff\u018d\33\1\uffff\u07f1\33\7\uffff\1\33\3\uffff\44"+
            "\33\1\uffff\57\33\1\uffff\u0fa0\33\1\uffff\ua7ff\33\u0800\uffff"+
            "\u1fff\33",
            "\1\112\37\uffff\1\112",
            "\1\113\37\uffff\1\113",
            "\1\114\37\uffff\1\114",
            "\12\33\7\uffff\32\33\6\uffff\32\33\5\uffff\40\33\1\uffff\u15df"+
            "\33\1\uffff\u018d\33\1\uffff\u07f1\33\7\uffff\1\33\3\uffff\44"+
            "\33\1\uffff\57\33\1\uffff\u0fa0\33\1\uffff\ua7ff\33\u0800\uffff"+
            "\u1fff\33",
            "\12\33\7\uffff\32\33\6\uffff\32\33\5\uffff\40\33\1\uffff\u15df"+
            "\33\1\uffff\u018d\33\1\uffff\u07f1\33\7\uffff\1\33\3\uffff\44"+
            "\33\1\uffff\57\33\1\uffff\u0fa0\33\1\uffff\ua7ff\33\u0800\uffff"+
            "\u1fff\33",
            "\1\116\37\uffff\1\116",
            "\1\117\37\uffff\1\117",
            "\1\120\37\uffff\1\120",
            "\1\121\37\uffff\1\121",
            "",
            "\12\33\7\uffff\32\33\6\uffff\32\33\5\uffff\40\33\1\uffff\u15df"+
            "\33\1\uffff\u018d\33\1\uffff\u07f1\33\7\uffff\1\33\3\uffff\44"+
            "\33\1\uffff\57\33\1\uffff\u0fa0\33\1\uffff\ua7ff\33\u0800\uffff"+
            "\u1fff\33",
            "\12\33\7\uffff\32\33\6\uffff\32\33\5\uffff\40\33\1\uffff\u15df"+
            "\33\1\uffff\u018d\33\1\uffff\u07f1\33\7\uffff\1\33\3\uffff\44"+
            "\33\1\uffff\57\33\1\uffff\u0fa0\33\1\uffff\ua7ff\33\u0800\uffff"+
            "\u1fff\33",
            "\1\123\37\uffff\1\123",
            "",
            "\1\124\37\uffff\1\124",
            "\12\33\7\uffff\32\33\6\uffff\32\33\5\uffff\40\33\1\uffff\u15df"+
            "\33\1\uffff\u018d\33\1\uffff\u07f1\33\7\uffff\1\33\3\uffff\44"+
            "\33\1\uffff\57\33\1\uffff\u0fa0\33\1\uffff\ua7ff\33\u0800\uffff"+
            "\u1fff\33",
            "\12\33\7\uffff\32\33\6\uffff\32\33\5\uffff\40\33\1\uffff\u15df"+
            "\33\1\uffff\u018d\33\1\uffff\u07f1\33\7\uffff\1\33\3\uffff\44"+
            "\33\1\uffff\57\33\1\uffff\u0fa0\33\1\uffff\ua7ff\33\u0800\uffff"+
            "\u1fff\33",
            "\12\33\7\uffff\32\33\6\uffff\32\33\5\uffff\40\33\1\uffff\u15df"+
            "\33\1\uffff\u018d\33\1\uffff\u07f1\33\7\uffff\1\33\3\uffff\44"+
            "\33\1\uffff\57\33\1\uffff\u0fa0\33\1\uffff\ua7ff\33\u0800\uffff"+
            "\u1fff\33",
            "",
            "\1\127\37\uffff\1\127",
            "\1\130\37\uffff\1\130",
            "",
            "",
            "\1\131\37\uffff\1\131",
            "\12\33\7\uffff\32\33\6\uffff\32\33\5\uffff\40\33\1\uffff\u15df"+
            "\33\1\uffff\u018d\33\1\uffff\u07f1\33\7\uffff\1\33\3\uffff\44"+
            "\33\1\uffff\57\33\1\uffff\u0fa0\33\1\uffff\ua7ff\33\u0800\uffff"+
            "\u1fff\33",
            "\12\33\7\uffff\32\33\6\uffff\32\33\5\uffff\40\33\1\uffff\u15df"+
            "\33\1\uffff\u018d\33\1\uffff\u07f1\33\7\uffff\1\33\3\uffff\44"+
            "\33\1\uffff\57\33\1\uffff\u0fa0\33\1\uffff\ua7ff\33\u0800\uffff"+
            "\u1fff\33",
            "",
            ""
    };

    static final short[] DFA14_eot = DFA.unpackEncodedString(DFA14_eotS);
    static final short[] DFA14_eof = DFA.unpackEncodedString(DFA14_eofS);
    static final char[] DFA14_min = DFA.unpackEncodedStringToUnsignedChars(DFA14_minS);
    static final char[] DFA14_max = DFA.unpackEncodedStringToUnsignedChars(DFA14_maxS);
    static final short[] DFA14_accept = DFA.unpackEncodedString(DFA14_acceptS);
    static final short[] DFA14_special = DFA.unpackEncodedString(DFA14_specialS);
    static final short[][] DFA14_transition;

    static {
        int numStates = DFA14_transitionS.length;
        DFA14_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA14_transition[i] = DFA.unpackEncodedString(DFA14_transitionS[i]);
        }
    }

    class DFA14 extends DFA {

        public DFA14(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 14;
            this.eot = DFA14_eot;
            this.eof = DFA14_eof;
            this.min = DFA14_min;
            this.max = DFA14_max;
            this.accept = DFA14_accept;
            this.special = DFA14_special;
            this.transition = DFA14_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( BACKSLASH | LPAREN | RPAREN | COMMA | DEREF | AND | OR | NOT | SELECT | FROM | WHERE | OBJECT | EDITABLE | BOOLEAN_VALUE | UNARY_OPERATOR | BINARY_OPERATOR | TOKEN_START_CHAR | TOKEN | ESC | STRING_VALUE | DIGIT | INT_VALUE | NUMERIC_ARG | WS );";
        }
    }
 

}
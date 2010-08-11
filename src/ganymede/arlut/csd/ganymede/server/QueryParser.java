// $ANTLR 3.2 Sep 23, 2009 12:02:23 /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g 2010-08-10 20:52:04

package arlut.csd.ganymede.server;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.antlr.runtime.tree.*;

public class QueryParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "SELECT", "OBJECT", "COMMA", "FROM", "EDITABLE", "WHERE", "AND", "OR", "LPAREN", "RPAREN", "NOT", "UNARY_OPERATOR", "BINARY_OPERATOR", "DEREF", "TOKEN", "STRING_VALUE", "INT_VALUE", "DECIMAL_VALUE", "BOOLEAN_VALUE", "BACKSLASH", "TOKEN_START_CHAR", "DIGIT", "ESC", "NUMERIC_ARG", "WS"
    };
    public static final int WHERE=9;
    public static final int TOKEN_START_CHAR=24;
    public static final int ESC=26;
    public static final int DECIMAL_VALUE=21;
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


        public QueryParser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public QueryParser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        
    protected TreeAdaptor adaptor = new CommonTreeAdaptor();

    public void setTreeAdaptor(TreeAdaptor adaptor) {
        this.adaptor = adaptor;
    }
    public TreeAdaptor getTreeAdaptor() {
        return adaptor;
    }

    public String[] getTokenNames() { return QueryParser.tokenNames; }
    public String getGrammarFileName() { return "/home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g"; }


    public static class query_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "query"
    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:60:1: query : select_clause from_clause ( where_clause )? EOF ;
    public final QueryParser.query_return query() throws RecognitionException {
        QueryParser.query_return retval = new QueryParser.query_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token EOF4=null;
        QueryParser.select_clause_return select_clause1 = null;

        QueryParser.from_clause_return from_clause2 = null;

        QueryParser.where_clause_return where_clause3 = null;


        Object EOF4_tree=null;

        try {
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:60:7: ( select_clause from_clause ( where_clause )? EOF )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:61:8: select_clause from_clause ( where_clause )? EOF
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_select_clause_in_query45);
            select_clause1=select_clause();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, select_clause1.getTree());
            pushFollow(FOLLOW_from_clause_in_query54);
            from_clause2=from_clause();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, from_clause2.getTree());
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:63:8: ( where_clause )?
            int alt1=2;
            int LA1_0 = input.LA(1);

            if ( (LA1_0==WHERE) ) {
                alt1=1;
            }
            switch (alt1) {
                case 1 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:63:8: where_clause
                    {
                    pushFollow(FOLLOW_where_clause_in_query63);
                    where_clause3=where_clause();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, where_clause3.getTree());

                    }
                    break;

            }

            EOF4=(Token)match(input,EOF,FOLLOW_EOF_in_query73); if (state.failed) return retval;

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "query"

    public static class select_clause_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "select_clause"
    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:67:1: select_clause : SELECT ( ( OBJECT )? | ( token ( COMMA token )* ) ) ;
    public final QueryParser.select_clause_return select_clause() throws RecognitionException {
        QueryParser.select_clause_return retval = new QueryParser.select_clause_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token SELECT5=null;
        Token OBJECT6=null;
        Token COMMA8=null;
        QueryParser.token_return token7 = null;

        QueryParser.token_return token9 = null;


        Object SELECT5_tree=null;
        Object OBJECT6_tree=null;
        Object COMMA8_tree=null;

        try {
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:67:14: ( SELECT ( ( OBJECT )? | ( token ( COMMA token )* ) ) )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:68:8: SELECT ( ( OBJECT )? | ( token ( COMMA token )* ) )
            {
            root_0 = (Object)adaptor.nil();

            SELECT5=(Token)match(input,SELECT,FOLLOW_SELECT_in_select_clause96); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            SELECT5_tree = (Object)adaptor.create(SELECT5);
            root_0 = (Object)adaptor.becomeRoot(SELECT5_tree, root_0);
            }
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:68:16: ( ( OBJECT )? | ( token ( COMMA token )* ) )
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0==OBJECT||LA4_0==FROM) ) {
                alt4=1;
            }
            else if ( ((LA4_0>=TOKEN && LA4_0<=STRING_VALUE)) ) {
                alt4=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("", 4, 0, input);

                throw nvae;
            }
            switch (alt4) {
                case 1 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:68:17: ( OBJECT )?
                    {
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:68:17: ( OBJECT )?
                    int alt2=2;
                    int LA2_0 = input.LA(1);

                    if ( (LA2_0==OBJECT) ) {
                        alt2=1;
                    }
                    switch (alt2) {
                        case 1 :
                            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:68:17: OBJECT
                            {
                            OBJECT6=(Token)match(input,OBJECT,FOLLOW_OBJECT_in_select_clause100); if (state.failed) return retval;
                            if ( state.backtracking==0 ) {
                            OBJECT6_tree = (Object)adaptor.create(OBJECT6);
                            adaptor.addChild(root_0, OBJECT6_tree);
                            }

                            }
                            break;

                    }


                    }
                    break;
                case 2 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:68:27: ( token ( COMMA token )* )
                    {
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:68:27: ( token ( COMMA token )* )
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:68:28: token ( COMMA token )*
                    {
                    pushFollow(FOLLOW_token_in_select_clause106);
                    token7=token();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, token7.getTree());
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:68:34: ( COMMA token )*
                    loop3:
                    do {
                        int alt3=2;
                        int LA3_0 = input.LA(1);

                        if ( (LA3_0==COMMA) ) {
                            alt3=1;
                        }


                        switch (alt3) {
                    	case 1 :
                    	    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:68:35: COMMA token
                    	    {
                    	    COMMA8=(Token)match(input,COMMA,FOLLOW_COMMA_in_select_clause109); if (state.failed) return retval;
                    	    pushFollow(FOLLOW_token_in_select_clause112);
                    	    token9=token();

                    	    state._fsp--;
                    	    if (state.failed) return retval;
                    	    if ( state.backtracking==0 ) adaptor.addChild(root_0, token9.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop3;
                        }
                    } while (true);


                    }


                    }
                    break;

            }


            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "select_clause"

    public static class from_clause_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "from_clause"
    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:71:1: from_clause : FROM ( EDITABLE )? token ;
    public final QueryParser.from_clause_return from_clause() throws RecognitionException {
        QueryParser.from_clause_return retval = new QueryParser.from_clause_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token FROM10=null;
        Token EDITABLE11=null;
        QueryParser.token_return token12 = null;


        Object FROM10_tree=null;
        Object EDITABLE11_tree=null;

        try {
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:71:12: ( FROM ( EDITABLE )? token )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:72:8: FROM ( EDITABLE )? token
            {
            root_0 = (Object)adaptor.nil();

            FROM10=(Token)match(input,FROM,FOLLOW_FROM_in_from_clause138); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            FROM10_tree = (Object)adaptor.create(FROM10);
            root_0 = (Object)adaptor.becomeRoot(FROM10_tree, root_0);
            }
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:72:14: ( EDITABLE )?
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( (LA5_0==EDITABLE) ) {
                alt5=1;
            }
            switch (alt5) {
                case 1 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:72:14: EDITABLE
                    {
                    EDITABLE11=(Token)match(input,EDITABLE,FOLLOW_EDITABLE_in_from_clause141); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    EDITABLE11_tree = (Object)adaptor.create(EDITABLE11);
                    adaptor.addChild(root_0, EDITABLE11_tree);
                    }

                    }
                    break;

            }

            pushFollow(FOLLOW_token_in_from_clause144);
            token12=token();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, token12.getTree());

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "from_clause"

    public static class where_clause_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "where_clause"
    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:75:1: where_clause : WHERE expression ;
    public final QueryParser.where_clause_return where_clause() throws RecognitionException {
        QueryParser.where_clause_return retval = new QueryParser.where_clause_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token WHERE13=null;
        QueryParser.expression_return expression14 = null;


        Object WHERE13_tree=null;

        try {
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:75:13: ( WHERE expression )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:76:8: WHERE expression
            {
            root_0 = (Object)adaptor.nil();

            WHERE13=(Token)match(input,WHERE,FOLLOW_WHERE_in_where_clause166); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            WHERE13_tree = (Object)adaptor.create(WHERE13);
            root_0 = (Object)adaptor.becomeRoot(WHERE13_tree, root_0);
            }
            pushFollow(FOLLOW_expression_in_where_clause176);
            expression14=expression();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, expression14.getTree());

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "where_clause"

    public static class expression_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "expression"
    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:79:1: expression : atom ( ( AND | OR ) atom )* ;
    public final QueryParser.expression_return expression() throws RecognitionException {
        QueryParser.expression_return retval = new QueryParser.expression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token AND16=null;
        Token OR17=null;
        QueryParser.atom_return atom15 = null;

        QueryParser.atom_return atom18 = null;


        Object AND16_tree=null;
        Object OR17_tree=null;

        try {
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:79:11: ( atom ( ( AND | OR ) atom )* )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:80:8: atom ( ( AND | OR ) atom )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_atom_in_expression190);
            atom15=atom();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, atom15.getTree());
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:80:13: ( ( AND | OR ) atom )*
            loop7:
            do {
                int alt7=2;
                int LA7_0 = input.LA(1);

                if ( ((LA7_0>=AND && LA7_0<=OR)) ) {
                    alt7=1;
                }


                switch (alt7) {
            	case 1 :
            	    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:80:14: ( AND | OR ) atom
            	    {
            	    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:80:14: ( AND | OR )
            	    int alt6=2;
            	    int LA6_0 = input.LA(1);

            	    if ( (LA6_0==AND) ) {
            	        alt6=1;
            	    }
            	    else if ( (LA6_0==OR) ) {
            	        alt6=2;
            	    }
            	    else {
            	        if (state.backtracking>0) {state.failed=true; return retval;}
            	        NoViableAltException nvae =
            	            new NoViableAltException("", 6, 0, input);

            	        throw nvae;
            	    }
            	    switch (alt6) {
            	        case 1 :
            	            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:80:15: AND
            	            {
            	            AND16=(Token)match(input,AND,FOLLOW_AND_in_expression194); if (state.failed) return retval;
            	            if ( state.backtracking==0 ) {
            	            AND16_tree = (Object)adaptor.create(AND16);
            	            root_0 = (Object)adaptor.becomeRoot(AND16_tree, root_0);
            	            }

            	            }
            	            break;
            	        case 2 :
            	            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:80:22: OR
            	            {
            	            OR17=(Token)match(input,OR,FOLLOW_OR_in_expression199); if (state.failed) return retval;
            	            if ( state.backtracking==0 ) {
            	            OR17_tree = (Object)adaptor.create(OR17);
            	            root_0 = (Object)adaptor.becomeRoot(OR17_tree, root_0);
            	            }

            	            }
            	            break;

            	    }

            	    pushFollow(FOLLOW_atom_in_expression203);
            	    atom18=atom();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, atom18.getTree());

            	    }
            	    break;

            	default :
            	    break loop7;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "expression"

    public static class atom_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "atom"
    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:83:1: atom : ( LPAREN expression RPAREN | NOT atom | ( token ( UNARY_OPERATOR | BINARY_OPERATOR ) )=> simple_expression | ( token DEREF )=> deref_expression );
    public final QueryParser.atom_return atom() throws RecognitionException {
        QueryParser.atom_return retval = new QueryParser.atom_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LPAREN19=null;
        Token RPAREN21=null;
        Token NOT22=null;
        QueryParser.expression_return expression20 = null;

        QueryParser.atom_return atom23 = null;

        QueryParser.simple_expression_return simple_expression24 = null;

        QueryParser.deref_expression_return deref_expression25 = null;


        Object LPAREN19_tree=null;
        Object RPAREN21_tree=null;
        Object NOT22_tree=null;

        try {
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:83:5: ( LPAREN expression RPAREN | NOT atom | ( token ( UNARY_OPERATOR | BINARY_OPERATOR ) )=> simple_expression | ( token DEREF )=> deref_expression )
            int alt8=4;
            switch ( input.LA(1) ) {
            case LPAREN:
                {
                alt8=1;
                }
                break;
            case NOT:
                {
                alt8=2;
                }
                break;
            case TOKEN:
            case STRING_VALUE:
                {
                int LA8_3 = input.LA(2);

                if ( (LA8_3==DEREF) && (synpred2_Query())) {
                    alt8=4;
                }
                else if ( (LA8_3==UNARY_OPERATOR) && (synpred1_Query())) {
                    alt8=3;
                }
                else if ( (LA8_3==BINARY_OPERATOR) && (synpred1_Query())) {
                    alt8=3;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 8, 3, input);

                    throw nvae;
                }
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("", 8, 0, input);

                throw nvae;
            }

            switch (alt8) {
                case 1 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:84:8: LPAREN expression RPAREN
                    {
                    root_0 = (Object)adaptor.nil();

                    LPAREN19=(Token)match(input,LPAREN,FOLLOW_LPAREN_in_atom227); if (state.failed) return retval;
                    pushFollow(FOLLOW_expression_in_atom230);
                    expression20=expression();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, expression20.getTree());
                    RPAREN21=(Token)match(input,RPAREN,FOLLOW_RPAREN_in_atom232); if (state.failed) return retval;

                    }
                    break;
                case 2 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:85:10: NOT atom
                    {
                    root_0 = (Object)adaptor.nil();

                    NOT22=(Token)match(input,NOT,FOLLOW_NOT_in_atom244); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    NOT22_tree = (Object)adaptor.create(NOT22);
                    root_0 = (Object)adaptor.becomeRoot(NOT22_tree, root_0);
                    }
                    pushFollow(FOLLOW_atom_in_atom247);
                    atom23=atom();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, atom23.getTree());

                    }
                    break;
                case 3 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:86:10: ( token ( UNARY_OPERATOR | BINARY_OPERATOR ) )=> simple_expression
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_simple_expression_in_atom272);
                    simple_expression24=simple_expression();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, simple_expression24.getTree());

                    }
                    break;
                case 4 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:87:10: ( token DEREF )=> deref_expression
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_deref_expression_in_atom291);
                    deref_expression25=deref_expression();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, deref_expression25.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "atom"

    public static class simple_expression_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "simple_expression"
    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:90:1: simple_expression : token ( UNARY_OPERATOR | ( BINARY_OPERATOR argument ) ) ;
    public final QueryParser.simple_expression_return simple_expression() throws RecognitionException {
        QueryParser.simple_expression_return retval = new QueryParser.simple_expression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token UNARY_OPERATOR27=null;
        Token BINARY_OPERATOR28=null;
        QueryParser.token_return token26 = null;

        QueryParser.argument_return argument29 = null;


        Object UNARY_OPERATOR27_tree=null;
        Object BINARY_OPERATOR28_tree=null;

        try {
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:90:18: ( token ( UNARY_OPERATOR | ( BINARY_OPERATOR argument ) ) )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:91:8: token ( UNARY_OPERATOR | ( BINARY_OPERATOR argument ) )
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_token_in_simple_expression313);
            token26=token();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, token26.getTree());
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:91:14: ( UNARY_OPERATOR | ( BINARY_OPERATOR argument ) )
            int alt9=2;
            int LA9_0 = input.LA(1);

            if ( (LA9_0==UNARY_OPERATOR) ) {
                alt9=1;
            }
            else if ( (LA9_0==BINARY_OPERATOR) ) {
                alt9=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("", 9, 0, input);

                throw nvae;
            }
            switch (alt9) {
                case 1 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:91:15: UNARY_OPERATOR
                    {
                    UNARY_OPERATOR27=(Token)match(input,UNARY_OPERATOR,FOLLOW_UNARY_OPERATOR_in_simple_expression316); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    UNARY_OPERATOR27_tree = (Object)adaptor.create(UNARY_OPERATOR27);
                    root_0 = (Object)adaptor.becomeRoot(UNARY_OPERATOR27_tree, root_0);
                    }

                    }
                    break;
                case 2 :
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:91:33: ( BINARY_OPERATOR argument )
                    {
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:91:33: ( BINARY_OPERATOR argument )
                    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:91:34: BINARY_OPERATOR argument
                    {
                    BINARY_OPERATOR28=(Token)match(input,BINARY_OPERATOR,FOLLOW_BINARY_OPERATOR_in_simple_expression322); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    BINARY_OPERATOR28_tree = (Object)adaptor.create(BINARY_OPERATOR28);
                    root_0 = (Object)adaptor.becomeRoot(BINARY_OPERATOR28_tree, root_0);
                    }
                    pushFollow(FOLLOW_argument_in_simple_expression325);
                    argument29=argument();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, argument29.getTree());

                    }


                    }
                    break;

            }


            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "simple_expression"

    public static class deref_expression_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "deref_expression"
    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:94:1: deref_expression : token DEREF atom ;
    public final QueryParser.deref_expression_return deref_expression() throws RecognitionException {
        QueryParser.deref_expression_return retval = new QueryParser.deref_expression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token DEREF31=null;
        QueryParser.token_return token30 = null;

        QueryParser.atom_return atom32 = null;


        Object DEREF31_tree=null;

        try {
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:94:17: ( token DEREF atom )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:95:8: token DEREF atom
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_token_in_deref_expression349);
            token30=token();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, token30.getTree());
            DEREF31=(Token)match(input,DEREF,FOLLOW_DEREF_in_deref_expression351); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            DEREF31_tree = (Object)adaptor.create(DEREF31);
            root_0 = (Object)adaptor.becomeRoot(DEREF31_tree, root_0);
            }
            pushFollow(FOLLOW_atom_in_deref_expression354);
            atom32=atom();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, atom32.getTree());

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "deref_expression"

    public static class token_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "token"
    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:98:1: token : ( TOKEN | STRING_VALUE );
    public final QueryParser.token_return token() throws RecognitionException {
        QueryParser.token_return retval = new QueryParser.token_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set33=null;

        Object set33_tree=null;

        try {
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:99:3: ( TOKEN | STRING_VALUE )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:
            {
            root_0 = (Object)adaptor.nil();

            set33=(Token)input.LT(1);
            if ( (input.LA(1)>=TOKEN && input.LA(1)<=STRING_VALUE) ) {
                input.consume();
                if ( state.backtracking==0 ) adaptor.addChild(root_0, (Object)adaptor.create(set33));
                state.errorRecovery=false;state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return retval;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }


            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "token"

    public static class argument_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "argument"
    // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:103:1: argument : ( STRING_VALUE | INT_VALUE | DECIMAL_VALUE | BOOLEAN_VALUE );
    public final QueryParser.argument_return argument() throws RecognitionException {
        QueryParser.argument_return retval = new QueryParser.argument_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set34=null;

        Object set34_tree=null;

        try {
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:104:3: ( STRING_VALUE | INT_VALUE | DECIMAL_VALUE | BOOLEAN_VALUE )
            // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:
            {
            root_0 = (Object)adaptor.nil();

            set34=(Token)input.LT(1);
            if ( (input.LA(1)>=STRING_VALUE && input.LA(1)<=BOOLEAN_VALUE) ) {
                input.consume();
                if ( state.backtracking==0 ) adaptor.addChild(root_0, (Object)adaptor.create(set34));
                state.errorRecovery=false;state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return retval;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }


            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "argument"

    // $ANTLR start synpred1_Query
    public final void synpred1_Query_fragment() throws RecognitionException {   
        // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:86:10: ( token ( UNARY_OPERATOR | BINARY_OPERATOR ) )
        // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:86:11: token ( UNARY_OPERATOR | BINARY_OPERATOR )
        {
        pushFollow(FOLLOW_token_in_synpred1_Query259);
        token();

        state._fsp--;
        if (state.failed) return ;
        if ( (input.LA(1)>=UNARY_OPERATOR && input.LA(1)<=BINARY_OPERATOR) ) {
            input.consume();
            state.errorRecovery=false;state.failed=false;
        }
        else {
            if (state.backtracking>0) {state.failed=true; return ;}
            MismatchedSetException mse = new MismatchedSetException(null,input);
            throw mse;
        }


        }
    }
    // $ANTLR end synpred1_Query

    // $ANTLR start synpred2_Query
    public final void synpred2_Query_fragment() throws RecognitionException {   
        // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:87:10: ( token DEREF )
        // /home/broccol/ganymede/src/trunk/src/ganymede/arlut/csd/ganymede/server/Query.g:87:11: token DEREF
        {
        pushFollow(FOLLOW_token_in_synpred2_Query284);
        token();

        state._fsp--;
        if (state.failed) return ;
        match(input,DEREF,FOLLOW_DEREF_in_synpred2_Query286); if (state.failed) return ;

        }
    }
    // $ANTLR end synpred2_Query

    // Delegated rules

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


 

    public static final BitSet FOLLOW_select_clause_in_query45 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_from_clause_in_query54 = new BitSet(new long[]{0x0000000000000200L});
    public static final BitSet FOLLOW_where_clause_in_query63 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_query73 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SELECT_in_select_clause96 = new BitSet(new long[]{0x00000000000C0022L});
    public static final BitSet FOLLOW_OBJECT_in_select_clause100 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_token_in_select_clause106 = new BitSet(new long[]{0x0000000000000042L});
    public static final BitSet FOLLOW_COMMA_in_select_clause109 = new BitSet(new long[]{0x00000000000C0060L});
    public static final BitSet FOLLOW_token_in_select_clause112 = new BitSet(new long[]{0x0000000000000042L});
    public static final BitSet FOLLOW_FROM_in_from_clause138 = new BitSet(new long[]{0x00000000000C0120L});
    public static final BitSet FOLLOW_EDITABLE_in_from_clause141 = new BitSet(new long[]{0x00000000000C0020L});
    public static final BitSet FOLLOW_token_in_from_clause144 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_WHERE_in_where_clause166 = new BitSet(new long[]{0x00000000000FD020L});
    public static final BitSet FOLLOW_expression_in_where_clause176 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_atom_in_expression190 = new BitSet(new long[]{0x0000000000000C02L});
    public static final BitSet FOLLOW_AND_in_expression194 = new BitSet(new long[]{0x00000000000FD020L});
    public static final BitSet FOLLOW_OR_in_expression199 = new BitSet(new long[]{0x00000000000FD020L});
    public static final BitSet FOLLOW_atom_in_expression203 = new BitSet(new long[]{0x0000000000000C02L});
    public static final BitSet FOLLOW_LPAREN_in_atom227 = new BitSet(new long[]{0x00000000000FD020L});
    public static final BitSet FOLLOW_expression_in_atom230 = new BitSet(new long[]{0x0000000000002000L});
    public static final BitSet FOLLOW_RPAREN_in_atom232 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NOT_in_atom244 = new BitSet(new long[]{0x00000000000FD020L});
    public static final BitSet FOLLOW_atom_in_atom247 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_simple_expression_in_atom272 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_deref_expression_in_atom291 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_token_in_simple_expression313 = new BitSet(new long[]{0x0000000000018000L});
    public static final BitSet FOLLOW_UNARY_OPERATOR_in_simple_expression316 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_BINARY_OPERATOR_in_simple_expression322 = new BitSet(new long[]{0x0000000000780000L});
    public static final BitSet FOLLOW_argument_in_simple_expression325 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_token_in_deref_expression349 = new BitSet(new long[]{0x0000000000020000L});
    public static final BitSet FOLLOW_DEREF_in_deref_expression351 = new BitSet(new long[]{0x00000000000FD020L});
    public static final BitSet FOLLOW_atom_in_deref_expression354 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_token0 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_argument0 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_token_in_synpred1_Query259 = new BitSet(new long[]{0x0000000000018000L});
    public static final BitSet FOLLOW_set_in_synpred1_Query261 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_token_in_synpred2_Query284 = new BitSet(new long[]{0x0000000000020000L});
    public static final BitSet FOLLOW_DEREF_in_synpred2_Query286 = new BitSet(new long[]{0x0000000000000002L});

}
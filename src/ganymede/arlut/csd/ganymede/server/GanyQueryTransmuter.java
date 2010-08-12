/*

   GanyQueryTransmuter.java

   This is the textual query processor for the Ganymede server.  It
   takes a string query, applies an ANTLR grammar to it, and generates
   a traditional Ganymede-style arlut.csd.ganymede.common.Query out of
   it.
   
   Created: 31 August 2004

   Module By: Deepak Giridharagopal, Jonathan Abbey

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

package arlut.csd.ganymede.server;

import arlut.csd.ganymede.common.GanyParseException;
import arlut.csd.ganymede.common.DumpResult;
import arlut.csd.ganymede.common.FieldType;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryAndNode;
import arlut.csd.ganymede.common.QueryDeRefNode;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.QueryNode;
import arlut.csd.ganymede.common.QueryNotNode;
import arlut.csd.ganymede.common.QueryOrNode;

import arlut.csd.Util.StringUtils;
import arlut.csd.Util.TranslationService;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;

import java.io.StringReader;

import java.text.DateFormat;
import java.text.ParseException;

import java.util.ArrayList;
import java.util.HashMap;


/*------------------------------------------------------------------------------
                                                                           class
                                                             GanyQueryTransmuter

------------------------------------------------------------------------------*/

/**
 * This class processes textual queries using an ANTLR-generated parser, and
 * generates an old, Ganymede-style arlut.csd.ganymede.common.Query, with attendant
 * arlut.csd.ganymede.common.QueryNode tree.
 *
 * @see arlut.csd.ganymede.common.QueryNode
 * @see arlut.csd.ganymede.common.Query
 *
 * @author Deepak Giridharagopal, deepak@arlut.utexas.edu
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu
 */

public class GanyQueryTransmuter {

  private static HashMap op_scalar_mapping;
  private static HashMap op_vector_mapping;
  private static HashMap validity_mapping;

  /**
   * TranslationService object for handling string localization in the Ganymede
   * server.
   */

  static TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.GanyQueryTransmuter");

  static
  {
    // initialize op_scalar_mapping

    op_scalar_mapping = new HashMap();
    op_scalar_mapping.put("=~", Byte.valueOf(QueryDataNode.MATCHES));
    op_scalar_mapping.put("=~_ci", Byte.valueOf(QueryDataNode.NOCASEMATCHES));
    op_scalar_mapping.put("==", Byte.valueOf(QueryDataNode.EQUALS));
    op_scalar_mapping.put("==_ci", Byte.valueOf(QueryDataNode.NOCASEEQ));
    op_scalar_mapping.put("<", Byte.valueOf(QueryDataNode.LESS));
    op_scalar_mapping.put("<=", Byte.valueOf(QueryDataNode.LESSEQ));
    op_scalar_mapping.put(">", Byte.valueOf(QueryDataNode.GREAT));
    op_scalar_mapping.put(">=", Byte.valueOf(QueryDataNode.GREATEQ));
    op_scalar_mapping.put("starts", Byte.valueOf(QueryDataNode.STARTSWITH));
    op_scalar_mapping.put("ends", Byte.valueOf(QueryDataNode.ENDSWITH));
    op_scalar_mapping.put("defined", Byte.valueOf(QueryDataNode.DEFINED));

    // initialize op_vector_mapping

    op_vector_mapping = new HashMap();
    op_vector_mapping.put("len<", Byte.valueOf(QueryDataNode.LENGTHLE));
    op_vector_mapping.put("len<=", Byte.valueOf(QueryDataNode.LENGTHLEEQ));
    op_vector_mapping.put("len>", Byte.valueOf(QueryDataNode.LENGTHGR));
    op_vector_mapping.put("len>=", Byte.valueOf(QueryDataNode.LENGTHGREQ));
    op_vector_mapping.put("len==", Byte.valueOf(QueryDataNode.LENGTHEQ));

    validity_mapping = new HashMap();
    validity_mapping.put(Integer.valueOf(FieldType.DATE), new String[] {"<", ">", "<=", ">=", "==", "defined"});
    validity_mapping.put(Integer.valueOf(FieldType.NUMERIC), new String[] {"<", ">", "<=", ">=", "==", "defined"});
    validity_mapping.put(Integer.valueOf(FieldType.FLOAT), new String[] {"<", ">", "<=", ">=", "==", "defined"});
    validity_mapping.put(Integer.valueOf(FieldType.BOOLEAN), new String[] {"==", "defined"});
    validity_mapping.put(Integer.valueOf(FieldType.IP), new String[] {"==", "=~", "==_ci", "=~_ci", "starts", "ends", "defined"});
    validity_mapping.put(Integer.valueOf(FieldType.STRING), new String[] {"<", ">", "<=", ">=", "=~", "=~_ci", "==", "==_ci", "starts", "ends", "defined"});
    validity_mapping.put(Integer.valueOf(FieldType.INVID), new String[] {"<", ">", "<=", ">=", "=~", "=~_ci", "==", "==_ci", "starts", "ends", "defined"});
    validity_mapping.put(Integer.valueOf(FieldType.PASSWORD), new String[] {"defined"});
    validity_mapping.put(Integer.valueOf(FieldType.PERMISSIONMATRIX), new String[] {"defined"});
    validity_mapping.put(Integer.valueOf(FieldType.FIELDOPTIONS), new String[] {"defined"});
  }

  // ---

  DBObjectBase objectBase = null;
  ArrayList selectFields = null;
  boolean editableFilter = false;
  String myQueryString = null;
  CommonTree myQueryTree = null;

  public GanyQueryTransmuter()
  {
  }

  public Query transmuteQueryString(String queryString) throws GanyParseException
  {
    myQueryString = queryString;

    QueryLexer lexer = new QueryLexer(new ANTLRStringStream(queryString));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    QueryParser parser = new QueryParser(tokens);

    try
      {
	myQueryTree = (CommonTree) parser.query().getTree();
      }
    catch (RecognitionException ex)
      {
      }

    if (myQueryTree == null)
      {
	// "Error parsing GanyQL query string.  Make sure you've parenthesized and quoted everything properly.\n\n{0}"
	throw new GanyParseException(ts.l("transmuteQueryString.global_parse_error", queryString));
      }

    QueryNode root = null;

    try
      {
	root = parse_tree(myQueryTree);
      }
    catch (RuntimeException ex)
      {
	// "An exception was encountered parsing your query string: {0}\nQuery: "{1}"\nExpanded Parse Tree: "{2}""
	String mesg = ts.l("global.parse_exception", ex.getMessage(), queryString, myQueryTree.toStringTree());
	throw new RuntimeException(mesg, ex);
      }

    Query query = new Query(objectBase.getName(), root, this.editableFilter);

    if (selectFields == null)
      {
	query.resetPermitSet(); // return the default list of fields
      }
    else
      {
	for (int i = 0; i < selectFields.size(); i ++)
	  {
	    DBObjectBaseField field = (DBObjectBaseField) selectFields.get(i);
	    query.addField(field.getID());
	  }
      }

    // clear out our refs for GC

    objectBase = null;

    if (selectFields != null)
      {
	selectFields.clear();
	selectFields = null;
      }

    // et voila

    return query;
  }

  private QueryNode parse_tree(Tree ast) throws GanyParseException
  {
    this.selectFields = parse_select_tree(ast.getChild(0));
    this.objectBase = parse_from_tree(ast.getChild(1));

    if (ast.getChildCount() > 2)
      {
	Tree whereTokenNode = ast.getChild(2);

	if (whereTokenNode != null && whereTokenNode.getType() == QueryParser.WHERE)
	  {
	    Tree where_node = whereTokenNode.getChild(0);

	    if (where_node != null)
	      {
		QueryNode where_tree = parse_where_clause(where_node, objectBase);
	    
		return where_tree;
	      }
	  }
      }

    return null;
  }

  private DBObjectBase parse_from_tree(Tree ast) throws GanyParseException
  {
    String from_objectbase = null;
    Tree node = ast.getChild(0);

    if (node == null)		// the grammar _should_ prevent this
      {
	// "An exception was encountered parsing your query string: {0}\nQuery: "{1}"\nExpanded Parse Tree: "{2}""
	// "Missing from clause."
	throw new GanyParseException(ts.l("global.parse_exception", ts.l("parse_from_tree.missing_from"), myQueryString, myQueryTree.toStringTree()));
      }

    for (int i = 0; i < node.getChildCount(); i++)
      {
	Tree subNode = node.getChild(i);

	if (node.getType() == QueryParser.EDITABLE)
	  {
	    this.editableFilter = true;
	  }
	else if (node.getType() == QueryParser.STRING_VALUE)
	  {
	    from_objectbase = StringUtils.dequote(node.getText());
	  }
	else if (node.getType() == QueryParser.TOKEN)
	  {
	    from_objectbase = node.getText();
	  }
      }

    if (from_objectbase == null) // the grammar _should_ prevent this
      {
	// "An exception was encountered parsing your query string: {0}\nQuery: "{1}"\nExpanded Parse Tree: "{2}""
	// "From clause does not contain an object type to search."
	throw new GanyParseException(ts.l("global.parse_exception", ts.l("parse_from_tree.no_objectbase"), myQueryString, myQueryTree.toStringTree()));
      }

    this.objectBase = Ganymede.db.getObjectBase(from_objectbase);

    if (objectBase == null)
      {
	// "An exception was encountered parsing your query string: {0}\nQuery: "{1}"\nExpanded Parse Tree: "{2}""
	// "The object type "{0}" in the query''s from clause does not exist."
	throw new GanyParseException(ts.l("global.parse_exception", 
					  ts.l("parse_from_tree.bad_objectbase", from_objectbase),
					  myQueryString, myQueryTree.toStringTree()));
      }

    return this.objectBase;
  }

  private ArrayList parse_select_tree(Tree ast) throws GanyParseException
  {
    if (ast.getChildCount() == 0)
      {
	return null;		// "select from", with no field list
      }

    if (ast.getChild(0).getType() == QueryParser.OBJECT)
      {
	return null; 		// "select object from", also no field list.
      }

    ArrayList selectFields = new ArrayList();

    for (int i = 0; i < ast.getChildCount(); i++)
      {
	Tree select_node = ast.getChild(i);

	String field_name = StringUtils.dequote(select_node.getText());
	DBObjectBaseField field = (DBObjectBaseField) objectBase.getField(field_name);

	if (field == null)
	  {
	    // "An exception was encountered parsing your query string: {0}\nQuery: "{1}"\nExpanded Parse Tree: "{2}""
	    // "Can''t find field "{0}" in the "{1}" object type.  Make sure you have capitalized the field name correctly."
	    throw new GanyParseException(ts.l("global.parse_exception",
					      ts.l("global.no_such_field", field_name, objectBase.getName()),
					      myQueryString,
					      myQueryTree.toStringTree()));
	  }

	selectFields.add(field);
      }

    return selectFields;
  }

  private QueryNode parse_where_clause(Tree ast, DBObjectBase base) throws GanyParseException
  {
    int root_type;
    QueryNode child1 = null, child2 = null;
    DBObjectBase target_objectbase = null;
    String field_name = null;
    DBObjectBaseField field = null;
    byte scalar_operator = -1, vector_operator = -1;
    int field_type = -1, argument_type = -1;
    String op;
    Object argument;
    Tree field_node, argument_node;
    
    /* -- */

    root_type = ast.getType();

    switch (root_type)
      {
      case QueryParser.NOT:
	return new QueryNotNode(parse_where_clause(ast.getChild(0), base));

      case QueryParser.AND:
	child1 = parse_where_clause(ast.getChild(0), base);
	child2 = parse_where_clause(ast.getChild(1), base);
	return new QueryAndNode(child1, child2);

      case QueryParser.OR:
	child1 = parse_where_clause(ast.getChild(0), base);
	child2 = parse_where_clause(ast.getChild(1), base);
	return new QueryOrNode(child1, child2);

      case QueryParser.DEREF:
	field_name = StringUtils.dequote(ast.getChild(0).getText());

	if (base != null)
	  {
	    field = (DBObjectBaseField) base.getField(field_name);

	    if (field == null)
	      {
		// "An exception was encountered parsing your query string: {0}\nQuery: "{1}"\nExpanded Parse Tree: "{2}""
		// "Can''t find field "{0}" in the "{1}" object type.  Make sure you have capitalized the field name correctly."
		throw new GanyParseException(ts.l("global.parse_exception",
						  ts.l("global.no_such_field", field_name, base.getName()),
						  myQueryString,
						  myQueryTree.toStringTree()));
	      }
	    
	    if (field.getType() != FieldType.INVID)
	      {
		// "An exception was encountered parsing your query string: {0}\nQuery: "{1}"\nExpanded Parse Tree: "{2}""
		// "The "{0}" field can''t be dereferenced.  Not an invid field."
		throw new GanyParseException(ts.l("global.parse_exception",
						  ts.l("parse_where_clause.not_invid", field_name),
						  myQueryString,
						  myQueryTree.toStringTree()));
	      }

	    short target_objectbase_id = field.getTargetBase();

	    if (target_objectbase_id >= 0)
	      {
		target_objectbase = Ganymede.db.getObjectBase(target_objectbase_id);
	      }
	    else
	      {
		target_objectbase = null;
	      }
	  }
	else
	  {
	    target_objectbase = null;
	  }

	child2 = parse_where_clause(ast.getChild(1), target_objectbase);
	
	return new QueryDeRefNode(field_name, child2);

      case QueryParser.BINARY_OPERATOR:
      case QueryParser.UNARY_OPERATOR:

	op = ast.getText();
	field_node = ast.getChild(0);
	field_name = StringUtils.dequote(field_node.getText());

	if (base == null)
	  {
	    field = null;
	  }
	else
	  {
	    field = (DBObjectBaseField) base.getField(field_name);
	    
	    if (field == null)
	      {
		// "An exception was encountered parsing your query string: {0}\nQuery: "{1}"\nExpanded Parse Tree: "{2}""
		// "Can''t find field "{0}" in the "{1}" object type.  Make sure you have capitalized the field name correctly."
		throw new GanyParseException(ts.l("global.parse_exception",
						  ts.l("global.no_such_field", field_name, base.getName()),
						  myQueryString,
						  myQueryTree.toStringTree()));
	      }

	    field_type = field.getType();
	  }

	if (root_type == QueryParser.BINARY_OPERATOR)
	  {
	    argument_node = ast.getChild(1);
	    argument_type = argument_node.getType();
	    argument = parse_argument(op, argument_node.getText(), argument_type, field);
	  }
	else
	  {
	    argument = null;
	  }

        /*
         * If we don't know the objectbase, then we are chasing a
         * vague pointer derefence, and we have to think. We can't
         * check and see if the field we're querying is a scalar or a
         * vector field. Thus, we'll have to trust that the user knows
         * what he wants.  If he's wrong about the vector/scalar type
         * of the field he's querying, then the query engine will try
         * its best to figure out what the user really meant.
         */
         
	if (base == null)
          {
            if (op_vector_mapping.containsKey(op))
              {
                Byte opI = (Byte) op_vector_mapping.get(op);
                vector_operator = opI.byteValue();
                scalar_operator = QueryDataNode.NONE;
              }
            else if (op_scalar_mapping.containsKey(op))
              {
                Byte opI = (Byte) op_scalar_mapping.get(op);
                vector_operator = QueryDataNode.NONE;
                scalar_operator = opI.byteValue();
              }
            else
              {
		// "An exception was encountered parsing your query string: {0}\nQuery: "{1}"\nExpanded Parse Tree: "{2}""
		// "The "{0}" operator makes no sense to me.  GanyQueryTransmuter.java has not been kept up to date with the grammar."
		throw new GanyParseException(ts.l("global.parse_exception",
						  ts.l("parse_where_clause.mystery_operator", op),
						  myQueryString,
						  myQueryTree.toStringTree()));
              }
          }
        else
          {
            if (field.isArray())
              {
                if (op_vector_mapping.containsKey(op))
                  {
                    Byte opI = (Byte) op_vector_mapping.get(op);

                    vector_operator = opI.byteValue();
                    scalar_operator = QueryDataNode.NONE;
                  }
                else if (op_scalar_mapping.containsKey(op))
                  {
                    Byte opI = (Byte) op_scalar_mapping.get(op);
                    scalar_operator = opI.byteValue();
                    vector_operator = QueryDataNode.CONTAINS;
                  }
                else
                  {
		    // "An exception was encountered parsing your query string: {0}\nQuery: "{1}"\nExpanded Parse Tree: "{2}""
		    // "The "{0}" operator makes no sense to me.  GanyQueryTransmuter.java has not been kept up to date with the grammar."
		    throw new GanyParseException(ts.l("global.parse_exception",
						      ts.l("parse_where_clause.mystery_operator", op),
						      myQueryString,
						      myQueryTree.toStringTree()));
                  }
              }
            else
              {
                if (op_scalar_mapping.containsKey(op))
                  {
                    Byte opI = (Byte) op_scalar_mapping.get(op);
                    scalar_operator = opI.byteValue();
                    vector_operator = QueryDataNode.NONE;
                  }
              }
          }
	
	if (base != null && scalar_operator != QueryDataNode.NONE && !valid_op(op, field_type))
	  {
	    // "An exception was encountered parsing your query string: {0}\nQuery: "{1}"\nExpanded Parse Tree: "{2}""
	    // "The "{0}" operator is not valid on a "{1}" object''s "{2}" field.  {2} is of type {3}."
	    throw new GanyParseException(ts.l("global.parse_exception",
					      ts.l("parse_where_clause.bad_operator", op, base.getName(), field_name, field.getTypeDesc()),
					      myQueryString,
					      myQueryTree.toStringTree()));
	  }

	return new QueryDataNode(field_name, scalar_operator, vector_operator, argument);

      default:

	// "An exception was encountered parsing your query string: {0}\nQuery: "{1}"\nExpanded Parse Tree: "{2}""
	// "I couldn''t process parser node type {0}.  GanyQueryTransmuter.java has probably not been kept up to date with the grammar."
	throw new GanyParseException(ts.l("global.parse_exception",
					  ts.l("parse_where_clause.bad_type", Integer.valueOf(root_type)),
					  myQueryString,
					  myQueryTree.toStringTree()));
      }
  }

  private boolean valid_op(String op, int field_type)
  {
    String[] ops = (String[]) validity_mapping.get(Integer.valueOf(field_type));

    if (ops == null)		// should only happen if field_type is invalid
      {
	return false;
      }

    for (int i = 0; i < ops.length; i++)
      {
	if (ops[i].equals(op))
	  {
	    return true;
	  }
      }

    return false;
  }

  private Object parse_argument(String operator, String argument, int argument_type, DBObjectBaseField field) throws GanyParseException
  {
    if (field == null)
      {
	switch (argument_type)
	  {
	  case QueryParser.INT_VALUE:
	    return Integer.valueOf(argument);

	  case QueryParser.DECIMAL_VALUE:
	    return Double.valueOf(argument);

	  case QueryParser.STRING_VALUE:
	    return StringUtils.dequote(argument);

	  default:

	    // "An exception was encountered parsing your query string: {0}\nQuery: "{1}"\nExpanded Parse Tree: "{2}""
	    // "Unrecognized argument type parsing argument {0}."
	    throw new GanyParseException(ts.l("global.parse_exception",
					      ts.l("parse_argument.unrecognized_argument", Integer.valueOf(argument)),
					      myQueryString,
					      myQueryTree.toStringTree()));
	  }
      }

    if (field.isArray() && argument_type == QueryParser.INT_VALUE &&
	op_vector_mapping.containsKey(operator))
      {
	return Integer.valueOf(argument);
      } 

    int field_type = field.getType();

    if (field_type == FieldType.NUMERIC && argument_type == QueryParser.INT_VALUE)
      {
	return Integer.valueOf(argument);
      }
    else if (field_type == FieldType.FLOAT && argument_type == QueryParser.DECIMAL_VALUE)
      {
	return new Double(argument);
      }
    else if (argument_type == QueryParser.BOOLEAN_VALUE)
      {
	if (argument.toLowerCase().equals("true"))
	  {
	    return Boolean.TRUE;
	  }
	else
	  {
	    return Boolean.FALSE;
	  }
      }
    else if (argument_type == QueryParser.STRING_VALUE)
      {
	switch (field_type)
	  {
	  case FieldType.STRING:
	  case FieldType.INVID:
	  case FieldType.IP:
	    return StringUtils.dequote(argument);
	  case FieldType.DATE:
	    DateFormat format = DateFormat.getInstance();
	    try
	      {
		return format.parse(StringUtils.dequote(argument));
	      }
	    catch (ParseException ex)
	      {
		// "An exception was encountered parsing your query string: {0}\nQuery: "{1}"\nExpanded Parse Tree: "{2}""
		// "I couldn''t make any sense of "{0}" as a date value."
		throw new GanyParseException(ts.l("global.parse_exception",
						  ts.l("parse_argument.bad_date", argument),
						  myQueryString,
						  myQueryTree.toStringTree()));
	      }
	  }
      }

    // "An exception was encountered parsing your query string: {0}\nQuery: "{1}"\nExpanded Parse Tree: "{2}""
    // "Error, field "{0}" requires a {1} argument type."
    throw new GanyParseException(ts.l("global.parse_exception",
				      ts.l("parse_argument.bad_argument_type", field.getName(), field.getTypeDesc()),
				      myQueryString,
				      myQueryTree.toStringTree()));
  }
}

/*

   Query.java

   The query class is used to do database lookups and listings.

   The Query class is serializable, in order to be passed over
   an RMI link.
   
   Created: 21 October 1996
   Version: $Revision: 1.6 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import arlut.csd.ganymede.*;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                           Query

------------------------------------------------------------------------------*/

/**
 * <p>The Query class is used to submit query requests to the GANYMEDE server,
 * both for complex database searches and for simple object listings.  A
 * Query class can fully specify a boolean search expression on any field
 * of any object type in the GANYMEDE database.  Each Query, however, can
 * only search one object type at a time.</p>
 *
 * <p>While providing support for arbitrarily complex queries, the Query
 * class also includes constructors for the simple "list object" case.</p>
 *
 */

public class Query implements java.io.Serializable {

  String objectName = null;
  String returnName = null;
  short objectType = -1;
  short returnType = -1;
  QueryNode root;
  boolean editableOnly;
  Hashtable permitList = null;

  /* -- */

  /**
   * Constructor for fully specified query (base by id)
   *
   * @param objectType numeric object type code to search over
   * @param root       root node of a boolean logic tree to be processed in an in-order traversal
   * @param editableOnly if true, the server will only return objects that the user's session 
   * currently has permission to edit
   *
   */

  public Query(short objectType, QueryNode root, boolean editableOnly)
  {
    this.returnType = this.objectType = objectType;
    this.root = root;
    this.editableOnly = editableOnly;

    objectName = null;
  }

  /**
   * Constructor for fully specified query (base by name)
   *
   * @param objectName name of object type to query
   * @param root       root node of a boolean logic tree to be processed in an in-order traversal
   * @param editableOnly if true, the server will only return objects that the user's session currently has permission to edit
   */

  public Query(String objectName, QueryNode root, boolean editableOnly)
  {  
    this.returnName = this.objectName = objectName;
    this.root = root;
    this.editableOnly = editableOnly;

    objectType = -1;
  }

  /**
   * Constructor to create a query returning only editable objects (base by id)
   *
   * @param objectType numeric object type code to search over
   * @param root       root node of a boolean logic tree to be processed in an in-order traversal
   */

  public Query(short objectType, QueryNode root)
  {
    this.returnType = this.objectType = objectType;
    this.root = root;

    editableOnly = true;
    objectName = null;
  }

  /**
   * Constructor to create a query returning only editable objects (base by name)
   *
   * @param objectName name of object type to query
   * @param root       root node of a boolean logic tree to be processed in an in-order traversal
   */

  public Query(String objectName, QueryNode root)
  {
    this.returnName = this.objectName = objectName;
    this.root = root;

    objectType = -1;
    editableOnly = true;
  }

  /**
   * Constructor for a query to list all editable objects of the specified type.
   *
   * @param objectType numeric object type code to search over
   */

  public Query(short objectType)
  {
    this.returnType = this.objectType = objectType;

    objectName = null;
    root = null;
    editableOnly = true;
  }

  /**
   * Constructor for a query to list all editable objects of the specified type.
   *
   * @param objectName name of object type to query
   */

  public Query(String objectName)
  {
    this.returnName = this.objectName = objectName;

    objectType = -1;
    root = null;
    editableOnly = true;
  }

  /**
   *
   * This method resets the permitList, allowing
   * all fields to be returned by default.
   * 
   */

  public void resetPermitList()
  {
    permitList = null;
  }

  /**
   *
   * This method sets the desired return type, for use
   * in performing queries on embedded types when a
   * parent type is what is desired.
   *
   */

  public void setReturnType(short returnType)
  {
    this.returnType = returnType;
  }

  /**
   *
   * This method adds a field identifier to the list of
   * fields that may be returned.  Once this method
   * is called with a field identifier, the query will
   * only return fields that have been explicitly added.
   *
   * resetPermitList() may be called to reset the
   * list to the initial allow-all state.
   */

  public void addField(short id)
  {
    if (permitList == null)
      {
	permitList = new Hashtable();
      }

    permitList.put(new Short(id), this);
  }

  
  public String dumpToString()
    {
      /** 
       *
       * The dumpToSting method for the query creates a string representation
       * of the query's actual values. It calls each node's dumpToString method 
       * until the number of nodes in the query has been exhausted
       *
       */



      String queryString = null;
 
      
      return queryString;
      
    }
}


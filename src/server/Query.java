/*

   Query.java

   The query class is used to do database lookups and listings.

   The Query class is serializable, in order to be passed over
   an RMI link.
   
   Created: 21 October 1996
   Version: $Revision: 1.3 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import arlut.csd.ganymede.*;

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

  String objectName;
  short objectType;
  QueryNode root;
  boolean editableOnly;
  String restrictionList = null;

  /* -- */

  /**
   * Constructor for fully specified query (base by id)
   *
   * @param objectType numeric object type code to search over
   * @param root       root node of a boolean logic tree to be processed in an in-order traversal
   * @param editableOnly if true, the server will only return objects that the user's session currently has permission to edit
   *
   */

  public Query(short objectType, QueryNode root, boolean editableOnly)
  {
    this.objectType = objectType;
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
    this.objectName = objectName;
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
    this.objectType = objectType;
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
    this.objectName = objectName;
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
    this.objectType = objectType;

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
    this.objectName = objectName;

    objectType = -1;
    root = null;
    editableOnly = true;
  }

  /**
   *
   * This method takes a comma separated list of numeric field id's
   * that should be returned by this query.  If restrictionList is
   * null, all possible fields will be returned.
   * 
   */

  public setRestrictionList(String restrictionList)
  {
    this.restrictionList = restrictionList;
  }

}


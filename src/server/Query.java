/*

   Query.java

   The query class is used to do database lookups and listings.

   The Query class is serializable, in order to be passed over
   an RMI link.
   
   Created: 21 October 1996
   Release: $Name:  $
   Version: $Revision: 1.12 $
   Last Mod Date: $Date: 1999/01/22 18:05:51 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

   Contact information

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

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

  static final long serialVersionUID = 2321965429672608993L;

  // ---

  /**
   *
   * The name of the object type that the queryNodes are looking
   * to match on.
   *
   */

  String objectName = null;

  /**
   *
   * The name of the object type that the query should return.. used
   * in the case where the search is performed on embedded objects.
   *
   */

  String returnName = null;

  /**
   *
   * We want to be able to save a query on the server and re-issue it
   * on behalf of the user.  If we are saved, the name to save under
   * will be here.  We may or may not want it here.
   *
   */

  String saveName = null;

  /**
   *
   * The id of the object type that the queryNodes are looking
   * to match on.
   *
   */

  short objectType = -1;

  /**
   *
   * The id of the object type that the query should return.. used
   * in the case where the search is performed on embedded objects.
   *
   */

  short returnType = -1;

  /**
   *
   * The root of a graph of QueryNodes that encodes the desired
   * search criteria.
   *
   */

  QueryNode root;

  /**
   *
   * If true, this query will only be matched against objects in the
   * database that the user has permission to edit.
   *
   */

  boolean editableOnly;

  /**
   *
   * If true, this query will only be matched against the subset of
   * objects in the database that the user has requested via
   * the Session filter mechanism.
   *
   */

  boolean filtered;

  /**
   *
   * A list of field id's in Short form that the server will take into
   * account when returning a data dump.  If null, the default fields
   * will be returned.
   * 
   */

  Hashtable permitList = null;

  /**
   *
   * A Vector of Query's that can be associated with this query.<br><br>
   * 
   * This vector is used to allow the inclusion of queries on embedded objects..
   * If linkedQueries != null, the server will issue a second (third, fourth)
   * query, returning the intersection of the results.<br><br>
   *
   * It does no good to have linkedQueries that do not map back to the same
   * result object type.
   * 
   */

  Vector linkedQueries = null;

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
   * @param editableOnly if true, the server will only return objects that
   * the user's session currently has permission to edit
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
   * This method determines whether the query engine
   * will filter the query according to the current
   * list of visible owner groups.  Queries by default
   * are filtered.
   *
   * @param filtered If true, the query will be masked by ownership
   *
   * @see arlut.csd.ganymede.Session#filterQueries()
   * 
   */

  public void setFiltered(boolean filtered)
  {
    this.filtered = filtered;
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
   * <br><br>
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

  /**
   *
   * This method allows the client to add a list of subordinate queries
   * to this query.  The queries attached to this query *will not*
   * have their subordinate queries processed.<br><br>
   * 
   * Queries added with this method are used to allow queries to
   * include checks on embedded objects.<br><br>
   *
   * It does no good to pass queries to addQuery that do not have
   * the same return type as this query.
   *   */

  public void addQuery(Query query)
  {
    if (query.returnType != this.returnType)
      {
	throw new IllegalArgumentException("Couldn't add an incompatible query");
      }

    if (linkedQueries == null)
      {
	linkedQueries = new Vector();
      }

    linkedQueries.addElement(query);
  }
}


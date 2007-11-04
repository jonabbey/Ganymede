/*

   Query.java

   The query class is used to do database lookups and listings.

   The Query class is serializable, in order to be passed over
   an RMI link.
   
   Created: 21 October 1996
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2007
   The University of Texas at Austin

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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.common;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                           class
                                                                           Query

------------------------------------------------------------------------------*/

/**
 * The Query class is used to submit query requests to the GANYMEDE server,
 * both for complex database searches and for simple object listings.  A
 * Query class can fully specify a boolean search expression on any field
 * of any object type in the GANYMEDE database.  Each Query, however, can
 * only search one object type at a time.
 *
 * While providing support for arbitrarily complex queries, the Query
 * class also includes constructors for the simple "list object" case.
 *
 */

public class Query implements java.io.Serializable {

  static final long serialVersionUID = 7921652227732781133L;

  // ---

  /**
   * The id of the object type that the queryNodes are looking
   * to match on.
   *
   * If this value is left at -1, objectName will be consulted
   * instead.
   */

  public short objectType = -1;

  /**
   * The name of the object type that the queryNodes are looking
   * to match on.
   *
   * This value is consulted only if objectType is left at -1.
   */

  public String objectName = null;

  /**
   * We want to be able to save a query on the server and re-issue it
   * on behalf of the user.  If we are saved, the name to save under
   * will be here.  We may or may not want it here.
   *
   * I don't believe anything in the server actually uses this yet.
   */

  public String saveName = null;

  /**
   * The root of a graph of QueryNodes that encodes the desired
   * search criteria.
   */

  public QueryNode root;

  /**
   * If true, this query will only be matched against objects in the
   * database that the user has permission to edit.
   */

  public boolean editableOnly;

  /**
   * If true, this query will only be matched against the subset of
   * objects in the database that the user has requested via
   * the Session filter mechanism.
   */

  public boolean filtered = false;

  /**
   * A list of field id's in Short form that the server will take into
   * account when returning a data dump.  If null, the default fields
   * will be returned.
   */

  public List permitList = null;

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
   * @param editableOnly if true, the server will only return objects that
   * the user's session currently has permission to edit
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
   * This method determines whether the query engine
   * will filter the query according to the current
   * list of visible owner groups.  Queries by default
   * are filtered.
   *
   * @param filtered If true, the query will be masked by ownership
   *
   * @see arlut.csd.ganymede.rmi.Session#filterQueries(java.util.Vector)
   */

  public void setFiltered(boolean filtered)
  {
    this.filtered = filtered;
  }

  /**
   * This method resets the permitList, allowing
   * all fields to be returned by default.
   */

  public void resetPermitList()
  {
    permitList = null;
  }

  /**
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
	permitList = new ArrayList();
      }

    permitList.add(new Short(id));
  }

  /**
   * This method returns true if the field with the given id number
   * should be returned.
   */

  public boolean returnField(short id)
  {
    if (permitList == null)
      {
	return true;
      }

    Short key = new Short(id);

    return permitList.contains(key);
  }

  public String toString()
  {
    StringBuffer result = new StringBuffer();

    if (objectType != -1)
      {
	result.append("objectType = ");
	result.append(objectType);
	result.append("\n");
      }

    if (objectName != null)
      {
	result.append("objectName = ");
	result.append(objectName);
	result.append("\n");
      }

    result.append("editableOnly = ");
    result.append(editableOnly ? "True\n" : "False\n");

    if (root != null)
      {
        result.append(root.toString());
      }

    return result.toString();
  }
}

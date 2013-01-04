/*

   Query.java

   The query class is used to do database lookups and listings.

   The Query class is serializable, in order to be passed over
   an RMI link.

   Created: 21 October 1996

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

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
 */

public class Query implements java.io.Serializable {

  static final long serialVersionUID = 7921652227732781133L;

  // ---

  /**
   * <p>The id of the object type that the queryNodes are looking
   * to match on.</p>
   *
   * <p>If this value is left at -1, objectName will be consulted
   * instead.</p>
   */

  public short objectType = -1;

  /**
   * <p>The name of the object type that the queryNodes are looking
   * to match on.</p>
   *
   * <p>This value is consulted only if objectType is left at -1.</p>
   */

  public String objectName = null;

  /**
   * <p>We want to be able to save a query on the server and re-issue it
   * on behalf of the user.  If we are saved, the name to save under
   * will be here.  We may or may not want it here.</p>
   *
   * <p>I don't believe anything in the server actually uses this yet.</p>
   */

  public String saveName = null;

  /**
   * <p>The root of a graph of QueryNodes that encodes the desired
   * search criteria.</p>
   */

  public QueryNode root;

  /**
   * <p>If true, this query will only be matched against objects in the
   * database that the user has permission to edit.</p>
   */

  public boolean editableOnly;

  /**
   * <p>If true, this query will only be matched against the subset of
   * objects in the database that the user has requested via
   * the Session filter mechanism.</p>
   */

  public boolean filtered = false;

  /**
   * <p>A Set of field id's in Short form that the server will take into
   * account when returning a data dump.  If null, all default fields
   * should be returned.</p>
   */

  private Set<Short> permitSet = null;

  /**
   * <p>If we are on the server, we'll have a generic reference to a
   * GanymedeSession so that we can look up the description of the
   * object type.</p>
   */

  transient QueryDescriber describer = null;

  /* -- */

  /**
   * <p>Constructor for fully specified query (base by id)</p>
   *
   * @param objectType numeric object type code to search over
   * @param root       root node of a boolean logic tree to be processed in an in-order traversal
   * @param editableOnly if true, the server will only return objects that the user's session
   * currently has permission to edit
   */

  public Query(short objectType, QueryNode root, boolean editableOnly)
  {
    this.objectType = objectType;
    this.root = root;
    this.editableOnly = editableOnly;

    objectName = null;
  }

  /**
   * <p>Constructor for fully specified query (base by name)</p>
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
   * <p>Constructor to create a query returning only editable objects (base by id)</p>
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
   * <p>Constructor to create a query returning only editable objects (base by name)</p>
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
   * <p>Constructor for a query to list all editable objects of the specified type.</p>
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
   * <p>Constructor for a query to list all editable objects of the specified type.</p>
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
   * <p>Sets a reference to a GanymedeSession on the server only, for
   * use in providing more descriptive toString() results.</p>
   */

  public void setDescriber(QueryDescriber describer)
  {
    this.describer = describer;
  }

  /**
   * <p>This method determines whether the query engine will filter
   * the query according to the current list of visible owner groups.
   * Queries by default are filtered.</p>
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
   * <p>This method resets the permitSet, allowing
   * all fields to be returned by default.</p>
   */

  public synchronized void resetPermitSet()
  {
    permitSet = null;
  }

  /**
   * <p>This method returns true if this Query is requesting a restricted
   * set of fields.</p>
   */

  public boolean hasPermitSet()
  {
    return this.permitSet != null;
  }

  /**
   * <p>This method adds a field identifier to the list of fields that
   * may be returned.  Once this method is called with a field
   * identifier, the query will only return fields that have been
   * explicitly added.</p>
   *
   * <p>resetPermitSet() may be called to reset the list to the
   * initial allow-all state.</p>
   */

  public synchronized void addField(short id)
  {
    if (permitSet == null)
      {
        permitSet = new HashSet<Short>();
      }

    permitSet.add(Short.valueOf(id));
  }

  /**
   * <p>This method adds a field identifier to the list of fields that
   * may be returned.  Once this method is called with a field
   * identifier, the query will only return fields that have been
   * explicitly added.</p>
   *
   * <p>resetPermitSet() may be called to reset the
   * list to the initial allow-all state.</p>
   */

  public synchronized void removeField(short id)
  {
    if (permitSet == null)
      {
        return;
      }

    permitSet.remove(Short.valueOf(id));
  }

  /**
   * <p>This method returns true if the field with the given id number
   * should be returned.</p>
   */

  public synchronized boolean returnField(short id)
  {
    if (permitSet == null)
      {
        return true;
      }

    return permitSet.contains(Short.valueOf(id));
  }

  /**
   * <p>This method returns true if the field with the given id number
   * should be returned.</p>
   */

  public synchronized boolean returnField(Short value)
  {
    if (permitSet == null)
      {
        return true;
      }

    return permitSet.contains(value);
  }

  /**
   * <p>Returns a copy of the restricted list of fields that this Query
   * wants to have returned.</p>
   */

  public synchronized Set<Short> getFieldSet()
  {
    if (permitSet == null)
      {
        return new HashSet<Short>();
      }
    else
      {
        return new HashSet<Short>(permitSet);
      }
  }

  public String toString()
  {
    StringBuilder result = new StringBuilder();

    if (objectType != -1)
      {
        result.append("objectType = ");

        if (describer != null)
          {
            result.append(describer.describeType(objectType));
          }
        else
          {
            result.append(String.valueOf(objectType));
          }

        result.append(",");
      }

    if (objectName != null)
      {
        result.append("objectName = ");
        result.append(objectName);
        result.append(",");
      }

    result.append("editableOnly = ");
    result.append(editableOnly ? "True" : "False");

    if (root != null)
      {
        result.append(",");
        result.append(root.toString());
      }

    return result.toString();
  }

  public String describeField(short fieldId)
  {
    if (describer == null)
      {
        return "<" + String.valueOf(fieldId) + ">";
      }

    if (objectType != -1)
      {
        return describer.describeField(objectType, fieldId);
      }
    else
      {
        return describer.describeField(objectName, fieldId);
      }
  }
}

/*

   QueryEngine.java

   Contains all the query execution logic for the GanymedeSession
   class.

   Created: 17 April 2012

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2012
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


import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;

import arlut.csd.ganymede.common.DumpResult;
import arlut.csd.ganymede.common.GanyParseException;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.ObjectHandle;
import arlut.csd.ganymede.common.ObjectStatus;
import arlut.csd.ganymede.common.PermEntry;
import arlut.csd.ganymede.common.PermMatrix;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.QueryResultContainer;
import arlut.csd.ganymede.common.Result;
import arlut.csd.ganymede.common.SchemaConstants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     QueryEngine

------------------------------------------------------------------------------*/

/**
 * <p>Helper class to handle all the query engine logic that used to
 * be embedded in the GanymedeSession class.</p>
 *
 * <p>Most methods in this class are synchronized to avoid race condition
 * security holes between the persona change logic and the actual operations.</p>
 *
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class QueryEngine {

  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.QueryEngine");

  // ---

  /**
   * Reference to our containing GanymedeSession which is consulted to
   * perform permission checks.
   */

  private GanymedeSession gSession = null;

  /**
   * Reference to the DBSession connected with gSession, used to access
   * transaction-consistent objects associated with gSession.
   */

  private DBSession session = null;

  /* -- */

  public QueryEngine(GanymedeSession gSession, DBSession session)
  {
    this.gSession = gSession;
    this.session = session;
  }

  /**
   * <p>This method returns a QueryResult of owner groups that the
   * current persona has access to.  This list is the transitive
   * closure of the list of owner groups in the current persona.  That
   * is, the list includes all the owner groups in the current persona
   * along with all of the owner groups those owner groups own, and so
   * on.</p>
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public QueryResult getOwnerGroups()
  {
    Query q;
    QueryResult result = new QueryResult();
    QueryResult fullOwnerList;
    Vector alreadySeen = new Vector();
    Invid inv;

    /* -- */

    if (gSession.getPersonaInvid() == null)
      {
	return result;		// End users don't have any owner group access
      }

    q = new Query(SchemaConstants.OwnerBase);
    q.setFiltered(false);

    fullOwnerList = query(q);

    // if we're in supergash mode, return a complete list of owner groups

    if (gSession.isSuperGash())
      {
	return fullOwnerList;
      }

    // otherwise, we've got to do a very little bit of legwork

    for (int i = 0; i < fullOwnerList.size(); i++)
      {
	alreadySeen.removeAllElements();

	inv = fullOwnerList.getInvid(i);

	if (recursePersonaMatch(inv, alreadySeen))
	  {
	    result.addRow(inv, session.viewDBObject(inv).getLabel(), false);
	  }
      }

    return result;
  }

  /**
   * <p>This method allows the client to get a status update on a
   * specific list of invids.</p>
   *
   * <p>If any of the invids are not currently defined in the server, or
   * if the client doesn't have permission to view any of the invids,
   * those invids' status will not be included in the returned
   * QueryResult.</p>
   *
   * @param invidVector Vector of Invid's to get the status for.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public QueryResult queryInvids(Vector invidVector)
  {
    QueryResult result = new QueryResult(true);	// for transport
    DBObject obj;
    Invid invid;
    PermEntry perm;

    /* -- */

    for (int i = 0; i < invidVector.size(); i++)
      {
	invid = (Invid) invidVector.elementAt(i);

	// the DBSession.viewDBObject() will look in the
	// current DBEditSet, if any, to find the version of
	// the object as it exists in the current transaction.

	obj = session.viewDBObject(invid);

	if (obj == null)
	  {
	    continue;
	  }

	perm = gSession.getPerm(obj);

	if (!perm.isVisible())
	  {
	    continue;
	  }

	result.addRow(obj.getInvid(), obj.getLabel(), obj.isInactivated(),
		      obj.willExpire(), obj.willBeRemoved(), perm.isEditable());
      }

    return result;
  }

  /**
   * <p>Returns an Invid for an object of a specified type and name, or
   * null if no such object could be found.</p>
   *
   * <p>If the user does not have permission to view the object, null will
   * be returned even if an object by that name does exist.</p>
   *
   * <p>This method uses the GanymedeSession query() apparatus, and
   * may not be called from a DBEditObject's commitPhase1/2() methods
   * without risking deadlock.</p>
   *
   * @param objectName Label for the object to lookup
   * @param objectType Name of the object type
   */

  public Invid findLabeledObject(String objectName, String objectType)
  {
    return this.findLabeledObject(objectName, objectType, false);
  }

  /**
   * <p>Returns an Invid for an object of a specified type and name, or
   * null if no such object could be found.</p>
   *
   * <p>If the user does not have permission to view the object, null will
   * be returned even if an object by that name does exist.</p>
   *
   * <p>This method uses the GanymedeSession query() apparatus, and
   * may not be called from a DBEditObject's commitPhase1/2() methods
   * without risking deadlock.</p>
   *
   * @param objectName Label for an object
   * @param type Object type id number
   */

  public Invid findLabeledObject(String objectName, short type)
  {
    return this.findLabeledObject(objectName, type, false);
  }

  /**
   * <p>Returns an Invid for an object of a specified type and name, or
   * null if no such object could be found.</p>
   *
   * <p>If the user does not have permission to view the object, null will
   * be returned even if an object by that name does exist.</p>
   *
   * <p>This method uses the GanymedeSession query() apparatus, and
   * may not be called from a DBEditObject's commitPhase1/2() methods
   * without risking deadlock.</p>
   *
   * @param objectName Label for the object to lookup
   * @param objectType Name of the object type
   * @param allowAliases If true, findLabeledObject will return an
   * Invid that has name attached to the same namespace as the label
   * field for the object type sought.
   */

  public Invid findLabeledObject(String objectName, String objectType, boolean allowAliases)
  {
    DBObjectBase base = Ganymede.db.getObjectBase(objectType);

    if (base == null)
      {
	// "Error, "{0}" is not a valid object type in this Ganymede server."
	throw new RuntimeException(ts.l("global.no_such_object_type", objectType));
      }

    return this.findLabeledObject(objectName, base.getTypeID(), allowAliases);
  }

  /**
   * <p>Returns an Invid for an object of a specified type and name, or
   * null if no such object could be found.</p>
   *
   * <p>If the user does not have permission to view the object, null will
   * be returned even if an object by that name does exist.</p>
   *
   * <p>This method uses the GanymedeSession query() apparatus, and
   * may not be called from a DBEditObject's commitPhase1/2() methods
   * without risking deadlock.</p>
   *
   * @param objectName Label for an object
   * @param type Object type id number.
   * @param allowAliases If true, findLabeledObject will return an
   * Invid that has objectName attached to the same namespace as the
   * label field for the object type sought, even if the Invid is of a
   * different object type.
   */

  public Invid findLabeledObject(String objectName, short type, boolean allowAliases)
  {
    Invid value;

    /* -- */

    Query localquery = new Query(type,
				 new QueryDataNode(QueryDataNode.EQUALS, objectName),
				 false);

    List<Result> results = internalQuery(localquery);

    if (debug)
      {
	Ganymede.debug("findLabeledObject() found results, size = " + results.size());
      }

    if (results != null && results.size() == 1)
      {
	Result tmp = results.get(0);

	value = tmp.getInvid();

	// make sure we've got the right kind of object back.. this is a
	// debugging assertion to make sure that we're always handling
	// embedded objects properly.

	if (value.getType() != type)
	  {
	    throw new RuntimeException("findLabeledObject() ASSERTFAIL: Error in query processing," +
				       " didn't get back right kind of object");
	  }

	if (debug)
	  {
	    Ganymede.debug("findLabeledObject() found results, returning = " + value);
	  }

	return value;
      }

    if (!allowAliases)
      {
	return null;
      }

    // we can allow aliases.  let's see if the objectName maps to
    // an object of the desired type through the namespace.

    try
      {
	DBObjectBase base = DBStore.db.getObjectBase(type);
	DBObjectBaseField labelField = base.getLabelFieldDef();
	DBNameSpace namespace = labelField.getNameSpace();
	DBField targetField = namespace.lookupPersistent(objectName);
	DBObject targetObject = targetField.getOwner();

	while (targetObject.isEmbedded())
	  {
	    targetObject = targetObject.getParentObj();
	  }

	return targetObject.getInvid();
      }
    catch (NullPointerException ex)
      {
	return null;
      }
  }

  /**
   * <p>This method provides the hook for doing a
   * fast database dump to a string form.  The
   * {@link arlut.csd.ganymede.common.DumpResult DumpResult}
   * returned comprises a formatted dump of all visible
   * fields and objects that match the given query.</p>
   *
   * <p>This version of dump() takes a query in string
   * form, based on Deepak's ANTLR-specified Ganymede
   * query grammar.</p>
   *
   * <p>This method uses the GanymedeSession query() apparatus, and
   * may not be called from a DBEditObject's commitPhase1/2() methods
   * without risking deadlock.</p>
   *
   * @see arlut.csd.ganymede.common.Query
   * @see arlut.csd.ganymede.rmi.Session
   */

  public DumpResult dump(String queryString) throws GanyParseException
  {
    GanyQueryTransmuter transmuter = new GanyQueryTransmuter();
    Query query = transmuter.transmuteQueryString(queryString);

    return dump(query);
  }

  /**
   * <p>This method provides the hook for doing a
   * fast database dump to a string form.  The
   * {@link arlut.csd.ganymede.common.DumpResult DumpResult}
   * returned comprises a formatted dump of all visible
   * fields and objects that match the given query.</p>
   *
   * <p>This method uses the GanymedeSession query() apparatus, and
   * may not be called from a DBEditObject's commitPhase1/2() methods
   * without risking deadlock.</p>
   *
   * @see arlut.csd.ganymede.common.Query
   * @see arlut.csd.ganymede.rmi.Session
   */

  public DumpResult dump(Query query)
  {
    DumpResultBuilder resultBuilder;

    /**
     *
     * What base is the query being done on?
     *
     */

    DBObjectBase base = null;

    boolean embedded;

    /* -- */

    if (query == null)
      {
	gSession.setLastError("null query");
	return null;
      }

    // objectType will be -1 if the query is specifying the
    // base with the base's name

    if (query.objectType != -1)
      {
	base = Ganymede.db.getObjectBase(query.objectType);
      }
    else if (query.objectName != null)
      {
	base = Ganymede.db.getObjectBase(query.objectName);
      }

    if (base == null)
      {
	gSession.setLastError("No such base");
	return null;
      }

    if (debug)
      {
	Ganymede.debug("Processing dump query\nSearching for matching objects of type " + base.getName());
      }

    if (debug)
      {
	if (!query.hasPermitSet())
	  {
	    Ganymede.debug("Returning default fields");
	  }
	else
	  {
	    Ganymede.debug("Returning custom fields");
	  }
      }

    if (debug)
      {
	Ganymede.debug("dump(): " + gSession.getPersonaLabel() + " : got read lock");
      }

    // search for the invid's matching the given query

    QueryResult temp_result = queryDispatch(query, false, false, null, null);

    if (debug)
      {
	System.err.println("dump(): processed queryDispatch, building dumpResult buffer");
      }

    // Figure out which fields we want to include in our result buffer

    Vector<DBObjectBaseField> fieldDefs = new Vector<DBObjectBaseField>();

    for (DBObjectBaseField field: base.getFields())
      {
	if (query.returnField(field.getKey()) &&
	    (gSession.isSuperGash() ||
	     gSession.getPerm(base.getTypeID(), field.getID(), true).isVisible()))
	  {
	    fieldDefs.addElement(field);
	  }
      }

    // prepare the result buffer, given the requested fields

    resultBuilder = new DumpResultBuilder(fieldDefs);

    // and encode the desired fields into the result

    if (temp_result != null)
      {
	Invid invid;

	Vector invids = temp_result.getInvids();

	for (int i = 0; i < invids.size(); i++)
	  {
	    invid = (Invid) invids.elementAt(i);

	    if (debug)
	      {
		System.err.print(".");
	      }

	    // it's okay to use session.viewDBObject() because
	    // DumpResult.addRow() uses the GanymedeSession reference
	    // we pass in to handle per-field permissions

	    // using view_db_object() here would be disastrous,
	    // because it would entail making exported duplicates of
	    // all objects matching our query

	    resultBuilder.addRow(session.viewDBObject(invid), gSession);
	  }
      }

    if (debug)
      {
	Ganymede.debug("dump(): completed processing, returning buffer");
      }

    return resultBuilder.getDumpResult();
  }

  /**
   * <p>This method applies this GanymedeSession's current owner filter
   * to the given QueryResult &lt;qr&gt; and returns a QueryResult
   * with any object handles that are not matched by the filter
   * stripped.</p>
   *
   * <p>If the submitted QueryResult &lt;qr&gt; is null, filterQueryResult()
   * will itself return null.</p>
   */

  public QueryResult filterQueryResult(QueryResult qr)
  {
    if (qr == null)
      {
	return null;
      }

    QueryResult result = new QueryResult(qr.isForTransport());
    ObjectHandle handle;

    /* -- */

    Vector handles = qr.getHandles();

    for (int i = 0; i < handles.size(); i++)
      {
	handle = (ObjectHandle) handles.elementAt(i);

	Invid invid = handle.getInvid();

	if (invid != null)
	  {
	    DBObject obj = session.viewDBObject(invid);

	    if (filterMatch(obj))
	      {
		result.addRow(handle);
	      }
	  }
      }

    return result;
  }

  /**
   * <p>This method provides the hook for doing all manner of simple
   * object listing for the Ganymede database.</p>
   *
   * <p>This version of query() takes a query in string form, based on
   * Deepak's ANTLR-specified Ganymede query grammar.</p>
   *
   * <p>This method may not be called from a DBEditObject's
   * commitPhase1/2() methods without risking deadlock.</p>
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public QueryResult query(String queryString) throws GanyParseException
  {
    GanyQueryTransmuter transmuter = new GanyQueryTransmuter();
    Query query = transmuter.transmuteQueryString(queryString);

    return queryDispatch(query, false, true, null, null);
  }

  /**
   * <p>This method provides the hook for doing all
   * manner of simple object listing for the Ganymede
   * database.</p>
   *
   * <p>This method may not be called from a DBEditObject's
   * commitPhase1/2() methods without risking deadlock.</p>
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public QueryResult query(Query query)
  {
    return queryDispatch(query, false, true, null, null);
  }

  /**
   * Server-side method for doing object listing with support for DBObject's
   * {@link arlut.csd.ganymede.server.DBObject#lookupLabel(arlut.csd.ganymede.server.DBObject) lookupLabel}
   * method.
   *
   * @param query The query to be performed
   * @param perspectiveObject There are occasions when the server will want to do internal
   * querying in which the label of an object matching the query criteria is synthesized
   * for use in a particular context.  If non-null, perspectiveObject's
   * {@link arlut.csd.ganymede.server.DBObject#lookupLabel(arlut.csd.ganymede.server.DBObject) lookupLabel}
   * method will be used to generate the label for a result entry.
   */

  public QueryResult query(Query query, DBEditObject perspectiveObject)
  {
    return queryDispatch(query, false, true, null, perspectiveObject);
  }

  public QueryResultContainer testQuery(String queryString) throws GanyParseException
  {
    return testQuery(queryString, QueryResultContainer.ARRAYROWS);
  }

  public QueryResultContainer testQuery(String queryString, int rowType) throws GanyParseException
  {
    GanyQueryTransmuter transmuter = new GanyQueryTransmuter();
    Query q = transmuter.transmuteQueryString(queryString);
    QueryResult qr = queryDispatch(q, false, true, null, null);
    QueryResultContainer qrc = new QueryResultContainer(rowType);

    /* Get the list of fields the user wants returned */
    Set<Short> fieldIDs = q.getFieldSet();

    /* Now add them to the result container */

    for (Short fieldID: fieldIDs)
      {
      	DBObjectBaseField field =
	  (DBObjectBaseField) Ganymede.db.getObjectBase(q.objectName).getField(fieldID.shortValue());

      	qrc.addField(field.getName(), fieldID);
      }

    /* Now we'll add a row for each matching object */
    List<Invid> invids = qr.getInvids();

    PermEntry perm;
    boolean editable;

    for (Invid invid: invids)
      {
        DBObject object = Ganymede.db.getObject(invid);

        if (gSession.isSuperGash())
          {
            perm = PermEntry.fullPerms;
          }
        else
          {
            perm = gSession.getPerm(object);
          }

        if ((perm == null) ||
	    (q.editableOnly && !perm.isEditable()) || (!perm.isVisible()))
          {
            editable = false;
          }
        else
          {
            editable = perm.isEditable();
          }

        Object value;
	Object[] row = new Object[fieldIDs.size()];
        int i = 0;

        for (Short key: fieldIDs)
          {
            value = object.getFieldValueLocal(key.shortValue());
            row[i++] = value;
          }

        qrc.addRow(invid,
		   object.getLabel(),
		   row,
		   object.isInactivated(),
		   object.willExpire(),
		   object.willBeRemoved(),
		   editable);
      }

    return qrc;
  }

  /**
   * This method provides the hook for doing all manner of internal
   * object listing for the Ganymede database.  This method will not
   * take into account any optional owner filtering, but it will honor
   * the editableOnly flag in the Query.
   *
   * @return A Vector of {@link arlut.csd.ganymede.common.Result Result} objects
   */

  public Vector<Result> internalQuery(Query query)
  {
    Vector<Result> result = new Vector<Result>();
    QueryResult internalResult = queryDispatch(query, true, false, null, null);
    Invid key;
    String val;

    /* -- */

    if (internalResult != null)
      {
	for (int i = 0; i < internalResult.size(); i++)
	  {
	    key = (Invid) internalResult.getInvid(i);
	    val = (String) internalResult.getLabel(i);

	    result.add(new Result(key, val));
	  }
      }

    return result;
  }

  /**
   * This method is the primary Query engine for the Ganymede
   * databases.  It is used by dump(), query(), and internalQuery().
   *
   * @param query The query to be handled
   * @param internal If true, the query filter setting will not be honored
   * @param forTransport If true, the QueryResult will build a buffer for serialization
   * @param extantLock If non-null, queryDispatch will not attempt to establish its
   * own lock on the relevant base(s) for the duration of the query.  The extantLock must
   * have any bases that the queryDispatch method determines it needs access to locked, or
   * an IllegalArgumentException will be thrown.
   * @param perspectiveObject There are occasions when the server will want to do internal
   * querying in which the label of an object matching the query criteria is synthesized
   * for use in a particular context.  If non-null, perspectiveObject's
   * {@link arlut.csd.ganymede.server.DBObject#lookupLabel(arlut.csd.ganymede.server.DBObject) lookupLabel}
   * method will be used to generate the label for a result entry.
   */

  public synchronized QueryResult queryDispatch(Query query, boolean internal,
						boolean forTransport, DBLock extantLock,
						DBEditObject perspectiveObject)
  {
    QueryResult result = new QueryResult(forTransport);
    DBObjectBase base = null;
    Iterator<DBObject> it;
    DBObject obj;
    DBLock rLock = null;

    /* -- */

    if (query == null)
      {
	throw new IllegalArgumentException(ts.l("queryDispatch.null_query"));
      }

    // objectType will be -1 if the query is specifying the
    // base with the base's name

    if (query.objectType != -1)
      {
	base = Ganymede.db.getObjectBase(query.objectType);
      }
    else if (query.objectName != null)
      {
	base = Ganymede.db.getObjectBase(query.objectName); // *sync* DBStore
      }

    if (base == null)
      {
	gSession.setLastError("No such base");
	return null;
      }

    // are we able to optimize the query into a direct lookup?  If so,
    // we won't need to get a lock on the database, since viewDBObject()
    // will be nice and atomic for our needs

    if ((query.root instanceof QueryDataNode) &&
	((QueryDataNode) query.root).comparator == QueryDataNode.EQUALS)
      {
	QueryDataNode node = (QueryDataNode) query.root;
	DBObjectBaseField fieldDef = null;

	/* -- */

	// we're looking for a specific invid.. go ahead and do it

	if (node.fieldId == -2)
	  {
	    DBObject resultobject = session.viewDBObject((Invid) node.value);

	    addResultRow(resultobject, query, result, internal, perspectiveObject);

	    return result;
	  }

	// we're looking at a data field.. determine which field we're
	// looking at, find the dictionary definition for that field,
	// see if it is in a namespace so we can do a direct lookup
	// via a namespace hash.

	if (node.fieldId >= 0)
	  {
	    fieldDef = (DBObjectBaseField) base.getField(node.fieldId);
	  }
	else if (node.fieldname != null)
	  {
	    fieldDef = (DBObjectBaseField) base.getField(node.fieldname); // *sync* DBObjectBase
	  }
	else if (node.fieldId == -1)
	  {
	    fieldDef = (DBObjectBaseField) base.getField(base.getLabelField()); // *sync* DBObjectBase
	  }

        if (fieldDef == null)
          {
            // "Invalid field identifier"
            throw new IllegalArgumentException(ts.l("queryDispatch.bad_field"));
          }

	// now we've got a field definition that we can try to do a
	// direct look up on.  check to see if it has a namespace
	// index we can use

	if (fieldDef.getNameSpace() != null)
	  {
	    // aha!  We've got an optimized case!

	    if (debug)
	      {
		System.err.println("Eureka!  Optimized query!\n" + query.toString());
	      }

	    DBObject resultobject;
	    DBNameSpace ns = fieldDef.getNameSpace();

	    synchronized (ns)
	      {
		DBField resultfield = null;

		// if we are looking to match against an IP address
		// field and we were given a String, we need to
		// convert that String to an array of Bytes before
		// looking it up in the namespace

		if (fieldDef.isIP() && node.value instanceof String)
		  {
		    Byte[] ipBytes = null;

		    try
		      {
			ipBytes = IPDBField.genIPV4bytes((String) node.value);
		      }
		    catch (IllegalArgumentException ex)
		      {
		      }

		    if (ipBytes != null)
		      {
			resultfield = ns.lookupMyValue(gSession, ipBytes);
		      }

		    // it's hard to tell here whether any fields of
		    // this type will accept IPv6 bytes, so if we
		    // don't find it as an IPv4 address, look for it
		    // as an IPv6 address

		    if (resultfield == null)
		      {
			try
			  {
			    ipBytes = IPDBField.genIPV6bytes((String) node.value);
			  }
			catch (IllegalArgumentException ex)
			  {
			  }

			if (ipBytes != null)
			  {
			    resultfield = ns.lookupMyValue(gSession, ipBytes);
			  }
		      }
		  }
		else
		  {
		    // we don't allow associating Invid fields
		    // with a namespace, so we don't need to try
		    // to convert strings to invids here for a
		    // namespace-optimized lookup

		    if (node.value != null)
		      {
			resultfield = ns.lookupMyValue(gSession, node.value); // *sync* DBNameSpace

			if (debug)
			  {
			    System.err.println("Did a namespace lookup in " + ns.getName() +
					       " for value " + node.value);
			    System.err.println("Found " + resultfield);
			  }
		      }
		  }

		if (resultfield == null)
		  {
		    return result;
		  }
		else
		  {
		    // a namespace can map across different field and
		    // object types.. make sure we've got an instance
		    // of the right kind of field

		    if (resultfield.getFieldDef() != fieldDef)
		      {
			if (debug)
			  {
			    System.err.println("Error, didn't find the right kind of field");
			    System.err.println("Found: " + resultfield.getFieldDef());
			    System.err.println("Wanted: " + fieldDef);
			  }

			return result;
		      }

		    // since we used this GanymedeSession to do
		    // the namespace lookup, we know that the
		    // owner object will be in the version we are
		    // editing, if any

		    resultobject = resultfield.owner;

		    if (debug)
		      {
			System.err.println("Found object: " + resultobject);
		      }

		    // addResultRow() will do our permissions checking for us

		    addResultRow(resultobject, query, result, internal, perspectiveObject);

		    if (debug)
		      {
			System.err.println("Returning result from optimized query");
		      }

		    return result;
		  }
	      }
	  }
      }

    // okay, so we weren't able to do a namespace index lookup

    // now we need to generate a vector listing the object bases that
    // need to be locked to perform this query.  Note that we need to
    // get each of these bases locked at the same time to avoid potential
    // deadlock situations.  DBSession.openReadLock() will take care of
    // that for us by taking a vector to lock.

    // XXX need to revise this bit to try and create a list of bases
    // to lock by traversing over any QueryDeRefNodes in the QueryNode
    // tree.

    Vector baseLock = new Vector();

    baseLock.addElement(base);

    // lock the containing base as well, if it differs.. this will
    // keep things consistent

    if (debug)
      {
	System.err.println("Query: " + gSession.getPersonaLabel() + " : opening read lock on " + VectorUtils.vectorString(baseLock));
      }

    // okay.. now we want to lock the database, handle the search, and
    // return results.  We'll depend on the try..catch to handle
    // releasing the read lock if it is one we open.

    try
      {
	// with the new DBObjectBase.iterationSet support, we no
	// longer need to use a DBReadLock to lock the database unless
	// we are needing to do a query involving invid field
	// dereferencing as when doing a query that includes embedded
	// types.  If our baseLock vector only has one base, we can
	// just do a direct iteration over the base's iterationSet
	// snapshot, and avoid doing locking entirely.

	if (extantLock != null)
	  {
	    // check to make sure that the lock we were passed in has everything
	    // locked that we'll need to examine.

	    if (!extantLock.isLocked(baseLock))	// *sync* DBStore
	      {
		throw new IllegalArgumentException(ts.l("queryDispatch.lock_exception"));
	      }

	    rLock = extantLock;
	  }
	else if (baseLock.size() > 1)
	  {
	    try
	      {
		rLock = session.openReadLock(baseLock);	// *sync* DBSession DBStore
	      }
	    catch (InterruptedException ex)
	      {
		gSession.setLastError("lock interrupted");
		return null;		// we're probably being booted off
	      }
	  }

	if (rLock != null)
	  {
	    if (debug)
	      {
		System.err.println("Query: " + gSession.getPersonaLabel() + " : got read lock");
	      }

	    it = base.getObjects().iterator();
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("Query: " + gSession.getPersonaLabel() +
				   " : skipping read lock, iterating over iterationSet snapshot");
	      }

	    it = base.getIterationSet().iterator();
	  }

	// iterate over the objects in the base we're searching on,
	// looking for matching objects.  Note that we need to check
	// in here to see if we've had our DBSession's logout() method
	// called.. this shouldn't really ever happen here due to
	// synchronization on GanymedeSession, but if somehow it does
	// happen, we want to go ahead and break out of our query.  We
	// could well have our lock revoked during execution
	// of a query, so we'll check that as well.

	while (gSession.isLoggedIn() &&
	       (rLock == null || session.isLocked(rLock)) && it.hasNext())
	  {
	    obj = it.next();

	    // if we're editing it, let's look at our version of it

	    if (obj.shadowObject != null && obj.shadowObject.getSession() == session)
	      {
		obj = obj.shadowObject;
	      }

	    if (DBQueryHandler.matches(gSession, query, obj))
	      {
		addResultRow(obj, query, result, internal, perspectiveObject);
	      }
	  }

	if (!gSession.isLoggedIn())
	  {
	    throw new RuntimeException(ts.l("queryDispatch.logged_out_exception"));
	  }

	if (rLock != null && !session.isLocked(rLock))
	  {
	    throw new RuntimeException(ts.l("queryDispatch.read_lock_exception"));
	  }

	if (debug)
	  {
	    System.err.println("Query: " + gSession.getPersonaLabel() + " : completed query over primary hash.");
	  }

	// find any objects created or being edited in the current
	// transaction that match our criteria that we didn't see before

	// note that we have to do this even though
	// DBSession.viewDBObject() will look in our transaction's
	// working set for us, as there may be newly created objects
	// that are not yet held in the database, so our loop over
	// iterationSet might have missed something.

	// that is, viewDBObject() above will provide the version of
	// the object in our transaction if it has been changed in the
	// transaction, but that doesn't mean that we will have seen
	// objects that haven't yet been integrated into the object
	// tables, so we check our transaction's working set here.

	if (session.isTransactionOpen()) // should be safe since we are sync'ed on GanymedeSession
	  {
	    if (debug)
	      {
		System.err.println("Query: " + gSession.getPersonaLabel() +
				   " : scanning intratransaction objects");
	      }

	    DBEditObject transactionObjects[] = session.editSet.getObjectList();

	    for (int i = 0; i < transactionObjects.length; i++)
	      {
		DBEditObject transaction_object = transactionObjects[i];

		// don't consider objects of the wrong type here.

		if (transaction_object.getTypeID() != base.getTypeID())
		  {
		    continue;
		  }

		// don't consider objects we already have stored in the result

		if (result.containsInvid(transaction_object.getInvid()))
		  {
		    continue;
		  }

		// don't show objects in our transaction that are
		// being deleted or dropped

		if (transaction_object.getStatus() == ObjectStatus.DELETING ||
		    transaction_object.getStatus() == ObjectStatus.DROPPING)
		  {
		    continue;
		  }

		if (DBQueryHandler.matches(gSession, query, transaction_object))
		  {
		    addResultRow(transaction_object, query, result, internal, perspectiveObject);
		  }
	      }

	    if (debug)
	      {
		System.err.println("Query: " + gSession.getPersonaLabel() +
				   " : completed scanning intratransaction objects");
	      }
	  }

	if (debug)
	  {
	    Ganymede.debug("Query: " + gSession.getPersonaLabel() + ", object type " +
			   base.getName() + " completed");
	  }

	return result;
      }
    finally
      {
	// no matter where we depart, make sure to release our locks if
	// we created them here.

	if (extantLock == null && rLock != null && rLock.isLocked())
	  {
	    session.releaseLock(rLock);	// *sync* DBSession DBStore
	  }
      }
  }

  /**
   * <p>This private method takes care of adding an object to a query
   * result, checking permissions and what-not as needed.</p>
   *
   * <p>This method is not synchronized for performance reasons, but
   * is only to be called from methods synchronized on this
   * GanymedeSession.</p>
   *
   * @param obj The object to add to the query results
   * @param query The query that we are processing, used to get
   * the list of fields we're wanting to return
   * @param result The QueryResult we're building up
   * @param internal If true, we won't check permissions
   * @param perspectiveObject This is an object that can be consulted
   * to see what its
   * {@link arlut.csd.ganymede.server.DBObject#lookupLabel(arlut.csd.ganymede.server.DBObject) lookupLabel()}
   * method will return.  This can be null without harmful effect, but if
   * is it not null, a custom DBEditObject subclass can choose to present
   * the label of obj from its perspective.  This is used to simulate
   * a sort of relational effect for objects linked from the object
   * being added, by letting different fields in the object take on the
   * role of the label when seen from different objects.  Yes, it is a big ugly mess.
   */

  private void addResultRow(DBObject obj, Query query,
			    QueryResult result, boolean internal,
			    DBEditObject perspectiveObject)
  {
    PermEntry perm;

    /* -- */

    // if the object we're looking at is being deleted or dropped,
    // we'll consider it an ex-object, and not include it in the query
    // results.

    if (obj instanceof DBEditObject)
      {
	DBEditObject eObj = (DBEditObject) obj;

	if (eObj.getStatus() == ObjectStatus.DELETING ||
	    eObj.getStatus() == ObjectStatus.DROPPING)
	  {
	    return;
	  }
      }

    if (gSession.isSuperGash())
      {
	// we'll report it as editable

	perm = PermEntry.fullPerms;
      }
    else
      {
	perm = gSession.getPerm(obj);

	if (perm == null)
	  {
	    return;		// permissions prohibit us from adding this result
	  }

	if (query.editableOnly && !perm.isEditable())
	  {
	    return;		// permissions prohibit us from adding this result
	  }

	if (!perm.isVisible())
	  {
	    return;		// permissions prohibit us from adding this result
	  }
      }

    if (debug)
      {
	if (perspectiveObject == null)
	  {
	    Ganymede.debug("Query: " + gSession.getPersonaLabel() + " : adding element " +
			   obj.getLabel() + ", invid: " + obj.getInvid());
	  }
	else
	  {
	    Ganymede.debug("Query: " + gSession.getPersonaLabel() + " : adding element " +
			   perspectiveObject.lookupLabel(obj) + ", invid: " + obj.getInvid());
	  }
      }

    if (internal || !query.filtered || filterMatch(obj))
      {
	if (debug)
	  {
	    Ganymede.debug("not discounting out of hand");
	  }

	if (perspectiveObject == null)
	  {
	    if (debug)
	      {
                Ganymede.debug("not using perspective object");
	      }

	    if (obj.isEmbedded())
	      {
		result.addRow(obj.getInvid(), obj.getEmbeddedObjectDisplayLabel(),
			      obj.isInactivated(), obj.willExpire(), obj.willBeRemoved(),
			      perm.isEditable());
	      }
	    else
	      {
		result.addRow(obj.getInvid(), obj.getLabel(),
			      obj.isInactivated(), obj.willExpire(), obj.willBeRemoved(),
			      perm.isEditable());
	      }
	  }
	else
	  {
	    if (debug)
	      {
		Ganymede.debug("using perspective object");
	      }

	    result.addRow(obj.getInvid(), perspectiveObject.lookupLabel(obj),
			  obj.isInactivated(), obj.willExpire(), obj.willBeRemoved(),
			  perm.isEditable());
	  }
      }
  }

  /**
   * <p>Recursive helper method for personaMatch.. this method does a
   * depth first search up the owner tree for each Invid contained in
   * the invids Vector to see if the gSession's personaInvid is a
   * member of any of the containing owner groups.</p>
   *
   * @param owners A vector of invids pointing to OwnerBase objects
   * @param alreadySeen A vector of owner group Invid's that have
   * already been checked.  (For infinite loop avoidance).
   *
   * @return true if a match is found
   */

  public boolean recursePersonasMatch(Vector owners, Vector alreadySeen)
  {
    // *** It is critical that this method not modify the owners parameter passed
    // *** in, as it is 'live' in a DBField.

    if (owners == null)
      {
	return false;
      }

    for (int i = 0; i < owners.size(); i++)
      {
	if (recursePersonaMatch((Invid) owners.elementAt(i), alreadySeen))
	  {
	    return true;
	  }
      }
    
    return false;
  }

  /**
   * <p>Recursive helper method for personaMatch.. this method does a
   * depth first search up the owner tree for the owner Invid to see
   * if the gSession's personaInvid is a member of any of the
   * containing owner groups.</p>
   *
   * @param owner An Invid pointing to an OwnerBase object
   * @param alreadySeen A vector of owner group Invid's that have
   * already been checked.  (For infinite loop avoidance).
   *
   * @return true if a match is found
   */

  public boolean recursePersonaMatch(Invid owner, Vector alreadySeen)
  {
    DBObject ownerObj;
    InvidDBField inf;

    /* -- */

    if (owner == null)
      {
	throw new IllegalArgumentException("Null owner passed to recursePersonaMatch");
      }

    if (alreadySeen.contains(owner))
      {
	return false;
      }
    else
      {
	alreadySeen.addElement(owner);
      }
    
    ownerObj = session.viewDBObject(owner);
    
    inf = (InvidDBField) ownerObj.getField(SchemaConstants.OwnerMembersField);
    
    if (inf != null)
      {
	if (inf.getValuesLocal().contains(gSession.getPersonaInvid()))
	  {
	    return true;
	  }
      }
    
    // didn't find, recurse up
    
    inf = (InvidDBField) ownerObj.getField(SchemaConstants.OwnerListField);
    
    if (inf != null)
      {
	if (recursePersonasMatch(inf.getValuesLocal(), alreadySeen))
	  {
	    return true;
	  }
      }

    return false;
  }

  /**
   * This method returns true if the visibility filter vector allows
   * visibility of the object in question.  The visibility vector
   * works by direct ownership identity (i.e., no recursing up), so
   * it's a simple loop-di-loop.
   */

  private boolean filterMatch(DBObject obj)
  {
    Vector owners;
    InvidDBField inf;
    Invid tmpInvid;

    /* -- */

    if (obj == null)
      {
	return false;
      }

    if (gSession.visibilityFilterInvids == null || gSession.visibilityFilterInvids.size() == 0)
      {
	return true;		// no visibility restriction, go for it
      }

    inf = (InvidDBField) obj.getField(SchemaConstants.OwnerListField);

    if (inf == null)
      {
	return false;   // we have a restriction, but the object is only owned by supergash.. nope.
      }
    
    owners = inf.getValuesLocal();

    // *** Caution!  getValuesLocal() does not clone the field's contents..
    // 
    // DO NOT modify owners here!

    if (owners == null)
      {
	return false;   // we shouldn't get here, but we don't really care either
      }
    
    // we've got the owners for this object.. now, is there any match between our
    // visibilityFilterInvids and the owners of this object?

    for (int i = 0; i < gSession.visibilityFilterInvids.size(); i++)
      {
	tmpInvid = (Invid) gSession.visibilityFilterInvids.elementAt(i);

	for (int j = 0; j < owners.size(); j++)
	  {
	    if (tmpInvid.equals((Invid)owners.elementAt(j)))
	      {
		return true;
	      }
	  }
      }

    return false;
  }
}
/*

   GanymedeSession.java

   The GanymedeSession class is the template for the server-side objects
   that track a client's login and provide operations to be performed in
   the Ganymede server.
   
   Created: 17 January 1997
   Version: $Revision: 1.39 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

import arlut.csd.JDialog.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 GanymedeSession

------------------------------------------------------------------------------*/

/**
 *
 * The GanymedeSession class is the template for the server-side objects
 * that track a client's login and provide operations to be performed in
 * the Ganymede server.
 *
 */

final public class GanymedeSession extends UnicastRemoteObject implements Session, Unreferenced {

  static final boolean debug = false;

  // -- 

  Client client;

  boolean logged_in;
  boolean forced_off = false;

  Date connecttime;

  String username;
  String clienthost;
  String lastError;

  DBSession session;

  // the following variables are used to allow us to cache data that we use
  // in permissions handling in this GanymedeSession instead of having to
  // always check the database

  DBObjectBase personaBase = null;
  Date personaTimeStamp;
  DBObject personaObj;		// our current persona object

  DBObjectBase permBase = null;
  Date permTimeStamp;

  DBObject selfPermObj;
  PermMatrix selfPerm = null;

  Invid selfPermissionObjectInvid = new Invid(SchemaConstants.PermBase,
					      SchemaConstants.PermSelfUserObj);
  Invid personaInvid;
  Invid userInvid;

  // the following variables keep track of general permissions and visibility
  // state in this GanymedeSession

  boolean supergashMode = false;
  boolean beforeversupergash = false; // Be Forever Yamamoto

  PermMatrix personaPerms;	// what permission bits are applicable to objects accessible by the current persona?

  PermMatrix defaultPerms;	// what permission bits are applicable to any generic objects not 
				// specifically accessible by ownership relations by this persona?

  Vector newObjectOwnerInvids = null;	// vector of Invids pointing to owner groups that the admin wants
				        // newly created objects to be placed in

  Vector visibilityFilterInvids = null; // vector of Invids pointing to owner groups that the admin wants
				        // his set of objects displayed to be limited to.

  QueryResult ownerList = null;	// if we've had a getOwnerGroups call made, the result will be cached here

  /* -- */

  /**
   *
   * Constructor for a server-internal GanymedeSession.  Used when
   * the server's internal code needs to do a query, etc.  Note that
   * the Ganymede server will create this fairly early on, and will
   * keep it around for internal usage.  Note that we don't add
   * this to the data structures used for the admin console.
   *
   * Note that all internal session activities (queries, etc.) are
   * currently using a single, synchronized DBSession object.. this
   * mean that only one user at a time can currently be processed for
   * login. 8-(
   * 
   */

  GanymedeSession() throws RemoteException
  {
    super();			// UnicastRemoteObject initialization

    // construct our DBSession

    logged_in = true;
    client = null;
    username = "internal";
    clienthost = "internal";
    session = new DBSession(Ganymede.db, this, "internal");

    supergashMode = true;
    beforeversupergash = true;

    updatePerms();
  }

  /**
   *
   * Constructor used to create a server-side attachment for a Ganymede
   * client.
   *
   * @param client Remote object exported by the client, provides id callbacks
   * @param userObject The user record for this login
   * @param personaObject The user's initial admin persona 
   * 
   */
  
  GanymedeSession(Client client, DBObject userObject, DBObject personaObject) throws RemoteException
  {
    super();			// UnicastRemoteObject initialization

    // --

    String temp;
    int i=2;

    /* -- */
    
    this.client = client;

    if (userObject != null)
      {
	userInvid = userObject.getInvid();
      }
    else
      {
	userInvid = null;
      }

    if (personaObject != null)
      {
	personaInvid = personaObject.getInvid();
      }
    else
      {
	personaInvid = null;	// shouldn't happen
      }
    
    try
      {
	temp = client.getName();

	username = temp;

	// find a unique name for this user's session

	while (GanymedeServer.activeUsers.containsKey(username))
	  {
	    username = temp + i;
	    i++;
	  }
	
	GanymedeServer.activeUsers.put(username, username);

	logged_in = true;
      }
    catch (RemoteException ex)
      {
	Ganymede.debug("GanymedeSession: couldn't verify username on client" + ex);
	logged_in = false;
      }

    try
      {
	clienthost = getClientHost();
      }
    catch (ServerNotActiveException ex)
      {
	clienthost = "unknown";
      }

    // record our login time

    connecttime = new Date();

    // construct our DBSession

    session = new DBSession(Ganymede.db, this, client.getName());

    GanymedeServer.sessions.addElement(this);
    GanymedeAdmin.refreshUsers();
    updatePerms();

    Ganymede.debug("User " + username + " is " + (supergashMode ? "" : "not ") + "active with supergash privs");
  }

  //************************************************************
  //
  // Non-remote methods (for server-side code)
  //
  //************************************************************

  synchronized void setLastError(String status)
  {
    lastError = status;
    Ganymede.debug("GanymedeSession [" + username + "]: setLastError (" + lastError + ")");
  }

  // if the server decides this person needs to get off
  // (if the user times out, is forced off by an admin, the
  // server is going down),
  // it will call this method to knock them off.

  void forceOff(String reason)
  {
    Ganymede.debug("Forcing " + username + " off for " + reason);

    if (client != null)
      {
	try
	  {
	    client.forceDisconnect(reason);
	  }
	catch (RemoteException e)
	  {
	    // ok, they're already gone.. (?)
	  }
      }

    Vector objects = new Vector();

    if (userInvid != null)
      {
	objects.addElement(userInvid);
      }

    if (personaInvid != null)
      {
	objects.addElement(personaInvid);
      }

    if (Ganymede.log != null)
      {
	Ganymede.log.logSystemEvent(new DBLogEvent("abnormallogout",
						   "Abnormal termination for username: " + username + "\n" +
						   reason,
						   userInvid,
						   username,
						   objects,
						   null));
      }

    forced_off = true;		// keep logout from logging a normal logout

    this.logout();
  }

  /**
   *
   * This method is called when the Java RMI system detects that this
   * remote object is no longer referenced by any remote objects.
   *
   * This method handles abnormal logouts.
   *
   * @see java.rim.server.Unreferenced
   *
   */

  public void unreferenced()
  {
    if (logged_in)
      {
	forceOff("dead client");
      }
  }

  //************************************************************
  //
  // All methods from this point on are part of the Server remote
  // interface, and can be called by the client via RMI.
  //
  //************************************************************

  /** 
   * getLastError() returns text explaining the last
   * error condition 
   *
   * @see arlut.csd.ganymede.Session
   */
  
  synchronized public String getLastError()
  {
    String result = null;

    /* -- */

    if (lastError != null)
      {
	result = lastError;
	lastError = null;
      }
    else if (session != null)
      {
	result = session.lastError;
	session.lastError = null;
      }

    return result;
  }

  /**
   * 
   * Log out this session.
   *
   * @see arlut.csd.ganymede.Session
   */

  synchronized public void logout()
  {
    if (client == null)
      {
	return;
      }

    Ganymede.debug("User " + username + " logging off");
    logged_in = false;
    this.client = null;

    // logout the client, abort any DBSession transaction going

    session.logout();

    if (!forced_off)
      {
	Vector objects = new Vector();

	if (userInvid != null)
	  {
	    objects.addElement(userInvid);
	  }

	if (personaInvid != null)
	  {
	    objects.addElement(personaInvid);
	  }

	if (Ganymede.log != null)
	  {
	    Ganymede.log.logSystemEvent(new DBLogEvent("normallogout",
						       "OK logout for username: " + username,
						       userInvid,
						       username,
						       objects,
						       null));
	  }
      }

    GanymedeServer.sessions.removeElement(this);
    GanymedeServer.activeUsers.remove(username);
    GanymedeAdmin.refreshUsers();

    ownerList = null;		// make GC happier

    Ganymede.debug("User " + username + " logged off");

    this.username = null;
    this.lastError = null;
  }

  /**
   *
   * This method returns a list of personae names available
   * to the user logged in.
   *
   * @see arlut.csd.ganymede.Session
   *
   */

  public synchronized Vector getPersonae()
  {
    DBObject user;
    Vector results;
    InvidDBField inv;

    /* -- */

    user = getUser();

    if (user == null)
      {
	return null;
      }

    results = new Vector();

    inv = (InvidDBField) user.getField(SchemaConstants.UserAdminPersonae);

    for (int i = 0; i < inv.size(); i++)
      {
	results.addElement(viewObjectLabel((Invid)inv.getElement(i)));
      }

    results.addElement(user.getLabel()); // add their 'end-user' persona

    return results;
  }

  /**
   *
   * This method is used to select an admin persona, changing the
   * permissions that the user has and the objects that are
   * accessible in the database.
   *
   * @see arlut.csd.ganymede.Session
   * 
   */

  public synchronized boolean selectPersona(String persona, String password)
  {
    DBObject 
      user,
      personaObject = null;

    InvidDBField inv;
    Invid invid;
    PasswordDBField pdbf;

    /* -- */

    user = getUser();

    if (user == null)
      {
	return false;
      }

    // if they are selecting their username, go ahead and 
    // clear out the persona privs and return true

    if (user.getLabel().equals(persona))
      {
	personaObject = null;
	personaInvid = null;
	personaTimeStamp = null;
	updatePerms();
	return true;
      }

    inv = (InvidDBField) user.getField(SchemaConstants.UserAdminPersonae);

    for (int i = 0; i < inv.size(); i++)
      {
	invid = (Invid) inv.getElement(i);

	// it's okay to use viewDBObject() here, because we are always
	// going to be doing this for internal purposes

	personaObject = session.viewDBObject(invid);

	if (personaObject.getLabel().equals(persona))
	  {
	    break;
	  }

	personaObject = null;
      }

    if (personaObject == null)
      {
	Ganymede.debug("Couldn't find persona " + persona + " for user:" + user.getLabel());
	return false;
      }

    pdbf = (PasswordDBField) personaObject.getField(SchemaConstants.PersonaPasswordField);
    
    if (pdbf != null && pdbf.matchPlainText(password))
      {
	Ganymede.debug("Found persona " + persona + " for user:" + user.getLabel());
	personaInvid = personaObject.getInvid();
	personaTimeStamp = null;
	updatePerms();
	ownerList = null;
	return true;
      }

    return false;
  }

  /**
   *
   * This method returns a QueryResult of owner groups that the current
   * persona has access to.  This list is the transitive closure of
   * the list of owner groups in the current persona.  That is, the
   * list includes all the owner groups in the current persona along
   * with all of the owner groups those owner groups own, and so on.
   *
   */

  public synchronized QueryResult getOwnerGroups()
  {
    QueryResult result = new QueryResult();
    InvidDBField inf;
    Vector groups, children, temp;
    Hashtable seen = new Hashtable();
    Invid inv;
    DBObject owner;

    /* -- */

    if (ownerList != null)
      {
	return ownerList;
      }

    if (personaInvid == null)
      {
	return result;		// empty list
      }

    // if we're in supergash mode, return all the owner groups
    // defined

    if (supergashMode)
      {
	Query q = new Query(SchemaConstants.OwnerBase);
	q.setFiltered(false);

	ownerList = query(q);	// save for our cache

	return ownerList;
      }

    // otherwise, we've got to do a very little bit of legwork

    inf = (InvidDBField) personaObj.getField(SchemaConstants.PersonaGroupsField);

    if (inf == null)
      {
	return result;		// empty list
      }

    // do a breadth-first search of the owner groups

    groups = inf.getValuesLocal();

    while (groups != null && (groups.size() > 0))
      {
	children = new Vector();

	for (int i = 0; i < groups.size(); i++)
	  {
	    inv = (Invid) groups.elementAt(i);

	    if (seen.contains(inv))
	      {
		continue;
	      }

	    // it's okay to use session.viewDBObject here because we
	    // are always going to be doing this operation for supergash's
	    // benefit.  Objects that are pulled directly from the hashes
	    // don't have owners, and so will always grant us access.

	    owner = session.viewDBObject(inv);

	    if (owner == null)
	      {
		continue;
	      }
	    else
	      {
		seen.put(inv, inv);
	      }

	    result.addRow(inv, owner.getLabel());

	    // got the parent.. now add any children

	    inf = (InvidDBField) owner.getField(SchemaConstants.OwnerObjectsOwned);

	    temp = inf.getValuesLocal();

	    for (int j = 0; j < temp.size(); j++)
	      {
		inv = (Invid) temp.elementAt(j);
		
		if (inv.getType() == SchemaConstants.OwnerBase)
		  {
		    children.addElement(inv);
		  }
	      }
	  }

	groups = children;
      }
    
    ownerList = result;
    return ownerList;
  }

  /**
   *
   * This method may be used to set the owner groups of any objects
   * created hereafter.
   *
   * @param ownerInvids a Vector of Invid objects pointing to
   * ownergroup objects.
   *
   */

  public ReturnVal setDefaultOwner(Vector ownerInvids)
  {
    if (ownerInvids == null)
      {
	newObjectOwnerInvids = ownerInvids;
	return null;
      }

    if (!supergashMode && !isMemberAll(ownerInvids))
      {
	return createErrorDialog("Error",
				 "Error.. ownerInvids contains invid that the persona is not a member of.");
      }
    else
      {
	newObjectOwnerInvids = ownerInvids;
	return null;
      }
  }

  /**
   *
   * This method may be used to cause the server to pre-filter any object
   * listing to only show those objects directly owned by owner groups
   * referenced in the ownerInvids list.  This filtering will not restrict
   * the ability of the client to directly view any object that the client's
   * persona would normally have access to, but will reduce clutter and allow
   * the client to present the world as would be seen by administrator personas
   * with just the listed ownerGroups accessible.
   *
   * This method cannot be used to grant access to objects that are accessible
   * by the client's adminPersona.
   *
   * Calling this method with ownerInvids set to null will turn off the filtering.
   *
   * @param ownerInvids a Vector of Invid objects pointing to ownergroup objects.
   *
   */

  public ReturnVal filterQueries(Vector ownerInvids)
  {
    if (ownerInvids == null)
      {
	visibilityFilterInvids = ownerInvids;
	return null;
      }

    if (!supergashMode && !isMemberAll(ownerInvids))
      {
	return createErrorDialog("Server Error",
				 "Error.. ownerInvids contains invid that the persona is not a member of.");
      }
    else
      {
	visibilityFilterInvids = ownerInvids;
	return null;
      }
  }


  //  Database operations

  /**
   *
   * This method returns a list of remote references to the Ganymede
   * object type definitions.  This method will throws a RuntimeException
   * if it is called when the server is in schemaEditMode.
   * 
   * @see arlut.csd.ganymede.Session
   */

  public Vector getTypes()
  {
    Enumeration enum;
    Vector result = new Vector();

    /* -- */

    synchronized (Ganymede.db)
      {
	if (Ganymede.db.schemaEditInProgress)
	  {
	    throw new RuntimeException("schemaEditInProgress");
	  }

	enum = Ganymede.db.objectBases.elements();

	while (enum.hasMoreElements())
	  {
	    DBObjectBase base = (DBObjectBase) enum.nextElement();

	    if (getPerm(base.getTypeID()).isVisible())
	      {
		result.addElement(base);
	      }
	  }
      }

    return result;
  }

  /**
   *
   * Returns the root of the category tree on the server
   *
   * @see arlut.csd.ganymede.Category
   *
   */

  public Category getRootCategory()
  {
    return Ganymede.db.rootCategory;
  }

  /**
   *
   * Returns a vector of field definition templates, in display order.
   *
   * This vector may be cached, as it is static for this object type.
   *
   * @see arlut.csd.ganymede.FieldTemplate
   * @see arlut.csd.ganymede.Session
   */

  public synchronized Vector getFieldTemplateVector(short baseId)
  {
    Vector results = new Vector();
    Enumeration enum;
    DBObjectBaseField fieldDef;
    DBObjectBase base;

    /* -- */

    base = session.store.getObjectBase(baseId);

    synchronized (base)
      {
	enum = base.sortedFields.elements();;
	
	while (enum.hasMoreElements())
	  {
	    fieldDef = (DBObjectBaseField) enum.nextElement();

	    results.addElement(fieldDef.template);
	  }
      }

    return results;
  }


  /**
   *
   * This method call initiates a transaction on the server.  This
   * call must be executed before any objects are modified (created,
   * edited, inactivated, removed).
   *
   * Currently each client can only have one transaction open.. it
   * is an error to call openTransaction() while another transaction
   * is still open, and an exception will be thrown.
   *
   * @param describe An optional string containing a comment to be
   * stored in the modification history for objects modified by this
   * transaction.
   * 
   * @see arlut.csd.ganymede.Session
   */

  public ReturnVal openTransaction(String describe)
  {
    if (session.editSet != null)
      {
	return createErrorDialog("Server: Error in openTransaction()",
				 "Error.. transaction already opened");
      }

    session.openTransaction(describe);

    return null;
  }

  /**
   *
   * This method causes all changes made by the client to be 'locked in'
   * to the database.  When commitTransaction() is called, the changes
   * made by the client during this transaction is logged to a journal
   * file on the server, and the changes will become visible to other
   * clients.
   *
   * If the transaction cannot be committed for some reason,
   * commitTransaction() will instead abort the transaction.  In any
   * case, calling commitTransaction() will close the transaction.
   *
   * @return a ReturnVal object if the transaction could not be committed,
   *            or null if there were no problems
   * 
   * @see arlut.csd.ganymede.Session
   */

  public synchronized ReturnVal commitTransaction()
  {
    if (session.editSet == null)
      {
	return createErrorDialog("Server: Error in commitTransaction()",
				 "Error.. no transaction in progress");
      }

    return session.commitTransaction();
  }

  /**
   *
   * This method causes all changes made by the client to be thrown out
   * by the database, and the transaction is closed.
   *
   * @return a ReturnVal object if the transaction could not be committed,
   *         or null if there were no problems
   *
   * @see arlut.csd.ganymede.Session
   */

  public ReturnVal abortTransaction()
  {
    if (session.editSet == null)
      {
	throw new IllegalArgumentException("no transaction in progress");
      }

    return session.abortTransaction();
  }

  /**
   *
   * This method provides the hook for doing a
   * fast, full or partial, database dump to a string form.
   *
   * @see arlut.csd.ganymede.Query
   * @see arlut.csd.ganymede.Result
   *
   * @see arlut.csd.ganymede.Session
   *
   */

  public synchronized DumpResult dump(Query query)
  {
    DumpResult result;
    DBObjectBase base = null;
    DBObjectBase containingBase = null;
    Vector baseLock = new Vector();
    Enumeration enum;
    Integer key;
    DBObject obj;
    DBReadLock rLock;

    DBField dbf;
    boolean embedded;

    /* -- */

    if (query == null)
      {
	setLastError("null query");
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

    if (query.returnType != -1)
      {
	containingBase = Ganymede.db.getObjectBase(query.returnType);
      }
    else if (query.returnName != null)
      {
	containingBase = Ganymede.db.getObjectBase(query.returnName);
      }

    if (base == null)
      {
	setLastError("No such base");
	return null;
      }

    if (containingBase == null)
      {
	setLastError("No such return type");
	return null;
      }

    embedded = base.isEmbedded();

    Ganymede.debug("Processing dump query\nSearching for matching objects of type " + base.getName());

    // if we're an end user looking at the user base, we'll want to be able to see
    // the fields that the user could see for his own record

    if (!supergashMode && userInvid != null && 
	base.getTypeID() == SchemaConstants.UserBase)
      {
	update_selfPerm();
      }
    
    if (embedded)
      {
	if (debug)
	  {
	    Ganymede.debug("Searching for results of type " + containingBase.getName());
	  }
      }

    if (query.permitList == null)
      {
	if (debug)
	  {
	    Ganymede.debug("Returning default fields");
	  }
      }
    else
      {
	if (debug)
	  {
	    Ganymede.debug("Returning custom fields");
	  }
      }

    baseLock.addElement(base);

    if (debug)
      {
	Ganymede.debug("dump(): " + username + " : opening read lock on " + base.getName());
      }

    try
      {
	rLock = session.openReadLock(baseLock);	// wait for it
      }
    catch (InterruptedException ex)
      {
	setLastError("lock interrupted");
	return null;		// we're probably being booted off
      }

    if (debug)
      {
	Ganymede.debug("dump(): " + username + " : got read lock");
      }

    // Figure out which fields we want to return

    Vector fieldDefs = new Vector();
    DBObjectBaseField field;

    for (int i = 0; i < containingBase.sortedFields.size(); i++)
      {
	field = (DBObjectBaseField) containingBase.sortedFields.elementAt(i);
	
	if (query.permitList == null)
	  {
	    // If they haven't specified the list of fields they want back, make
	    // sure we don't show them built in fields and we don't show them the
	    // objects owned field in the OwnerBase.. that could entail many
	    // thousands of objects listed.  If they really, really want to see
	    // them, let them say so explicitly.

	    if (!field.isBuiltIn() && 
		!(containingBase.getTypeID() == SchemaConstants.OwnerBase &&
		  field.getID() == SchemaConstants.OwnerObjectsOwned))
	      {
		if (supergashMode)
		  {
		    fieldDefs.addElement(field);
		  }
		else
		  {
		    // if selfPerm isn't null, we're going to want to check to see what fields
		    // the user would be able to see of this object type.. even though selfPerms
		    // are only intended to apply to the user's own object record, we need to
		    // get those fields enabled here so that when we later come across the user's
		    // own record, we'll be able to properly dump it.

		    // the getPerm() call, of course, takes into account the current persona and
		    // associated permission objects

		    if (getPerm(base.getTypeID(), field.getID()).isVisible())
		      {
			fieldDefs.addElement(field);
		      }
		    else if (selfPerm != null && selfPerm.getPerm(base.getTypeID(), field.getID()).isVisible())
		      {
			fieldDefs.addElement(field);
		      }
		  }
	      }
	  }
	else if (query.permitList.get(field.getKey()) != null)
	  {
	    if (supergashMode)
	      {
		fieldDefs.addElement(field);
	      }
	    else
	      {
		// if selfPerm isn't null, we're going to want to check to see what fields
		// the user would be able to see of this object type.. even though selfPerms
		// are only intended to apply to the user's own object record, we need to
		// get those fields enabled here so that when we later come across the user's
		// own record, we'll be able to properly dump it.

		// the getPerm() call, of course, takes into account the current persona and
		// associated permission objects
		
		if (getPerm(base.getTypeID(), field.getID()).isVisible())
		  {
		    fieldDefs.addElement(field);
		  }
		else if (selfPerm != null && selfPerm.getPerm(base.getTypeID(), field.getID()).isVisible())
		  {
		    fieldDefs.addElement(field);
		  }
	      }
	  }
      }

    // Now iterate over the objects in the base we're searching on,
    // looking for matches

    result = new DumpResult(fieldDefs);

    QueryResult temp_result = queryDispatch(query, false, false);

    if (debug)
      {
	System.err.println("dump(): processed queryDispatch, building dumpResult buffer");
      }

    if (temp_result != null)
      {
	Invid invid;

	enum = temp_result.invidHash.keys();

	while (enum.hasMoreElements())
	  {
	    invid = (Invid) enum.nextElement();

	    if (debug)
	      {
		System.err.print(".");
	      }

	    // it's okay to use session.viewDBObject because
	    // DumpResult.addRow() uses the GanymedeSession reference
	    // we pass in to handle per-field permissions
	    //
	    // using view_db_object() here would be disastrous, because
	    // it would entail making duplicates of all objects matching
	    // our query

	    result.addRow(session.viewDBObject(invid), this);
	  }
      }

    Ganymede.debug("dump(): completed processing, returning buffer");

    return result;
  }

  /**
   *
   * This method provides the hook for doing all
   * manner of simple object listing for the Ganymede
   * database.  
   *
   * @see arlut.csd.ganymede.Query
   * @see arlut.csd.ganymede.Result
   *
   * @see arlut.csd.ganymede.Session
   */

  public synchronized QueryResult query(Query query)
  {
    return queryDispatch(query, false, true);
  }

  /**
   *
   * This method provides the hook for doing all
   * manner of internal object listing for the Ganymede
   * database.  Unfiltered.
   *
   * @see arlut.csd.ganymede.Query
   * @see arlut.csd.ganymede.Result
   */

  public synchronized Vector internalQuery(Query query)
  {
    Vector result = new Vector();
    QueryResult internalResult = queryDispatch(query, true, false);
    Invid key;
    String val;
    Enumeration enum;

    /* -- */

    if (internalResult != null)
      {
	enum = internalResult.invidHash.keys();

	while (enum.hasMoreElements())
	  {
	    key = (Invid) enum.nextElement();
	    val = (String) internalResult.invidHash.get(key);

	    result.addElement(new Result(key, val));
	  }
      }

    return result;
  }

  /**
   *
   * This method is the primary Query engine for the Ganymede
   * databases
   *
   * @param query The query to be handled
   * @param internal If true, the query filter setting will not be honored
   * @param forTransport If true, the QueryResult will build a buffer for serialization
   *
   */

  public synchronized QueryResult queryDispatch(Query query, boolean internal, boolean forTransport)
  {
    QueryResult result = new QueryResult(forTransport);
    DBObjectBase base = null;
    DBObjectBase containingBase = null;
    Vector baseLock = new Vector();
    Enumeration enum;
    Integer key;
    DBObject obj;
    PermEntry perm;
    DBReadLock rLock;

    // for processing embedded containment

    DBField dbf;
    boolean embedded;

    /* -- */

    if (query == null)
      {
	setLastError("null query");
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

    if (query.returnType != -1)
      {
	containingBase = Ganymede.db.getObjectBase(query.returnType);
      }
    else if (query.returnName != null)
      {
	containingBase = Ganymede.db.getObjectBase(query.returnName);
      }

    if (base == null)
      {
	setLastError("No such base");
	return null;
      }

    if (containingBase == null)
      {
	setLastError("No such return type");
	return null;
      }

    // is this base corresponding to an embedded object?

    embedded = base.isEmbedded();

    if (embedded)
      {
	// Ganymede.debug("Query on embedded type");

	System.err.println("Query on embedded type");
      }
    else
      {
	// Ganymede.debug("Query on non-embedded type");

	System.err.println("Query on non-embedded type");
      }

    // are we able to optimize the query into a direct lookup?

    if ((query.root instanceof QueryDataNode) &&
	((QueryDataNode) query.root).comparator == QueryDataNode.EQUALS)
      {
	// we have a simple search for value == query.. is the field
	// being checked hashed in a namespace?

	QueryDataNode node = (QueryDataNode) query.root;
	DBObjectBaseField fieldDef;

	/* -- */

	if (node.fieldId != -1)
	  {
	    fieldDef = (DBObjectBaseField) base.getField(node.fieldId);
	  }
	else if (node.fieldname != null)
	  {
	    fieldDef = (DBObjectBaseField) base.getField(node.fieldId);
	  }
	else
	  {
	    if (base.getLabelField() != -1)
	      {
		fieldDef = (DBObjectBaseField) base.getField(base.getLabelField());
	      }
	    else
	      {
		setLastError("Couldn't find field identifier in query optimizer");
		return null;
	      }
	  }

	if (fieldDef == null)
	  {
	    Ganymede.debug("ERROR: wound up with a null fieldDef in query optimizer");
	    return null;
	  }

	if (fieldDef.namespace != null)
	  {
	    // aha!  We've got an optimized case!
	    
	    System.err.println("Eureka!  Optimized query!");

	    DBObject resultobject;
	    DBNameSpace ns = fieldDef.namespace;

	    synchronized (ns)
	      {
		DBField resultfield = ns.lookup(node.value);

		// note that DBNameSpace.lookup() will always point us to a field
		// that is currently in the main database, so we don't need to
		// worry that the namespace slot is pointing to a field in a
		// checked-out object.
		
		if (resultfield == null)
		  {
		    return result;	// no result
		  }
		else
		  {
		    // a namespace can map across different field and
		    // object types.. make sure we've got an instance of
		    // the right kind of field

		    if (resultfield.definition != fieldDef)
		      {
			return result; // no match
		      }

		    resultobject = resultfield.owner;
		    
		    // addResultRow() will do our permissions checking for us

		    addResultRow(resultobject, query, result, internal);
		
		    System.err.println("Returning result from optimized query");
		
		    return result;
		  }
	      }
	  }
      }

    baseLock.addElement(base);

    if (debug)
      {
	Ganymede.debug("Query: " + username + " : opening read lock on " + base.getName());
      }
    else
      {
	System.err.println("Query: " + username + " : opening read lock on " + base.getName());
      }

    try
      {
	rLock = session.openReadLock(baseLock);	// wait for it
      }
    catch (InterruptedException ex)
      {
	setLastError("lock interrupted");
	return null;		// we're probably being booted off
      }

    if (debug)
      {
	Ganymede.debug("Query: " + username + " : got read lock");
      }
    else
      {
	System.err.println("Query: " + username + " : got read lock");
      }

    enum = base.objectHash.elements();

    // need to check in here to see if we've had the lock yanked

    while (session.isLocked(rLock) && enum.hasMoreElements())
      {
	obj = (DBObject) enum.nextElement();

	if (DBQueryHandler.matches(query, obj))
	  {
	    if (embedded)
	      {
		while ((obj != null) && 
		       obj.isEmbedded() && 
		       (obj.getTypeID() != containingBase.getTypeID()))
		  {
		    dbf = (DBField) obj.getField(SchemaConstants.ContainerField);
		    
		    // okay to use session.viewDBObject() here because addResultRow
		    // does its own permissions checking if necessary
		    
		    obj = session.viewDBObject((Invid) dbf.getValue());
		  }

		if (obj == null)
		  {
		    Ganymede.debug("Error, couldn't find a containing object for an embedded query");
		    continue;	// try next match
		  }
	      }

	    addResultRow(obj, query, result, internal);
	  }
      }

    session.releaseReadLock(rLock);

    if (debug)
      {
	Ganymede.debug("Query: " + username + " : completed query over primary hash, releasing read lock");
      }
    else
      {
	System.err.println("Query: " + username + " : completed query over primary hash, releasing read lock");
      }

    // find any objects created or being edited in the current transaction that
    // match our criteria

    if (session.isTransactionOpen())
      {
	if (debug)
	  {
	    Ganymede.debug("Query: " + username + " : scanning intratransaction objects");
	  }
	else
	  {
	    System.err.println("Query: " + username + " : scanning intratransaction objects");
	  }

	synchronized (session.editSet)
	  {
	    enum = session.editSet.objects.elements();

	    DBEditObject x;

	    while (enum.hasMoreElements())
	      {
		x = (DBEditObject) enum.nextElement();

		// don't consider objects we already have stored in the result

		if (result.containsInvid(x.getInvid()))
		  {
		    continue;
		  }

		if (x.getStatus() == ObjectStatus.CREATING ||
		    x.getStatus() == ObjectStatus.EDITING)
		  {
		    if (DBQueryHandler.matches(query, x))
		      {
			obj = x;

			if (embedded)
			  {
			    while ((obj != null) && 
				   obj.isEmbedded() && 
				   (obj.getTypeID() != containingBase.getTypeID()))
			      {
				dbf = (DBField) obj.getField(SchemaConstants.ContainerField);

				// it's okay to use session.viewDBObject() here because
				// addResultRow() does its own permissions checking

				obj = session.viewDBObject((Invid) dbf.getValue());
			      }
			
			    if (obj == null)
			      {
				Ganymede.debug("Error, couldn't find a containing object for an embedded query");
				continue;	// try next match
			      }
			  }

			// make sure we've found an object of the proper type.. if we're not
			// querying on an embedded object type, the above clause won't have
			// been run.  DBQueryHandler.matches() doesn't check object type, so
			// we need to do it here before we add this to our result.

			if (obj.getTypeID() != containingBase.getTypeID())
			  {
			    continue;
			  }

			addResultRow(obj, query, result, internal);
		      }
		  }
	      }
	  }

	if (debug)
	  {
	    Ganymede.debug("Query: " + username + " : completed scanning intratransaction objects");
	  }
	else
	  {
	    System.err.println("Query: " + username + " : completed scanning intratransaction objects");
	  }
      }
    
    Ganymede.debug("Query: " + username + ", object type " + base.getName() + " completed");
    return result;
  }

  private final void addResultRow(DBObject obj, Query query, QueryResult result, boolean internal)
  {
    PermEntry perm;

    /* -- */

    if (!supergashMode && !internal)
      {
	perm = getPerm(obj);

	if ((query.editableOnly || !perm.isVisible()) &&
	    (!query.editableOnly || !perm.isEditable()))
	  {
	    return;		// permissions prohibit us from adding this result
	  }
      }

    if (debug)
      {
	Ganymede.debug("Query: " + username + " : adding element " + obj.getLabel() + ", invid: " + obj.getInvid());
      }
    
    if (internal || !query.filtered || filterMatch(obj))
      {
	DBEditObject x;
	
	if (session.isTransactionOpen())
	  {
	    // Do we have a version of this object checked out?

	    x = session.editSet.findObject(obj.getInvid());

	    if (x == null)
	      {
		// nope, go ahead and return the object as we found it in the
		// main hash
		
		result.addRow(obj);
	      }
	    else
	      {
		// yup we found it.. if we are deleting or dropping it, we don't want 
		// to return it
		
		if (x.getStatus() != ObjectStatus.DELETING &&
		    x.getStatus() != ObjectStatus.DROPPING)
		  {
		    // ok, we're editing it or creating it.. now, does it still meet
		    // the search criteria?
		    
		    if (DBQueryHandler.matches(query, x))
		      {
			result.addRow(x);
		      }
		  }
	      }
	  }
	else
	  {
	    // we don't have a transaction open, so there's no worry about us
	    // having a different version of the object open in our transaction
	    
	    result.addRow(obj);
	  }
      }
  }

  /**
   *
   * This method is intended as a lightweight way of returning the
   * current label of the specified invid.  No locking is done,
   * and the label returned will be viewed through the context
   * of the current transaction, if any.
   *
   * @see arlut.csd.ganymede.Session
   */ 

  public String viewObjectLabel(Invid invid)
  {
    // We don't check permissions here, as we use session.viewDBObject().
    //
    // We have made the command decision that finding the label for an
    // invid is not something we need to guard against.  Using
    // session.viewDBObject() here makes this a much more lightweight
    // operation.

    try
      {
	return session.viewDBObject(invid).getLabel();
      }
    catch (NullPointerException ex)
      {
	return null;
      }
  }

  /**
   *
   * This method returns a multi-line string containing excerpts from
   * the Ganymede log relating to <invid>, since time <since>.
   *
   * @param invid The invid identifier for the object whose history is sought
   * @param since Report events since this date, or all events if this is null.
   *
   * @see arlut.csd.ganymede.Session
   */

  public StringBuffer viewObjectHistory(Invid invid, Date since)
  {
    DBObject obj;

    /* -- */

    if (invid == null)
      {
	setLastError("Null invid passed into viewObjectHistory");
	return null;
      }

    // we do our own permissions checking, so we can use session.viewDBObject().

    obj = session.viewDBObject(invid);

    if (obj == null)
      {
	throw new NullPointerException("argh!! null-o in viewObjectHistory on invid " + invid.toString());
      }

    if (!getPerm(obj).isVisible())
      {
	setLastError("Permissions denied to view the history for this invid.");
	return null;
      }

    if (Ganymede.log == null)
      {
	setLastError("Log not active, can't view invid history");
	return null;
      }

    return Ganymede.log.retrieveHistory(invid, since, false);
  }

  /**
   *
   * This method returns a multi-line string containing excerpts from
   * the Ganymede log relating to <invid>, since time <since>.
   *
   * @param invid The invid identifier for the admin Persona whose history is sought
   * @param since Report events since this date, or all events if this is null.
   *
   * @return A String containing a record of events for the Invid in question,
   * or null if permissions are denied to view the history.
   *
   * @see arlut.csd.ganymede.Session
   *
   */

  public StringBuffer viewAdminHistory(Invid invid, Date since)
  {
    DBObject obj;

    /* -- */

    if (invid == null)
      {
	setLastError("Null invid passed into viewObjectHistory");
	return null;
      }

    if (invid.getType() != SchemaConstants.PersonaBase)
      {
	setLastError("Wrong type of invid passed into viewAdminHistory");
	return null;
      }

    // we do our own permissions checking, so we can use session.viewDBObject().

    obj = session.viewDBObject(invid);

    if (obj == null)
      {
	throw new NullPointerException("argh!! null-o in viewObjectHistory on invid " + invid.toString());
      }

    if (!getPerm(obj).isVisible())
      {
	setLastError("Permissions denied to view the history for this invid.");
	return null;
      }

    if (Ganymede.log == null)
      {
	setLastError("Log not active, can't view invid history");
	return null;
      }

    return Ganymede.log.retrieveHistory(invid, since, true);
  }

  /**
   * View an object from the database.  If the return value is null,
   * getLastError() should be called for a description of the problem.
   *
   * view_db_object() can be done at any time, outside of the bounds of
   * any transaction.  view_db_object() returns a snapshot of the object's
   * state at the time the view_db_object() call is processed, and will
   * be transaction-consistent internally.
   *
   * If view_db_object() is called during a transaction, the object
   * will be returned as it stands during the transaction.. that is,
   * if the object has been changed during the transaction, that
   * changed object will be returned, even if the transaction has
   * not yet been committed, and other clients would not be able to
   * see that version of the object.
   *
   * NOTE: It is critical that any code that looks at the values of
   * fields in a DBObject go through a view_db_object() method
   * or else the object will not properly know who owns it, which
   * is critical for it to be able to properly authenticate field
   * access.  Keep in mind, however, that view_db_object clones the
   * DBObject in question, so this method is very heavyweight.
   *
   * @see arlut.csd.ganymede.Session
   */

  public db_object view_db_object(Invid invid)
  {
    return view_db_object(invid, true);
  }

  /**
   * View an object from the database.  If the return value is null,
   * getLastError() should be called for a description of the problem.
   *
   * view_db_object() can be done at any time, outside of the bounds of
   * any transaction.  view_db_object() returns a snapshot of the object's
   * state at the time the view_db_object() call is processed, and will
   * be transaction-consistent internally.
   *
   * If view_db_object() is called during a transaction, the object
   * will be returned as it stands during the transaction.. that is,
   * if the object has been changed during the transaction, that
   * changed object will be returned, even if the transaction has
   * not yet been committed, and other clients would not be able to
   * see that version of the object.
   *
   * NOTE: It is critical that any code that looks at the values of
   * fields in a DBObject go through a view_db_object() method
   * or else the object will not properly know who owns it, which
   * is critical for it to be able to properly authenticate field
   * access.  Keep in mind, however, that view_db_object clones the
   * DBObject in question, so this method is very heavyweight.
   *
   */

  public synchronized db_object view_db_object(Invid invid, boolean checkPerms)
  {
    DBObject obj;
    PermEntry perm;

    /* -- */

    obj = (DBObject) session.viewDBObject(invid);

    if (obj == null)
      {
	throw new NullPointerException("argh!! null-o in view_db_object on invid " + invid.toString());
      }

    if (!checkPerms || getPerm(obj).isVisible())
      {
	try
	  {
	    return new DBObject(obj, this);	// return a copy that knows what GanymedeSession is looking at it
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("Couldn't create copy of DBObject: " + ex);
	  }
      }
    else
      {
	setLastError("Permission to view invid " + invid + " denied.");
	return null;
      }
  }

  /**
   * This method provides a handle to an editable
   * object from the Ganymede database.
   *
   * @see arlut.csd.ganymede.Session
   */

  public db_object edit_db_object(Invid invid)
  {
    DBObject obj;

    /* -- */

    obj = (DBObject) session.viewDBObject(invid);

    if (getPerm(obj).isEditable())
      {
	return session.editDBObject(invid);
      }
    else
      {
	setLastError("Permission to edit invid " + invid + " denied.");
	return null;
      }
  }

  /**
   * This method provides a handle to a newly
   * created object of the specified type in the
   * Ganymede database.
   *
   * @see arlut.csd.ganymede.Session
   */

  public synchronized db_object create_db_object(short type) 
  {
    DBObject newObj;
    
    if (getPerm(type).isCreatable())
      {
	if (newObjectOwnerInvids == null)
	  {
	    if (ownerList == null)
	      {
		getOwnerGroups();
	      }

	    // if we have only one group possible, we'll assume we're putting it in that,
	    // otherwise since the client hasn't done a setDefaultOwner() call, we're
	    // gonna have to abort before even trying to create the object.	    

	    if (ownerList.size() != 1)
	      {
		setLastError("Can't create new object, no way of knowing what owner group to place it in");
		return null;
	      }
	  }

	newObj = session.createDBObject(type);

	if (newObj == null)
	  {
	    return null;	// in case the createDBObject() method rejected for some reason
	  }
	
	if (newObjectOwnerInvids != null)
	  {
	    InvidDBField inf = (InvidDBField) newObj.getField(SchemaConstants.OwnerListField);

	    for (int i = 0; i < newObjectOwnerInvids.size(); i++)
	      {
		inf.addElement(newObjectOwnerInvids.elementAt(i));
	      }
	  }
	else
	  {
	    InvidDBField inf = (InvidDBField) newObj.getField(SchemaConstants.OwnerListField);

	    inf.addElement(ownerList.getInvid(0));
	  }
      }
    else
      {
	setLastError("Permission to create object of type " + type + " denied.");
	return null;
      }

    return (db_object) newObj;
  }

  /**
   *
   * Clone a new object from object <invid>. If the return value is null,
   * getLastError() should be called for a description of the problem. 
   *
   * Typically, only certain values will be cloned.  What values are
   * retained is up to the specific code module provided for the
   * invid type of object.
   *
   * @return the newly created object for editing
   *
   * @see arlut.csd.ganymede.Session
   */

  public synchronized db_object clone_db_object(Invid invid)
  {
    setLastError("clone_db_object is not yet implemented.");
    return null;
  }

  /**
   * Inactivate an object in the database
   *
   * Objects inactivated will typically be altered to reflect their inactive
   * status, but the object itself might not be purged from the Ganymede
   * server for a defined period of time, to allow other network systems
   * to have time to do accounting, clean up, etc., before a user id or
   * network address is re-used.
   *
   * @return a ReturnVal object if the object could not be inactivated,
   *         or null if there were no problems
   *
   * @see arlut.csd.ganymede.Session
   */
  
  public synchronized ReturnVal inactivate_db_object(Invid invid) 
  {
    return createErrorDialog("Server Error",
			     "Error.. inactivate_db_object is not yet implemented on the server");
  }

  /**
   * Remove an object from the database
   *
   * Certain objects cannot be inactivated, but must instead be
   * simply removed on demand.  The active permissions for the client
   * may determine whether a particular type of object may be removed.
   *
   * @return a ReturnVal object if the object could not be inactivated,
   *         or null if there were no problems
   *
   * @see arlut.csd.ganymede.Session
   */
  
  public synchronized ReturnVal remove_db_object(Invid invid) 
  {
    if (debug)
      {
	Ganymede.debug("Attempting to delete object: " + invid);
      }

    if ((invid.getType() == SchemaConstants.PermBase) &&
	(invid.getNum() == SchemaConstants.PermDefaultObj))
      {
	setLastError("Can't delete default permissions definitions"); // for logging

	return createErrorDialog("Server: Error in remove_db_object()",
				"Error.. can't delete default permissions definitions");
      }

    if ((invid.getType() == SchemaConstants.PersonaBase) &&
	(invid.getNum() == 0))
      {
	setLastError("Can't delete supergash persona");	// for logging

	return createErrorDialog("Server: Error in remove_db_object()",
				 "Error.. can't delete supergash persona");
      }

    DBObjectBase objBase = Ganymede.db.getObjectBase(invid.getType());
    DBObject vObj = session.viewDBObject(invid);

    if (vObj == null)
      {
	setLastError("Can't delete non-existent object");

	return createErrorDialog("Server: Error in remove_db_object()",
				 "Error.. can't delete non-existent object");
      }

    if (!getPerm(vObj).isEditable())
      {
	setLastError("Don't have permission to delete object" + vObj.getLabel());

	return createErrorDialog("Server: Error in remove_db_object()",
				 "Don't have permission to delete object" + vObj.getLabel());
      }

    if (!objBase.objectHook.canRemove(session, vObj))
      {
	setLastError("object manager refused deletion" + vObj.getLabel());

	return createErrorDialog("Server: Error in remove_db_object()",
				 "Object Manager refused deletion for " + vObj.getLabel());
      }
    
    session.deleteDBObject(invid);
    return null;
  }

  /**
   *
   * Convenience method to get access to this session's UserBase
   * instance.
   *
   */

  DBObject getUser()
  {
    if (userInvid != null)
      {
	// okay to use session.viewDBObject() here, because
	// getUser() is only used for internal purposes,
	// and we don't need or want to do permissions checking

	return session.viewDBObject(userInvid);
      }
    
    return null;
  }

  // **
  // the following are the non-exported permissions management
  // **

  /**
   *
   * This method takes the administrator's current
   * persona, considers the owner groups the administrator
   * is a member of, checks to see if the object is owned
   * by that group, and determines the appropriate permission
   * bits for the object.  getPerm() will or any proprietary
   * ownership bits with the default permissions to give
   * an appopriate result.
   *
   */

  final PermEntry getPerm(DBObject object)
  {
    PermEntry result;
    DBObject localObj;
    boolean useSelfPerm = false;

    /* -- */

    if (object == null)
      {
	return null;
      }

    if (supergashMode)
      {
	return PermEntry.fullPerms;
      }
    
    // if we're looking at an embedded object, lets cascade up and find the top-level
    // ancestor

    localObj = object;

    while (localObj != null && localObj.isEmbedded())
      {
	InvidDBField inf = (InvidDBField) localObj.getField(SchemaConstants.ContainerField); // container

	if (inf == null)
	  {
	    setLastError("getPerm() error.. couldn't find owner of embedded object " + object.getLabel());
	  }

	Invid inv = (Invid) inf.getValueLocal();

	if (inv == null)
	  {
	    setLastError("getPerm() error <2>.. couldn't find owner of embedded object " + object.getLabel());
	  }

	localObj = session.viewDBObject(inv);
      }

    if (localObj == null)
      {
	setLastError("getPerm() error <3>.. couldn't find owner of embedded object" + object.getLabel());
      }

    // is our current persona an owner of this object in some
    // fashion?

    if (!personaMatch(localObj))
      {
	// nope, so look to our default permissions

	result = defaultPerms.getPerm(localObj.getTypeID());
      }
    else
      {
	// yup, use our persona's permissions
	
	result = personaPerms.getPerm(localObj.getTypeID());
      }

    // if we are operating on behalf of an end user and the object in
    // question happens to be that user's record, we may gain some
    // permissions for this object

    if (userInvid != null && userInvid.equals(localObj.getInvid()))
      {
	update_selfPerm();
	useSelfPerm = true;
      }

    if (result == null)
      {
	if (useSelfPerm && selfPerm != null)
	  {
	    return selfPerm.getPerm(localObj.getTypeID());
	  }
	else
	  {
	    return PermEntry.noPerms;
	  }
      }
    else
      {
	if (useSelfPerm && selfPerm != null)
	  {
	    return selfPerm.getPerm(localObj.getTypeID()).union(result);
	  }
	else
	  {
	    return result;
	  }
      }
  }

  /**
   *
   * This method takes the administrator's current
   * persona, considers the owner groups the administrator
   * is a member of, checks to see if the object is owned
   * by that group, and determines the appropriate permission
   * bits for the field in the object.
   *
   */

  final PermEntry getPerm(DBObject object, short fieldId)
  {
    PermEntry objPerm;
    PermEntry result;
    DBObject localObj;
    boolean useSelfPerm = false;

    /* -- */

    if (object == null)
      {
	return null;
      }

    if (supergashMode)
      {
	return PermEntry.fullPerms;
      }

    // if we're looking at an embedded object, lets cascade up and find the top-level
    // ancestor

    localObj = object;

    while (localObj != null && localObj.isEmbedded())
      {
	InvidDBField inf = (InvidDBField) localObj.getField(SchemaConstants.ContainerField); // container

	if (inf == null)
	  {
	    setLastError("getPerm() error.. couldn't find owner of embedded object " + object.getLabel());
	  }

	Invid inv = (Invid) inf.getValueLocal();

	if (inv == null)
	  {
	    setLastError("getPerm() error <2>.. couldn't find owner of embedded object " + object.getLabel());
	  }

	localObj = session.viewDBObject(inv);
      }

    if (localObj == null)
      {
	setLastError("getPerm() error <3>.. couldn't find owner of embedded object" + object.getLabel());
      }

    // look to see if we have permissions set for the object.. this will
    // be our default permissions for each field in the object unless
    // we have an explicit other permission for the field

    objPerm = getPerm(localObj);

    // is our current persona an owner of this object in some
    // fashion?

    if (!personaMatch(localObj))
      {
	// Ganymede.debug("getPerm for object " + object.getLabel() + " failed.. no persona match");

	result = defaultPerms.getPerm(localObj.getTypeID(), fieldId);
      }
    else
      {
	// ok, we know our persona has ownership.. return the
	// permission entry for this object
	
	result = personaPerms.getPerm(localObj.getTypeID(), fieldId);
      }

    // if we are operating on behalf of an end user and the object in
    // question happens to be that user's record, we may gain some
    // permissions for this object

    if (userInvid != null && userInvid.equals(localObj.getInvid()))
      {
	update_selfPerm();
	useSelfPerm = true;
      }

    if (result == null)
      {
	if (useSelfPerm && selfPerm != null)
	  {
	    // System.err.println("getPerm(2): Returning self permission access for user's object, field: " + fieldId);

	    return selfPerm.getPerm(localObj.getTypeID(), fieldId);
	  }
	else
	  {
	    return objPerm;
	  }
      }
    else
      {
	if (useSelfPerm && selfPerm != null)
	  {
	    // System.err.println("getPerm(2): Returning union and intersection of self permission access, field: " + fieldId);

	    return selfPerm.getPerm(localObj.getTypeID(), fieldId).union(result).intersection(objPerm);
	  }
	else
	  {
	    return result.intersection(objPerm);
	  }
      }
  }

  /**
   *
   * This method takes the administrator's current
   * persona's set of appropriate permission matrices,
   * does a binary OR'ing of the permission bits for
   * the given base, and returns the effective
   * permission entry.
   *
   */

  final PermEntry getPerm(short baseID)
  {
    PermEntry result;

    /* -- */

    if (supergashMode)
      {
	return PermEntry.fullPerms;
      }

    updatePerms(); // make sure we have personaPerms up to date

    // note that we can use personaPerms, since the persona's
    // base type privileges apply generically to objects of the
    // given type

    result = personaPerms.getPerm(baseID);

    if (result == null)
      {
	return PermEntry.noPerms;
      }
    else
      {
	return result;
      }
  }

  /**
   *
   * This method takes the administrator's current
   * persona's set of appropriate permission matrices,
   * does a binary OR'ing of the permission bits for
   * the given base/field pair, and returns the effective
   * permission entry.
   *
   */

  final PermEntry getPerm(short baseID, short fieldID)
  {
    PermEntry result = null;

    /* -- */

    if (supergashMode)
      {
	return PermEntry.fullPerms;
      }

    updatePerms();		// make sure we have personaPerms up to date

    if (personaPerms != null)
      {
	result = personaPerms.getPerm(baseID, fieldID);
	
	// if we don't have a specific permissions entry for
	// this field, inherit the one for the base	
	
	if (result == null)
	  {
	    result = personaPerms.getPerm(baseID);
	  }
      }

    if (result == null)
      {
	return PermEntry.noPerms;
      }
    else
      {
	return result;
      }
  }

  /**
   *
   * This method insures that an up-to-date copy of the selfPermObj
   * permission object is loaded to provide the getPerm() methods with
   * the ability to rapidly verify a user's permission to view himself.
   *
   */
  
  private final synchronized void update_selfPerm()
  {
    if (permBase == null)
      {
	permBase = Ganymede.db.getObjectBase(SchemaConstants.PermBase);
      }

    if (permTimeStamp == null || !permTimeStamp.before(permBase.lastChange))
      {
	// it's okay to use session.viewDBObject() here because the self permissions
	// object is a unique object that all instances of GanymedeSession must
	// have access to.

	selfPermObj = session.viewDBObject(selfPermissionObjectInvid);

	PermissionMatrixDBField field = (PermissionMatrixDBField) selfPermObj.getField(SchemaConstants.PermMatrix);

	if (field == null)
	  {
	    System.err.println("update_selfPerm(): Error: no PermMatrix field in selfperm object");
	  }
	else
	  {
	    selfPerm = field.getMatrix();
	    
	    if (selfPerm == null)
	      {
		System.err.println("update_selfperm(): Error: PermMatrix field's value is null in selfperm object");
	      }
	  }
	
	if (permTimeStamp == null)
	  {
	    permTimeStamp = new Date();
	  }
	else
	  {
	    permTimeStamp.setTime(System.currentTimeMillis());
	  }
      }
  }

  /**
   *
   * This non-exported method is used to generate a comprehensive permissions
   * matrix that applies to all objects owned by the active persona for this
   * user.
   *
   */

  final synchronized void updatePerms()
  {
    if (personaBase == null)
      {
	personaBase = Ganymede.db.getObjectBase(SchemaConstants.PersonaBase);
      }

    if (personaTimeStamp != null && !personaTimeStamp.before(personaBase.lastChange))
      {
	return;
      }

    if (personaInvid != null)
      {
	personaObj = session.viewDBObject(personaInvid);
      }
    else
      {
	personaObj = null;
      }
    
    DBObject defaultObj = session.viewDBObject(SchemaConstants.PermBase, SchemaConstants.PermDefaultObj);

    if (defaultObj != null)
      {
	PermissionMatrixDBField permField = (PermissionMatrixDBField) defaultObj.getField(SchemaConstants.PermMatrix);

	if (permField != null)
	  {
	    defaultPerms = permField.getMatrix();
	  }
	else
	  {
	    Ganymede.debug("GanymedeSession.updatePerms(): Couldn't find get permission field from default privs object");
	  }
      }
    else
      {
	Ganymede.debug("GanymedeSession.updatePerms(): ERROR!  Couldn't find default privs object");
      }
    
    // if we're not locked into supergash mode (for internal sessions, etc.), lets find
    // out whether we're in supergash mode currently
    
    if (!beforeversupergash)
      {
	supergashMode = false;

	if (personaObj == null)
	  {
	    personaPerms = new PermMatrix(defaultPerms);
	    return;
	  }
	else
	  {
	    InvidDBField idbf = (InvidDBField) personaObj.getField(SchemaConstants.PersonaGroupsField);
	    Invid inv;
		
	    if (idbf != null)
	      {
		Vector vals = idbf.getValuesLocal();

		// loop over the owner groups this persona is a member of, see if it includes
		// the supergash owner group
		    
		for (int i = 0; i < vals.size(); i++)
		  {
		    inv = (Invid) vals.elementAt(i);
			
		    if (inv.getNum() == SchemaConstants.OwnerSupergash)
		      {
			supergashMode = true;
			Ganymede.debug("GanymedeSession.updatePerms(): setting supergashMode to true");
			break;
		      }
		  }
	      }

	    if (!supergashMode)
	      {
		// since we're not in supergash mode, we need to take into account the
		// operational privileges granted us by the default permission matrix
		// and all the permission matrices associated with this persona.  Calculate
		// the union of all of the applicable permission matrices.

		personaPerms = new PermMatrix(defaultPerms);

		idbf = (InvidDBField) personaObj.getField(SchemaConstants.PersonaPrivs);

		if (idbf != null)
		  {
		    Vector vals = idbf.getValuesLocal();

		    // calculate the union of all permission matrices in effect for this persona

		    PermissionMatrixDBField pmdbf;
		    DBObject pObj;
		    
		    for (int i = 0; i < vals.size(); i++)
		      {
			inv = (Invid) vals.elementAt(i);
			    
			pObj = session.viewDBObject(inv);
			
			//	 Ganymede.debug("GanymedeSession.updatePerms(): adding perms for " + pObj.getLabel());

			if (pObj != null)
			  {
			    pmdbf = (PermissionMatrixDBField) pObj.getField(SchemaConstants.PermMatrix);

			    if (pmdbf != null)
			      {
				personaPerms = personaPerms.union(pmdbf);
			      }
			  }
		      }
		  }
	      }
	  }
      }

    // remember the last time we pulled personaPerms / defaultPerms

    if (personaTimeStamp == null)
      {
	personaTimeStamp = new Date();
      }
    else
      {
	personaTimeStamp.setTime(System.currentTimeMillis());
      }
    
    return;
  }

  /**
   *
   * This method gives access to the DBObject for the administrator's
   * persona record, if any.  This is used by DBSession to get the
   * label for the administrator for record keeping.
   * 
   */
  
  public DBObject getPersona()
  {
    return personaObj;
  }

  /**
   *
   * This method returns a reference to the DBSession object encapsulated
   * by this GanymedeSession object.  This is intended to be used by
   * subclasses of DBEditObject that might not necessarily be in the
   * arlut.csd.ganymede package.
   *
   */

  public DBSession getSession()
  {
    return session;
  }

  /***
   *
   * This is a convenience method used by the server to return a
   * standard error dialog.
   *
   */

  public ReturnVal createErrorDialog(String title, String body)
  {
    ReturnVal retVal = new ReturnVal(false);
    retVal.setDialog(new JDialogBuff(title,
				     body,
				     "OK",
				     null,
				     "error.gif"));
    return retVal;
  }

  /**
   *
   * This method returns true if the active persona has some sort of
   * owner/access relationship with the object in question through
   * its list of owner groups.
   * 
   */

  private final boolean personaMatch(DBObject obj)
  {
    InvidDBField inf;

    /* -- */

    if (obj == null || personaInvid == null)
      {
	//	Ganymede.debug("Null obj/personaInvid");
	return false;
      }

    inf = (InvidDBField) obj.getField(SchemaConstants.OwnerListField); // owner or container

    if (inf == null)
      {
	//	Ganymede.debug("Null ownerlistfield in object");
	return false;
      }
    
    return recursePersonaMatch(inf.getValuesLocal());
  }

  /**
   *
   * Recursive helper method for personaMatch.. this method
   * does a depth first search up the owner tree for each
   * Invid contained in the invids Vector to see if personaInvid
   * is a member of any of the containing owner groups.
   *
   * @param owners A vector of invids pointing to OwnerBase objects
   *
   */

  private final boolean recursePersonaMatch(Vector owners)
  {
    Invid owner;
    DBObject ownerObj;
    InvidDBField inf;
    Vector members;

    /* -- */

    if (owners == null)
      {
	return false;
      }

    for (int i = 0; i < owners.size(); i++)
      {
	owner = (Invid) owners.elementAt(i);

	ownerObj = session.viewDBObject(owner);

	if (ownerObj == null)
	  {
	    continue;
	  }

	inf = (InvidDBField) ownerObj.getField(SchemaConstants.OwnerMembersField);

	if (inf == null)
	  {
	    continue;
	  }

	members = inf.getValuesLocal();

	for (int j = 0; j < members.size(); j++)
	  {
	    if (personaInvid.equals((Invid) members.elementAt(j)))
	      {
		return true;
	      }
	  }

	// didn't find, recurse up

	inf = (InvidDBField) ownerObj.getField(SchemaConstants.OwnerListField);

	if (inf != null)
	  {
	    if (inf.isVector())
	      {
		if (recursePersonaMatch(inf.getValuesLocal()))
		  {
		    return true;
		  }
	      }
	    else
	      {
		throw new RuntimeException("Owner field not a vector!!");
	      }
	  }
      }

    return false;
  }

  /**
   *
   * This private helper method iterates through the owners
   * vector and checks to see if the current personaInvid is a
   * member of all of the groups through either direct membership
   * or through membership of an owning group.  This method is
   * depends on recursePersonaMatch().
   *
   */

  private final boolean isMemberAll(Vector owners)
  {
    Invid owner;
    DBObject ownerObj;
    InvidDBField inf;
    Vector members;
    boolean found;

    /* -- */

    if (owners == null)
      {
	return false;		// shouldn't happen in context
      }

    // loop over all the owner groups in the vector, make sure
    // that we are a valid member of each of these groups, either
    // directly or through being a member of a group that owns
    // one of these groups.

    for (int i = 0; i < owners.size(); i++)
      {
	found = false;	// yes, but what have you done for me _lately_?

	owner = (Invid) owners.elementAt(i);

	ownerObj = session.viewDBObject(owner);

	if (ownerObj == null)
	  {
	    return false;	// we expect to find it
	  }

	inf = (InvidDBField) ownerObj.getField(SchemaConstants.OwnerMembersField);

	if (inf == null)
	  {
	    return false;	// we expect to find it
	  }

	// see if we are a member of this particular owner group

	members = inf.getValuesLocal();

	for (int j = 0; j < members.size(); j++)
	  {
	    if (personaInvid.equals((Invid) members.elementAt(j)))
	      {
		found = true;
	      }
	  }

	// didn't find, recurse up

	inf = (InvidDBField) ownerObj.getField(SchemaConstants.OwnerListField);

	if (inf != null)
	  {
	    if (inf.isVector())
	      {
		if (recursePersonaMatch(inf.getValuesLocal()))
		  {
		    found = true;
		  }
	      }
	    else
	      {
		throw new RuntimeException("Owner field not a vector!!");
	      }
	  }

	if (!found)
	  {
	    return false;
	  }
      }

    return true;
  }

  /**
   *
   * This method returns true if the visibility filter vector allows
   * visibility of the object in question.  The visibility vector
   * works by direct ownership identity (i.e., no recursing up), so
   * it's a simple loop-di-loop.
   *  
   */

  private final boolean filterMatch(DBObject obj)
  {
    Vector owners;
    InvidDBField inf;
    Invid tmpInvid;

    /* -- */

    if (obj == null)
      {
	return false;
      }

    if (visibilityFilterInvids == null)
      {
	return true;		// no visibility restriction, go for it
      }

    inf = (InvidDBField) obj.getField(SchemaConstants.OwnerListField);

    if (inf == null)
      {
	return false;   // we have a restriction, but the object is only owned by supergash.. nope.
      }
    
    owners = inf.getValuesLocal();

    if (owners == null)
      {
	return false;   // we shouldn't get here, but we don't really care either
      }
    
    // we've got the owners for this object.. now, is there any match between our
    // visibilityFilterInvids and the owners of this object?

    for (int i = 0; i < visibilityFilterInvids.size(); i++)
      {
	tmpInvid = (Invid) visibilityFilterInvids.elementAt(i);

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

/*

   GanymedeSession.java

   The GanymedeSession class is the template for the server-side objects
   that track a client's login and provide operations to be performed in
   the Ganymede server.
   
   Created: 17 January 1997
   Version: $Revision: 1.25 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

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

final class GanymedeSession extends UnicastRemoteObject implements Session {

  static final boolean debug = false;

  // -- 

  Client client;

  boolean logged_in;

  Date connecttime;

  String username;
  String clienthost;
  String lastError;

  DBSession session;

  Date personaTimeStamp;
  DBObject personaObj;		// our current persona object
  Invid personaInvid;
  Invid userInvid;

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

    this.logout();
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

    session.releaseAllReadLocks(); // given that we're synchronized, will we ever have locks here?
    session.logout();
 
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

	personaObject = (DBObject) Ganymede.internalSession.view_db_object(invid);

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

    groups = inf.getValues();

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

	    temp = inf.getValues();

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

  public void setDefaultOwner(Vector ownerInvids)
  {
    if (ownerInvids == null)
      {
	newObjectOwnerInvids = ownerInvids;
	return;
      }

    if (!supergashMode && !isMemberAll(ownerInvids))
      {
	throw new IllegalArgumentException("Error.. ownerInvids contains invid that the persona is not a member of.");
      }
    else
      {
	newObjectOwnerInvids = ownerInvids;
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

  public void filterQueries(Vector ownerInvids)
  {
    if (ownerInvids == null)
      {
	visibilityFilterInvids = ownerInvids;
	return;
      }

    if (!supergashMode && !isMemberAll(ownerInvids))
      {
	throw new IllegalArgumentException("Error.. ownerInvids contains invid that the persona is not a member of.");
      }
    else
      {
	visibilityFilterInvids = ownerInvids;
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

  public void openTransaction(String describe)
  {
    if (session.editSet != null)
      {
	throw new IllegalArgumentException("transaction already opened");
      }

    session.openTransaction(describe);
  }

  /**
   *
   * This method causes all changes made by the client to be 'locked in'
   * to the database.  When commitTransaction() is called, the changes
   * made by the client during this transaction is logged to a journal
   * file on the server, and the changes will become visible to other
   * clients, or to subsequent queries and view_db_object() calls by
   * this client.
   *
   * If the transaction cannot be committed for some reason,
   * commitTransaction() will instead abort the transaction.  In any
   * case, calling commitTransaction() will close the transaction.
   *
   * @returns false if the transaction could not be committed.  
   *                getLastError() can be called to obtain an explanation
   *                of commit failure.
   * 
   * @see arlut.csd.ganymede.Session
   */

  public synchronized boolean commitTransaction()
  {
    if (session.editSet == null)
      {
	throw new IllegalArgumentException("no transaction in progress");
      }

    return session.commitTransaction();
  }

  /**
   *
   * This method causes all changes made by the client to be thrown out
   * by the database, and the transaction is closed.
   *
   * @see arlut.csd.ganymede.Session
   */

  public void abortTransaction()
  {
    if (session.editSet == null)
      {
	throw new IllegalArgumentException("no transaction in progress");
      }

    session.abortTransaction();
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

    Ganymede.debug("Processing dump query");
    Ganymede.debug("Searching for matching objects of type " + base.getName());
    
    if (embedded)
      {
	Ganymede.debug("Searching for results of type " + containingBase.getName());
      }

    if (query.permitList == null)
      {
	Ganymede.debug("Returning default fields");
      }
    else
      {
	Ganymede.debug("Returning custom fields");
      }

    baseLock.addElement(base);

    Ganymede.debug("Query: " + username + " : opening read lock on " + base.getName());

    try
      {
	rLock = session.openReadLock(baseLock);	// wait for it
      }
    catch (InterruptedException ex)
      {
	setLastError("lock interrupted");
	return null;		// we're probably being booted off
      }

    Ganymede.debug("Query: " + username + " : got read lock");

    // Figure out which fields we want to return

    Vector fieldDefs = new Vector();
    DBObjectBaseField field;

    for (int i = 0; i < containingBase.sortedFields.size(); i++)
      {
	field = (DBObjectBaseField) containingBase.sortedFields.elementAt(i);
	
	if (query.permitList == null)
	  {
	    if (!field.isBuiltIn())
	      {	    
		if (supergashMode)
		  {
		    fieldDefs.addElement(field);
		  }
		else
		  {
		    if (getPerm(base.getTypeID(), field.getID()).isVisible())
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
		if (getPerm(base.getTypeID(), field.getID()).isVisible())
		  {
		    fieldDefs.addElement(field);
		  }
	      }
	  }
      }

    // Now iterate over the objects in the base we're searching on,
    // looking for matches

    result = new DumpResult(fieldDefs);

    enum = base.objectHash.keys();

    // need to check in here to see if we've had the lock yanked

    while (session.isLocked(rLock) && enum.hasMoreElements())
      {
	key = (Integer) enum.nextElement();
	obj = (DBObject) base.objectHash.get(key);

	if (DBQueryHandler.matches(query, obj))
	  {
	    if (embedded)
	      {
		while ((obj != null) && 
		       obj.isEmbedded() && 
		       (obj.getTypeID() != containingBase.getTypeID()))
		  {
		    dbf = (DBField) obj.getField(SchemaConstants.ContainerField);
		    obj = (DBObject) view_db_object((Invid) dbf.getValue());
		  }

		if (obj.getTypeID() != containingBase.getTypeID())
		  {
		    // wrong container type

		    Ganymede.debug("Error, couldn't find parent of proper type");
		    continue;	// try next match
		  }

		if (obj == null)
		  {
		    Ganymede.debug("Error, couldn't find a containing object for an embedded query");
		    continue;	// try next match
		  }
	      }

	    if (supergashMode)
	      {
		if (!query.filtered || filterMatch(obj))
		  {
		    result.addRow(obj);
		  }
	      } 
	    else
	      {
		PermEntry perm;

		/* -- */

		perm = getPerm(obj);

		if ((!query.editableOnly && perm.isVisible()) ||
		    (query.editableOnly && perm.isEditable()))
		  {
		    if (!query.filtered || filterMatch(obj))
		      {
			result.addRow(obj);
		      }
		  }
	      }
	  }
      }

    if (!session.isLocked(rLock))
      {
	setLastError("lock interrupted");
	return null;
      }
    
    session.releaseReadLock(rLock);

    Ganymede.debug("Query: " + username + " : released read lock");

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
    QueryResult result = new QueryResult();
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
	Ganymede.debug("Query on embedded type");
      }
    else
      {
	Ganymede.debug("Query on non-embedded type");
      }

    baseLock.addElement(base);

    Ganymede.debug("Query: " + username + " : opening read lock on " + base.getName());

    try
      {
	rLock = session.openReadLock(baseLock);	// wait for it
      }
    catch (InterruptedException ex)
      {
	setLastError("lock interrupted");
	return null;		// we're probably being booted off
      }

    Ganymede.debug("Query: " + username + " : got read lock");

    enum = base.objectHash.keys();

    // need to check in here to see if we've had the lock yanked

    while (session.isLocked(rLock) && enum.hasMoreElements())
      {
	key = (Integer) enum.nextElement();
	obj = (DBObject) base.objectHash.get(key);

	if (DBQueryHandler.matches(query, obj))
	  {
	    if (embedded)
	      {
		while ((obj != null) && 
		       obj.isEmbedded() && 
		       (obj.getTypeID() != containingBase.getTypeID()))
		  {
		    dbf = (DBField) obj.getField(SchemaConstants.ContainerField);
		    obj = (DBObject) view_db_object((Invid) dbf.getValue());
		  }

		if (obj == null)
		  {
		    Ganymede.debug("Error, couldn't find a containing object for an embedded query");
		    continue;	// try next match
		  }
	      }

	    if (supergashMode)
	      {
		if (debug)
		  {
		    Ganymede.debug("Query: " + username + " : adding element " + obj.getLabel() + ", invid: " + obj.getInvid());
		  }

		if (!query.filtered || filterMatch(obj))
		  {
		    result.addRow(obj);
		  }
	      } 
	    else
	      {
		perm = getPerm(obj);

		if ((!query.editableOnly && perm.isVisible()) ||
		    (query.editableOnly && perm.isEditable()))
		  {
		    if (debug)
		      {
			Ganymede.debug("Query: " + username + " : adding element " + obj.getLabel() + ", invid: " + obj.getInvid());
		      }
			
		    if (!query.filtered || filterMatch(obj))
		      {
			result.addRow(obj);
		      }
		  }
	      }
	  }
      }

    if (!session.isLocked(rLock))
      {
	setLastError("lock interrupted");
	return null;
      }
    
    session.releaseReadLock(rLock);

    Ganymede.debug("Query: " + username + " : released read lock");

    return result;
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
    DBObjectBase base = null;
    Vector baseLock = new Vector();
    Enumeration enum;
    Integer key;
    DBObject obj;
    DBReadLock rLock;

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

    if (base == null)
      {
	setLastError("No such base");
	return null;
      }

    baseLock.addElement(base);

    Ganymede.debug("Query: " + username + " : opening read lock on " + base.getName());

    try
      {
	rLock = session.openReadLock(baseLock);	// wait for it
      }
    catch (InterruptedException ex)
      {
	setLastError("lock interrupted");
	return null;		// we're probably being booted off
      }

    Ganymede.debug("Query: " + username + " : got read lock");

    enum = base.objectHash.keys();

    // need to check in here to see if we've had the lock yanked

    while (session.isLocked(rLock) && enum.hasMoreElements())
      {
	key = (Integer) enum.nextElement();
	obj = (DBObject) base.objectHash.get(key);

	if (DBQueryHandler.matches(query, obj))
	  {
	    //	    Ganymede.debug("Query: " + username + " : adding element " + obj.getLabel());
	    result.addElement(new Result(obj.getInvid(), obj.getLabel()));
	  }
      }

    if (!session.isLocked(rLock))
      {
	setLastError("lock interrupted");
	return null;
      }
    
    session.releaseReadLock(rLock);

    Ganymede.debug("Query: " + username + " : released read lock");

    return result;
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
    // should we try to check permissions here?  This would
    // make this operation a bit heavier.. there's little
    // harm in allowing viewing of object labels if they
    // have the invid, is there?

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
   * @see arlut.csd.ganymede.Session
   */

  public synchronized db_object view_db_object(Invid invid)
  {
    DBObject obj;
    PermEntry perm;

    /* -- */

    obj = (DBObject) session.viewDBObject(invid);

    if (getPerm(obj).isVisible())
      {
        return obj;
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
   * @see arlut.csd.ganymede.Session
   */
  
  public synchronized boolean inactivate_db_object(Invid invid) 
  {
    setLastError("inactivate_db_object is not yet implemented.");
    return false;
  }

  /**
   * Remove an object from the database
   *
   * Certain objects cannot be inactivated, but must instead be
   * simply removed on demand.  The active permissions for the client
   * may determine whether a particular type of object may be removed.
   *
   * @return true if the object was removed, if false, check
   * getLastError()
   *
   * @see arlut.csd.ganymede.Session
   */
  
  public synchronized boolean remove_db_object(Invid invid) 
  {
    if (debug)
      {
	Ganymede.debug("Attempting to delete object: " + invid);
      }

    if ((invid.getType() == SchemaConstants.PermBase) &&
	(invid.getNum() == SchemaConstants.PermDefaultObj))
      {
	setLastError("Can't delete default permissions definitions");
	return false;
      }

    if ((invid.getType() == SchemaConstants.PersonaBase) &&
	(invid.getNum() == 0))
      {
	setLastError("Can't delete supergash persona");
	return false;
      }

    DBObjectBase objBase = Ganymede.db.getObjectBase(invid.getType());
    DBObject vObj = session.viewDBObject(invid);

    if (vObj == null)
      {
	setLastError("Can't delete non-existent object");
	return false;
      }

    if (!getPerm(vObj).isEditable())
      {
	setLastError("Don't have permission to delete object" + vObj.getLabel());
	return false;
      }

    if (!objBase.objectHook.canRemove(session, vObj))
      {
	setLastError("object manager refused deletion");
	return false;
      }
    
    session.deleteDBObject(invid);
    return true;
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
	return (DBObject) view_db_object(userInvid);
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

    /* -- */

    if (object == null)
      {
	return null;
      }

    if (supergashMode)
      {
	return PermEntry.fullPerms;
      }

    // is our current persona an owner of this object in some
    // fashion?

    if (!personaMatch(object))
      {
	// Ganymede.debug("getPerm for object " + object.getLabel() + " failed.. no persona match");

	result = defaultPerms.getPerm(object.getTypeID());

	if (result == null)
	  {
	    return PermEntry.noPerms;
	  }
	else
	  {
	    return result;
	  }
      }

    // ok, we know our persona has ownership.. return the
    // permission entry for this object

    result = personaPerms.getPerm(object.getTypeID());

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
   * This non-exported method is used to generate a comprehensive permissions
   * matrix that applies to all objects owned by the active persona for this
   * user.
   *
   */

  final synchronized void updatePerms()
  {
    DBObjectBase personaBase = Ganymede.db.getObjectBase(SchemaConstants.PersonaBase);

    /* -- */

    if (personaTimeStamp == null || personaTimeStamp.before(personaBase.lastChange))
      {
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
	    defaultPerms = new PermMatrix((PermissionMatrixDBField) defaultObj.getField(SchemaConstants.PermMatrix));
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
		    Vector vals = idbf.getValues();

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
			Vector vals = idbf.getValues();

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
      }

    return;
  }

  DBObject getPersona()
  {
    return personaObj;
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

    inf = (InvidDBField) obj.getField(SchemaConstants.OwnerListField);

    if (inf == null)
      {
	//	Ganymede.debug("Null ownerlistfield in object");
	return false;
      }
    
    return recursePersonaMatch(inf.getValues());
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

	members = inf.getValues();

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
		if (recursePersonaMatch(inf.getValues()))
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

	members = inf.getValues();

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
		if (recursePersonaMatch(inf.getValues()))
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
    
    owners = inf.getValues();

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

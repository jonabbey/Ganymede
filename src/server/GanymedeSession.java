/*

   GanymedeSession.java

   The GanymedeSession class is the template for the server-side objects
   that track a client's login and provide operations to be performed in
   the Ganymede server.
   
   Created: 17 January 1997
   Version: $Revision: 1.6 $ %D%
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

class GanymedeSession extends UnicastRemoteObject implements Session {

  Client client;

  boolean logged_in;

  Date connecttime;

  String username;
  String clienthost;
  String lastError;

  DBSession session;

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
   * login.
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
    session = new DBSession(Ganymede.db, null, "internal");
  }

  /**
   *
   * Constructor used to create a server-side attachment for a Ganymede
   * client.
   *
   * @param client Remote object exported by the client, provides id callbacks
   * 
   */
  
  GanymedeSession(Client client) throws RemoteException
  {
    super();			// UnicastRemoteObject initialization

    String temp;
    int i=2;
    
    this.client = client;
    
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

    session = new DBSession(Ganymede.db, null, client.getName());

    GanymedeServer.sessions.addElement(this);
    GanymedeAdmin.refreshUsers();
  }

  //************************************************************
  //
  // Non-remote methods (for server-side code)
  //
  //************************************************************

  synchronized void setLastError(String status)
  {
    lastError = status;
    GanymedeAdmin.refreshUsers();
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
    return lastError;
  }

  public boolean set_admin_info()
  {
    return true;
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

    if (session.lock != null)
      {
	session.lock.abort();
	session.lock = null;
      }

    session.logout();
 
    GanymedeServer.sessions.removeElement(this);
    GanymedeServer.activeUsers.remove(username);
    GanymedeAdmin.refreshUsers();

    Ganymede.debug("User " + username + " logged off");

    this.username = null;
    this.lastError = null;
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
	    result.addElement(enum.nextElement());
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
   * @see arlut.csd.ganymede.Session
   */

  public void openTransaction()
  {
    if (session.editSet != null)
      {
	throw new IllegalArgumentException("transaction already opened");
      }

    session.openTransaction();
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
   * This method provides the hook for doing all
   * manner of object listing for the Ganymede
   * database.
   *
   * @see arlut.csd.ganymede.Query
   * @see arlut.csd.ganymede.Result
   *
   * @see arlut.csd.ganymede.Session
   */

  public synchronized Vector query(Query query)
  {
    DBObjectBase base = null;
    Vector baseLock = new Vector();
    Vector result = new Vector();
    Enumeration enum;
    Integer key;
    DBObject obj;

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
	session.openReadLock(baseLock);	// wait for it
      }
    catch (InterruptedException ex)
      {
	setLastError("lock interrupted");
	return null;		// we're probably being booted off
      }

    Ganymede.debug("Query: " + username + " : got read lock");

    enum = base.objectHash.keys();

    // need to check in here to see if we've had the lock yanked

    while (session.isLocked() && enum.hasMoreElements())
      {
	key = (Integer) enum.nextElement();
	obj = (DBObject) base.objectHash.get(key);

	if (DBQueryHandler.matches(query, obj))
	  {
	    Ganymede.debug("Query: " + username + " : adding element " + obj.getLabel());
	    result.addElement(new Result(obj));
	  }
      }

    if (!session.isLocked())
      {
	setLastError("lock interrupted");
	return null;
      }
    
    session.releaseReadLock();

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
    db_object result;
    Vector baseLock;

    /* -- */

    //    baseLock = new Vector();
    //    baseLock.addElement(session.store.getObjectBase(invid.getType()));
    //
    //    try
    //      {
    //	session.openReadLock(baseLock);	// wait for it
    //      }
    //    catch (InterruptedException ex)
    //      {
    //	return null;		// we're probably being booted off
    //      }
    
    result = session.viewDBObject(invid);

    // session.releaseReadLock();

    return result;
  }

  /**
   * This method provides a handle to an editable
   * object from the Ganymede database.
   *
   * @see arlut.csd.ganymede.Session
   */

  public synchronized db_object edit_db_object(Invid invid)
  {
    db_object result;
    Vector baseLock;

    /* -- */

    //    baseLock = new Vector();
    //    baseLock.addElement(session.store.getObjectBase(invid.getType()));
    //
    //    try
    //      {
    //	session.openReadLock(baseLock);	// wait for it
    //      }
    //    catch (InterruptedException ex)
    //      {
    //	return null;		// we're probably being booted off
    //      }
    
    result = session.editDBObject(invid);

    //    session.releaseReadLock();

    return result;
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
    return session.createDBObject(type);
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
    session.deleteDBObject(invid);
    return true;
  }

}

/*

   GanymedeServer.java

   The GanymedeServer object is created by Ganymede at start-up time
   and published to the net for client logins via RMI.  As such,
   the GanymedeServer object is the first Ganymede code that a client
   will directly interact with.
   
   Created: 17 January 1997
   Version: $Revision: 1.16 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  GanymedeServer

------------------------------------------------------------------------------*/

/**
 *
 * The GanymedeServer object is created by Ganymede at start-up time
 * and published to the net for client logins via RMI.  As such,
 * the GanymedeServer object is the first Ganymede code that a client
 * will directly interact with.
 *
 */

public class GanymedeServer extends UnicastRemoteObject implements Server {

  static GanymedeServer server = null;
  static Vector sessions = new Vector();
  static Hashtable activeUsers = new Hashtable(); 
  private int limit;

  /* -- */

  /**
   *
   * GanymedeServer constructor.  We only want one server running
   * per invocation of Ganymede, so we'll check that here.
   *
   */

  public GanymedeServer(int limit) throws RemoteException
  {
    super();			// UnicastRemoteObject initialization
 
    if (server == null)
      {
	this.limit = limit;
	server = this;
      }
    else
      {
	Ganymede.debug("Error: attempted to start a second server");
	throw new RemoteException("Error: attempted to start a second server");
      }
  } 

  /**
   *
   * Establishes a Session object in the server.  The Session object
   * contains all of the server's knowledge about a given client's
   * status.  This method is to be called by the client via RMI.  In
   * addition to returning a Session RMI reference to the client,
   * login() keeps a local reference to the Ganymede Session object
   * for the server's bookkeeping.
   *
   * Does login() need to be synchronized?  If two threads were doing
   * login() simultaneously, would they have their own copies of i?
   * Would clients.addElement() do the right thing?  Is vector.addElement()
   * synchronized?
   * 
   * @see arlut.csd.ganymede.Server
   */

  public synchronized Session login(Client client) throws RemoteException
  {
    String clientName;
    String clientPass;
    boolean found = false;
    Query userQuery;
    QueryNode root;
    DBObject user = null, persona = null;
    PasswordDBField pdbf;

    /* -- */

    synchronized (Ganymede.db)
      {
	if (Ganymede.db.schemaEditInProgress)
	  {
	    client.forceDisconnect("Schema Edit In Progress");
	    return null;
	  }

	if (Ganymede.db.sweepInProgress)
	  {
	    client.forceDisconnect("Invid Sweep In Progress");
	    return null;
	  }
      }
    
    clientName = client.getName();
    clientPass = client.getPassword();

    root = new QueryDataNode(SchemaConstants.UserUserName,QueryDataNode.EQUALS, clientName);
    userQuery = new Query(SchemaConstants.UserBase, root, false);

    Vector results = Ganymede.internalSession.internalQuery(userQuery);

    for (int i = 0; !found && (i < results.size()); i++)
      {
	user = (DBObject) Ganymede.internalSession.view_db_object(((Result) results.elementAt(i)).getInvid());
	
	pdbf = (PasswordDBField) user.getField(SchemaConstants.UserPassword);
	
	if (pdbf.matchPlainText(clientPass))
	  {
	    found = true;
	  }
      }

    if (!found)
      {
	root = new QueryDataNode(SchemaConstants.PersonaNameField, QueryDataNode.EQUALS, clientName);
	userQuery = new Query(SchemaConstants.PersonaBase, root, false);

	results = Ganymede.internalSession.internalQuery(userQuery);

	for (int i = 0; !found && (i < results.size()); i++)
	  {
	    persona = (DBObject) Ganymede.internalSession.view_db_object(((Result) results.elementAt(i)).getInvid());
	    
	    pdbf = (PasswordDBField) persona.getField(SchemaConstants.PersonaPasswordField);
	    
	    if (pdbf.matchPlainText(clientPass))
	      {
		found = true;
	      }
	  }
      }

    if (found)
      {
	GanymedeSession session = new GanymedeSession(client, user, persona);
 	Ganymede.debug("Client logged in: " + session.username);

	Vector objects = new Vector();

	if (user != null)
	  {
	    objects.addElement(user.getInvid());
	  }
	else
	  {
	    objects.addElement(persona.getInvid());
	  }

	String clienthost;

	try
	  {
	    clienthost = getClientHost();
	  }
	catch (ServerNotActiveException ex)
	  {
	    clienthost = "unknown";
	  }

	if (Ganymede.log != null)
	  {
	    Ganymede.log.logSystemEvent(new DBLogEvent("normallogin",
						       "OK login for username: " + 
						       clientName + 
						       " from host " + 
						       clienthost,
						       null,
						       clientName,
						       objects,
						       null));
	  }

	return (Session) session;
      }
    else
      {
	String clienthost;

	try
	  {
	    clienthost = getClientHost();
	  }
	catch (ServerNotActiveException ex)
	  {
	    clienthost = "unknown";
	  }

	Ganymede.debug("Bad login attempt: " + clientName + " from host " + clienthost);

	if (Ganymede.log != null)
	  {
	    Vector recipients = new Vector();

	    recipients.addElement(clientName); // this might well bounce.  C'est la vie.

	    Ganymede.log.logSystemEvent(new DBLogEvent("badpass",
						       "Bad login attempt for username: " + 
						       clientName + " from host " + 
						       clienthost,
						       null,
						       clientName,
						       null,
						       recipients));
	  }

	return null;
      }
  }

  /**
   * Establishes an GanymedeAdmin object in the server. 
   *
   * Adds <admin> as a monitoring admin console
   *
   * @see arlut.csd.ganymede.Server
   *
   */

  public synchronized adminSession admin(Admin admin) throws RemoteException
  {
    String clientName;
    String clientPass;
    boolean found = false;
    Query userQuery;
    QueryNode root;
    DBObject obj;
    PasswordDBField pdbf;

    String clienthost;

    /* -- */

    clientName = admin.getName();
    clientPass = admin.getPassword();

    root = new QueryDataNode(SchemaConstants.PersonaNameField, QueryDataNode.EQUALS, clientName);
    userQuery = new Query(SchemaConstants.PersonaBase, root, false);

    Vector results = Ganymede.internalSession.internalQuery(userQuery);

    for (int i = 0; !found && (i < results.size()); i++)
      {
	obj = (DBObject) Ganymede.internalSession.view_db_object(((Result) results.elementAt(i)).getInvid());
	    
	pdbf = (PasswordDBField) obj.getField(SchemaConstants.PersonaPasswordField);
	    
	if (pdbf.matchPlainText(clientPass))
	  {
	    found = true;
	  }
      }

    try
      {
	clienthost = getClientHost();
      }
    catch (ServerNotActiveException ex)
      {
	clienthost = "unknown";
      }


    if (!found)
      {
	if (Ganymede.log != null)
	  {
	    Ganymede.log.logSystemEvent(new DBLogEvent("badpass",
						       "Bad console attach attempt by: " + 
						       clientName + " from host " + 
						       clienthost,
						       null,
						       clientName,
						       null,
						       null));
	  }

	throw new RemoteException("Bad Admin Account / Password"); // do we have to throw remote here?
      }

    adminSession aSession = new GanymedeAdmin(admin, clientName, clientPass);
    Ganymede.debug("Admin console attached for admin " + clientName + " from: " + clienthost + " " + new Date());

    if (Ganymede.log != null)
      {
	Ganymede.log.logSystemEvent(new DBLogEvent("adminconnect",
						   "Admin console attached by: " + 
						   clientName + " from host " + 
						   clienthost,
						   null,
						   clientName,
						   null,
						   null));
      }

    return aSession;
  }

  public synchronized void dump()
  {
    try
      {
	Ganymede.db.dump(Ganymede.dbFilename, false); // don't release lock
      }
    catch (IOException ex)
      {
	throw new RuntimeException("GanymedeServer.dump error: " + ex);
      }
  }

  /**
   *
   * This method is used when the Ganymede server module is being
   * driven by a direct-linked main method.  This method sweeps
   * through all invid's listed in the (loaded) database, and
   * removes any invid's that point to objects not in the database.
   *
   * @return true if there were any invalid invids in the database
   *
   */

  public synchronized boolean sweepInvids()
  {
    Enumeration
      enum1, enum2, enum3, enum4;

    DBObjectBase
      base;

    DBObject
      object;

    DBField
      field;

    InvidDBField
      iField;

    Vector
      removeVector,
      tempVector;

    Invid
      invid;

    boolean
      vectorEmpty = true,
      swept = false;

    DBSession session = Ganymede.internalSession.session;

    /* -- */

    // make sure we're ok to sweep
    
    synchronized (Ganymede.db)
      {
	if (Ganymede.db.schemaEditInProgress ||
	    Ganymede.db.sweepInProgress)
	  {
	    return false;
	  }

	Ganymede.db.sweepInProgress = true;
      }

    // loop 1: iterate over the object bases

    enum1 = Ganymede.db.objectBases.elements();

    while (enum1.hasMoreElements())
      {
	base = (DBObjectBase) enum1.nextElement();

	// loop 2: iterate over the objects in the current object base

	enum2 = base.objectHash.elements();

	while (enum2.hasMoreElements())
	  {
	    object = (DBObject) enum2.nextElement();

	    removeVector = new Vector();

	    // loop 3: iterate over the fields present in this object

	    enum3 = object.fields.elements();

	    while (enum3.hasMoreElements())
	      {
		field = (DBField) enum3.nextElement();

		if (!(field instanceof InvidDBField))
		  {
		    continue;	// only check invid fields
		  }

		iField = (InvidDBField) field;

		if (iField.isVector())
		  {
		    tempVector = iField.values;
		    vectorEmpty = true;

		    // clear out the invid's held in this field pending
		    // successful lookup

		    iField.values = new Vector(); 

		    // iterate over the invid's held in this vector
		    
		    enum4 = tempVector.elements();

		    while (enum4.hasMoreElements())
		      {
			invid = (Invid) enum4.nextElement();

			if (session.viewDBObject(invid) != null)
			  {
			    iField.values.addElement(invid); // keep this invid
			    vectorEmpty = false;
			  }
			else
			  {
			    Ganymede.debug("Removing invid: " + invid + 
					   " from vector field " + iField.getName() +
					   " from object " +  base.getName() + 
					   ":" + object.getLabel());
			    swept = true;
			  }
		      }

		    // now, if the vector is totally empty, we'll be removing
		    // this field from definition

		    if (vectorEmpty)
		      {
			removeVector.addElement(new Short(iField.getID()));
			iField.defined = false;
		      }
		  }
		else
		  {
		    invid = (Invid) iField.value;

		    if (session.viewDBObject(invid) == null)
		      {
			swept = true;
			removeVector.addElement(new Short(iField.getID()));
			iField.defined = false;

			Ganymede.debug("Removing invid: " + invid + 
				       " from scalar field " + iField.getName() +
				       " from object " +  base.getName() + 
				       ":" + object.getLabel());
		      }
		  }
	      }

	    // need to remove undefined fields now

	    for (int i = 0; i < removeVector.size(); i++)
	      {
		object.fields.remove(removeVector.elementAt(i));

		Ganymede.debug("Undefining (now) empty field: " + 
			       removeVector.elementAt(i) +
			       " from object " +  base.getName() + 
			       ":" + object.getLabel());
	      }
	  }
      }

    synchronized (Ganymede.db)
      {
	Ganymede.db.sweepInProgress = false;
      }

    return swept;
  }

  /**
   *
   * This method is used for testing.  This method sweeps 
   * through all invid's listed in the (loaded) database, and
   * checks to make sure that they all have valid back pointers.
   *
   * Since the backlinks field (SchemaConstants.BackLinksField)
   * is a general sink for invid's with no homes, we won't explicitly
   * check to see if an invid in a backlink field has a field pointing
   * to it in the target object.. we'll just verify the existence of
   * the object listed.
   *
   * @return true if there were any invids without back-pointers in
   * the database
   * 
   */

  public synchronized boolean checkInvids()
  {
    Enumeration
      enum1, enum2, enum3, enum4;

    DBObjectBase
      base;

    DBObject
      object;

    DBField
      field;

    InvidDBField
      iField;

    Invid
      invid;

    boolean
      ok = true;

    DBSession session = Ganymede.internalSession.session;

    /* -- */
    
    synchronized (Ganymede.db)
      {
	if (Ganymede.db.schemaEditInProgress ||
	    Ganymede.db.sweepInProgress)
	  {
	    return false;
	  }

	Ganymede.db.sweepInProgress = true;
      }

    // loop over the object bases

    enum1 = Ganymede.db.objectBases.elements();

    while (enum1.hasMoreElements())
      {
	base = (DBObjectBase) enum1.nextElement();

	// loop over the objects in this base

	Ganymede.debug("Testing invid links for objects of type " + base.getName());
	
	enum2 = base.objectHash.elements();

	while (enum2.hasMoreElements())
	  {
	    object = (DBObject) enum2.nextElement();

	    Ganymede.debug("Testing invid links for object " + object.getLabel());

	    // loop over the fields in this object	    

	    enum3 = object.fields.elements();

	    while (enum3.hasMoreElements())
	      {
		field = (DBField) enum3.nextElement();

		// we only care about invid fields

		if (!(field instanceof InvidDBField))
		  {
		    continue;
		  }

		// 

		if (field.getID() == SchemaConstants.BackLinksField)
		  {
		    iField = (InvidDBField) field;

		    if (iField.isVector())
		      {
			enum4 = iField.values.elements();
			
			while (enum4.hasMoreElements())
			  {
			    invid = (Invid) enum4.nextElement();

			    if (session.viewDBObject(invid) == null)
			      {
				ok = false;

				Ganymede.debug("*** Backlink field in object " +
					       base.getName() + ":" + object.getLabel() +
					       " has an invid pointing to a non-existent object: " + invid);
			      }
			  }
		      }
		    else
		      {
			Ganymede.debug("*** Error, back links field isn't vector???");
		      }
		  }
		else
		  {
		    iField = (InvidDBField) field;
		    
		    if (!iField.test(session, (base.getName() + ":" + object.getLabel())))
		      {
			ok = false;
		      }
		  }
	      }
	  }
      }

    synchronized (Ganymede.db)
      {
	Ganymede.db.sweepInProgress = false;
      }

    return ok;
  }
}

/*

   GanymedeServer.java

   The GanymedeServer object is created by Ganymede at start-up time
   and published to the net for client logins via RMI.  As such,
   the GanymedeServer object is the first Ganymede code that a client
   will directly interact with.
   
   Created: 17 January 1997
   Version: $Revision: 1.14 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

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
	return (Session) session;
      }
    else
      {
	Ganymede.debug("Bad login attempt: " + clientName);
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

    if (!found)
      {
	throw new RemoteException("Bad Admin Account / Password"); // do we have to throw remote here?
      }

    adminSession aSession = new GanymedeAdmin(admin, clientName, clientPass);
    Ganymede.debug("Admin console attached for admin " + clientName + " " + new Date());
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
}

/*

   GASHAdmin.java

   Admin console for the Java RMI Gash Server

   Created: 28 May 1996
   Version: $Revision: 1.3 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package gash;

import java.rmi.*;
import java.rmi.server.*;
import java.awt.*;
import java.applet.*;
import java.util.*;
import ReportTable;
import ReportTableAttr;

/*------------------------------------------------------------------------------
                                                                           class
                                                                          iAdmin

iClient does all the heavy lifting to connect the server with the client, and
provides callbacks that the server can use to notify the client when something
happens.

------------------------------------------------------------------------------*/

class iAdmin extends UnicastRemoteServer implements Admin {

  private GASHAdmin applet = null;
  private Server server = null;
  private adminSession aSession = null;

  /* -- */

  public iAdmin(GASHAdmin applet, Server server) throws RemoteException
  {
    // UnicastRemoteServer can throw RemoteException 

    this.applet = applet;
    this.server = server;

    try
      {
	aSession = server.admin(this);
      }
    catch (RemoteException ex)
      {
	System.err.println("RMI Error: Couldn't log in to server.\n" + ex.getMessage());
	System.exit(0);
      }
    catch (NullPointerException ex)
      {
	System.err.println("Error: Didn't get server reference.  Exiting now.");
	System.exit(0);
      }

    System.err.println("Got Admin");
  }

  public String getPassword()
  {
    return "Dilbert";
  }
  
  public void disconnect() throws RemoteException
  {
    aSession.logout();
  }

  public void changeStatus(String status)
  {
    applet.statusField.setText(status);
  }

  public void changeUsers(Vector users, Vector status)
  {
    applet.table.clearCells();

    for (int i = 0; i < users.size(); i++)
      {
	applet.table.setCellText(0, i, (String)users.elementAt(i), false);
	applet.table.setCellText(1, i, (String)status.elementAt(i), false);
      }

    applet.table.refreshTable();
  }

}

/*------------------------------------------------------------------------------
                                                                           class
                                                                       GASHAdmin

------------------------------------------------------------------------------*/
public class GASHAdmin extends Applet {

  static GASHAdmin applet = null;
  static iAdmin admin = null;
  static Frame frame = null;

  Panel p1 = null;
  Button quitButton = null;
  TextField statusField = null;
  ReportTable table = null;

  String headers[] = {"User", "Status"};
  int colWidths[] = {100,100};

  /* -- */
  
  public void init() 
  {
    String url = "rmi://csdsun1.arlut.utexas.edu:7211/gash.server";
    Server server = null;

    /* -- */

    /* RMI initialization stuff. We do this for our iClient object. */

    System.setSecurityManager(new StubSecurityManager());

    /* Get a reference to the server */

    try
      {
	Remote obj = Naming.lookup(url);

	if (obj instanceof Server)
	  {
	    server = (Server) obj;
	  }
      }
    catch (NotBoundException ex)
      {
	System.err.println("RMI: Couldn't bind to server object\n" + ex );
      }
    catch (java.net.UnknownHostException ex)
      {
	System.err.println("RMI: Couldn't find server\n" + url );
      }
    catch (RemoteException ex)
      {
	ex.printStackTrace();
	System.err.println("RMI: RemoteException during lookup.\n" + ex);
      }
    catch (java.net.MalformedURLException ex)
      {
	System.err.println("RMI: Malformed URL " + url );
      }

    System.err.println("Bound to server object");


    /* Get our client hooked up to our server */

    try
      {
	admin = new iAdmin(this, server);
      }
    catch (RemoteException ex)
      {
	System.err.println("RMI Error: Couldn't log in to server.\n" + ex.getMessage());
	return;
      }
    catch (NullPointerException ex)
      {
	System.err.println("Error: Didn't get server reference.  Exiting now.");
	return;
      }
  }


  public void resize() 
  {
    resize(600,300);
  }

  public boolean action(Event event, Object obj)
  {
    if (event.target instanceof Button)
      {
	if (event.target == quitButton)
	  {
	    System.err.println("Quitting");
	    try
	      {
		admin.disconnect();
	      }
	    catch (RemoteException ex)
	      {
		System.err.println("Couldn't logout cleanly: " + ex);
	      }
	    finally
	      {
		System.exit(0);
	      }

	    return true;	// we handled the event
	  }
      }
    return false;
  }

  // Our primary constructor.  This will always be called, either
  // from main(), below, or by the environment building our applet.

  public GASHAdmin() 
  {
    setLayout(new BorderLayout());

    table = new ReportTable(colWidths, headers);
    add("Center", table);

    statusField = new TextField("Admin Console Testing", 40);
    statusField.setEditable(false);
    statusField.setBackground(Color.red);
    statusField.setForeground(Color.white);
    add("North", statusField);

    p1 = new Panel();
    p1.setLayout(new FlowLayout());
    add("South", p1);

    quitButton = new Button("Quit");
    p1.add(quitButton);
  }

  public static void main(String[] argv)
  {
    /* Define the applet */

    Frame frame = new Frame("GASH2 Admin Console");
    applet = new GASHAdmin();

    /* present the applet */

    frame.add("Center", applet);
    frame.resize(300, 300);
    frame.show();  
    applet.init();
    applet.start();
  }
}
 

/*

   GASHAdmin.java

   Admin console for the Java RMI Gash Server

   Created: 28 May 1996
   Version: $Revision: 1.6 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;
import java.rmi.server.*;
import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.util.*;

import gjt.Box;

import csd.Table.*;
import csd.Dialog.YesNoDialog;

/*------------------------------------------------------------------------------
                                                                           class
                                                                          iAdmin

iClient does all the heavy lifting to connect the server with the client, and
provides callbacks that the server can use to notify the client when something
happens.

------------------------------------------------------------------------------*/

class iAdmin extends UnicastRemoteObject implements Admin {

  private GASHAdminFrame frame = null;
  private Server server = null;
  private adminSession aSession = null;

  /* -- */

  public iAdmin(GASHAdminFrame frame, Server server) throws RemoteException
  {
    // UnicastRemoteServer can throw RemoteException 

    this.frame = frame;
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

  public void changeStatus(String status)
  {
    frame.statusArea.append(new Date() + " " + status + "\n");
    frame.statusArea.setCaretPosition(frame.statusArea.getText().length());
  }

  public void changeUsers(Vector entries)
  {
    AdminEntry e;

    /* -- */

    frame.table.clearCells();
    
    // Process entries from the server

    for (int i = 0; i < entries.size(); i++)
      {
	e = (AdminEntry) entries.elementAt(i);

	frame.table.newRow(e.username);
	frame.table.setCellText(e.username, 0, e.username, false);
	frame.table.setCellText(e.username, 1, e.hostname, false);
	frame.table.setCellText(e.username, 2, e.status, false);
	frame.table.setCellText(e.username, 3, e.connecttime, false);
	frame.table.setCellText(e.username, 4, e.event, false);
      }

    // And refresh our table

    frame.table.refreshTable();
  }

  public void disconnect() throws RemoteException
  {
    aSession.logout();
  }

  /**
   *
   * Callback: The server can tell us to disconnect if the server is 
   * going down.
   *
   */

  public void forceDisconnect(String reason)
  {
    changeStatus("Disconnected: " + reason + "\n");
    server = null;
  }

  // ------------------------------------------------------------
  // convenience methods for our GASHAdminFrame
  // ------------------------------------------------------------

  void kill(String username) throws RemoteException
  {
    aSession.kill(username);
  }

  void shutdown() throws RemoteException
  {
    aSession.shutdown();
  }

  void dumpDB() throws RemoteException
  {
    aSession.dumpDB();
  }

}

/*------------------------------------------------------------------------------
                                                                           class
                                                                  GASHAdminFrame

------------------------------------------------------------------------------*/

class GASHAdminFrame extends Frame implements ActionListener, rowSelectCallback {

  static iAdmin admin = null;
  static boolean WeAreApplet = true;

  /* - */

  Server server = null;

  MenuBar mbar = null;
  Menu controlMenu = null;
  MenuItem quitMI = null;
  MenuItem dumpMI = null;
  MenuItem shutdownMI = null;

  PopupMenu popMenu = null;
  MenuItem killUserMI = null;

  Panel topPanel = null;

  YesNoDialog shutdownDialog = null;
  YesNoDialog dumpDialog = null;
  YesNoDialog killDialog = null;

  String killVictim = null;

  Label hostLabel = null;
  TextField hostField = null;
  TextArea statusArea = null;
  
  rowTable table = null;

  String url = "rmi://www.arlut.utexas.edu/ganymede.server";
  String headers[] = {"User", "System", "Status", "Connect Time", "Last Event"};
  int colWidths[] = {100,100,100,100,100};
  
  /* -- */

  public GASHAdminFrame(String title, boolean WeAreApplet)
  {
    super(title);

    this.WeAreApplet = WeAreApplet;

    mbar = new MenuBar();
    controlMenu = new Menu("Control", false);

    dumpMI = new MenuItem("Dump Database");
    dumpMI.addActionListener(this);

    shutdownMI = new MenuItem("Shutdown Ganymede");
    shutdownMI.addActionListener(this);

    quitMI = new MenuItem("Close Console");
    quitMI.addActionListener(this);

    controlMenu.add(dumpMI);
    controlMenu.add(shutdownMI);
    controlMenu.addSeparator();
    controlMenu.add(quitMI);

    mbar.add(controlMenu);

    setMenuBar(mbar);

    popMenu = new PopupMenu();

    killUserMI = new MenuItem("Kill User");
    popMenu.add(killUserMI);

    shutdownDialog = new YesNoDialog(this,
				     "Confirm Ganymede Server Shutdown", 
				     "Are you sure you want to shutdown the Ganymede server?", 
				     this);

    dumpDialog = new YesNoDialog(this,
				 "Ganymede Server Dump",
				 "Are you sure you want to schedule a full dump of the Ganymede database?", 
				 this);

    //    setBackground(Color.white);

    GridBagLayout topGBL = new GridBagLayout();
    GridBagConstraints topGBC = new GridBagConstraints();

    setLayout(topGBL);

    // set up our top panel, containing a labeled
    // text field showing the server we're connected
    // to.

    hostLabel = new Label("Ganymede Server Host:");

    hostField = new TextField(url, 40);
    hostField.setEditable(false);
    hostField.setBackground(SystemColor.text);
    hostField.setForeground(SystemColor.textText);

    topPanel = new Panel();

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();

    topPanel.setLayout(gbl);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(hostLabel, gbc);
    topPanel.add(hostLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbl.setConstraints(hostField, gbc);
    topPanel.add(hostField);

    Box topBox = new Box(topPanel, "Server");

    //topGBC.anchor = GridBagConstraints.NORTH;
    topGBC.fill = GridBagConstraints.HORIZONTAL;
    topGBC.gridwidth = GridBagConstraints.REMAINDER;
    topGBC.gridheight = 1;
    topGBC.weightx = 1.0;

    topGBC.weighty = 0;

    topGBL.setConstraints(topBox, topGBC);
    add(topBox);

    // set up our middle text area

    topGBC.fill = GridBagConstraints.BOTH;
    topGBC.weighty = 50;

    statusArea = new TextArea("Admin Console Testing\n", 6, 50);
    statusArea.setEditable(false);
    statusArea.setBackground(SystemColor.text);
    statusArea.setForeground(SystemColor.textText);

    Box statusBox = new Box(statusArea, "Server Log");

    topGBL.setConstraints(statusBox, topGBC);
    add(statusBox);

    // and our bottom user table

    table = new rowTable(colWidths, headers, this, popMenu);
    Box tableBox = new Box(table, "Users Connected");

    topGBL.setConstraints(tableBox, topGBC);
    add(tableBox);

    pack();
    show();

    /* RMI initialization stuff. We do this for our iClient object. */

    System.setSecurityManager(new RMISecurityManager());

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
    catch (java.rmi.UnknownHostException ex)
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

  // our button / dialog handler

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == quitMI)
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
      }
    else if (event.getSource() == dumpMI)
      {
	dumpDialog.show();
      }
    else if (event.getSource() == shutdownMI)
      {
	shutdownDialog.show();
      }
    else if (event.getSource() == shutdownDialog)
      {
	if (shutdownDialog.answeredYes())
	  {
	    System.err.println("Affirmative shutdown request");
	    try
	      {
		admin.shutdown();
	      }
	    catch (RemoteException e)
	      {
		admin.forceDisconnect("Couldn't talk to server");
	      }
	  }
      }
    else if (event.getSource() == dumpDialog)
      {
	if (dumpDialog.answeredYes())
	  {
	    System.err.println("Affirmative dump request");
	    try
	      {
		admin.dumpDB();
	      }
	    catch (RemoteException e)
	      {
		admin.forceDisconnect("Couldn't talk to server");
	      }
	  }
      }
    else if (event.getSource() == killDialog)
      {
	if (killDialog.answeredYes())
	  {
	    System.err.println("Affirmative kill request");
	    if (killVictim != null)
	      {
		try
		  {
		    admin.kill(killVictim);
		  }
		catch (RemoteException e)
		  {
		    admin.forceDisconnect("Couldn't talk to server");
		  }
	      }
	    killVictim = null;
	  }
	else
	  {
	    System.err.println("Negative dump request");
	    killVictim = null;
	  }
      }
  }

  // methods for the rowSelectCallback

  public void rowSelected(Object key)
  {
  }

  public void rowDoubleSelected(Object key)
  {
  }

  public void rowUnSelected(Object key, boolean finalState)
  {
  }

  public void rowMenuPerformed(Object key, ActionEvent e)
  {

    System.err.println("rowMenuPerformed");

    if (e.getSource() == killUserMI)
      {
	System.err.println("kill " + key + " selected");
      }

    killVictim = (String) key;

    killDialog = new YesNoDialog(this,
				 "Confirm User Kill",
				 "Are you sure you want to disconnect user " + key + "?",
				 this);

    killDialog.show();
  }

}

/*------------------------------------------------------------------------------
                                                                          class
                                                                       GASHAdmin

------------------------------------------------------------------------------*/
public class GASHAdmin extends Applet {

  static GASHAdmin applet = null;
  static GASHAdminFrame frame = null;

  /* -- */

  // Our primary constructor.  This will always be called, either
  // from main(), below, or by the environment building our applet.

  public GASHAdmin() 
  {

  }
  
  public void init() 
  {
    frame = new GASHAdminFrame("Ganymede Admin Console", true);
  }

  public static void main(String[] argv)
  {
    frame = new GASHAdminFrame("Ganymede Admin Console", false);
  }
}
 

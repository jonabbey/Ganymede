/*

   GASHAdmin.java

   Admin console for the Java RMI Gash Server

   Created: 28 May 1996
   Version: $Revision: 1.19 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;
import java.rmi.server.*;
//import java.awt.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.PopupMenu;
import java.awt.SystemColor;
import java.awt.MenuItem;
import java.awt.event.*;
import java.applet.*;
import java.util.*;

import com.sun.java.swing.*;
import com.sun.java.swing.border.*;

//import gjt.Box;

import arlut.csd.JTable.*;
import arlut.csd.JDialog.*;

import jdj.PackageResources;

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
  private String adminName = null;
  private String adminPass = null;

  JFrame schemaFrame;

  /* -- */

  public iAdmin(GASHAdminFrame frame, Server server, String name, String pass) throws RemoteException
  {
    // UnicastRemoteServer can throw RemoteException 

    this.frame = frame;
    this.server = server;
    this.adminName = name;
    this.adminPass = pass;

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

  public String getName()
  {
    return adminName;
  }

  public String getPassword()
  {
    return adminPass;
  }

  public void setServerStart(Date date)
  {
    frame.startField.setText(date.toString());
  }

  public void setLastDumpTime(Date date)
  {
    if (date == null)
      {
	frame.dumpField.setText("no dump since server start");
      }
    else
      {
	frame.dumpField.setText(date.toString());
      }
  }

  public void setTransactionsInJournal(int trans)
  {
    frame.journalField.setText("" + trans);
  }

  public void setObjectsCheckedOut(int objs)
  {
    frame.checkedOutField.setText("" + objs);
  }

  public void setLocksHeld(int locks)
  {
    frame.locksField.setText("" + locks);
  }

  public void changeStatus(String status)
  {
    frame.statusArea.append(new Date() + " " + status + "\n");
    frame.statusArea.setCaretPosition(frame.statusArea.getText().length());
  }

  public void changeAdmins(String adminStatus)
  {
    frame.adminField.setText(adminStatus);
  }

  public void changeState(String state)
  {
    frame.stateField.setText(state);
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

  // ------------------------------------------------------------
  // convenience methods for our GASHAdminFrame
  // ------------------------------------------------------------

  void killAll() throws RemoteException
  {
    aSession.killAll();
  }

  void shutdown() throws RemoteException
  {
    if (!adminName.equals("supergash"))
      {
	return;
      }

    aSession.shutdown();
  }

  void dumpDB() throws RemoteException
  {
    if (!adminName.equals("supergash"))
      {
	return;
      }

    aSession.dumpDB();
  }

  void dumpSchema() throws RemoteException
  {
    if (!adminName.equals("supergash"))
      {
	return;
      }

    aSession.dumpSchema();
  }

  void reloadClasses() throws RemoteException
  {
    if (!adminName.equals("supergash"))
      {
	return;
      }

    aSession.reloadCustomClasses();
  }

  void runInvidTest() throws RemoteException
  {
    if (!adminName.equals("supergash"))
      {
	return;
      }

    aSession.runInvidTest();
  }

  void pullSchema() throws RemoteException
  {
    SchemaEdit editor = null;

    System.err.println("Trying to get SchemaEdit handle");

    try
      {
	editor = aSession.editSchema();
      }
    catch (RemoteException ex)
      {
	System.err.println("editSchema() exception: " + ex);
      }

    if (editor == null)
      {
	System.err.println("null editor handle");
      }
    else
      {
	System.err.println("Got SchemaEdit handle");
	
	schemaFrame = new GASHSchema("Schema Editor", editor);
      }

    //    try
    //      {
    //	editor.release();
    //      }
    //    catch (RemoteException ex)
    //      {
    //	System.err.println("release() exception: " + ex);
    //      }

    //    System.err.println("Released SchemaEdit handle");
  }

}

/*------------------------------------------------------------------------------
                                                                           class
                                                                  GASHAdminFrame

------------------------------------------------------------------------------*/

class GASHAdminFrame extends JFrame implements ActionListener, rowSelectCallback {

  static iAdmin admin = null;
  static boolean WeAreApplet = true;

  /* - */

  Image question = null;

  Server server = null;

  JMenuBar mbar = null;
  JMenu controlMenu = null;
  JMenuItem quitMI = null;
  JMenuItem dumpMI = null;
  JMenuItem dumpSchemaMI = null;
  JMenuItem reloadClassesMI = null;
  JMenuItem schemaMI = null;
  JMenuItem shutdownMI = null;
  JMenuItem runInvidTestMI = null;
  JMenuItem killAllMI = null;

  PopupMenu popMenu = null;
  MenuItem killUserMI = null;

  JPanel topPanel = null;

  StringDialog
    shutdownDialog = null,
    dumpDialog = null,
    killDialog = null,
    invidTestDialog = null;

  String killVictim = null;

  JLabel hostLabel = null;
  JTextField hostField = null;

  JLabel adminLabel = null;
  JTextField adminField = null;

  JLabel stateLabel = null;
  JTextField stateField = null;

  JLabel startLabel = null;
  JTextField startField = null;

  JLabel dumpLabel = null;
  JTextField dumpField = null;

  JLabel journalLabel = null;
  JTextField journalField = null;

  JLabel checkedOutLabel = null;
  JTextField checkedOutField = null;

  JLabel locksLabel = null;
  JTextField locksField = null;

  JTextArea statusArea = null;
  
  rowTable table = null;

  String url = "rmi://www.arlut.utexas.edu/ganymede.server";
  String headers[] = {"User", "System", "Status", "Connect Time", "Last Event"};
  int colWidths[] = {100,100,100,100,100};
  
  /* -- */

  public GASHAdminFrame(String title, boolean WeAreApplet)
  {
    super(title);

    this.WeAreApplet = WeAreApplet;

    mbar = new JMenuBar();
    controlMenu = new JMenu("Control", false);

    dumpMI = new JMenuItem("Dump Database");
    dumpMI.addActionListener(this);

    dumpSchemaMI = new JMenuItem("Dump Schema");
    dumpSchemaMI.addActionListener(this);

    reloadClassesMI = new JMenuItem("Reload Custom Classes");
    reloadClassesMI.addActionListener(this);

    shutdownMI = new JMenuItem("Shutdown Ganymede");
    shutdownMI.addActionListener(this);

    killAllMI = new JMenuItem("Kill Off All Users");
    killAllMI.addActionListener(this);

    schemaMI = new JMenuItem("Edit Schema");
    schemaMI.addActionListener(this);

    runInvidTestMI = new JMenuItem("Run Invid Test");
    runInvidTestMI.addActionListener(this);

    quitMI = new JMenuItem("Close Console");
    quitMI.addActionListener(this);

    controlMenu.add(shutdownMI);
    controlMenu.add(killAllMI);
    controlMenu.add(schemaMI);
    controlMenu.add(reloadClassesMI);
    controlMenu.add(runInvidTestMI);
    controlMenu.addSeparator();
    controlMenu.add(dumpMI);
    controlMenu.add(dumpSchemaMI);
    controlMenu.addSeparator();
    controlMenu.add(new arlut.csd.JDataComponent.LAFMenu(this));
    controlMenu.add(quitMI);

    mbar.add(controlMenu);

    setJMenuBar(mbar);

    popMenu = new PopupMenu();

    killUserMI = new MenuItem("Kill User");
    popMenu.add(killUserMI);

    question = PackageResources.getImageResource(this, "question.gif", getClass());

    shutdownDialog = new StringDialog(this,
				      "Confirm Ganymede Server Shutdown", 
				      "Are you sure you want to \nshutdown the Ganymede server?", 
				      "Yes", "No", question);

    dumpDialog = new StringDialog(this,
				  "Ganymede Server Dump",
				  "Are you sure you want to schedule \na full dump of the Ganymede database?", 
				  "Yes", "No", question);

    invidTestDialog = new StringDialog(this,
				       "Invid Test",
				       "Are you you want to trigger a full invid sweep?  It'll take forever.",
				       "Yes", "No", question);

    //    setBackground(Color.white);

    java.awt.GridBagLayout topGBL = new java.awt.GridBagLayout();
    java.awt.GridBagConstraints topGBC = new java.awt.GridBagConstraints();

    getContentPane().setLayout(topGBL);

    // set up our top panel, containing a labeled
    // text field showing the server we're connected
    // to.

    hostLabel = new JLabel("Ganymede Server Host:");

    hostField = new JTextField(url, 40);
    hostField.setEditable(false);
    //hostField.setBackground(SystemColor.text);
    //hostField.setForeground(SystemColor.textText);

    topPanel = new JPanel();

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();

    topPanel.setLayout(gbl);

    gbc.insets = new java.awt.Insets(2,1,2,1);

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

    //

    adminLabel = new JLabel("Admin consoles connected to server:");

    adminField = new JTextField("", 40);
    adminField.setEditable(false);
    //adminField.setBackground(SystemColor.text);
    //adminField.setForeground(SystemColor.textText);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(adminLabel, gbc);
    topPanel.add(adminLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbl.setConstraints(adminField, gbc);
    topPanel.add(adminField);

    stateLabel = new JLabel("Server State:");

    stateField = new JTextField("", 40);
    stateField.setEditable(false);
    //stateField.setBackground(SystemColor.text);
    //stateField.setForeground(SystemColor.textText);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(stateLabel, gbc);
    topPanel.add(stateLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbl.setConstraints(stateField, gbc);
    topPanel.add(stateField);

    startLabel = new JLabel("Server Start Time:");

    startField = new JTextField("", 40);
    startField.setEditable(false);
    //startField.setBackground(SystemColor.text);
    //startField.setForeground(SystemColor.textText);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(startLabel, gbc);
    topPanel.add(startLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbl.setConstraints(startField, gbc);
    topPanel.add(startField);

    dumpLabel = new JLabel("Last Dump Time:");

    dumpField = new JTextField("", 40);
    dumpField.setEditable(false);
    //dumpField.setBackground(SystemColor.text);
    //dumpField.setForeground(SystemColor.textText);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(dumpLabel, gbc);
    topPanel.add(dumpLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbl.setConstraints(dumpField, gbc);
    topPanel.add(dumpField);

    journalLabel = new JLabel("Transactions in Journal:");

    journalField = new JTextField("", 40);
    journalField.setEditable(false);
    //journalField.setBackground(SystemColor.text);
    //journalField.setForeground(SystemColor.textText);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(journalLabel, gbc);
    topPanel.add(journalLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbl.setConstraints(journalField, gbc);
    topPanel.add(journalField);

    checkedOutLabel = new JLabel("Objects Checked Out:");

    checkedOutField = new JTextField("", 40);
    checkedOutField.setEditable(false);
    //checkedOutField.setBackground(SystemColor.text);
    //checkedOutField.setForeground(SystemColor.textText);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(checkedOutLabel, gbc);
    topPanel.add(checkedOutLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbl.setConstraints(checkedOutField, gbc);
    topPanel.add(checkedOutField);

    locksLabel = new JLabel("Locks held:");

    locksField = new JTextField("", 40);
    locksField.setEditable(false);
    //locksField.setBackground(SystemColor.text);
    //locksField.setForeground(SystemColor.textText);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(locksLabel, gbc);
    topPanel.add(locksLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbl.setConstraints(locksField, gbc);
    topPanel.add(locksField);

    topGBC.fill = GridBagConstraints.HORIZONTAL;
    topGBC.gridwidth = GridBagConstraints.REMAINDER;
    topGBC.gridheight = 1;
    topGBC.weightx = 1.0;
    topGBC.weighty = 0;
    
    JPanel topBox = new JPanel(new BorderLayout());
    topBox.add("Center",topPanel);
    topBox.setBorder(new TitledBorder("Ganymede Server"));
    topGBL.setConstraints(topBox, topGBC);
    getContentPane().add(topBox);

    // set up our middle text area

    topGBC.fill = GridBagConstraints.BOTH;
    topGBC.weighty = 50;

    statusArea = new JTextArea("Admin Console Testing\n", 6, 50);
    statusArea.setEditable(false);
    JScrollPane statusAreaPane = new JScrollPane(statusArea);
    //statusArea.setBackground(SystemColor.text);
    //statusArea.setForeground(SystemColor.textText);

    JPanel statusBox = new JPanel(new java.awt.BorderLayout());
    statusBox.add("Center", statusAreaPane);
    statusBox.setBorder(new TitledBorder("Server Log"));

    topGBL.setConstraints(statusBox, topGBC);
    getContentPane().add(statusBox);

    // and our bottom user table

    table = new rowTable(colWidths, headers, this, popMenu);
    JPanel tableBox = new JPanel(new BorderLayout());
    tableBox.add("Center", table);
    tableBox.setBorder(new TitledBorder("Users Connected"));

    topGBL.setConstraints(tableBox, topGBC);
    getContentPane().add(tableBox);

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

    DialogRsrc loginResrc;
    StringDialog loginDialog;

    loginResrc = new DialogRsrc(this, "Admin Login", "Enter your administrator account name & password");
    loginResrc.addString("Account:");
    loginResrc.addPassword("Password:");
    
    loginDialog = new StringDialog(loginResrc);
    Hashtable results = loginDialog.DialogShow();

    if (results == null)
      {
	System.out.println("Good bye.");
	quitMI.doClick();
	return;
      }

    if (!results.get("Account:").equals("supergash"))
      {
	controlMenu.remove(dumpMI);
	controlMenu.remove(dumpSchemaMI);
	controlMenu.remove(shutdownMI);
	controlMenu.remove(reloadClassesMI);
	controlMenu.remove(runInvidTestMI);
	controlMenu.remove(schemaMI);
	controlMenu.remove(killAllMI);
      }

    try
      {
	admin = new iAdmin(this, server, 
			   (String) results.get("Account:"),
			   (String) results.get("Password:"));
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
	if (dumpDialog.DialogShow() != null)
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
    else if (event.getSource() == dumpSchemaMI)
      {
	try
	  {
	    admin.dumpSchema();
	  }
	catch (RemoteException e)
	  {
	    admin.forceDisconnect("Couldn't talk to server");
	  }
      }
    else if (event.getSource() == reloadClassesMI)
      {
	try
	  {
	    admin.reloadClasses();
	  }
	catch (RemoteException e)
	  {
	    admin.forceDisconnect("Couldn't talk to server");
	  }
      }
    else if (event.getSource() == runInvidTestMI)
      {
	if (invidTestDialog.DialogShow() != null)
	  {
	    System.err.println("Affirmative invid test request");

	    try
	      {
		admin.runInvidTest();
	      }
	    catch (RemoteException ex)
	      {
		admin.forceDisconnect("Couldn't talk to server" + ex);
	      }
	  }
      }
    else if (event.getSource() == shutdownMI)
      {
	if (shutdownDialog.DialogShow() != null)
	  {
	    System.err.println("Affirmative shutdown request");
	    try
	      {
		admin.shutdown();
	      }
	    catch (RemoteException ex)
	      {
		admin.forceDisconnect("Couldn't talk to server" + ex);
	      }
	  }
      }
    else if (event.getSource() == killAllMI)
      {
	DialogRsrc killAllDLGR;
	StringDialog killAllDLG;

	killAllDLGR = new DialogRsrc(this,
				     "Are you sure you want to log out all users?", 
				     "Are you sure you want to log out all users?", 
				     "Hell Yes", "No", question);
    
	killAllDLG = new StringDialog(killAllDLGR);
	Hashtable results = killAllDLG.DialogShow();
	
	if (results == null)
	  {
	    return;
	  }

	try
	  {
	    admin.killAll();
	  }
	catch (RemoteException ex)
	  {
	    admin.forceDisconnect("Couldn't talk to server" + ex);
	  }
      }
    else if (event.getSource() == schemaMI)
      {
	try
	  {
	    admin.pullSchema();
	  }
	catch (RemoteException e)
	  {
	    admin.forceDisconnect("Couldn't talk to server");
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

    killDialog = new StringDialog(this,
				  "Confirm User Kill",
				  "Are you sure you want to disconnect user " + key + "?",
				  "Yes", "No", question);

    if (killDialog.DialogShow() != null)
      {
	System.err.println("Affirmative kill request");

	if (killVictim != null)
	  {
	    try
	      {
		admin.kill(killVictim);
	      }
	    catch (RemoteException ex)
	      {
		admin.forceDisconnect("Couldn't talk to server" + ex);
	      }
	  }
	killVictim = null;
      }
    else
      {
	System.err.println("Negative kill request");
	killVictim = null;
      }
  }

}

/*------------------------------------------------------------------------------
                                                                          class
                                                                       GASHAdmin

------------------------------------------------------------------------------*/
public class GASHAdmin extends JApplet {

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
 

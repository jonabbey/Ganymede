/*

   GASHAdmin.java

   Admin console for the Java RMI Gash Server

   Created: 28 May 1996
   Version: $Revision: 1.65 $
   Last Mod Date: $Date: 2000/06/26 21:37:02 $
   Release: $Name:  $

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/

package arlut.csd.ganymede;

import javax.swing.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;

import java.rmi.*;
import java.rmi.server.*;

import java.io.*;
import java.util.*;

import arlut.csd.JTable.*;
import arlut.csd.JDialog.*;
import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       GASHAdmin

------------------------------------------------------------------------------*/

/**
 * <P>Ganymede GUI admin console.</P>
 *
 * <P>GASHAdmin is a dual-mode (applet/application) GUI app for monitoring and
 * controlling the Ganymede server.  In addition to monitoring users and tasks
 * on the Ganymede server, the admin console includes a full-functioned
 * {@link arlut.csd.ganymede.GASHSchema schema editor}.</P>
 *
 * <P>GASHAdmin connects to a running
 * {@link arlut.csd.ganymede.GanymedeServer GanymedeServer} using the 
 * {@link arlut.csd.ganymede.GanymedeServer#admin(arlut.csd.ganymede.Admin) admin()}
 * method.  In order to get logged into the server, GASHAdmin 
 * itself publishes an {@link arlut.csd.ganymede.iAdmin iAdmin}
 * object via RMI implementing the {@link arlut.csd.ganymede.Admin Admin}
 * interface so that the server can dynamically update us as things happen on
 * the server.</P>
 */

public class GASHAdmin extends JApplet implements Runnable, ActionListener {

  static final boolean debug = false;

  /**
   * We assume that we're only ever going to have one console running in
   * any given JVM, we keep track of it here as a convenience.
   */

  static GASHAdmin applet = null;

  /**
   * We keep track of the single admin window that gets opened up here.
   */

  static GASHAdminFrame frame = null;

  /**
   * The iAdmin object is the remote reference to the Ganymede server
   * used by the admin console.
   */

  static iAdmin admin = null;

  /**
   * If true, we are running as an applet and are limited by the Java sandbox.
   * A few features of the client will be disabled if this is true (saving query
   * reports to disk, etc.).
   */

  static boolean WeAreApplet = true;

  static String serverhost = null;
  static int registryPortProperty = 1099;
  static String url = null;

  protected boolean connected = false;

  static Server server = null;
  private static Container appletContentPane = null;
  static Image admin_logo = null;

  JTextField username = null;
  JPasswordField password = null;
  JButton quitButton = new JButton("Quit");
  JButton loginButton= null;

  Image errorImage = null;

  /* -- */

  // Our primary constructor.  This will always be called, either
  // from main(), below, or by the environment building our applet.

  public GASHAdmin() 
  {
    admin_logo = PackageResources.getImageResource(this, "admin.gif", getClass());
    GASHAdmin.applet = this;
  }

  public static void main(String[] argv)
  {
    WeAreApplet = false;

    if (argv.length < 1)
      {
	System.err.println("Error, no properties file specified.");
	return;
      }
    else
      {
	if (!loadProperties(argv[0]))
	  {
	    System.err.println("Error, couldn't successfully load properties from file " + argv[0]);
	    return;
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("Successfully loaded properties from file " + argv[0]);
	      }
	  }
      }

    if (argv.length > 1)
      {
	GASHAdminFrame.debugFilename = argv[1];
      }

    new GASHAdmin();		// this constructor sets static admin ref

    JFrame loginFrame = new JFrame("Admin console login");

    appletContentPane = loginFrame.getContentPane();

    appletContentPane.setLayout(new BorderLayout());
    appletContentPane.add("Center", applet);

    loginFrame.pack();
    loginFrame.setSize(265,380);
    loginFrame.show();

    applet.init();
    applet.getContentPane().getLayout().layoutContainer(applet);
  }
  
  public void init()
  {
    if (WeAreApplet)
      {
	loadParameters();
      }

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add("Center", createLoginPanel());

    /* Spawn a thread to try to get a reference to the server */

    new Thread(this).start();
  }

  public void stop()
  {
    if (debug)
      {
	System.err.println("applet stop()");
      }

    if (admin != null)
      {
	try
	  {
	    admin.disconnect();
	  }
	catch (RemoteException ex)
	  {
	  }
      }
  }

  public void destroy()
  {
    if (debug)
      {
	System.err.println("applet destroy()");
      }

    if (admin != null)
      {
	try
	  {
	    admin.disconnect();
	  }
	catch (RemoteException ex)
	  {
	  }
      }
  }

  public JPanel createLoginPanel()
  {
    JPanel panel = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();

    /* -- */

    panel.setLayout(gbl);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridheight = 1;
    gbc.insets = new Insets(1,1,0,0);

    if (admin_logo != null)
      {
	JLabel image = new JLabel(new ImageIcon(admin_logo));
	image.setOpaque(true);
	image.setBackground(Color.black);

	gbc.fill = GridBagConstraints.BOTH;
	gbc.gridx = 0;
	gbc.gridy = 0;
	gbc.gridwidth = GridBagConstraints.REMAINDER;
	gbc.weighty = 1.0;
	gbc.weightx = 1.0;
	gbl.setConstraints(image, gbc);
	panel.add(image);
      }

    JLabel label = new JLabel("Ganymede Admin Console");
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0.0;
    gbc.gridy = 1;
    gbl.setConstraints(label, gbc);
    panel.add(label);

    gbc.ipady = 4;

    JLabel ul = new JLabel("Username:");
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridy = 2;
    gbc.gridwidth = 1;
    gbc.weightx = 0.0;
    gbl.setConstraints(ul, gbc);
    panel.add(ul);
    
    username = new JTextField(20);
    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx = 1.0;
    gbl.setConstraints(username, gbc);
    username.addActionListener(this);
    panel.add(username);

    JLabel pl = new JLabel("Password:");
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 1;
    gbc.weightx = 0.0;
    gbl.setConstraints(pl, gbc);
    panel.add(pl);

    password = new JPasswordField(20);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx = 1;
    gbc.weightx = 1.0;

    gbl.setConstraints(password, gbc);
    panel.add(password);
    
    gbc.gridx = 0;
    gbc.gridy = 4;

    gbc.gridwidth = 2;
    JPanel buttonPanel = new JPanel(new BorderLayout());
    gbl.setConstraints(buttonPanel, gbc);
    panel.add(buttonPanel);

    loginButton = new JButton("Connecting...");
    loginButton.setOpaque(true);

    buttonPanel.add(loginButton, "Center");

    if (!WeAreApplet)
      {
	quitButton.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e)
	    {
	      System.exit(0);
	    }});

	buttonPanel.add(quitButton, "East");
      }
   
    return panel;
  }

  /**
   * This will be executed in the thread that tries to connect to the
   * server.  The thread will terminate after a connection to the
   * server has been made.
   */

  public void run() 
  {
    if (connected)
      {
	return;
      }

    int try_number = 0;
    
    while (!connected)
      {
	if (try_number++ > 5)
	  {
	    System.err.println("I've tried five times to connect, but I can't do it.  Maybe the server is down?");

	    break;
	  }

	try
	  {
	    Remote obj = Naming.lookup(GASHAdmin.url);

	    if (obj instanceof Server)
	      {
		server = (Server) obj;
	      }

	    connected = true;
	  }
	catch (NotBoundException ex)
	  {
	    System.err.println("RMI: Couldn't bind to server object\n" + ex );
	  }
	catch (java.rmi.UnknownHostException ex)
	  {
	    System.err.println("RMI: Couldn't find server\n" + GASHAdmin.url );
	  }
	catch (RemoteException ex)
	  {
	    ex.printStackTrace();
	    System.err.println("RMI: RemoteException during lookup.\n" + ex);
	  }
	catch (java.net.MalformedURLException ex)
	  {
	    System.err.println("RMI: Malformed URL " + GASHAdmin.url );
	  }
	
	try 
	  {
	    // Wait for 1 sec before retrying to connect to server
	    Thread.sleep(1000);
	  }
	catch (InterruptedException e) 
	  {
	  }
      }

    if (connected)
      {
	loginButton.setText("Login to server");
	loginButton.addActionListener(this);
	username.setEnabled(true);
	password.setEnabled(true);
	password.addActionListener(this);
	
	invalidate();
	validate();
      }
  }

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == username)
      {
	password.requestFocus();
      }
    else if (e.getSource() == password)
      {
	loginButton.doClick();
      }
    else if (e.getSource() == loginButton)
      {
	if (!connected)
	  {
	    return;
	  }

	try
	  {
	    admin = new iAdmin(server,
			       username.getText(),
			       new String(password.getPassword()));
	  }
	catch (RemoteException rx)
	  {
	    new StringDialog(new JFrame(),
			     "Login error",
			     "Couldn't log in to the Ganymede server... perhaps it is down?\n\nException: " + 
			     rx.getMessage(),
			     "OK", null,
			     getErrorImage()).DialogShow();
	  }
	catch (IllegalArgumentException ex)
	  {
	    new StringDialog(new JFrame(),
			     "Couldn't log in to the Ganymede Server",
			     "Couldn't log in to the Ganymede server.\n\nBad username/password or " +
			     "insufficient permissions to run the Ganymede admin console.",
			     "OK", null, 
			     getErrorImage()).DialogShow();
	    
	    password.setText("");
	    return;
	  }

	username.setText("");
	password.setText("");
	quitButton.setEnabled(false);
	loginButton.setEnabled(false);

	frame = new GASHAdminFrame("Ganymede Admin Console", applet);
	
	// Now that the frame is completely initialized, tie the iAdmin object
	// to the frame, and vice-versa.
	
	frame.admin = admin;
	admin.setFrame(frame);
	
	try
	  {
	    admin.refreshMe();
	  }
	catch (RemoteException rx)
	  {
	    System.err.println("Problem trying to refresh: " + rx);
	  }
      }
  }

  /**
   * <P>Private method to load the Ganymede console's parameters
   * from a file.  Used when GASHAdmin is run from the command line..
   * {@link arlut.csd.ganymede.GASHAdmin#loadParameters() loadParameters()}
   * is for use in an applet context.</P>
   */ 

  private static boolean loadProperties(String filename)
  {
    Properties props = new Properties();
    boolean success = true;

    /* -- */

    try
      {
	props.load(new BufferedInputStream(new FileInputStream(filename)));
      }
    catch (IOException ex)
      {
	return false;
      }

    serverhost = props.getProperty("ganymede.serverhost");

    // get the registry port number

    String registryPort = props.getProperty("ganymede.registryPort");

    if (registryPort != null)
      {
	try
	  {
	    registryPortProperty = java.lang.Integer.parseInt(registryPort);
	  }
	catch (NumberFormatException ex)
	  {
	    System.err.println("Couldn't get a valid registry port number from ganymede properties file: " + 
			       registryPort);
	  }
      }

    if (serverhost == null)
      {
	System.err.println("Couldn't get the server host property");
	success = false;
      }
    else
      {
	url = "rmi://" + serverhost + ":" + registryPortProperty + "/ganymede.server";
      }

    return success;
  }

  /**
   * <P>Private method to load the Ganymede console's parameters
   * from an applet's HTML parameters.  Used when GASHAdmin is run as an applet..
   * {@link arlut.csd.ganymede.GASHAdmin#loadProperties(java.lang.String) loadProperties()}
   * is for use in an application context.</P>
   */ 

  private void loadParameters()
  {
    serverhost = getParameter("ganymede.serverhost");

    String registryPort = getParameter("ganymede.registryPort");
    
    if (registryPort != null)
      {
	try
	  {
	    registryPortProperty = java.lang.Integer.parseInt(registryPort);
	  }
	catch (NumberFormatException ex)
	  {
	    System.err.println("Couldn't get a valid registry port number from ganymede applet parameters: " + 
			       registryPort);
	  }
      }

    if (serverhost == null)
      {
	System.err.println("Couldn't get the server host property");
	throw new RuntimeException("Couldn't get the server host property");
      }
    else
      {
	url = "rmi://" + serverhost + ":" + registryPortProperty + "/ganymede.server";
      }
  }

  /**
   * <p>Loads and returns the error Image for use in client dialogs.</p>
   * 
   * <p>Once the image is loaded, it is cached for future calls to 
   * getErrorImage().</p>
   */

  public final Image getErrorImage()
  {
    if (errorImage == null)
      {
	errorImage = PackageResources.getImageResource(this, "error.gif", getClass());
      }
    
    return errorImage;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                  GASHAdminFrame

------------------------------------------------------------------------------*/

/**
 *
 * GASHAdminFrame is the main class for the Ganymede admin console.  The
 * GASHAdminFrame constructor is the first piece of common code that is executed
 * both in an applet context and as a stand-alone app.
 *
 */

class GASHAdminFrame extends JFrame implements ActionListener, rowSelectCallback {

  static iAdmin admin = null;
  static final boolean debug = false;
  static String debugFilename = null;

  // ---

  Image question = null;

  JMenuBar mbar = null;
  JMenu controlMenu = null;
  JMenuItem quitMI = null;
  JMenuItem dumpMI = null;
  JMenuItem dumpSchemaMI = null;
  JMenuItem reloadClassesMI = null;
  JMenuItem schemaMI = null;
  JMenuItem shutdownMI = null;
  JMenuItem runInvidTestMI = null;
  JMenuItem runInvidSweepMI = null;
  JMenuItem killAllMI = null;

  JPopupMenu popMenu = null;
  JMenuItem killUserMI = null;

  JPanel topPanel = null;

  JTabbedPane tabPane = null;

  StringDialog
    shutdownDialog = null,
    dumpDialog = null,
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

  JButton clearLogButton;
  JTextArea statusArea = null;

  // resources for the users connected table
  
  rowTable table = null;

  String headers[] = {"User", "System", "Status", "Connect Time", "Last Event", "Objects Checked Out"};
  int colWidths[] = {100,100,100,100,100,100};

  // resources for the task monitor table

  rowTable taskTable = null;

  String taskHeaders[] = {"Task", "Status", "Last Run", "Next Run", "Interval"};
  int taskColWidths[] = {100,100,100,100,100};

  JPopupMenu taskPopMenu = null;
  JMenuItem runNowMI = null;
  JMenuItem stopTaskMI = null;
  JMenuItem disableTaskMI = null;
  JMenuItem enableTaskMI = null;
  
  GASHAdmin adminPanel;

  /* -- */

  /**
   *
   * Constructor
   *
   */

  public GASHAdminFrame(String title, GASHAdmin adminPanel)
  {
    super(title);

    this.adminPanel = adminPanel;

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

    runInvidSweepMI = new JMenuItem("Run Invid Sweep");
    runInvidSweepMI.addActionListener(this);

    quitMI = new JMenuItem("Close Console");
    quitMI.addActionListener(this);

    controlMenu.add(shutdownMI);
    controlMenu.add(killAllMI);
    controlMenu.add(schemaMI);
    controlMenu.add(reloadClassesMI);
    controlMenu.add(runInvidTestMI);
    controlMenu.add(runInvidSweepMI);
    controlMenu.addSeparator();
    controlMenu.add(dumpMI);
    controlMenu.add(dumpSchemaMI);
    controlMenu.addSeparator();
    controlMenu.add(new arlut.csd.JDataComponent.LAFMenu(this));
    controlMenu.add(quitMI);

    mbar.add(controlMenu);

    setJMenuBar(mbar);

    question = PackageResources.getImageResource(this, "question.gif", getClass());

    java.awt.GridBagLayout topGBL = new java.awt.GridBagLayout();
    java.awt.GridBagConstraints topGBC = new java.awt.GridBagConstraints();

    getContentPane().setLayout(topGBL);

    // set up our top panel, containing a labeled
    // text field showing the server we're connected
    // to.

    hostLabel = new JLabel("Ganymede Server Host:");

    hostField = new JTextField(GASHAdmin.url, 40);
    hostField.setEditable(false);

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
    topGBC.gridx = 0;
    topGBC.weightx = 1.0;
    
    JPanel topBox = new JPanel(new BorderLayout());
    topBox.add("Center",topPanel);
    topBox.setBorder(new TitledBorder("Ganymede Server"));
    topGBL.setConstraints(topBox, topGBC);
    getContentPane().add(topBox);

    // set up our middle text area

    topGBC.fill = GridBagConstraints.BOTH;
    topGBC.weighty = 1.0;

    statusArea = new JTextArea("Ganymede Admin Console\n", 6, 50);
    statusArea.setEditable(false);
    JScrollPane statusAreaPane = new JScrollPane(statusArea);

    clearLogButton = new JButton("Clear Log");
    clearLogButton.addActionListener(this);

    JPanel clearPanel = new JPanel(new java.awt.BorderLayout());
    clearPanel.add("East", clearLogButton);
    
    JPanel statusBox = new JPanel(new java.awt.BorderLayout());
    statusBox.add("Center", statusAreaPane);
    statusBox.add("South", clearPanel);
    statusBox.setBorder(new TitledBorder("Server Log"));

    topGBL.setConstraints(statusBox, topGBC);
    getContentPane().add(statusBox);

    // bottom area, a tab pane with tables for things

    // create our user table

    popMenu = new JPopupMenu();

    killUserMI = new JMenuItem("Kill User");
    popMenu.add(killUserMI);

    table = new rowTable(colWidths, headers, this, false, popMenu);
    JPanel tableBox = new JPanel(new BorderLayout());
    tableBox.add("Center", table);
    tableBox.setBorder(new TitledBorder("Users Connected"));

    // create background task monitor

    taskPopMenu = new JPopupMenu();

    runNowMI = new JMenuItem("Run Task Now");
    stopTaskMI = new JMenuItem("Stop Running Task");
    disableTaskMI = new JMenuItem("Disable Task");
    enableTaskMI = new JMenuItem("Enable Task");

    taskPopMenu.add(runNowMI);
    taskPopMenu.add(stopTaskMI);
    taskPopMenu.add(disableTaskMI);
    taskPopMenu.add(enableTaskMI);

    taskTable = new rowTable(taskColWidths, taskHeaders, this, false, taskPopMenu);
    taskTable.setHeadBackColor(Color.red, false);

    JPanel taskBox = new JPanel(new java.awt.BorderLayout());
    taskBox.add("Center", taskTable);
    taskBox.setBorder(new TitledBorder("Task Monitor"));

    // and put them into our tab pane

    tabPane = new JTabbedPane();
    tabPane.addTab("Users Connected", tableBox);
    tabPane.addTab("Task Monitor", taskBox);

    // and put the tab pane into our frame with the
    // same constraints that the text area had

    topGBL.setConstraints(tabPane, topGBC);
    getContentPane().add(tabPane);

    pack();
    show();

    // along with processWindowEvent(), this method allows us
    // to properly handle window system close events.

    enableEvents(AWTEvent.WINDOW_EVENT_MASK);

    if (debugFilename != null)
      {
	try
	  {
	    RemoteServer.setLog(new FileOutputStream(debugFilename));
	  }
	catch (IOException ex)
	  {
	    System.err.println("couldn't open RMI debug log: " + ex);
	  }
      }
  }

  /**
   * local convenience method to handle disconnecting the admin console
   */

  void disconnect()
  {
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
	adminPanel.quitButton.setEnabled(true);
	adminPanel.loginButton.setEnabled(true);
	setVisible(false);

	// This shouldn't kill everything off, but it does for now.  Need to fix this later.
	    
	if (!GASHAdmin.WeAreApplet)
	  {
	    System.exit(0);
	  }
      }
  }

  // our button / dialog handler

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == quitMI)
      {
	if (debug)
	  {
	    System.err.println("Quitting");
	  }

	this.disconnect();
      }
    else if (event.getSource() == dumpMI)
      {
	if (dumpDialog == null)
	  {
	    dumpDialog = new StringDialog(this,
					  "Ganymede Server Dump",
					  "Are you sure you want to schedule \na full dump of the Ganymede database?", 
					  "Yes", "No", question);
	  }

	if (dumpDialog.DialogShow() != null)
	  {
	    if (debug)
	      {
		System.err.println("Affirmative dump request");
	      }

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
	if (invidTestDialog == null)
	  {
	    invidTestDialog = new StringDialog(this,
					       "Invid Test",
					       "Are you sure you want to trigger a full invid consistency test?\n"+
					       "It may take awhile.",
					       "Yes", "No", question);
	  }

	if (invidTestDialog.DialogShow() != null)
	  {
	    if (debug)
	      {
		System.err.println("Affirmative invid test request");
	      }

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
    else if (event.getSource() == runInvidSweepMI)
      {
	if (invidTestDialog == null)
	  {
	    invidTestDialog = new StringDialog(this,
					       "Invid Sweep",
					       "Are you sure you want to trigger a full invid sweep?\n"+
					       "It may take awhile.",
					       "Yes", "No", question);
	  }

	if (invidTestDialog.DialogShow() != null)
	  {
	    if (debug)
	      {
		System.err.println("Affirmative invid sweep request");
	      }

	    try
	      {
		admin.runInvidSweep();
	      }
	    catch (RemoteException ex)
	      {
		admin.forceDisconnect("Couldn't talk to server" + ex);
	      }
	  }
      }
    else if (event.getSource() == shutdownMI)
      {
	boolean waitForUsers=false;

	shutdownDialog = new StringDialog(this,
					  "Confirm Ganymede Server Shutdown", 
					  "Are you sure you want to \nshutdown the Ganymede server?", 
					  "Yes", "No", question);

	if (shutdownDialog.DialogShow() != null)
	  {
	    shutdownDialog = new StringDialog(this,
					      "Wait for users to log out?",
					      "Do you want to suspend shutdown until all users are logged off?",
					      "Yes, wait", "No, shutdown now", question);

	    if (shutdownDialog.DialogShow() != null)
	      {
		waitForUsers = true;
	      }

	    if (debug)
	      {
		System.err.println("Affirmative shutdown request");
	      }

	    boolean success = true;

	    try
	      {
		success = admin.shutdown(waitForUsers);
	      }
	    catch (RemoteException ex)
	      {
		admin.forceDisconnect("Couldn't talk to server" + ex);
	      }

	    if (success)
	      {
		adminPanel.quitButton.setEnabled(true);
		adminPanel.loginButton.setEnabled(true);
		setVisible(false);
		
		if (!GASHAdmin.WeAreApplet)
		  {
		    System.exit(0);
		  }
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
				     "Yes", "No", question);
    
	killAllDLG = new StringDialog(killAllDLGR);
	
	if (killAllDLG.DialogShow() == null)
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
    else if (event.getSource() == clearLogButton)
      {
	statusArea.setText("");
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
    if (debug)
      {
	System.err.println("rowMenuPerformed");
      }

    if (e.getSource() == killUserMI)
      {
	if (debug)
	  {
	    System.err.println("kill " + key + " selected");
	  }

	killVictim = (String) key;

	if (new StringDialog(this,
			     "Confirm User Kill",
			     "Are you sure you want to disconnect user " + key + "?",
			     "Yes", "No", question).DialogShow() != null)
	  {
	    if (debug)
	      {
		System.err.println("Affirmative kill request");
	      }

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
	    if (debug)
	      {
		System.err.println("Negative kill request");
	      }
	    killVictim = null;
	  }
      }
    else if (e.getSource() == runNowMI)
      {
	try
	  {
	    admin.runTaskNow((String) key);
	  }
	catch (RemoteException ex)
	  {
	    admin.forceDisconnect("Couldn't talk to server" + ex);
	  }
      }
    else if (e.getSource() == stopTaskMI)
      {
	try
	  {
	    admin.stopTask((String) key);
	  }
	catch (RemoteException ex)
	  {
	    admin.forceDisconnect("Couldn't talk to server" + ex);
	  }
      }
    else if (e.getSource() == disableTaskMI)
      {
	try
	  {
	    admin.disableTask((String) key);
	  }
	catch (RemoteException ex)
	  {
	    admin.forceDisconnect("Couldn't talk to server" + ex);
	  }
      }
    else if (e.getSource() == enableTaskMI)
      {
	try
	  {
	    admin.enableTask((String) key);
	  }
	catch (RemoteException ex)
	  {
	    admin.forceDisconnect("Couldn't talk to server" + ex);
	  }
      }
  }

  /**
   * <P>Method to handle properly logging out if the main admin
   * frame is closed by the window system.</P>
   *
   * <P>We do an enableEvents(AWT.WINDOW_EVENT_MASK) in the
   * GASHAdminFrame constructor to activate this method.</P>
   */

  protected void processWindowEvent(WindowEvent e) 
  {
    if (e.getID() == WindowEvent.WINDOW_CLOSING)
      {
	disconnect();
      }
	
    super.processWindowEvent(e);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                          iAdmin

------------------------------------------------------------------------------*/

/**
 * <P>RMI communications class for the admin console.  Handles login and 
 * update duties for {@link arlut.csd.ganymede.GASHAdmin GASHAdmin}.</P>
 */

class iAdmin extends UnicastRemoteObject implements Admin {

  static final boolean debug = false;

  private GASHAdminFrame frame = null;
  private Server server = null;
  private adminSession aSession = null;
  private String adminName = null;
  private String adminPass = null;
  private StringDialog permDialog = null;

  JFrame schemaFrame;

  Date serverStart;

  /* -- */

  public iAdmin(Server server, String name, String pass) throws RemoteException
  {
    // UnicastRemoteServer can throw RemoteException 

    this.server = server;
    this.adminName = name;
    this.adminPass = pass;

    try
      {
	aSession = server.admin(this);
      }
    catch (NullPointerException ex)
      {
	System.err.println("Error: Didn't get server reference.  Exiting now.");
	return;
      }

    if (aSession == null)
      {
	throw new IllegalArgumentException();
      }

    if (debug)
      {
	System.err.println("Got Admin");
      }
  }

  private StringDialog getDialog()
  {
    if (permDialog == null)
      {
	if (frame == null)
	  {
	    DialogRsrc permResrc = new DialogRsrc(new JFrame(), 
						  "Permissions Error", 
						  "You don't have permission to perform that operation",
						  "OK", null);
	    permDialog = new StringDialog(permResrc);
	  }
	else
	  {
	    DialogRsrc permResrc = new DialogRsrc(frame, 
						  "Permissions Error", 
						  "You don't have permission to perform that operation",
						  "OK", null, "error.gif");
	    permDialog = new StringDialog(permResrc);
	  }
      }

    return permDialog;
  }

  public void setFrame(GASHAdminFrame f)
  {
    if (frame == null)
      {
	frame = f;
      }
    else
      {
	System.err.println("I already have a frame, thank you very much.");
      }
  }

  /**
   * <p>This method is remotely called by the Ganymede server to obtain the username
   * given when the admin console was started.</p>
   */

  public String getName()
  {
    return adminName;
  }

  /**
   * <p>This method is remotely called by the Ganymede server to obtain the password
   * given when the admin console was started.</p>
   */

  public String getPassword()
  {
    return adminPass;
  }

  /**
   * <p>This method is remotely called by the Ganymede server to set the server start
   * date in the admin console.</p>
   */

  public void setServerStart(Date date)
  {
    if (debug)
      {
	System.err.println("GASHAdmin.setServerStart()");
      }

    serverStart = date;

    if (frame != null)
      {
	frame.startField.setText(date.toString());
      }
  }

  /**
   * <p>This method is remotely called by the Ganymede server to set the last dump
   * date in the admin console.</p>
   */

  public void setLastDumpTime(Date date)
  {
    if (debug)
      {
	System.err.println("GASHAdmin.setLastDumpTime()");
      }

    if (frame != null)
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
  }

  /**
   * <p>This method is remotely called by the Ganymede server to set the number of
   * transactions in the server's journal in the admin console.</p>
   */

  public void setTransactionsInJournal(int trans)
  {
    if (debug)
      {
	System.err.println("GASHAdmin.setTransactionsInJournal()");
      }

    if (frame != null)
      {
	frame.journalField.setText("" + trans);
      }
  }

  /**
   * <p>This method is remotely called by the Ganymede server to set the number of
   * objects checked out in the admin console.</p>
   */

  public void setObjectsCheckedOut(int objs)
  {
    if (debug)
      {
	System.err.println("GASHAdmin.setObjectsCheckedOut()");
      }

    if (frame != null)
      {
	frame.checkedOutField.setText("" + objs);
      }
  }

  /**
   * <p>This method is remotely called by the Ganymede server to set the number of
   * locks held in the admin console.</p>
   */

  public void setLocksHeld(int locks)
  {
    if (debug)
      {
	System.err.println("GASHAdmin.setLocksHeld()");
      }

    if (frame != null)
      {
	frame.locksField.setText("" + locks);
      }
  }

  /**
   * <p>This method is remotely called by the Ganymede server to add to the
   * admin console's log display.</p>
   *
   * @param status A string to add to the console's log display, with the
   * trailing newline included.
   */

  public void changeStatus(String status)
  {
    if (debug)
      {
	System.err.println("GASHAdmin.changeStatus()");
      }

    if (frame != null)
      {
	frame.statusArea.append(status);
	frame.statusArea.setCaretPosition(frame.statusArea.getText().length());
      }
  }

  /**
   * <p>This method is remotely called by the Ganymede server to update the
   * number of admin consoles attached to the server.</p>
   */

  public void changeAdmins(String adminStatus)
  {
    if (debug)
      {
	System.err.println("GASHAdmin.changeAdmins()");
      }

    if (frame != null)
      {
	frame.adminField.setText(adminStatus);
      }
  }

  /**
   * <p>This method is remotely called by the Ganymede server to update the
   * admin console's server state display.</p>
   */

  public void changeState(String state)
  {
    if (debug)
      {
	System.err.println("GASHAdmin.changeState()");
      }

    if (frame != null)
      {
	frame.stateField.setText(state);
      }
  }

  /**
   * <p>This method is remotely called by the Ganymede server to update the
   * admin console's connected user table.</p>
   *
   * @param entries a Vector of {@link arlut.csd.ganymede.AdminEntry AdminEntry}
   * login description objects.
   */

  public void changeUsers(Vector entries)
  {
    if (debug)
      {
	System.err.println("GASHAdmin.changeUsers()");
      }

    if (frame == null)
      {
	return;
      }

    AdminEntry e;

    /* -- */

    frame.table.clearCells();
    
    // Process entries from the server

    for (int i = 0; i < entries.size(); i++)
      {
	e = (AdminEntry) entries.elementAt(i);

	frame.table.newRow(e.username);

	if (e.personaName == null || e.personaName.equals(""))
	  {
	    frame.table.setCellText(e.username, 0, e.username, false);
	  }
	else
	  {
	    frame.table.setCellText(e.username, 0, e.personaName, false);
	  }

	frame.table.setCellText(e.username, 1, e.hostname, false);
	frame.table.setCellText(e.username, 2, e.status, false);
	frame.table.setCellText(e.username, 3, e.connecttime, false);
	frame.table.setCellText(e.username, 4, e.event, false);
	frame.table.setCellText(e.username, 5, Integer.toString(e.objectsCheckedOut), false);
      }

    // And refresh our table

    frame.table.refreshTable();
  }

  /**
   * <p>This method is remotely called by the Ganymede server to update the
   * admin console's task table.</p>
   *
   * @param tasks a Vector of {@link arlut.csd.ganymede.scheduleHandle scheduleHandle}
   * objects describing the tasks registered in the Ganymede server.
   */

  public void changeTasks(Vector tasks)
  {
    if (debug)
      {
	System.err.println("GASHAdmin.changeTasks()");
      }

    if (frame == null)
      {
	return;
      }

    scheduleHandle handle;
    String intervalString;

    /* -- */

    frame.taskTable.clearCells();

    // System.err.println("changeTasks: tasks size = " + tasks.size());
    
    // Sort entries according to their incep date,
    // to prevent confusion if new tasks are put into
    // the server-side hashes, and as they are shuffled
    // from hash to hash

    (new VecQuickSort(tasks, 
		      new arlut.csd.Util.Compare() 
		      {
			public int compare(Object a, Object b) 
			  {
			    scheduleHandle aH, bH;
			    
			    aH = (scheduleHandle) a;
			    bH = (scheduleHandle) b;
			    
			    if (aH.incepDate.before(bH.incepDate))
			      {
				return -1;
			      }
			    else if (aH.incepDate.after(bH.incepDate))
			      {
				return 1;
			      }
			    else
			      {
				return 0;
			      }
			  }
		      }
		      )).sort();

    // now reload the table with the current stats

    for (int i = 0; i < tasks.size(); i++)
      {
	handle = (scheduleHandle) tasks.elementAt(i);

	frame.taskTable.newRow(handle.name);
	frame.taskTable.setCellText(handle.name, 0, handle.name, false); // task name

	if (handle.isRunning)
	  {
	    frame.taskTable.setCellText(handle.name, 1, "Running", false);
	    frame.taskTable.setCellColor(handle.name, 1, Color.blue, false);
	    frame.taskTable.setCellBackColor(handle.name, 1, Color.white, false);
	  }
	else if (handle.suspend)
	  {
	    frame.taskTable.setCellText(handle.name, 1, "Suspended", false);
	    frame.taskTable.setCellColor(handle.name, 1, Color.red, false);
	    frame.taskTable.setCellBackColor(handle.name, 1, Color.white, false);
	  }
	else if (handle.startTime != null)
	  {
	    frame.taskTable.setCellText(handle.name, 1, "Scheduled", false);
	    frame.taskTable.setCellColor(handle.name, 1, Color.black, false);
	    frame.taskTable.setCellBackColor(handle.name, 1, Color.white, false);
	  }
	else
	  {
	    frame.taskTable.setCellText(handle.name, 1, "Waiting", false);
	    frame.taskTable.setCellColor(handle.name, 1, Color.black, false);
	    frame.taskTable.setCellBackColor(handle.name, 1, Color.white, false);
	  }

	if (handle.lastTime != null)
	  {
	    frame.taskTable.setCellText(handle.name, 2, handle.lastTime.toString(), false);
	  }

	if (handle.startTime != null)
	  {
	    frame.taskTable.setCellText(handle.name, 3, handle.startTime.toString(), false);
	  }
	else
	  {
	    frame.taskTable.setCellText(handle.name, 3, "On Demand", false);
	  }

	frame.taskTable.setCellText(handle.name, 4, handle.intervalString, false);
      }

    // And refresh our table

    frame.taskTable.refreshTable();
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

  void refreshMe() throws RemoteException
  {
    aSession.refreshMe();
  }

  void kill(String username) throws RemoteException
  {
    handleReturnVal(aSession.kill(username));
  }

  void runTaskNow(String taskName) throws RemoteException
  {
    handleReturnVal(aSession.runTaskNow(taskName));
  }

  void stopTask(String taskName) throws RemoteException
  {
    handleReturnVal(aSession.stopTask(taskName));
  }

  void disableTask(String taskName) throws RemoteException
  {
    handleReturnVal(aSession.disableTask(taskName));
  }

  void enableTask(String taskName) throws RemoteException
  {
    handleReturnVal(aSession.enableTask(taskName));
  }

  // ------------------------------------------------------------
  // convenience methods for our GASHAdminFrame
  // ------------------------------------------------------------

  void killAll() throws RemoteException
  {
    handleReturnVal(aSession.killAll());
  }

  boolean shutdown(boolean waitForUsers) throws RemoteException
  {
    ReturnVal retVal = handleReturnVal(aSession.shutdown(waitForUsers));

    return (retVal == null || retVal.didSucceed());
  }

  void dumpDB() throws RemoteException
  {
    handleReturnVal(aSession.dumpDB());
  }

  void dumpSchema() throws RemoteException
  {
    handleReturnVal(aSession.dumpSchema());
  }

  void reloadClasses() throws RemoteException
  {
    handleReturnVal(aSession.reloadCustomClasses());
  }

  void runInvidTest() throws RemoteException
  {
    handleReturnVal(aSession.runInvidTest());
  }

  void runInvidSweep() throws RemoteException
  {
    handleReturnVal(aSession.runInvidSweep());
  }

  void pullSchema() throws RemoteException
  {
    SchemaEdit editor = null;

    /* -- */

    if (debug)
      {
	System.err.println("Trying to get SchemaEdit handle");
      }

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
	if (debug)
	  {
	    System.err.println("Got SchemaEdit handle");
	  }
	
	schemaFrame = new GASHSchema("Schema Editor", editor);
      }

    // the GASHSchema constructor pops itself up at the end of
    // initialization, and has its own methods for closing itself
    // down.
  }

  /**
   * <p>This method takes a ReturnVal object from the server and, if necessary,
   * runs through a wizard interaction sequence, possibly displaying several
   * dialogs before finally returning a final result code.</p>
   *
   * <p>Use the ReturnVal returned from this function after this function is
   * called to determine the ultimate success or failure of any operation
   * which returns ReturnVal, because a wizard sequence may determine the
   * ultimate result.</p>
   *
   * <p>This method should not be synchronized, since handleReturnVal
   * may pop up modal (thread-blocking) dialogs, and if we we
   * synchronize this, some Swing or AWT code seems to block on our
   * synchronization when we do pop-up dialogs.  It's not any of my
   * code, so I assume that AWT tries to synchronize on the frame when
   * parenting a new dialog.</p> 
   */

  public ReturnVal handleReturnVal(ReturnVal retVal)
  {
    Hashtable dialogResults;

    /* -- */

    if (debug)
      {
	System.err.println("iAdmin.handleReturnVal(): Entering");
      }

    while ((retVal != null) && (retVal.getDialog() != null))
      {
	if (debug)
	  {
	    System.err.println("iAdmin.handleReturnVal(): retrieving dialog");
	  }

	JDialogBuff jdialog = retVal.getDialog();

	if (debug)
	  {
	    System.err.println("iAdmin.handleReturnVal(): extracting dialog");
	  }

	DialogRsrc resource = jdialog.extractDialogRsrc(frame);

	if (debug)
	  {
	    System.err.println("iAdmin.handleReturnVal(): constructing dialog");
	  }

	StringDialog dialog = new StringDialog(resource);

	if (debug)
	  {
	    System.err.println("iAdmin.handleReturnVal(): displaying dialog");
	  }

	// display the Dialog sent to us by the server, get the
	// result of the user's interaction with it.
	    
	dialogResults = dialog.DialogShow();

	if (debug)
	  {
	    System.err.println("iAdmin.handleReturnVal(): dialog done");
	  }

	if (retVal.getCallback() != null)
	  {
	    try
	      {
		if (debug)
		  {
		    System.err.println("iAdmin.handleReturnVal(): Sending result to callback: " + dialogResults);
		  }

		// send the dialog results to the server

		retVal = retVal.getCallback().respond(dialogResults);

		if (debug)
		  {
		    System.err.println("iAdmin.handleReturnVal(): Received result from callback.");
		  }
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("Caught remote exception: " + ex.getMessage());
	      }
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("iAdmin.handleReturnVal(): No callback, breaking");
	      }

	    break;		// we're done
	  }
      }

    if (debug)
      {
	if (retVal != null)
	  {
	    if (retVal.didSucceed())
	      {
		System.err.println("iAdmin.handleReturnVal(): returning success code");
	      }
	    else
	      {
		System.err.println("iAdmin.handleReturnVal(): returning failure code");
	      }
	  }
	else
	  {
	    System.err.println("iAdmin.handleReturnVal(): returning null retVal (success)");
	  }
      }

    return retVal;
  }
}


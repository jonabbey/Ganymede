/*
   GASHAdmin.java

   Admin console for the Java RMI Gash Server

   Created: 28 May 1996
   Version: $Revision: 1.91 $
   Last Mod Date: $Date: 2002/03/13 06:17:32 $
   Release: $Name:  $

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002
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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede;

import javax.swing.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;

import java.rmi.*;
import java.rmi.server.*;

import java.lang.reflect.InvocationTargetException;
import java.io.*;
import java.util.*;

import arlut.csd.JDataComponent.*;
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

    JFrame loginFrame = new GASHAdminLoginFrame("Admin console login", applet);

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
	
	username.requestFocus();
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
  JMenuItem forceBuildMI = null;
  JMenuItem clearLogMI = null;
  JMenuItem quitMI = null;
  JMenuItem dumpMI = null;
  JMenuItem killAllMI = null;
  JMenuItem schemaMI = null;
  JMenuItem shutdownMI = null;

  JMenu debugMenu = null;
  JMenuItem runInvidTestMI = null;
  JMenuItem runInvidSweepMI = null;
  JMenuItem runEmbeddedTestMI = null;
  JMenuItem runEmbeddedSweepMI = null;

  JMenu helpMenu = null;
  JMenuItem showAboutMI = null;

  JPopupMenu popMenu = null;
  JMenuItem killUserMI = null;

  JPanel topPanel = null;

  JTabbedPane tabPane = null;

  StringDialog
    dumpDialog = null;

  consoleShutdownDialog
    shutdownDialog = null;

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

  JLabel memLabel = null;
  JTextField memField = null;

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

  String
    aboutMessage = null;

  messageDialog
    about = null;

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
    controlMenu = new JMenu("Control", false); // no tear-off
    controlMenu.setMnemonic('c');

    clearLogMI = new JMenuItem("Clear Log Panel");
    clearLogMI.setMnemonic('l');
    clearLogMI.addActionListener(this);

    forceBuildMI = new JMenuItem("Force Build");
    forceBuildMI.setMnemonic('f');
    forceBuildMI.addActionListener(this);

    killAllMI = new JMenuItem("Kill Off All Users");
    killAllMI.setMnemonic('k');
    killAllMI.addActionListener(this);

    schemaMI = new JMenuItem("Edit Schema");
    schemaMI.setMnemonic('e');
    schemaMI.addActionListener(this);

    shutdownMI = new JMenuItem("Shutdown Ganymede");
    shutdownMI.setMnemonic('s');
    shutdownMI.addActionListener(this);

    dumpMI = new JMenuItem("Dump Database");
    dumpMI.setMnemonic('d');
    dumpMI.addActionListener(this);

    quitMI = new JMenuItem("Quit Console");
    quitMI.setMnemonic('q');
    quitMI.addActionListener(this);

    controlMenu.add(clearLogMI);
    controlMenu.add(forceBuildMI);
    controlMenu.add(killAllMI);
    controlMenu.add(schemaMI);
    controlMenu.add(shutdownMI);
    controlMenu.addSeparator();
    controlMenu.add(dumpMI);
    controlMenu.addSeparator();
    controlMenu.add(new arlut.csd.JDataComponent.LAFMenu(this)); // ??
    controlMenu.add(quitMI);

    debugMenu = new JMenu("Debug", false); // no tear-off
    debugMenu.setMnemonic('d');

    runInvidTestMI = new JMenuItem("Test Invid Integrity");
    runInvidTestMI.addActionListener(this);

    runInvidSweepMI = new JMenuItem("Repair Invid Integrity");
    runInvidSweepMI.addActionListener(this);

    runEmbeddedTestMI = new JMenuItem("Test Embedded Integrity");
    runEmbeddedTestMI.addActionListener(this);

    runEmbeddedSweepMI = new JMenuItem("Repair Embedded Integrity");
    runEmbeddedSweepMI.addActionListener(this);

    debugMenu.add(runInvidTestMI);
    debugMenu.add(runInvidSweepMI);
    debugMenu.add(runEmbeddedTestMI);
    debugMenu.add(runEmbeddedSweepMI);

    helpMenu = new JMenu("Help");
    helpMenu.setMnemonic('h');
    
    showAboutMI = new JMenuItem("About Ganymede");
    showAboutMI.setMnemonic('a');
    showAboutMI.addActionListener(this);
    helpMenu.add(showAboutMI);

    mbar.add(controlMenu);
    mbar.add(debugMenu);
    mbar.add(Box.createGlue());
    mbar.add(helpMenu);

    setJMenuBar(mbar);

    question = PackageResources.getImageResource(this, "question.gif", getClass());

    java.awt.GridBagLayout topGBL = new java.awt.GridBagLayout();
    java.awt.GridBagConstraints topGBC = new java.awt.GridBagConstraints();

    getContentPane().setLayout(topGBL);

    // set up our top panel, containing a labeled
    // text field showing the server we're connected
    // to.

    /* Ganymede Server Host */

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

    /* Admin consoles connected to server */

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

    /* Server State */

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

    /* Server Start Time */

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

    /* Last Dump Time */

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

    /* In-use / Free / Total Memory */

    memLabel = new JLabel("In-use / Free / Total Memory:");

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(memLabel, gbc);
    topPanel.add(memLabel);

    gbc.anchor = GridBagConstraints.WEST;

    memField = new JTextField("", 40);
    memField.setEditable(false);

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbl.setConstraints(memField, gbc);
    topPanel.add(memField);

    /* Transactions in Journal */

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

    /* Objects Checked Out */

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

    /* Locks held */

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

    JPanel statusBox = new JPanel(new java.awt.BorderLayout());
    statusBox.add("Center", statusAreaPane);
    statusBox.setBorder(new TitledBorder("Server Log"));

    //    topGBL.setConstraints(statusBox, topGBC);
    //    getContentPane().add(statusBox);

    // bottom area, a tab pane with tables for things

    // create our user table

    popMenu = new JPopupMenu();

    killUserMI = new JMenuItem("Kill User");
    popMenu.add(killUserMI);

    table = new rowTable(colWidths, headers, this, false, popMenu, false);
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

    taskTable = new rowTable(taskColWidths, taskHeaders, this, false, taskPopMenu, false);
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

    //    topGBL.setConstraints(tabPane, topGBC);
    //    getContentPane().add(tabPane);

    JSplitPane splitterPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, statusBox, tabPane);
    splitterPane.setOneTouchExpandable(true);
    splitterPane.setDividerLocation(0.5);

    topGBL.setConstraints(splitterPane, topGBC);
    getContentPane().add(splitterPane);

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
    if (event.getSource() == forceBuildMI)
      {
	try
	  {
	    admin.forceBuild();
	  }
	catch (RemoteException ex)
	  {
	    exceptionHandler(ex);
	  }
      }
    else if (event.getSource() == quitMI)
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
	    catch (RemoteException ex)
	      {
		exceptionHandler(ex);
	      }
	  }
      }
    else if (event.getSource() == runInvidTestMI)
      {
	StringDialog invidTestDialog = new StringDialog(this,
							"Invid Test",
							"Are you sure you want to trigger a full invid consistency test?\n"+
							"It may take awhile.",
							"Yes", "No", question);

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
		exceptionHandler(ex);
	      }
	  }
      }
    else if (event.getSource() == runInvidSweepMI)
      {
	StringDialog invidTestDialog = new StringDialog(this,
							"Invid Sweep",
							"Are you sure you want to trigger a full invid sweep?\n"+
							"It may take awhile.",
							"Yes", "No", question);

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
		exceptionHandler(ex);
	      }
	  }
      }
    else if (event.getSource() == runEmbeddedTestMI)
      {
	StringDialog invidTestDialog = new StringDialog(this,
							"Embedded Object Test",
							"Are you sure you want to trigger a full embedded object consistency test?",
							"Yes", "No", question);

	if (invidTestDialog.DialogShow() != null)
	  {
	    if (debug)
	      {
		System.err.println("Affirmative Embedded test request");
	      }

	    try
	      {
		admin.runEmbeddedTest();
	      }
	    catch (RemoteException ex)
	      {
		exceptionHandler(ex);
	      }
	  }
      }
    else if (event.getSource() == runEmbeddedSweepMI)
      {
	StringDialog invidTestDialog = new StringDialog(this,
							"Embedded Object Sweep",
							"Are you sure you want to trigger a full embedded object consistency sweep?",
							"Yes", "No", question);

	if (invidTestDialog.DialogShow() != null)
	  {
	    if (debug)
	      {
		System.err.println("Affirmative Embedded Sweep request");
	      }

	    try
	      {
		admin.runEmbeddedSweep();
	      }
	    catch (RemoteException ex)
	      {
		exceptionHandler(ex);
	      }
	  }
      }
    else if (event.getSource() == shutdownMI)
      {
	boolean waitForUsers=false;

	shutdownDialog = new consoleShutdownDialog(this);

	int result = shutdownDialog.DialogShow();

	if (result == 0)
	  {
	    return;
	  }

	if (result == 2)
	  {
	    waitForUsers = true;
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

	// if we are going to delay shutting down until all users log
	// out, don't close down the admin console.  We don't
	// currently provide a way to clear the
	// shutdown-on-users-logged-out, but it's still useful to be
	// able to continue to monitor things while we're waiting for
	// users to trickle off.

	if (!waitForUsers && success)
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
	    exceptionHandler(ex);
	  }
      }
    else if (event.getSource() == schemaMI)
      {
	schemaMI.setEnabled(false);

	try
	  {
	    admin.pullSchema();
	  }
	catch (RemoteException ex)
	  {
	    exceptionHandler(ex);
	  }
      }
    else if (event.getSource() == showAboutMI)
      {
	showAboutMessage();
      }
    else if (event.getSource() == clearLogMI)
      {
	statusArea.setText("");
      }
  }

  /**
   * Shows the About... dialog.
   */

  public void showAboutMessage()
  {
    if (about == null)
      {
	if (aboutMessage == null)
	  {
	    StringBuffer buffer = new StringBuffer();

	    buffer.append("<head></head>");
	    buffer.append("<body>");
	    buffer.append("<h1>Ganymede Directory Management System</h1><p>");
	    buffer.append("<p>Release number: ");
	    buffer.append(arlut.csd.Util.CVSVersion.getReleaseString());
	    buffer.append("<br>Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002<br>The University of Texas at Austin.</p>");
	    buffer.append("<p>Ganymede is licensed and distributed under the GNU General Public License ");
	    buffer.append("and comes with ABSOLUTELY NO WARRANTY.</p>");
	    buffer.append("<p>This is free software, and you are welcome to redistribute it ");
	    buffer.append("under the conditions of the GNU General Public License.</p>");
	    buffer.append("<p>Written by Jonathan Abbey, Michael Mulvaney, Navin Manohar, ");
	    buffer.append("Brian O'Mara, and Erik Grostic.</p>");
	    buffer.append("<br><p>Visit the Ganymede web site at http://www.arlut.utexas.edu/gash2/</p>");
	    buffer.append("</body>");

	    aboutMessage = buffer.toString();
	  }

	about = new messageDialog(this, "About Ganymede",  null);
	about.setHtmlText(aboutMessage);
      }

    about.setVisible(true);
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
		    exceptionHandler(ex);
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
	    exceptionHandler(ex);
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
	    exceptionHandler(ex);
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
	    exceptionHandler(ex);
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
	    exceptionHandler(ex);
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

  private void exceptionHandler(Throwable ex)
  {
    admin.changeStatus("******************** " +
		       "Error occurred while communicating with the server " +
		       "********************\n");
    StringWriter stringTarget = new StringWriter();
    PrintWriter writer = new PrintWriter(stringTarget);
    
    ex.printStackTrace(writer);
    writer.close();

    admin.changeStatus(stringTarget.toString());

    admin.changeStatus("****************************************" +
		       "****************************************\n");
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

  private boolean tasksLoaded = false;
  private Vector tasksKnown = null;

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

    if (frame == null)
      {
	return;
      }

    final Date lDate = date;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.startField.setText(lDate.toString());
      }
    });
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

    if (frame == null)
      {
	return;
      }

    final Date lDate = date;
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	if (lDate == null)
	  {
	    frame.dumpField.setText("no dump since server start");
	  }
	else
	  {
	    frame.dumpField.setText(lDate.toString());
	  }
      }
    });
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

    if (frame == null)
      {
	return;
      }

    final int lTrans = trans;
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.journalField.setText("" + lTrans);
      }
    });
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

    if (frame == null)
      {
	return;
      }

    final int lObjs = objs;
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.checkedOutField.setText("" + lObjs);
      }
    });
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

    if (frame == null)
      {
	return;
      }

    final int lLocks = locks;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.locksField.setText("" + lLocks);
      }
    });
  }

  /**
   * <p>This method is remotely called by the Ganymede server to update the
   * memory statistics display in the admin console.</p>
   */

  public void setMemoryState(long freeMemory, long totalMemory)
  {
    if (debug)
      {
	System.err.println("GASHAdmin.setMemoryState()");
      }

    if (frame == null)
      {
	return;
      }

    final long lFreeMemory = freeMemory;
    final long lTotalMemory = totalMemory;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.memField.setText((lTotalMemory - lFreeMemory) + " / " + lFreeMemory + " / " + lTotalMemory);
      }
    });
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

    if (frame == null)
      {
	return;
      }

    final String lStatus = status;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.statusArea.append(lStatus);
	frame.statusArea.setCaretPosition(frame.statusArea.getText().length());
      }
    });
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

    if (frame == null)
      {
	return;
      }

    final String lAdminStatus = adminStatus;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.adminField.setText(lAdminStatus);
      }
    });
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

    if (frame == null)
      {
	return;
      }

    final String lState = state;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.stateField.setText(lState);
      }
    });
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

    /* -- */

    final Vector localEntries = entries;

    // And refresh our table.. we'll wait until this succeeds so we
    // don't get the server sending us more updates before the table
    // finishes drawing

    try
      {
	SwingUtilities.invokeAndWait(new Runnable() {
	  public void run() {

	    AdminEntry e;
	    frame.table.clearCells();
    
	    // Process entries from the server

	    for (int i = 0; i < localEntries.size(); i++)
	      {
		e = (AdminEntry) localEntries.elementAt(i);

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

	    frame.table.refreshTable();
	  }
	});
      }
    catch (InvocationTargetException ite)
      {
	ite.printStackTrace();
      }
    catch (InterruptedException ie)
      {
	ie.printStackTrace();
      }
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

    if (!tasksLoaded)
      {
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
      }

    Vector taskNames = new Vector();

    // now reload the table with the current stats

    for (int i = 0; i < tasks.size(); i++)
      {
	handle = (scheduleHandle) tasks.elementAt(i);

	taskNames.addElement(handle.name);

	if (!frame.taskTable.containsKey(handle.name))
	  {
	    frame.taskTable.newRow(handle.name);
	  }

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

    // and take any rows out that are gone

    if (tasksKnown == null)
      {
	tasksKnown = taskNames;
      }
    else
      {
	Vector removedTasks = VectorUtils.difference(tasksKnown, taskNames);

	for (int i = 0; i < removedTasks.size(); i++)
	  {
	    frame.taskTable.deleteRow(removedTasks.elementAt(i), false);
	  }

	tasksKnown = taskNames;
      }

    // And refresh our table.. we'll wait until this succeeds so we
    // don't get the server sending us more updates before the table
    // finishes drawing

    try
      {
	SwingUtilities.invokeAndWait(new Runnable() {
	  public void run() {
	    frame.taskTable.refreshTable();
	  }
	});
      }
    catch (InvocationTargetException ite)
      {
	ite.printStackTrace();
      }
    catch (InterruptedException ie)
      {
	ie.printStackTrace();
      }
  }

  /**
   * <p>This method is called by admin console code to force
   * a complete rebuild of all external builds.  This means that
   * all databases will have their last modification timestamp
   * cleared and all builder tasks will be scheduled for immediate
   * execution.</p>
   */

  public void forceBuild() throws RemoteException
  {
    handleReturnVal(aSession.forceBuild());
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

  void runInvidTest() throws RemoteException
  {
    handleReturnVal(aSession.runInvidTest());
  }

  void runInvidSweep() throws RemoteException
  {
    handleReturnVal(aSession.runInvidSweep());
  }

  void runEmbeddedTest() throws RemoteException
  {
    handleReturnVal(aSession.runEmbeddedTest());
  }

  void runEmbeddedSweep() throws RemoteException
  {
    handleReturnVal(aSession.runEmbeddedSweep());
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
	
	new GASHSchema("Schema Editor", editor, frame.schemaMI);
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

/*------------------------------------------------------------------------------
                                                                           class
                                                           consoleShutdownDialog

------------------------------------------------------------------------------*/

/**
 * <P>GUI dialog for presenting server shutdown options in the admin console.</P>
 */

class consoleShutdownDialog extends JCenterDialog implements ActionListener, WindowListener {

  private final static boolean debug = false;

  GridBagLayout
    gbl;
  
  GridBagConstraints
    gbc;

  JButton
    now, later, cancel;

  JPanel
    mainPanel, imagePanel, buttonPanel;

  JMultiLineLabel 
    textLabel;

  Image image;

  JLabel
    imageCanvas;

  String body = "Shut down the Ganymede server?";
  String buttonText[] = {"Yes, Immediately",
			 "Yes, When Users Log Off",
			 "No, Cancel"};

  JButton
    button1, button2, button3;

  private int result = 0;
  private boolean done = false;

  /* -- */

  public consoleShutdownDialog(Frame frame)
  {
    super(frame, "Confirm Ganymede Server Shutdown?", true);

    this.addWindowListener(this);

    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();

    gbc.insets = new Insets(4,4,4,4);

    mainPanel = new JPanel();
    mainPanel.setBorder(new CompoundBorder(new EtchedBorder(),
					   new EmptyBorder(10, 10, 10, 10)));
    mainPanel.setLayout(gbl);
    setContentPane(mainPanel);

    //
    // Title at top of dialog
    //

    JLabel titleLabel = new JLabel("Confirm Ganymede Server Shutdown?", SwingConstants.CENTER);
    titleLabel.setFont(new Font("Helvetica", Font.BOLD, 14));

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 2;
    gbc.gridheight = 1;
    gbl.setConstraints(titleLabel, gbc);
    mainPanel.add(titleLabel);

    //
    // Text message under title
    //

    textLabel = new JMultiLineLabel(body);
    
    gbc.gridy = 1;
    gbc.gridx = 1;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(textLabel, gbc);
    mainPanel.add(textLabel);

    //
    // Separator goes all the way accross
    // 

    arlut.csd.JDataComponent.JSeparator sep = new arlut.csd.JDataComponent.JSeparator();

    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 2;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(sep, gbc);
    mainPanel.add(sep);

    //
    // ButtonPanel takes up the bottom of the dialog
    //

    buttonPanel = new JFocusRootPanel();

    button1 = new JButton(buttonText[0]);
    button1.addActionListener(this);
    buttonPanel.add(button1);

    button2 = new JButton(buttonText[1]);
    button2.addActionListener(this);
    buttonPanel.add(button2);

    button3 = new JButton(buttonText[2]);
    button3.addActionListener(this);
    buttonPanel.add(button3);

    //
    // buttonPanel goes underneath
    //

    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.gridwidth = 2;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(buttonPanel, gbc);
    mainPanel.add(buttonPanel);

    //
    // Image on left hand side
    //

    image = arlut.csd.Util.PackageResources.getImageResource(frame, "question.gif", frame.getClass());
    imagePanel = new JPanel();

    if (image != null)
      {
	imageCanvas = new JLabel(new ImageIcon(image));
	imagePanel.add(imageCanvas);
      }
    else
      {
	imagePanel.add(Box.createGlue());
      }

    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 1;
    gbc.gridheight = 2;
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbl.setConstraints(imagePanel, gbc);
    mainPanel.add(imagePanel);

    pack();
  }

  /**
   * <p>Display the dialog box, locks this thread while the dialog is being
   * displayed, and returns a hashtable of data field values when the
   * user closes the dialog box.</p>
   *
   * <p>Use this instead of Dialog.show().  If Hashtable returned is null,
   * then the cancel button was clicked.  Otherwise, it will contain a 
   * hash of labels(String) to results (Object).</p>
   *
   * @return HashTable of labels to values
   */

  public int DialogShow()
  {
    mainPanel.revalidate();
    show();

    // at this point we're frozen, since we're a modal dialog.. we'll continue
    // at this point when the ok or cancel buttons are pressed.

    if (debug)
      {
	System.err.println("Done invoking.");
      }

    return result;
  }

  public synchronized void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == button1)
      {
	result = 1;
      }
    else if (e.getSource() == button2)
      {
	result = 2;
      }
    else if (e.getSource() == button3)
      {
	result = 0;
      }
    else
      {
	return;
      }

    // pop down so that DialogShow() can proceed to completion.

    done = true;

    setVisible(false);
  }

  // WindowListener methods

  public void windowActivated(WindowEvent event)
  {
  }

  public void windowClosed(WindowEvent event)
  {
  }

  public synchronized void windowClosing(WindowEvent event)
  {
    if (!done)
      {
	if (debug)
	  {
	    System.err.println("Window is closing and we haven't done a cancel.");
	  }

	// by setting valueHash to null, we're basically treating
	// this window close as a cancel.
	
	result = 0;
      }

    done = true;

    this.setVisible(false);
  }

  public void windowDeactivated(WindowEvent event)
  {
  }

  public void windowDeiconified(WindowEvent event)
  {
  }

  public void windowIconified(WindowEvent event)
  {
  }

  public void windowOpened(WindowEvent event)
  {
    button3.requestFocus();
  }
}

/*------------------------------------------------------------------------------
                                                                           class
					                     GASHAdminLoginFrame

------------------------------------------------------------------------------*/

/**
 * <p>JFrame subclass which is used to hold the {@link
 * arlut.csd.ganymede.GASHAdmin GASHAdmin} applet when the Ganymede
 * admin console is run as an application rather than an applet.</p>
 */

class GASHAdminLoginFrame extends JFrame {

  static final boolean debug = false;
  GASHAdmin adminLogin;

  /* -- */
  
  public GASHAdminLoginFrame(String title, GASHAdmin adminLogin)
  {
    super(title);
    this.adminLogin = adminLogin;
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
  }

  protected void processWindowEvent(WindowEvent e) 
  {
    if (e.getID() == WindowEvent.WINDOW_CLOSING)
      {
	if (debug)
	  {
	    System.out.println("Window closing");
	  }

	if (adminLogin.quitButton.isEnabled())
	  {
	    if (debug)
	      {
		System.out.println("It's ok to log out.");
	      }

	    System.exit(0);
	    super.processWindowEvent(e);
	  }
	else if (debug)
	  {
	    System.out.println("No log out!");
	  }
      }
    else
      {
	super.processWindowEvent(e);
      }
  }
}

/*
   GASHAdmin.java

   Admin console for the Java RMI Gash Server

   Created: 28 May 1996
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996-2004
   The University of Texas at Austin

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

package arlut.csd.ddroid.admin;

import arlut.csd.ddroid.rmi.*;
import arlut.csd.ddroid.common.*;

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
 * <P>Directory Droid GUI admin console.</P>
 *
 * <P>GASHAdmin is a dual-mode (applet/application) GUI app for monitoring and
 * controlling the Directory Droid server.  In addition to monitoring users and tasks
 * on the Directory Droid server, the admin console includes a full-functioned
 * {@link arlut.csd.ddroid.admin.GASHSchema schema editor}.</P>
 *
 * <P>GASHAdmin connects to a running
 * {@link arlut.csd.ddroid.server.GanymedeServer GanymedeServer} using the 
 * {@link arlut.csd.ddroid.server.GanymedeServer#admin(arlut.csd.ganymede.Admin) admin()}
 * method.</P>
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
   * The GASHAdminDispatch object is an event switcher and hook interface
   * which we use to propagate events from the server.
   */

  static GASHAdminDispatch adminDispatch = null;

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
    appletContentPane.add(applet, "Center");

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

    if (adminDispatch != null)
      {
	try
	  {
	    adminDispatch.disconnect();
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

    if (adminDispatch != null)
      {
	try
	  {
	    adminDispatch.disconnect();
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

    JPanel labelPanel = new JPanel();
    labelPanel.setLayout(new BorderLayout());

    JLabel label = new JLabel("Directory Droid Server on: ");
    labelPanel.add("North", label);

    JLabel hostLabel = new JLabel(serverhost + ", port " + registryPortProperty);
    Font x = new Font("Courier", Font.ITALIC, 14);
    hostLabel.setFont(x);
    hostLabel.setForeground(Color.black);

    labelPanel.add("South", hostLabel);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0.0;
    gbc.gridy = 1;
    gbl.setConstraints(labelPanel, gbc);
    panel.add(labelPanel);

    gbc.ipady = 4;

    JLabel ul = new JLabel("Username:");
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.gridwidth = 1;
    gbc.weightx = 0.0;
    gbl.setConstraints(ul, gbc);
    panel.add(ul);
    
    username = new JTextField(15);
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

    password = new JPasswordField(15);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx = 1;
    gbc.weightx = 1.0;

    gbl.setConstraints(password, gbc);
    panel.add(password);
    
    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.ipady = 0;
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

	boolean success = false;

	try
	  {
	    adminDispatch = new GASHAdminDispatch(server);
	    success = adminDispatch.connect(username.getText(), 
					    new String(password.getPassword()));

	    if (!success)
	      {
		password.setText("");
		return;
	      }
	  }
	catch (RemoteException rx)
	  {
	    new StringDialog(new JFrame(),
			     "Login error",
			     "Couldn't log in to the Directory Droid server... perhaps it is down?\n\nException: " + 
			     rx.getMessage(),
			     "OK", null,
			     getErrorImage()).DialogShow();
	  }
	catch (IllegalArgumentException ex)
	  {
	    new StringDialog(new JFrame(),
			     "Couldn't log in to the Directory Droid Server",
			     "Couldn't log in to the Directory Droid server.\n\nBad username/password or " +
			     "insufficient permissions to run the Directory Droid admin console.",
			     "OK", null, 
			     getErrorImage()).DialogShow();
	    
	    password.setText("");
	    return;
	  }

	username.setText("");
	password.setText("");
	quitButton.setEnabled(false);
	loginButton.setEnabled(false);

	frame = new GASHAdminFrame("Directory Droid Admin Console", applet);
	
	// Now that the frame is completely initialized, tie the
	// GASHAdminDispatch object to the frame, and vice-versa.
	
	frame.setDispatch(adminDispatch);
	adminDispatch.setFrame(frame);

	try
	  {
	    adminDispatch.startAsyncPoller();
	  }
	catch (RemoteException rx)
	  {
	    System.err.println("Problem trying to start poll thread: " + rx);
	  }

	try
	  {
	    adminDispatch.refreshMe();
	  }
	catch (RemoteException rx)
	  {
	    System.err.println("Problem trying to refresh: " + rx);
	  }
      }
  }

  /**
   * <P>Private method to load the Directory Droid console's parameters
   * from a file.  Used when GASHAdmin is run from the command line..
   * {@link arlut.csd.ddroid.admin.GASHAdmin#loadParameters() loadParameters()}
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
   * <P>Private method to load the Directory Droid console's parameters
   * from an applet's HTML parameters.  Used when GASHAdmin is run as an applet..
   * {@link arlut.csd.ddroid.admin.GASHAdmin#loadProperties(java.lang.String) loadProperties()}
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
					                     GASHAdminLoginFrame

------------------------------------------------------------------------------*/

/**
 * <p>JFrame subclass which is used to hold the {@link
 * arlut.csd.ddroid.admin.GASHAdmin GASHAdmin} applet when the Directory Droid
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

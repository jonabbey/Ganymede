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
	    
   Ganymede Directory Management System
 
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

package arlut.csd.ganymede.admin;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import arlut.csd.JDialog.StringDialog;
import arlut.csd.Util.booleanSemaphore;
import arlut.csd.Util.PackageResources;
import arlut.csd.ganymede.rmi.Server;

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
 * {@link arlut.csd.ganymede.admin.GASHSchema schema editor}.</P>
 *
 * <P>GASHAdmin connects to a running
 * {@link arlut.csd.ganymede.server.GanymedeServer GanymedeServer} using the 
 * {@link arlut.csd.ganymede.server.GanymedeServer#admin(java.lang.String, java.lang.String) admin()}
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

  static Server server = null;	// remote reference
  static Image admin_logo = null;

  JTextField username = null;
  JPasswordField password = null;
  JButton quitButton;
  JButton loginButton= null;

  Image errorImage = null;

  char spinAry[] = {'/','-', '\\', '|'};
  int spindex = 0;

  String connectError = null;

  private booleanSemaphore connecting = new booleanSemaphore(false);
  private booleanSemaphore connected = new booleanSemaphore(false);

  /* -- */

  // Our primary constructor.  This will always be called, either
  // from main(), below, or by the environment building our applet.

  public GASHAdmin() 
  {
    admin_logo = PackageResources.getImageResource(this, "ganymede_admin.jpg", getClass());
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

    applet = new GASHAdmin();

    JFrame loginFrame = new GASHAdminLoginFrame("Admin console login", applet);

    loginFrame.getContentPane().setLayout(new BorderLayout());
    loginFrame.getContentPane().add("Center", applet);

    applet.init();		// init before visible for smoothness

    loginFrame.setVisible(true);
    loginFrame.pack();		// pack after visible so we size everything properly
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

    JLabel label = new JLabel("Ganymede Server on: ");
    labelPanel.add("North", label);

    JLabel hostLabel = new JLabel(serverhost + ", port " + registryPortProperty);
    Font x = new Font("Courier", Font.ITALIC, 14);
    hostLabel.setFont(x);
    hostLabel.setForeground(Color.black);

    labelPanel.add("South", hostLabel);

    gbc.insets = new Insets(1,1,0,0);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0.0;
    gbc.weightx = 1.0;
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
    username.setEnabled(false);
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
    password.setEnabled(false);
    password.addActionListener(this);

    gbl.setConstraints(password, gbc);
    panel.add(password);
    
    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.ipady = 0;
    gbc.gridwidth = 2;
    JPanel buttonPanel = new JPanel(new BorderLayout());
    gbl.setConstraints(buttonPanel, gbc);
    panel.add(buttonPanel);

    loginButton = new JButton("Connecting... " + spinAry[spindex]);
    loginButton.setOpaque(true);
    loginButton.setEnabled(true);
    loginButton.addActionListener(this);

    buttonPanel.add(loginButton, "Center");

    if (!WeAreApplet)
      {
	quitButton = new JButton("Quit");

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
    int try_number = 0;

    /* -- */

    if (connecting.set(true))
      {
	return;			// we already have a thread running
      }

    try
      {
	if (connected.isSet())
	  {
	    return;
	  }
    
	while (!connected.isSet())
	  {
	    if (try_number++ > 20)
	      {
		break;
	      }

	    try
	      {
		Remote obj = Naming.lookup(GASHAdmin.url);
		
		if (obj instanceof Server)
		  {
		    server = (Server) obj;
		    server.up();	// RMI call to verify our connection
		  }

		connected.set(true);
		break;
	      }
	    catch (Throwable ex)
	      {
		connectError = ex.getMessage();
	      }
	    
	    try 
	      {
		spindex++;
		
		if (spindex >= spinAry.length)
		  {
		    spindex = 0;
		  }
		
		try
		  {
		    final GASHAdmin localLoginBox = this;
		    
		    SwingUtilities.invokeAndWait(new Runnable()
		      {
			public void run()
			{
			  localLoginBox.loginButton.setText("Connecting... " + spinAry[spindex]);
			}
		      });
		  }
		catch (Exception ex)
		  {
		    ex.printStackTrace();
		  }
		
		// Wait for 1/4 sec before retrying to connect to server
		
		Thread.sleep(250);
	      }
	    catch (InterruptedException e) 
	      {
	      }
	  }
	
	if (connected.isSet())
	  {
	    SwingUtilities.invokeLater(new Runnable() 
	      {
		public void run()
		{
		  loginButton.setText("Login to server");
		  username.setEnabled(true);
		  password.setEnabled(true);
		  
		  username.requestFocus();
		  invalidate();
		  validate();
		}
	      });
	  }
	else
	  {
	    new StringDialog(new JFrame(),
			     "Login error",
			     "Couldn't locate Ganymede server... perhaps it is down?\n\n" + connectError,
			     "OK", null,
			     getErrorImage()).DialogShow();
	    
	    SwingUtilities.invokeLater(new Runnable() 
	      {
		public void run()
		{
		  loginButton.setText("Connect");
		  username.setEnabled(false);
		  password.setEnabled(false);
		  
		  username.requestFocus();
		  invalidate();
		  validate();
		}
	      });
	  }
      }
    finally
      {
	connecting.set(false);
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
	// if we haven't got a good RMI connection, try to spawn
	// another thread.  Note that the run() method starts off by
	// testing the connecting booleanSemaphore, so we won't allow
	// multiple connection threads to be running concurrently, no
	// matter how many times the user presses the login button
	// while waiting for a connection

	if (!connected.isSet())
	  {
	    new Thread(this).start();
	    return;
	  }

	try
	  {
	    adminDispatch = new GASHAdminDispatch(server);

	    if (!adminDispatch.connect(username.getText(), 
				       new String(password.getPassword())))
	      {
		return;
	      }
	  }
	catch (RemoteException rx)
	  {
	    new StringDialog(new JFrame(),
			     "Login error",
			     "Couldn't log in to the Ganymede server... perhaps it is down?\n\nException: " + 
			     rx.getMessage(),
			     "OK", null,
			     getErrorImage()).DialogShow();

	    connected.set(false);

	    loginButton.setText("Connecting... " + spinAry[spindex]);
	    new Thread(this).start();
	    return;
	  }
	catch (Exception ex)
	  {
	    new StringDialog(new JFrame(),
			     "Couldn't log in to the Ganymede Server",
			     "Exception caught during login attempt.\n\n.This condition may be due to a software error.\n\n" +
			     "Exception: " + ex.getMessage(),
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
   * <P>Private method to load the Ganymede console's parameters
   * from a file.  Used when GASHAdmin is run from the command line..
   * {@link arlut.csd.ganymede.admin.GASHAdmin#loadParameters() loadParameters()}
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
   * {@link arlut.csd.ganymede.admin.GASHAdmin#loadProperties(java.lang.String) loadProperties()}
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
 * arlut.csd.ganymede.admin.GASHAdmin GASHAdmin} applet when the Ganymede 
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
    // Since this frame holds the admin console's login box, we want
    // to be sure not to close it unless the admin console is already
    // disconnected.. if the main admin console window is running, the
    // quit button will be disabled, and we'll keep the admin console
    // login box.

    // It might, in theory, make sense to allow the login box to close
    // independent of the main console window, but in the case where
    // we run in a web browser as an applet, we need that login box
    // applet to keep running so the browser doesn't terminate.  Since
    // we run as an applet hosted within an application frame in the
    // application case, we still need to be able to respond to the
    // destroy() call.. we'd need to distinguish the destroy() case
    // when we are running as an application from the destroy() case
    // when we are running as an applet.  Easier just not to allow
    // closing the login box when running as an application.

    // If you the reader feel like changing this, be my guest.  -- jon

    if (e.getID() == WindowEvent.WINDOW_CLOSING)
      {
	if (debug)
	  {
	    System.out.println("Window closing");
	  }

	// it's safe to assume that adminLogin.quitButton is not null,
	// because we would not have created a GASHAdminLoginFrame if
	// we weren't running as an application.

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

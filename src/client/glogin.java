/*
   glogin.java

   Ganymede client login module

   This client has been developed so that it can run as both an applet,
   as well as an application.

   --

   Created: 22 Jan 1997
   Version: $Revision: 1.56 $
   Last Mod Date: $Date: 1999/04/28 09:32:27 $
   Release: $Name:  $

   Module By: Navin Manohar, Mike Mulvaney, and Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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

package arlut.csd.ganymede.client;

import javax.swing.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;
import java.util.*;

import arlut.csd.JDialog.*;
import arlut.csd.ganymede.*;
import arlut.csd.Util.ParseArgs;
import arlut.csd.Util.PackageResources;

/*------------------------------------------------------------------------------
                                                                           class
                                                                          glogin

------------------------------------------------------------------------------*/

/**
 * <p>Ganymede client start class.  This class can be run from the command
 * line via its static main() method, or as an applet loaded into a
 * web browser, generally with Sun's Java plug-in.</p>
 *
 * <p>This class has a run() method for attempting to connect to
 * the server in the background once the applet is initialized.</p>
 *
 * <p>Once glogin handles the user's login, a {@link arlut.csd.ganymede.client.gclient gclient}
 * object is constructed, which handles all of the user's interactions with the server.</p>
 *
 * @version $Revision: 1.56 $ $Date: 1999/04/28 09:32:27 $ $Name:  $
 * @author Navin Manohar, Mike Mulvaney, and Jonathan Abbey
 */

public class glogin extends JApplet implements Runnable, ActionListener, ClientListener {

  public static boolean debug = false;

  public static String 
    properties_file = null,
    serverhost = null,
    server_url = null,
    helpBase = null;

  public static int registryPortProperty = 1099;

  /** 
   * Client-side properties loaded from the command line or from the
   * web page which contains the definition for glogin as an applet.
   */

  public static Properties ganymedeProperties = null;

  /** 
   * The main client class, will be null until the user is logged in
   * to the server.
   */

  public static gclient g_client;

  /**
   * If we are run from the command line, this frame will be used to contain
   * the glogin applet in an application context.
   */

  protected static JFrame my_frame = null;

  /** 
   * Reference to the server acquired by RMI naming service.  Used to
   * log into the server.  
   */

  protected static Server  my_server;

  /**
   * RMI object to handle getting us logged into the server, and to handle
   * asynchronous callbacks from the server on our behalf.
   */

  protected static ClientBase my_client;

  /**
   * Reference to a user session on the server, will be null until the user is
   * logged into the server.
   */

  protected static Session my_session;
  protected static String my_username, my_passwd;

  /**
   * We're a singleton pattern.. this is a static reference to our actual login
   * applet.
   */

  protected static glogin my_glogin;

  /**
   * If true, we are running as an applet and are limited by the Java sandbox.
   * A few features of the client will be disabled if this is true (saving query
   * reports to disk, etc.).
   */

  private static boolean WeAreApplet = true;

  private static Container appletContentPane = null;

  /**
   * Background thread to handle force disconnect commands from the server.
   * We need this thread because jdk 1.2 has a bug where RMI callbacks are
   * not privileged to interact with the Swing thread.  By creating a thread
   * to handle forced logouts ourselves, we can have an RMI callback pass
   * a message to this thread (which has local privileges), which can then
   * throw up a dialog explaining about being disconnected, etc.
   */

  protected static DeathWatcherThread deathThread;

  // ---

  /**
   * Background thread used to attempt to get the initial RMI connection to the
   * Ganymede server.
   */

  protected Thread my_thread = new Thread(this);

  protected boolean connected = false;

  private GridBagLayout gbl;
  private GridBagConstraints gbc;

  protected Image ganymede_logo;
  protected JTextField username;
  protected JPasswordField passwd;
  protected JButton connector;
  protected JButton _quitButton;
  protected JPanel bPanel;

  /**
   * This main() function will allow this applet to run as an application
   * when it is not executed in the context of a browser.  
   */

  public static void main (String args[])
  {
    WeAreApplet = false;

    debug = ParseArgs.switchExists("-debug", args);
    properties_file = ParseArgs.getArg("properties", args);

    if (properties_file == null)
      {
	throw new IllegalArgumentException("Usage error: glogin [-debug] properties=properties_file\n\n");
      }

    my_glogin = new glogin();

    if (properties_file != null)
      {
	ganymedeProperties = new Properties();
	
	if (debug)
	  {
	    System.out.println("Starting up in debug mode.");
	    System.out.println("Loading properties from: " + properties_file);
	  }

	try
	  {
	    ganymedeProperties.load(new BufferedInputStream(new FileInputStream(properties_file)));
	  }
	catch (java.io.FileNotFoundException e)
	  {
	    throw new RuntimeException("File not found: " + e);
	  }
	catch (java.io.IOException e)
	  {
	    throw new RuntimeException("Whoa, io exception: " + e);
	  }
      }

    if (ganymedeProperties != null)
      {
	serverhost = ganymedeProperties.getProperty("ganymede.serverhost");

	// get the registry port number

	String registryPort = ganymedeProperties.getProperty("ganymede.registryPort");

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
      }

    if ((serverhost == null) || (serverhost.equals("")))
      {
	throw new RuntimeException("Trouble:  couldn't load server host from " + properties_file);
      }

    my_glogin.getContentPane().setLayout(new BorderLayout());

    my_frame = new JFrame("Ganymede Client");

    appletContentPane = my_frame.getContentPane();

    appletContentPane.setLayout(new BorderLayout());
   
    appletContentPane.add(my_glogin,"Center");

    my_frame.pack();
    my_frame.setSize(265,380);    
    my_frame.show();
 
    my_glogin.init();
    my_glogin.getContentPane().getLayout().layoutContainer(my_glogin);
  }

  /**
   * This method returns true if the Ganymede client is running
   * as an applet.
   */

  public static boolean isApplet()
  {
    return WeAreApplet;
  }

  /**
   * Standard applet initialization method.
   */

  public void init() 
  {
    if (debug)
      {
	System.out.println("init in glogin");
      }

    my_glogin = this;
    
    // Dowload the ganymede logo using the appropriate method
    
    ganymede_logo = PackageResources.getImageResource(this, "ganymede.jpg", getClass());
    
    if (WeAreApplet)
      {
	my_frame = new JFrame();
	serverhost = getParameter("ganymede.serverhost");

	if (serverhost == null || serverhost.equals(""))
	  {
	    throw new RuntimeException("Trouble:  Couldn't get ganymede.serverhost PARAM");
	  }

	String registryPort = getParameter("ganymede.registryPort");

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
      }

    server_url = "rmi://" + serverhost + ":" + registryPortProperty + "/ganymede.server";

    appletContentPane = getContentPane();
    appletContentPane.setLayout(new BorderLayout());

    JPanel loginBox = new JPanel();
    //    loginBox.setBorder(new BevelBorder(BevelBorder.RAISED));
    loginBox.setBorder(BorderFactory.createEtchedBorder());
    appletContentPane.add(loginBox, "Center");
    
    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridheight = 1;
    gbc.insets = new Insets(1,1,0,0);

    loginBox.setLayout(gbl);

    JLabel image = new JLabel(new ImageIcon(ganymede_logo));
    image.setOpaque(true);
    image.setBackground(Color.black);

    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 3;
    gbc.weighty = 1.0;
    gbc.weightx = 1.0;
    gbl.setConstraints(image, gbc);
    loginBox.add(image);

    JLabel label = new JLabel("Ganymede Network Management System");
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0.0;
    gbc.gridy = 1;
    gbl.setConstraints(label, gbc);
    loginBox.add(label);

    // the username and passwd fields here won't have their
    // callback set with addTextListener().. instead, we'll
    // trap the login/quit buttons, and query these
    // fields when we process the buttons.

    //gbc.insets = new Insets(2,2,2,2);
    gbc.ipady = 4;

    JLabel userL = new JLabel("Username:");
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridy = 2;
    gbc.gridwidth = 1;
    gbc.weightx = 0.0;
    gbl.setConstraints(userL, gbc);
    loginBox.add(userL);

    username = new JTextField(20);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx = 1;
    gbc.weightx = 1.0;
    gbl.setConstraints(username, gbc);
    loginBox.add(username);
    
    JLabel passL = new JLabel("Password:");
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 1;
    gbc.weightx = 0.0;
    gbl.setConstraints(passL, gbc);
    loginBox.add(passL);

    passwd = new JPasswordField(20);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx = 1;
    gbc.weightx = 1.0;
    gbl.setConstraints(passwd, gbc);
    loginBox.add(passwd);

    //gbc.insets = new Insets(0,0,0,0);
    gbc.ipady = 0;
    
    _quitButton = new JButton("Quit");
    _quitButton.setBackground(ClientColor.buttonBG);

    connector = new JButton("Connecting...");
    connector.setOpaque(true);
    connector.setBackground(ClientColor.buttonBG);
    connector.addActionListener(this);

    JPanel buttonPanel = new JPanel(new BorderLayout());

    buttonPanel.add("Center", connector);

    if (!WeAreApplet)
      {
	buttonPanel.add("East", _quitButton);
      }

    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbl.setConstraints(buttonPanel, gbc);
    loginBox.add(buttonPanel);

    passwd.addActionListener(this);
    username.addActionListener(this);

    _quitButton.addActionListener(this);

    // frames like to be packed

    if (!WeAreApplet)
      {
	my_frame.pack();
      }

    // The Login GUI has been set up.  Now the server connection needs
    // to be properly established.
    
    /* RMI initialization stuff. We do this for our iClient object. */

    if (!WeAreApplet)
      {
	//Applets don't like you setting the sercurity manager!
	//System.setSecurityManager(new RMISecurityManager());
      }
    
    /* Get a reference to the server */

    my_thread.start();
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
	if (try_number++ > 10)
	  {
	    System.out.println("I've tried ten times to connect, but I can't do it.  Maybe the server is down.");
	    break;
	  }

	try
	  {
	    my_client = new ClientBase(server_url, this);  // Exception will happen here
	    connected = true;
	  }
	catch (RemoteException rx)
	  {
	    if (debug)
	      {
		System.out.println("Could not start up the ClientBase, trying again..." + rx);
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
      }

    connector.setText("Login to server");
    username.setEnabled(true);
    passwd.setEnabled(true);

    invalidate();
    validate();
  }

  /**
   * Logout from the server. 
   *
   * This is called from the gclient.
   */

  public void logout()
  {
    try
      {
	my_client.disconnect();
      }
    catch (NullPointerException ex)
      {
      }
    catch (RemoteException ex)
      {
      }
    finally
      {
	try
	  {
	    g_client.statusThread.shutdown();
	  }
	catch (NullPointerException ex)
	  {
	  }

	try
	  {
	    g_client.setVisible(false);
	    g_client.dispose();
	  }
	catch (NullPointerException ex)
	  {
	  }

	enableButtons(true);
      }
  }

  /**
   *
   * If the applet is no longer visible on the page, we exit.
   *
   */

  public void destroy() 
  {
    logout();
  }

  /**
   * Reports our username to gclient
   */

  public String getUserName()
  {
    return my_username;
  }

  public void enableButtons(boolean enabled)
  {
    connector.setEnabled(enabled);
    _quitButton.setEnabled(enabled);
  }

  /**
   * Set the cursor to a wait cursor(usually a watch.)
   */

  public void setWaitCursor()
  {
    this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
  }

  /**
   * <p>Set the cursor to the normal cursor(usually a pointer).</p>
   *
   * <p>This is dependent on the operating system.</p>
   */

  public void setNormalCursor()
  {
    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }

  /**
   * Handle button clicks, and enter being hit in the password
   * field.
   */

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == username)
      {
	passwd.requestFocus();
      }
    else if (e.getSource() == passwd)
      {
	connector.doClick();
      }
    else if (e.getSource() == connector)
      {
	setWaitCursor();

	String uname = username.getText().trim();
	String pword = new String(passwd.getPassword());

	my_username = uname;
	my_passwd = pword;
	
	try
	  {
	    my_session = my_client.login(uname, pword);
	  }
	catch (RemoteException ex)
	  {
	    JErrorDialog d = new JErrorDialog(my_frame,
					      "RMI Error: Couldn't log into server: \n" +
					      ex.getMessage());
	    
	    connector.setEnabled(true);
	    _quitButton.setEnabled(true);

	    setNormalCursor();
	    return;
	  }
	catch (NullPointerException ex)
	  {
	    JErrorDialog d = new JErrorDialog(my_frame,
					      "Error: Didn't get server reference.  Please Quit and Restart");
	    
	    connector.setEnabled(true);
	    _quitButton.setEnabled(true);

	    setNormalCursor();	    
	    return;
	  }
	catch (Exception ex) 
	  {
	    JErrorDialog d = new JErrorDialog(my_frame,
					      "Error: " + ex.getMessage());

	    connector.setEnabled(true);
	    _quitButton.setEnabled(true);

	    setNormalCursor();
	    return;
	  }

	connector.setEnabled(false);
	_quitButton.setEnabled(false);

	if (my_session != null) 
	  {
	    startSession(my_session);
	  }
	else 
	  {
	    // This means that the user was not able to log into the server properly.

	    // We re-enable the "Login to server" button so that the user can try again.

	    connector.setEnabled(true);
	    _quitButton.setEnabled(true);
	  }

	setNormalCursor();
      }
    else if (e.getSource() == _quitButton)
      {
	System.exit(0);
      }
  }

  /**
   * Starts the main Ganymede client.
   */

  private void startSession(Session session)
  {
    // try to get the URL for the help document tree

    try
      {
	glogin.helpBase = session.getHelpBase();
      }
    catch (RemoteException ex)
      {
	System.err.println("Couldn't get help base from server: remote exception: " + ex.getMessage());
	glogin.helpBase = null;
      }

    // create the thread in our thread group that the disconnected()
    // method will use to knock us down.

    deathThread = new DeathWatcherThread();
    deathThread.start();

    // and pop up everything

    g_client = new gclient(session,this);

    passwd.setText("");

    /* At this point, all the login matters have been handled and we have
       a Session object in our hands.  We now instantiate the main client
       that will be used to interact with the Ganymede server.*/

    try 
      {
	// This will get the ball rolling.

	g_client.start();
      }
    catch (Exception e)
      {
	// Any exception thrown by glclient will be handled here.
	System.err.println("Error starting client: " + e);
      }
  }

  // These are for the ClientListener

  /**
   * Handle a message from the {@link arlut.csd.ganymede.client.ClientBase ClientBase}
   * RMI object.
   */

  public void messageReceived(ClientEvent e)
  {
    if (debug)
      {
	System.out.println(e.getMessage());
      }

    JErrorDialog d = new JErrorDialog(new JFrame(), e.getMessage());
  }

  /**
   * Handle a forced disconnect message from the
   * {@link arlut.csd.ganymede.client.ClientBase ClientBase} RMI object.
   */

  public void disconnected(ClientEvent e)
  {
    try
      {
	deathThread.die(e.getMessage());
      }
    catch (NullPointerException ex)
      {
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                              DeathWatcherThread

------------------------------------------------------------------------------*/

/** 
 * <p>Client-side background thread to handle force disconnect commands
 * from the server.  We need this thread because jdk 1.2 has a bug
 * where RMI callbacks are not privileged to interact with the Swing
 * thread.  By creating a thread to handle forced logouts ourselves,
 * we can have an RMI callback pass a message to this thread (which
 * has local privileges), which can then throw up a dialog explaining
 * about being disconnected, etc.</p>
 *
 * <p>When run, this thread waits for die() to be called, whereupon it
 * creates an {@link arlut.csd.ganymede.client.ExitThread ExitThread} to
 * actually shut down the client.</p>
 *
 * @version $Revision: 1.56 $ $Date: 1999/04/28 09:32:27 $ $Name:  $
 * @author Jonathan Abbey
 */

class DeathWatcherThread extends Thread {

  final boolean debug = false;

  String message = null;

  /* -- */

  public DeathWatcherThread()
  {
  }

  public synchronized void run()
  {
    while (message == null)
      {
	try
	  {
	    wait();
	  }
	catch (InterruptedException ex)
	  {
	    if (message == null)
	      {
		return;
	      }
	  }
      }

    ExitThread exitThread = new ExitThread(message);

    // start up the death timer, which will close all our active
    // windows in a bit.. we hold off on just doing it now to not
    // startle the user too much.

    exitThread.start();

    // throw up a modal dialog to get the user's attention

    // note, we really shouldn't use just 'new Date()'.toString() here,
    // but I'm just that lazy at the moment.

    new JErrorDialog(glogin.g_client, 
		     "The server is disconnecting us: \n\n" + message + 
		     "\n\n" + new Date());

    // if we get here, the dialog has been put down

    exitThread.dieNow();
  }

  /**
   * This method causes the DeathWatcherThread to kick off the
   * end-of-the-world process.
   */

  public synchronized void die(String message)
  {
    this.message = message;

    notify();
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      ExitThread

------------------------------------------------------------------------------*/

/**
 * <p>Client-side self-destruction thread.  This thread will be created and run
 * when the server sends the client's
 * {@link arlut.csd.ganymede.client.ClientBase ClientBase} a forced disconnect 
 * RMI call.  When run, this thread starts a 30 second timer, while the 
 * {@link arlut.csd.ganymede.client.DeathWatcherThread DeathWatcherThread} shows
 * a dialog to the user, explaining the disconnect.  The user can click ok on
 * that dialog, causing this thread's dieNow() method to terminate the timer.  In
 * any case, when the timer counts down to zero, the glogin's logout() method 
 * will be called, and the client's main window will be shutdown.</p>
 *
 * @version $Revision: 1.56 $ $Date: 1999/04/28 09:32:27 $ $Name:  $
 * @author Jonathan Abbey
 */

class ExitThread extends Thread {

  final boolean debug = false;

  String message;

  boolean dieNow = false;

  /* -- */

  public ExitThread(String message)
  {
    this.message = message;
  }

  public synchronized void run()
  {
    if (debug)
      {
	System.out.println("ExitThread: running");
      }

    int i = 30;
    
    System.out.print("System shutting down in 30 seconds");

    try
      {
	while (!dieNow && i > 0)
	  {
	    sleep(1000);
	    System.out.print(".");
	    i--;
	  }
      }
    catch (InterruptedException ie)
      {
	System.out.println("glogin: Interupted trying to sleep and quit: " + ie);
      }
    
    System.out.println("\nGanymede disconnected: " + message);

    glogin.my_glogin.logout();
  }

  public void dieNow()
  {
    this.dieNow = true;
  }
}

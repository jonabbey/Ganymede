/*
   glogin.java

   Ganymede client login module

   This client has been developed so that it can run as both an applet,
   as well as an application.

   --

   Created: 22 Jan 1997
   Version: $Revision: 1.51 $
   Last Mod Date: $Date: 1999/01/22 18:04:15 $
   Release: $Name:  $

   Module By: Navin Manohar and Mike Mulvaney

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

public class glogin extends JApplet implements Runnable, ActionListener, ClientListener {

  public static boolean debug = false;

  public static String 
    properties_file = null,
    serverhost = null,
    server_url = null,
    helpBase = null;

  public static Properties ganymedeProperties = null;

  // ---

  private GridBagLayout gbl;
  private GridBagConstraints gbc;

  public static gclient g_client;

  protected Image ganymede_logo;
  protected JTextField username;
  protected JPasswordField passwd;
  protected JButton connector;
  protected JButton _quitButton;
  protected JPanel bPanel;
  protected static JFrame my_frame = null;

  protected static Server  my_server;

  protected static ClientBase my_client;

  protected static Session my_session;
  protected static String my_username, my_passwd;

  protected static glogin my_glogin;

  protected Thread my_thread = new Thread(this);

  protected boolean connected = false;

  private static boolean WeAreApplet = true;

  private static Container appletContentPane = null;

  protected static DeathWatcherThread deathThread;

  /**
   *  This main() function will allow this applet to run as an application
   *  when it is not executed in the context of a browser.
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
    //my_glogin.start();
    my_glogin.getContentPane().getLayout().layoutContainer(my_glogin);
  }

  /**
   *
   * This method returns true if the Ganymede client is running
   * as an applet.
   *
   */

  public static boolean isApplet()
  {
    return WeAreApplet;
  }

  /**
   *
   *
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
      }

    server_url = "rmi://" + serverhost + "/ganymede.server";

    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridheight = 1;
    gbc.insets = new Insets(1,1,0,0);

    appletContentPane = getContentPane();

    appletContentPane.setLayout(gbl);

    JLabel image = new JLabel(new ImageIcon(ganymede_logo));
    image.setOpaque(true);
    image.setBackground(Color.black);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 3;
    gbl.setConstraints(image, gbc);
    appletContentPane.add(image);

    JLabel label = new JLabel("Ganymede Network Management System");
    gbc.gridy = 1;
    gbl.setConstraints(label, gbc);
    appletContentPane.add(label);

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
    gbl.setConstraints(userL, gbc);
    appletContentPane.add(userL);

    username = new JTextField(20);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx = 1;
    gbl.setConstraints(username, gbc);
    appletContentPane.add(username);
    
    JLabel passL = new JLabel("Password:");
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 1;
    gbl.setConstraints(passL, gbc);
    appletContentPane.add(passL);

    passwd = new JPasswordField(20);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx = 1;
    gbl.setConstraints(passwd, gbc);
    appletContentPane.add(passwd);

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
    gbc.fill = GridBagConstraints.BOTH;
    gbl.setConstraints(buttonPanel, gbc);
    appletContentPane.add(buttonPanel);

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
   * Set the cursor to the normal cursor(usually a pointer.)
   *
   * This is dependent on the operating system.
   */

  public void setNormalCursor()
  {
    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }

  /**
   *
   * Handle button clicks, and enter being hit in the password
   * field.
   *
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

  public void messageReceived(ClientEvent e)
  {
    if (debug)
      {
	System.out.println(e.getMessage());
      }

    JErrorDialog d = new JErrorDialog(new JFrame(), e.getMessage());
  }

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
   *
   * This method causes the DeathWatcherThread to kick off the
   * end-of-the-world process.
   * 
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

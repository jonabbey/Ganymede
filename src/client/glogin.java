/*
   glogin.java

   Ganymede client login module

   This client has been developed so that it can run as both an applet,
   as well as an application.

   --

   Created: 22 Jan 1997
   Version: $Revision: 1.37 $ %D%
   Module By: Navin Manohar and Mike Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/
package arlut.csd.ganymede.client;

import com.sun.java.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;
import java.util.*;

import jdj.*;

import arlut.csd.JDialog.*;
import arlut.csd.ganymede.*;
import arlut.csd.Util.ParseArgs;


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

  private GridBagLayout gbl;
  private GridBagConstraints gbc;

  protected gclient g_client;

  protected Image ganymede_logo;
  protected TextField username;
  protected TextField passwd;
  protected JButton connector;
  protected JButton _quitButton;
  protected JPanel bPanel;
  protected static JFrame my_frame = null;

  protected static Server  my_server;

  protected static ClientBase my_client;

  protected static Session my_session;
  protected static String my_username,my_passwd;

  protected static glogin my_glogin;

  protected Thread my_thread = new Thread(this);

  protected boolean connected = false;

  private static boolean WeAreApplet = true;

  private static Container appletContentPane = null;

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
   *
   */

  public void init() 
  {
    System.out.println("init in glogin");

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

    JLabel userL = new JLabel("Username:");
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridy = 2;
    gbc.gridwidth = 1;
    gbl.setConstraints(userL, gbc);
    appletContentPane.add(userL);

    username = new TextField(20);
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

    passwd = new TextField(20);
    passwd.setEchoChar('*');
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx = 1;
    gbl.setConstraints(passwd, gbc);
    appletContentPane.add(passwd);
    
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
	System.setSecurityManager(new RMISecurityManager());
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
	    my_client = new ClientBase(server_url, this);  //Exception will happen here
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
   * Logout fromt the server. 
   *
   * This is called from the gclient.
   */
  public void logout() throws RemoteException
  {
    my_client.disconnect();
    enableButtons(true);
  }

  public void stop() 
  {
    // If the applet is no longer visible on the page, we exit.
    
    try 
      {
	if (my_glogin.my_session != null)
	  {
	    my_glogin.my_session.logout();
	  }
      }
    catch (RemoteException ex) 
      {
      }
    
    //System.exit(1);
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
	String pword = passwd.getText();

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
	    //This means that the user was not able to log into the server properly.
	    
	    // We re-enable the "Login to server" button so that the user can try again.
	    connector.setEnabled(true);
	    _quitButton.setEnabled(true);
	    // Why is this line here? I'm commenting it out.
	    //connector.setEnabled(false);
	  }
	setNormalCursor();
      }
    else if (e.getSource() == _quitButton)
      {
	System.exit(1);
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
    if (debug)
      {
	System.out.println(e.getMessage());
      }

    JErrorDialog d = new JErrorDialog(new JFrame(), e.getMessage());
  }

}


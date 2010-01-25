/*
   glogin.java

   Ganymede client login module

   This client has been developed so that it can run as both an applet,
   as well as an application.

   Created: 22 Jan 1997

   Module By: Navin Manohar, Mike Mulvaney, and Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
   The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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

package arlut.csd.ganymede.client;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.EventQueue;
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
import java.rmi.RemoteException;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import arlut.csd.JDialog.JErrorDialog;
import arlut.csd.JDialog.StandardDialog;
import arlut.csd.JDialog.StringDialog;
import arlut.csd.Util.booleanSemaphore;
import arlut.csd.Util.PackageResources;
import arlut.csd.Util.ParseArgs;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.ClientMessage;
import arlut.csd.ganymede.rmi.Server;
import arlut.csd.ganymede.rmi.Session;

/*------------------------------------------------------------------------------
                                                                           class
                                                                          glogin

------------------------------------------------------------------------------*/

/**
 * Ganymede client start class.  This class can be run from the command
 * line via its static main() method, or as an applet loaded into a
 * web browser, generally with Sun's Java plug-in.
 *
 * This class has a run() method for attempting to connect to
 * the server in the background once the applet is initialized.
 *
 * Once glogin handles the user's login, a {@link arlut.csd.ganymede.client.gclient gclient}
 * object is constructed, which handles all of the user's interactions with the server.
 *
 * @version $Id$
 * @author Navin Manohar, Mike Mulvaney, and Jonathan Abbey
 */

public class glogin extends JApplet implements Runnable, ActionListener, ClientListener {

  public static boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.glogin");

  /**
   * If this boolean is set to true, when the Ganymede client is
   * run as an application, the login box will hide itself away when
   * the client's main frame is up.
   *
   * Unfortunately, I don't think that it's generally possible to
   * duplicate this sort of behavior when running Ganymede as an
   * applet, so it may be confusing to some to enable this behavior.
   *
   * I'm enabling it. - JDA 29 September 2005
   */

  public static final boolean hideLoginWhenApplication = true;

  public static String 
    properties_file = null,
    serverhost = null,
    server_url = null,
    helpBase = null;
  
  public static Integer
    registryPort = null;

  /**
   * The default registry port if we don't have one specified in a
   * property file or in an applet parameter.
   */

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

  protected static String active_username, active_passwd;

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

  /**
   * Background thread to handle force disconnect commands from the server.
   * We need this thread because jdk 1.2 has a bug where RMI callbacks are
   * not privileged to interact with the Swing thread.  By creating a thread
   * to handle forced logouts ourselves, we can have an RMI callback pass
   * a message to this thread (which has local privileges), which can then
   * throw up a dialog explaining about being disconnected, etc.
   */

  protected static DeathWatcherThread deathThread;

  /**
   * If true, we're running on a mac, and we might tweak our interface
   * a bit to make things look better.
   */

  private static Boolean runningOnMac = null;

  public static boolean isRunningOnMac()
  {
    if (runningOnMac == null)
      {
	runningOnMac = "Mac OS X".equals(System.getProperty("os.name"));
      }

    return runningOnMac;
  }

  // ---

  /**
   * Background thread used to attempt to get the initial RMI connection to the
   * Ganymede server.
   */

  protected Thread my_thread = new Thread(this);

  /**
   * The server will send us a login count message before we've got
   * the client ready to receive and display it, so we'll need to
   * retain the count for the client to consult on startup.
   */

  private int initialLoginCount;

  private GridBagLayout gbl;
  private GridBagConstraints gbc;

  Image errorImage = null;

  protected Image ganymede_logo;
  protected Image ganymede_ssl_logo;
  protected JTextField username;
  protected JPasswordField passwd;
  protected JButton connector;
  protected JButton _quitButton;
  protected JPanel bPanel;

  char spinAry[] = {'/','-', '\\', '|'};
  int spindex = 0;

  String connectError = null;

  private booleanSemaphore connected = new booleanSemaphore(false);
  private booleanSemaphore connecting = new booleanSemaphore(false);
  private boolean ssl = false;

  private JLabel image = null;

  /* -- */

  /**
   * This main() function will allow this applet to run as an application
   * when it is not executed in the context of a browser.  
   */

  public static void main (String args[])
  {
    WeAreApplet = false;

    debug = ParseArgs.switchExists("-debug", args);

    properties_file = ParseArgs.getArg("properties", args);

    // make sure that we start the Ganymede client on the GUI thread

    SwingUtilities.invokeLater(new Runnable() {
        public void run()
        {
          my_glogin = new glogin();

          my_frame = new gloginFrame(ts.l("main.frame_name"),	// "Ganymede Client"
                                     my_glogin);

          my_frame.getContentPane().setLayout(new BorderLayout());
          my_frame.getContentPane().add("Center", my_glogin);

          my_glogin.init();		// init before we setVisible(), so startup is smoother

          my_frame.pack();		// pack so that we fit everything properly

          my_frame.setLocationRelativeTo(null); // center on the screen, please

          my_frame.setVisible(true);
        }
      });
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
   * Returns a configuration String from a property file or applet
   * parameter element, depending on whether the Ganymede client is
   * being run as an application or as an applet.
   *
   * If glogin is being run as an application, the static variable
   * WeAreApplet must be set to false, and properties_file should be
   * set to point to the Ganymede property file on disk before
   * getConfigString() is called.
   *
   * If glogin is being run as an applet, the static variable my_login
   * must be set to point to the singleton glogin object before
   * getConfigString() is called.
   */

  static public String getConfigString(String configKey)
  {
    if (WeAreApplet)
      {
	return my_glogin.getParameter(configKey);
      }
    else
      {
	if (properties_file != null)
	  {
	    if (ganymedeProperties == null)
	      {
		ganymedeProperties = new Properties();
	
		if (debug)
		  {
		    System.out.println("Loading properties from: " + properties_file);
		  }

		if (properties_file != null)
		  {
		    BufferedInputStream bis = null;

		    try
		      {
			bis = new BufferedInputStream(new FileInputStream(properties_file));
			ganymedeProperties.load(bis);
		      }
		    catch (java.io.FileNotFoundException e)
		      {
			throw new RuntimeException("File not found: " + e);
		      }
		    catch (java.io.IOException e)
		      {
			throw new RuntimeException("Whoa, io exception: " + e);
		      }
		    finally
		      {
			if (bis != null)
			  {
			    try
			      {
				bis.close();
			      }
			    catch (java.io.IOException ex)
			      {
			      }
			  }
		      }
		  }
	      }

	    return ganymedeProperties.getProperty(configKey);
	  }
	else
	  {
	    return java.lang.System.getProperty(configKey);
	  }
      }
  }

  /**
   * Returns a configuration Integer from a property file or applet
   * parameter element, depending on whether the Ganymede client is
   * being run as an application or as an applet.
   *
   * If glogin is being run as an application, the static variable
   * WeAreApplet must be set to false, and properties_file should be
   * set to point to the Ganymede property file on disk before
   * getConfigInteger() is called.
   *
   * If glogin is being run as an applet, the static variable my_login
   * must be set to point to the singleton glogin object before
   * getConfigInteger() is called.
   *
   * @throws NumberFormatException if the config value returned for
   * configKey is not a number.
   */

  static public Integer getConfigInteger(String configKey)
  {
    return java.lang.Integer.parseInt(getConfigString(configKey));
  }

  /**
   * Returns a configuration boolean from a property file or applet
   * parameter element, depending on whether the Ganymede client is
   * being run as an application or as an applet.
   *
   * If glogin is being run as an application, the static variable
   * WeAreApplet must be set to false, and properties_file should be
   * set to point to the Ganymede property file on disk before
   * getConfigBoolean() is called.
   *
   * If glogin is being run as an applet, the static variable my_login
   * must be set to point to the singleton glogin object before
   * getConfigBoolean() is called.
   *
   * @returns defaultValue if there is no property or applet parameter
   * matching configKey, else returns true if the property/parameter
   * for configKey is equal to "true".
   */

  static public boolean getConfigBoolean(String configKey, boolean defaultValue)
  {
    String val = getConfigString(configKey);

    if (val == null)
      {
	return defaultValue;
      }
    else
      {
	if (val.equals("true"))
	  {
	    return true;
	  }
	else
	  {
	    return false;
	  }
      }
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

    // Look up our saved look and feel, if any

    gclient.sizer.restoreLookAndFeel();

    // Retrieve the ganymede logo using the appropriate method
    
    ganymede_logo = PackageResources.getImageResource(this, "ganymede.jpg", getClass());
    ganymede_ssl_logo = PackageResources.getImageResource(this, "ssl_ganymede.jpg", getClass());
   
    if (WeAreApplet)
      {
	my_glogin = this;
	my_frame = new JFrame(); // we need an invisible frame to attach pop-up dialogs to
      }

    serverhost = getConfigString("ganymede.serverhost");
    registryPort = getConfigInteger("ganymede.registryPort");
    server_url = "rmi://" + serverhost + ":" + registryPortProperty + "/ganymede.server";

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add("Center", createLoginPanel());

    // The Login GUI has been set up.  Now the server connection needs
    // to be properly established.
    
    my_client = new ClientBase(server_url, this);
    
    /* Spawn a thread to get connected to the server, using the
     * ClientBase we just created */

    my_thread.setPriority(Thread.NORM_PRIORITY);
    my_thread.start();
  }

  public JPanel createLoginPanel()
  {
    JPanel loginBox = new JPanel();

    loginBox.setBorder(BorderFactory.createEtchedBorder());

    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();

    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridheight = 1;

    loginBox.setLayout(gbl);

    image = new JLabel(new ImageIcon(ganymede_logo));
    image.setOpaque(true);
    image.setBackground(Color.black);

    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weighty = 1.0;
    gbc.weightx = 1.0;
    gbl.setConstraints(image, gbc);
    loginBox.add(image);

    JPanel labelPanel = new JPanel();
    labelPanel.setLayout(new BorderLayout());

    // "Ganymede Server on:"
    JLabel label = new JLabel(ts.l("createLoginPanel.server_label1"));
    labelPanel.add("North", label);

    // "{0}, port {1,number,#}"
    JLabel hostLabel = new JLabel(ts.l("createLoginPanel.server_label2", 
				       serverhost, 
				       Integer.valueOf(registryPortProperty)));
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
    loginBox.add(labelPanel);

    // the username and passwd fields here won't have their
    // callback set with addTextListener().. instead, we'll
    // trap the login/quit buttons, and query these
    // fields when we process the buttons.

    gbc.ipady = 4;

    // "Username:"
    JLabel userL = new JLabel(ts.l("createLoginPanel.username"));
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.gridwidth = 1;
    gbc.weightx = 0.0;
    gbl.setConstraints(userL, gbc);
    loginBox.add(userL);

    username = new JTextField(15);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx = 1;
    gbc.weightx = 1.0;
    gbl.setConstraints(username, gbc);
    username.setEnabled(false);
    loginBox.add(username);
    
    // "Password:"
    JLabel passL = new JLabel(ts.l("createLoginPanel.password"));
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 1;
    gbc.weightx = 0.0;
    gbl.setConstraints(passL, gbc);
    loginBox.add(passL);

    passwd = new JPasswordField(15);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx = 1;
    gbc.weightx = 1.0;
    gbl.setConstraints(passwd, gbc);
    passwd.setEnabled(false);
    loginBox.add(passwd);

    gbc.ipady = 0;

    // "Quit"    
    _quitButton = new JButton(ts.l("createLoginPanel.quitButton"));

    // "Connecting... {0}"
    connector = new JButton(ts.l("global.connecting_text",
				 Character.valueOf(spinAry[spindex])));
    connector.setOpaque(true);
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

    return loginBox;
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
	while (!connected.isSet())
	  {
	    if (try_number++ > 20)
	      {
		break;
	      }

	    try
	      {
		EventQueue.invokeAndWait(new Runnable()
		  {
		    public void run()
		    {
		      // "Connecting... {0}"
		      connector.setText(ts.l("global.connecting_text",
					     Character.valueOf(spinAry[spindex])));
		    }
		  });
	      }
	    catch (Exception ex)
	      {
		ex.printStackTrace();
	      }
	    
	    try
	      {
		my_client.connect();	// exceptions ahoy!

		connected.set(true);
		break;
	      }
	    catch (Throwable ex)
	      {
		connectError = ex.getMessage();

		if (debug)
		  {
		    ex.printStackTrace();
		  }
	      }

	    try 
	      {
		spindex++;
		
		if (spindex >= spinAry.length)
		  {
		    spindex = 0;
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
	    if (my_client.isSSLEnabled() && !ssl)
	      {
		ssl = true;
		image.setIcon(new ImageIcon(ganymede_ssl_logo));
	      }

	    EventQueue.invokeLater(new Runnable() {
		public void run() {
		  if (ssl)
		    {
		      // "Login to server"
		      connector.setText(ts.l("run.login_ssl"));
		    }
		  else
		    {
		      // "Login to server (NO SSL)"
		      connector.setText(ts.l("run.login_nossl"));
		    }

		  enableButtons(true);
		  connector.paintImmediately(connector.getVisibleRect());
		  setNormalCursor();
		
		  username.setEnabled(true);
		  passwd.setEnabled(true);
		  username.paintImmediately(username.getVisibleRect());
		  passwd.paintImmediately(passwd.getVisibleRect());
		  username.requestFocus();
		
		  invalidate();
		  validate();
		}
	      });
	  }
	else
	  {
	    // "Login Error"
	    // "Couldn''t locate Ganymede server.  Perhaps it is down?\n\n{0}"
	    // "OK"
	    new StringDialog(my_frame,
			     ts.l("run.login_error"), 
			     ts.l("run.login_error_text", connectError),
			     ts.l("global.ok"),
			     null,
			     getErrorImage(), StandardDialog.ModalityType.DOCUMENT_MODAL).showDialog();

	    EventQueue.invokeLater(new Runnable() 
	      {
		public void run()
		{
		  // "Connect"
		  connector.setText(ts.l("global.connect_text"));
		  username.setEnabled(false);
		  passwd.setEnabled(false);
		  
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

  /**
   * Logout from the server. 
   *
   * This is called from the gclient.
   */

  public void logout()
  {
    this.logout(false);
  }

  /**
   * Logout from the server. 
   *
   * This is called from the gclient.
   */

  public void logout(boolean andQuit)
  {
    try
      {
	my_client.disconnect();
      }
    catch (Exception ex)
      {
	if (glogin.g_client != null)
	  {
	    glogin.g_client.processException(ex);
	  }
	else
	  {
	    ex.printStackTrace();
	  }
      }
    finally
      {
	// clean everything up on the gui thread

	final boolean myAndQuit = andQuit;

	try
	  {
	    EventQueue.invokeLater(new Runnable() {
	      public void run() {
		gclient x = null;

		synchronized (glogin.class)
		  {
		    if (glogin.g_client != null)
		      {
			x = glogin.g_client;
			glogin.g_client = null;
		      }
		  }

		if (x != null)
		  {
		    x.setVisible(false);
		    x.dispose();
		    x.cleanUp();
		  }

		if (myAndQuit)
		  {
		    System.exit(0);
		  }
	      }
	    });
	  }
	catch (NullPointerException ex)
	  {
	  }

	showLoginBox();
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
   * Set the cursor to the normal cursor(usually a pointer).
   *
   * This is dependent on the operating system.
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

	try
	  {
	    if (!my_client.isConnected())
	      {
		if (connecting.isSet())
		  {
		    return;		// our connection thread is still trying to connect
		  }
		else
		  {
		    // looks like the ClientBase object lost connection to
		    // the RMI server.. let's try to re-acquire.

		    Thread reconnectThread = new Thread(this);
		    reconnectThread.setPriority(Thread.NORM_PRIORITY);
		    reconnectThread.start();

		    return;
		  }
	      }

	    String uname = username.getText().trim();
	    String pword = new String(passwd.getPassword());

	    my_passwd = pword;
            active_passwd = pword;
	    my_session = null;
	
	    try
	      {
		my_session = my_client.login(uname, pword);
	      }
	    catch (Exception ex)
	      {
		// "Couldn''t log into server: \n{0}"
		new JErrorDialog(my_frame,
				 ts.l("actionPerformed.login_failure", ex.getMessage()),
				 getErrorImage(), StandardDialog.ModalityType.DOCUMENT_MODAL);
	    
		enableButtons(true);
	      }
	    
	    if (my_session != null) 
	      {
		// we need to look up our real username from the
		// server, since we might have been logged in using a
		// composite username:persona string.

                active_username = uname;

		try
		  {
		    my_username = my_session.getMyUserName();
		  }
		catch (Exception ex)
		  {
		  }

		enableButtons(false);
		startSession(my_session);
	      }
	    else 
	      {
		// This means that the user was not able to log into the server properly.

		if (!my_client.isConnected() && !connecting.isSet())
		  {
		    // looks like the ClientBase object lost connection to
		    // the RMI server.. let's try to re-acquire.

		    Thread reconnectThread = new Thread(this);
		    reconnectThread.setPriority(Thread.NORM_PRIORITY);
		    reconnectThread.start();

		    return;
		  }
		else
		  {
		    // We are connected to the server.. bad password?
		    // re-enable the "Login to server" button so that the
		    // user can try again.

		    enableButtons(true);
		  }
	      }
	  }
	finally
	  {
	    setNormalCursor();
	  }
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
    catch (Exception ex)
      {
	ex.printStackTrace();
	glogin.helpBase = null;
      }

    // create the thread in our thread group that the disconnected()
    // method will use to knock us down.

    deathThread = new DeathWatcherThread();
    deathThread.setPriority(Thread.NORM_PRIORITY);
    deathThread.start();

    // and pop up everything

    g_client = new gclient(session,this);

    passwd.setText("");

    // and let's play hide the salami

    hideLoginBox();

    // now that we've got the g_client reference DeathWatcherThread
    // will need, have the client do its post-setup initialization,
    // including perhaps blocking on the persona dialog.
    
    g_client.start();
  }

  public void hideLoginBox()
  {
    if (hideLoginWhenApplication)
      {
	if (!isApplet() && my_frame != null)
	  {
	    my_frame.setVisible(false);
	  }
      }
  }

  public void showLoginBox()
  {
    if (hideLoginWhenApplication)
      {
	if (!isApplet() && my_frame != null)
	  {
	    my_frame.setVisible(true);
            passwd.setText(""); // clear the passwd field when we return
            passwd.paintImmediately(passwd.getVisibleRect());
	  }
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

    // constructing a JErrorDialog causes it to be shown.

    if (e.getType() == ClientMessage.ERROR)
      {
	new JErrorDialog(my_frame, e.getMessage(), getErrorImage(), StandardDialog.ModalityType.DOCUMENT_MODAL);
      }
    else if (e.getType() == ClientMessage.BUILDSTATUS)
      {
	if (g_client != null)
	  {
	    g_client.setBuildStatus(e.getMessage());
	  }
      }
    else if (e.getType() == ClientMessage.LOGIN ||
             e.getType() == ClientMessage.LOGOUT ||
             e.getType() == ClientMessage.COMMITNOTIFY ||
             e.getType() == ClientMessage.ABORTNOTIFY)
      {
	if (g_client != null)
	  {
	    g_client.setStatus(e.getMessage());
	  }
      }
    else if (e.getType() == ClientMessage.LOGINCOUNT)
      {
	if (g_client != null)
	  {
	    g_client.setLoginCount(Integer.valueOf(e.getMessage()).intValue());
	  }
        else
          {
            this.initialLoginCount = Integer.valueOf(e.getMessage()).intValue();
          }
      }
    else if (e.getType() == ClientMessage.SOFTTIMEOUT)
      {
	if (g_client != null)
	  {
	    g_client.softTimeout();
	  }
      }
  }

  public int getInitialLoginCount()
  {
    return this.initialLoginCount;
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

  /**
   * Loads and returns the error Image for use in client dialogs.
   * 
   * Once the image is loaded, it is cached for future calls to 
   * getErrorImage().
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
                                                              DeathWatcherThread

------------------------------------------------------------------------------*/

/** 
 * Client-side background thread to handle force disconnect commands
 * from the server.  We need this thread because jdk 1.2 has a bug
 * where RMI callbacks are not privileged to interact with the Swing
 * thread.  By creating a thread to handle forced logouts ourselves,
 * we can have an RMI callback pass a message to this thread (which
 * has local privileges), which can then throw up a dialog explaining
 * about being disconnected, etc.
 *
 * When run, this thread waits for die() to be called, whereupon it
 * creates an {@link arlut.csd.ganymede.client.ExitThread ExitThread} to
 * actually shut down the client.
 *
 * @version $Id$
 * @author Jonathan Abbey
 */

class DeathWatcherThread extends Thread {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.DeathWatcherThread");

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

    // if the user was stuck at the modal persona selection dialog,
    // close it so that we can put our own error message up.

    try
      {
	glogin.g_client.getPersonaDialog().changedOK = true;
	glogin.g_client.getPersonaDialog().setHidden(true);
      }
    catch (NullPointerException ex)
      {
      }

    ExitThread exitThread = new ExitThread(message);

    // start up the death timer, which will close all our active
    // windows in a bit.. we hold off on just doing it now to not
    // startle the user too much.

    exitThread.setPriority(Thread.NORM_PRIORITY);
    exitThread.start();

    // throw up a modal dialog to get the user's attention

    // "The Ganymede Server is disconnecting us:\n\n{0} "
    new JErrorDialog(glogin.g_client,
		     ts.l("run.kicked_off", message),
		     glogin.g_client.getErrorImage(), StandardDialog.ModalityType.DOCUMENT_MODAL);

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
 * Client-side self-destruction thread.  This thread will be created
 * and run when the server sends the client's {@link
 * arlut.csd.ganymede.client.ClientBase ClientBase} a forced
 * disconnect RMI call.  When run, this thread starts a 30 second
 * timer, while the {@link
 * arlut.csd.ganymede.client.DeathWatcherThread DeathWatcherThread}
 * shows a dialog to the user, explaining the disconnect.  The user
 * can click ok on that dialog, causing this thread's dieNow() method
 * to terminate the timer.  In any case, when the timer counts down to
 * zero, the glogin's logout() method will be called, and the client's
 * main window will be shutdown.
 *
 * @version $Id$
 * @author Jonathan Abbey
 */

class ExitThread extends Thread {

  final static boolean debug = false;

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

    if (debug)
      {
	System.out.print("System shutting down in 30 seconds");
      }

    try
      {
	while (!dieNow && i > 0)
	  {
	    sleep(1000);

	    if (debug)
	      {
		System.out.print(".");
	      }

	    i--;
	  }
      }
    catch (InterruptedException ie)
      {
	if (debug)
	  {
	    System.out.println("glogin: Interupted trying to sleep and quit: " + ie);
	  }
      }

    if (debug)
      {
	System.out.println("\nGanymede disconnected: " + message);
      }

    glogin.my_glogin.logout();
  }

  public void dieNow()
  {
    this.dieNow = true;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
							             gloginFrame

------------------------------------------------------------------------------*/

/**
 * JFrame subclass which is used to hold the {@link
 * arlut.csd.ganymede.client.glogin glogin} applet when the Ganymede
 * client is run as an application rather than an applet.
 */

class gloginFrame extends JFrame {

  static final boolean debug = false;
  glogin client;

  /* -- */
  
  public gloginFrame(String title, glogin client)
  {
    super(title);
    this.client = client;
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

	if (client._quitButton.isEnabled())
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

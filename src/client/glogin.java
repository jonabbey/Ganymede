/*
   glogin.java

   Ganymede client login module

   This client has been developed so that it can run as both an applet,
   as well as an application.

   --

   Created: 22 Jan 1997
   Version: $Revision: 1.21 $ %D%
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

import jdj.*;

import arlut.csd.JDialog.*;
import arlut.csd.ganymede.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                          glogin

------------------------------------------------------------------------------*/

public class glogin extends JApplet implements Runnable {

  private GridBagLayout gbl;
  private GridBagConstraints gbc;

  protected Image ganymede_logo;
  protected JTextField username;
  protected JPasswordField passwd;
  protected JButton connector;
  protected JButton _quitButton;
  protected JPanel bPanel;
  protected static JFrame my_frame = null;

  protected static Server  my_server;

  protected static iClient my_client;

  protected static Session my_session;
  protected static String my_username,my_passwd;

  protected static glogin my_glogin;

  private LoginHandler _loginHandler;

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

    my_glogin = new glogin();

    my_glogin.setLayout(new BorderLayout());

    my_frame = new JFrame("Ganymede Client");

    appletContentPane = my_frame.getContentPane();

    appletContentPane.setLayout(new BorderLayout());
   
    appletContentPane.add(my_glogin,"Center");

    my_frame.pack();
    my_frame.setSize(265,380);    
    my_frame.show();
 
    my_glogin.init();
    my_glogin.start();
    my_glogin.getLayout().layoutContainer(my_glogin);
  }

  /**
   *
   *
   */

  public void init() 
  {
    System.out.println("init in glogin");

    try
      {
	my_glogin = this;
      
	// Dowload the ganymede logo using the appropriate method

	if (!WeAreApplet)
	  {
	    //	    ganymede_logo = Toolkit.getDefaultToolkit().getImage(new URL(gConfig._GANYMEDE_LOGO_URL));
	    ganymede_logo = PackageResources.getImageResource(this, "ganymede.jpg", getClass());
	  }
	else
	  {
	    ganymede_logo = getImage(new URL(gConfig._GANYMEDE_LOGO_URL));

	    my_frame = new JFrame();
	  }
      }
    catch (java.net.MalformedURLException e) 
      {
	System.out.println("The URL was malformed");
      }
   
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
    
    username.setEnabled(false);
    username.setText("supergash");
    passwd.setEnabled(false);

    _quitButton = new JButton("Quit");
    _quitButton.setBackground(ClientColor.buttonBG);
    _loginHandler = new LoginHandler(this);

    connector = new JButton("Connecting...");
    connector.setOpaque(true);
    connector.setBackground(ClientColor.buttonBG);
    connector.addActionListener(_loginHandler);

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

    passwd.addActionListener(_loginHandler);
    username.addActionListener(_loginHandler);

    _quitButton.addActionListener(_loginHandler);

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

    int state = 0;
      
    do {

      try
	{
	  connected = true;

	  Remote obj = Naming.lookup(gConfig._GANYMEDE_SERVER_URL);
	  
	  if (obj instanceof Server)
	    {
	      my_server = (Server) obj;
	    }
	}
      catch (NotBoundException ex)
	{
	  connected = false;

	  //System.err.println("RMI: Couldn't bind to server object\n" + ex );
	}
      catch (java.rmi.UnknownHostException ex)
	{
	  connected = false;

	  //System.err.println("RMI: Couldn't find server\n" + gConfig._GANYMEDE_SERVER_URL );
	}
      catch (RemoteException ex)
	{
	  connected = false;
	  //ex.printStackTrace();

	  //	  System.err.println("RMI: RemoteException during lookup.\n" + ex);
	}
      catch (java.net.MalformedURLException ex)
	{
	  connected = false;
	  	  
	  //System.err.println("RMI: Malformed URL " + gConfig._GANYMEDE_SERVER_URL );
	}

      switch (state) 
	{
        case 0: 
	  connector.setText("Connecting... |");
	  state++;
	  break;

	case 1:
	  connector.setText("Connecting...  /");
	  state++;
	  break;

	case 2:
	  connector.setText("Connecting...  -");
	  state++;
	  break;

	case 3: 
	  connector.setText("Connecting... \\");
	  state = 0;
	  break;
	}

      try 
	{
	  // Wait for 1 sec before retrying to connect to server
	  Thread.sleep(1000);
	}
      catch (InterruptedException e) 
	{
	}

    } while (!connected);

    // At this point, a connection to the server has been established,
    // So we allow the "Login to Server" button to be visible.


    connector.setText("Login to server");
    username.setEnabled(true);
    passwd.setEnabled(true);
    passwd.requestFocus();

    invalidate();
    validate();
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

    System.exit(1);
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
}  


/**
 *
 *
 *
 */
class LoginHandler implements ActionListener {

  protected glogin my_glogin;
  
  /**
   * Constructor
   */
  public LoginHandler(glogin _glogin) 
  {
    super();

    if (_glogin == null)
      {
	throw new IllegalArgumentException("LoginHandler Constructor: _glogin is null");
      }

    my_glogin = _glogin;
  }

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == my_glogin.username)
      {
	my_glogin.passwd.requestFocus();
      }
    else if (e.getSource() == my_glogin.passwd)
      {
	my_glogin.connector.doClick();
      }
    
    else if (e.getSource() == my_glogin.connector)
      {
	String uname = my_glogin.username.getText().trim();
	String pword = my_glogin.passwd.getText();

	my_glogin.my_username = uname;
	my_glogin.my_passwd = pword;
	
	try
	  {
	    my_glogin.my_client = new iClient(my_glogin, my_glogin.my_server, uname, pword);
	  }
	catch (RemoteException ex)
	  {
	    JErrorDialog d = new JErrorDialog(my_glogin.my_frame, "RMI Error: Couldn't log into server: \n" + ex.getMessage());
	    
	    my_glogin.connector.setEnabled(true);
	    my_glogin._quitButton.setEnabled(true);


	    return;
	  }
	catch (NullPointerException ex)
	  {

	    JErrorDialog d = new JErrorDialog(my_glogin.my_frame, "Error: Didn't get server reference.  Please Quit and Restart");
	    
	    my_glogin.connector.setEnabled(true);
	    my_glogin._quitButton.setEnabled(true);
	    
	    return;
	  }
	catch (Exception ex) 
	  {
	    JErrorDialog d = new JErrorDialog(my_glogin.my_frame, "Error: " + ex.getMessage());
	    	    
	    my_glogin.connector.setEnabled(true);
	    my_glogin._quitButton.setEnabled(true);

	    return;
	  }

	my_glogin.my_session = my_glogin.my_client.session;

	my_glogin.connector.setEnabled(false);
	my_glogin._quitButton.setEnabled(false);

	if (my_glogin.my_session != null) 
	  {
	    startSession(my_glogin.my_session);
	  }
	else 
	  {
	    //This means that the user was not able to log into the server properly.
	    
	    // We re-enable the "Login to server" button so that the user can try again.
	    my_glogin.connector.setEnabled(true);
	    my_glogin._quitButton.setEnabled(true);
	    // Why is this line here? I'm commenting it out.
	    //my_glogin.connector.setEnabled(false);
	  }
      }
    else if (e.getSource() == my_glogin._quitButton)
      {
	System.exit(1);
      }
  }

  public void startSession(Session s) 
  {
    if (s == null)
      {
	throw new IllegalArgumentException("Ganymede Error: Parameter for Session s is null");;
      }

    Session _session = s;

    gclient _client = new gclient(_session,my_glogin);

    my_glogin.passwd.setText("");
    /* At this point, all the login matters have been handled and we have
       a Session object in our hands.  We now instantiate the main client
       that will be used to interact with the Ganymede server.*/

    try 
      {
	// This will get the ball rolling.

	_client.start();

      }
    catch (Exception e)
      {
	// Any exception thrown by glclient will be handled here.
	System.err.println("Error starting client: " + e);
      }
  }
}


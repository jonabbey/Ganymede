/*
   xmlclient.java

   This is a text client for the Ganymede server.  This client is
   designed to take the filename for an XML file on the command line,
   load the file, parse it, then connect to the server and transfer
   the file to the server for server-side integration into the Ganymede
   database.

   --

   Created: 2 May 2000
   Version: $Revision: 1.34 $
   Last Mod Date: $Date: 2000/11/24 03:23:43 $
   Release: $Name:  $

   Module By: Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.  

*/

package arlut.csd.ganymede.client;

import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;
import java.util.*;

import arlut.csd.ganymede.*;
import arlut.csd.Util.*;

import org.xml.sax.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       xmlclient

------------------------------------------------------------------------------*/

/**
 * <p>This is a text client for the Ganymede server.  This client is
 * designed to take the filename for an XML file on the command line,
 * load the file, parse it, then connect to the server and transfer
 * the file to the server for server-side integration into the Ganymede
 * database.</p>
 *
 * @version $Revision: 1.34 $ $Date: 2000/11/24 03:23:43 $ $Name:  $
 * @author Jonathan Abbey
 */

public final class xmlclient implements ClientListener {

  public static final boolean debug = false;

  /**
   * <p>This major version number is compared with the "major"
   * attribute in the Ganymede XML document element.  xmlclient won't
   * try to read Ganymede XML files whose major number is too high</p>
   */

  public static final int majorVersion = 1;

  // ---

  public String serverHostProperty = null;
  public int    registryPortProperty = 1099;
  public String server_url = null;
  public String propFilename = null;
  public String xmlFilename = null;
  public String username = null;
  public String password = null;
  private boolean dumpSchema = false;
  private boolean dumpData = false;
  private boolean doTest = false;
  private boolean schemaOnly = false;

  /**
   * RMI reference to a Ganymede server
   */

  private Server server = null;

  /**
   * <p>Remote session interface to the Ganymede server, used while
   * loading data objects into the server.</p>
   */

  public XMLSession session = null;

  /**
   * <p>The default buffer size in the {@link arlut.csd.Util.XMLReader XMLReader}.
   * This value determines how far ahead the XMLReader's i/o thread can get in
   * reading from the XML file.  Higher or lower values of this variable may
   * give better performance, depending on the characteristics of the JVM with
   * regards threading, etc.</p>
   */

  public int bufferSize = 20;

  /**
   * <p>Streaming XML reader.  xmlclient creates one of these on startup,
   * and from that point on, all XML reading is done through this
   * object.</p>
   */

  public XMLReader reader = null;

  // ---

  /**
   *
   * main
   *
   */

  public static void main(String argv[])
  {
    xmlclient xc = new xmlclient(argv);

    try
      {
	if (xc.dumpSchema)
	  {
	    if (xc.doXMLDump(true, false))
	      {
		System.exit(0);
	      }
	    else
	      {
		System.exit(1);
	      }
	  }

	if (xc.dumpData)
	  {
	    if (xc.doXMLDump(true, true))
	      {
		System.exit(0);
	      }
	    else
	      {
		System.exit(1);
	      }
	  }

	if (xc.doTest)
	  {
	    xc.runTest();
	    System.exit(0);
	  }

	if (xc.doEverything(true))
	  {
	    System.exit(0);
	  }
	else
	  {
	    System.err.println("\nXML submission failed.");
	  }
      }
    catch (RemoteException ex)
      {
	ex.printStackTrace();
      }
    catch (IOException ex)
      {
	ex.printStackTrace();
      }
    finally
      {
	System.exit(1);
      }
  }

  /**
   * <p>This constructor takes care of parsing the command line arguments
   * for xmlclient when run from the command line.</p>
   */  

  public xmlclient(String argv[])
  {
    boolean ok = true;
    File xmlFile = null;

    /* -- */

    // find the properties command line argument

    propFilename = ParseArgs.getArg("properties", argv);

    if (propFilename == null)
      {
	System.err.println("Ganymede xmlclient: Error, must specify properties");
 	System.err.println("Usage: java arlut.csd.ganymede.client.xmlclient properties=<properties file> [username=<username>] [password=<password>] [bufsize=<buffer size>] <xmlfile>");
	System.exit(1);
      }
    else
      {
	ok = loadProperties(propFilename);
      }

    username = ParseArgs.getArg("username", argv);

    if (username == null)
      {
	username = "supergash";
      }

    password = ParseArgs.getArg("password", argv);
    
    String bufferString = ParseArgs.getArg("bufsize", argv);

    if (bufferString != null)
      {
	try
	  {
	    bufferSize = Integer.parseInt(bufferString);
	  }
	catch (NumberFormatException ex)
	  {
	    System.err.println("Couldn't recognize bufsize argument: " + bufferString);
	    ok = false;
	  }
      }

    if (ParseArgs.switchExists("dumpschema", argv))
      {
	dumpSchema = true;
	return;
      }

    if (ParseArgs.switchExists("dumpdata", argv))
      {
	dumpData = true;
	return;
      }
    
    if (ParseArgs.switchExists("test", argv))
      {
	doTest = true;
      }

    xmlFilename = argv[argv.length-1];

    xmlFile = new File(xmlFilename);

    if (!xmlFile.exists())
      {
	System.err.println("Ganymede xmlclient: Error, file " + xmlFilename + " does not exist");
	ok = false;
      }

    if (!ok)
      {
 	System.err.println("Usage: xmlclient [username=<username>] [password=<password>] [bufsize=<buffer size>] <xmlfile>");
	System.exit(1);
      }
  }

  public boolean doXMLDump(boolean commandLine, boolean sendData) throws RemoteException
  {
    // now we should have the username and password if we are going to
    // get them, but do what we can here..

    if (username == null)
      {
	// we would prompt for the username here, but java gives us no
	// portable way to turn character echo on and off.. the script
	// that runs us has character echo off so that we can prompt
	// for the user's password, but since it is off, we can't
	// really prompt for a missing user name here.

	username = "supergash";
      }

    if (password == null)
      {
	if (commandLine)
	  {
	    java.io.BufferedReader in;
	    
	    // get an input stream so we can get the password from the user if we have to
	    
	    in = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
	    
	    try
	      {
		System.err.print("Password for \"" + username + "\":");
		password = in.readLine();
		System.err.println();
	      }
	    catch (java.io.IOException ex)
	      {
		throw new RuntimeException("Exception getting input: " + ex.getMessage());
	      }
	  }
	else
	  {
	    return false;
	  }
      }

    // find the server

    ClientBase client = new ClientBase(server_url, this);

    Session session = client.login(username, password);

    if (session == null)
      {
	System.err.println("Error, couldn't log in to server.. bad username or password?");
	return false;
      }

    // now do what we came for

    ReturnVal retVal;

    if (sendData)
      {
	retVal = session.getDataXML(new FileReceiverBase(new xmlclientPrintReceiver()), true);
      }
    else
      {
	retVal = session.getSchemaXML(new FileReceiverBase(new xmlclientPrintReceiver()), true);
      }

    if (retVal != null && !retVal.didSucceed())
      {
	String errorMessage = retVal.getDialogText();

	if (errorMessage != null)
	  {
	    System.err.println(errorMessage);
	  }
      }

    // and say goodbye

    session.logout();

    return (retVal == null || retVal.didSucceed());
  }

  /**
   * Simple test rig for XMLReader.getNextTree().
   */

  public void runTest()
  {
    try
      {
	reader = new XMLReader(xmlFilename, bufferSize, true); // skip meaningless whitespace

	XMLItem startDocument = getNextItem();

	if (!(startDocument instanceof XMLStartDocument))
	  {
	    System.err.println("XML parser error: first element " + startDocument + 
			       " not XMLStartDocument");
	    return;
	  }

	XMLItem docElement = reader.getNextTree();

	if (!docElement.matches("ganymede"))
	  {
	    System.err.println("Error, " + xmlFilename + " does not contain a Ganymede XML file.");
	    System.err.println("Unrecognized XML element: " + docElement);
	    return;
	  }
	else
	  {
	    docElement.debugPrintTree(0);
	  }
      }
    catch (Exception ex)
      {
	ex.printStackTrace();

	return;
      }
    finally
      {
	if (reader != null)
	  {
	    reader.close();
	  }
      }
  }

  /**
   * <p>Actually do the thing.</p>
   */

  public boolean doEverything(boolean commandLine) throws RemoteException, IOException
  {
    // first, check the basic integrity of the xml file and attempt to
    // find the username and password out of it

    if (!scanXML())
      {
	return false;		// malformed in some way
      }

    // now we should have the username and password if we are going to
    // get them, but do what we can here..

    if (username == null)
      {
	// we would prompt for the username here, but java gives us no
	// portable way to turn character echo on and off.. the script
	// that runs us has character echo off so that we can prompt
	// for the user's password, but since it is off, we can't
	// really prompt for a missing user name here.

	System.err.println("Ganymede xmlclient: Error, must specify ganymede account name in <ganymede> element, or on");
	System.err.println("command line.");
 	System.err.println("Usage: xmlclient [username=<username>] [password=<password>] [bufsize=<buffer size>] <xmlfile>");
	return false;
      }

    if (password == null)
      {
	if (commandLine)
	  {
	    java.io.BufferedReader in;
	    
	    // get an input stream so we can get the password from the user if we have to
	    
	    in = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
	    
	    try
	      {
		System.out.print("Password for \"" + username + "\":");
		password = in.readLine();
	      }
	    catch (java.io.IOException ex)
	      {
		throw new RuntimeException("Exception getting input: " + ex.getMessage());
	      }
	  }
	else
	  {
	    return false;
	  }
      }

    // find the server

    ClientBase client = new ClientBase(server_url, this);

    XMLSession xSession = client.xmlLogin(username, password);

    if (xSession == null)
      {
	System.err.println("Error, couldn't log in to server.. bad username or password?");
	return false;
      }

    System.out.println("Sending XML to server.");

    // logged in!  now we just need to spin through the xml file and
    // send it up to the server

    BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(xmlFilename));

    ReturnVal retVal = null;
    byte[] data = null;
    int oldavail = 0;

    if (schemaOnly)
      {
	String startWrap = "<ganymede>\n";
	data = startWrap.getBytes();

	retVal = xSession.xmlSubmit(data);

	if (retVal != null)
	  {
	    String message = retVal.getDialogText();
	    
	    if (message != null)
	      {
		System.err.println(message);
	      }
	    
	    if (!retVal.didSucceed())
	      {
		xSession.abort();
		return false;
	      }
	  }
      }

    int avail = inStream.available();

    while (avail > 0)
      {
	if (avail > 65536)
	  {
	    avail = 65536;
	  }

	if (oldavail != avail)
	  {
	    data = new byte[avail];
	  }

	inStream.read(data);

	try
	  {
	    retVal = xSession.xmlSubmit(data);

	    if (retVal != null)
	      {
		String message = retVal.getDialogText();

		if (message != null)
		  {
		    System.err.println(message);
		  }

		if (!retVal.didSucceed())
		  {
		    xSession.abort();
		    return false;
		  }
	      }
	  }
	catch (RemoteException ex)
	  {
	    ex.printStackTrace();
	    return false;
	  }

	// and round and round we go

	avail = inStream.available();
      }

    if (schemaOnly)
      {
	String endWrap = "\n</ganymede>";
	data = endWrap.getBytes();

	retVal = xSession.xmlSubmit(data);

	if (retVal != null)
	  {
	    String message = retVal.getDialogText();
	    
	    if (message != null)
	      {
		System.err.println(message);
	      }
	    
	    if (!retVal.didSucceed())
	      {
		xSession.abort();
		return false;
	      }
	  }
      }

    try
      {
	retVal = xSession.xmlEnd();

	if (retVal != null)
	  {
	    String message = retVal.getDialogText();

	    if (message != null)
	      {
		System.err.println(message);
	      }

	    return retVal.didSucceed();
	  }

	// a null ReturnVal signifies a successful result

	return true;
      }
    catch (RemoteException ex)
      {
	ex.printStackTrace();
	return false;
      }
  }

  /**
   * <p>This method handles the actual XML processing, once the
   * command line arguments have been parsed and handled by the
   * xmlclient constructor.</p>
   */

  public boolean scanXML()
  {
    try
      {
	reader = new XMLReader(xmlFilename, bufferSize, true); // skip meaningless whitespace

	XMLItem startDocument = getNextItem();

	if (!(startDocument instanceof XMLStartDocument))
	  {
	    System.err.println("XML parser error: first element " + startDocument + 
			       " not XMLStartDocument");
	    return false;
	  }

	XMLItem docElement = getNextItem();

	if (docElement.matches("ganyschema"))
	  {
	    schemaOnly = true;
	    return true;
	  }
	else if (!docElement.matches("ganymede"))
	  {
	    System.err.println("Error, " + xmlFilename + " does not contain a Ganymede XML file.");
	    System.err.println("Unrecognized XML element: " + docElement);
	    return false;
	  }

	Integer majorI = docElement.getAttrInt("major");
	Integer minorI = docElement.getAttrInt("minor");

	if (majorI == null || majorI.intValue() > majorVersion)
	  {
	    System.err.println("Error, the ganymede document element " + docElement +
			       " does not contain a compatible major version number");
	    return false;
	  }

	if (minorI == null)
	  {
	    System.err.println("Error, the ganymede document element " + docElement +
			       " does not contain a minor version number");
	    return false;
	  }

	if (docElement.getAttrStr("persona") != null)
	  {
	    username = docElement.getAttrStr("persona");
	  }

	if (docElement.getAttrStr("password") != null)
	  {
	    password = docElement.getAttrStr("persona");
	  }

	return true;
      }
    catch (Exception ex)
      {
	ex.printStackTrace();

	return false;
      }
    finally
      {
	if (reader != null)
	  {
	    reader.close();
	  }
      }
  }

  /**
   * <p>This method loads properties from the ganymede.properties
   * file.</p>
   *
   * <p>This method is public so that loader code linked with the
   * Ganymede server code can initialize the properties without
   * going through Ganymede.main().</p>
   */

  public boolean loadProperties(String filename)
  {
    Properties props = new Properties(System.getProperties());
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

    // make the combined properties file accessible throughout our
    // code.

    System.setProperties(props);

    serverHostProperty = System.getProperty("ganymede.serverhost");

    if (serverHostProperty == null)
      {
	System.err.println("Couldn't get the server host property");
	success = false;
      }

    // get the registry port number

    String registryPort = System.getProperty("ganymede.registryPort");

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

    if (success)
      {
	server_url = "rmi://" + serverHostProperty + ":" + registryPortProperty + "/ganymede.server";
      }

    return success;
  }

  /**
   * <p>Private helper method to process events from
   * the {@link arlut.csd.Util.XMLReader XMLReader}.  By using
   * this method, the rest of the code in the xmlclient doesn't
   * have to check for error and warning conditions.</p>
   */

  public XMLItem getNextItem() throws SAXException
  {
    XMLItem item = null;

    while (item == null)
      {
	item = reader.getNextItem();

	if (item instanceof XMLError)
	  {
	    System.err.println(item);
	    throw new SAXException(item.toString());
	  }

	if (item instanceof XMLWarning)
	  {
	    System.err.println("Warning!: " + item);
	    item = null;	// trigger retrieval of next, and check for warning
	  }
      }

    return item;
  }

  /**
   * <p>This method is called to consume XML elements until we
   * find the matching close.</p>
   */

  public void skipToClose(String name) throws SAXException
  {
    XMLItem item;

    /* -- */

    item = getNextItem();

    while (!item.matchesClose(name) && !(item instanceof XMLEndDocument))
      {
	item = getNextItem();
      }
  }

  // These are for the ClientListener

  /**
   * Handle a message from the {@link arlut.csd.ganymede.client.ClientBase ClientBase}
   * RMI object.
   */

  public void messageReceived(ClientEvent e)
  {
    if (e.getType() == e.ERROR)
      {
	System.err.println(e.getMessage());
      }
  }

  /**
   * Handle a forced disconnect message from the
   * {@link arlut.csd.ganymede.client.ClientBase ClientBase} RMI object.
   */

  public void disconnected(ClientEvent e)
  {
    System.err.println(e.getMessage());
    System.exit(1);
  }
}

/**
 * This class is used to act as a receiver of server-transmitted XML materials
 * by the XML client.  The server talks to this receiver when the xmlclient
 * is given the -dumpschema or -dumpdata command line parameters.
 */

class xmlclientPrintReceiver implements FileReceiver {

  /**
   * <p>This method is used to send chunks of a file, in order, to the
   * FileReceiver.  The FileReceiver can return a non-successful ReturnVal
   * if it doesn't want to stop receiving the file.  A null return value
   * indicates success, keep sending.</p>
   */
  
  public ReturnVal sendBytes(byte[] bytes)
  {
    try
      {
	System.out.write(bytes);
      }
    catch (IOException ex)
      {
	ex.printStackTrace();
      }

    return null;
  }

  /**
   * <p>This method is used to send chunks of a file, in order, to the
   * FileReceiver.  The FileReceiver can return a non-successful ReturnVal
   * if it doesn't want to stop receiving the file.  A null return value
   * indicates success, keep sending.</p>
   */
  
  public ReturnVal sendBytes(byte[] bytes, int offset, int len)
  {
    System.out.write(bytes, offset, len); // yup, we don't need to catch IOException

    return null;
  }
  
  /**
   * <p>This method is called to notify the FileReceiver that no more
   * of the file will be transmitted.  The boolean parameter will
   * be true if the file was completely sent, or false if the transmission
   * is being aborted by the sender for some reason.</p>
   *
   * @return Returns true if the FileReceiver successfully received
   * the file in its entirety.
   */
  
  public ReturnVal end(boolean completed)
  {
    return null;
  }
}

/*
   xmlclient.java

   This is a text client for the Ganymede server.  This client is
   designed to take the filename for an XML file on the command line,
   load the file, parse it, then connect to the server and attempt to
   transfer the objects specified in the XML file to the server using
   the standard Ganymede RMI API.

   --

   Created: 2 May 2000
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 2000/05/09 02:28:56 $
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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.  */

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
 * load the file, parse it, then connect to the server and attempt to
 * transfer the objects specified in the XML file to the server using
 * the standard Ganymede RMI API.</p>
 *
 * @version $Revision: 1.2 $ $Date: 2000/05/09 02:28:56 $ $Name:  $
 * @author Jonathan Abbey
 */

public class xmlclient implements ClientListener {

  /**
   * <p>This major version number is compared with the "major"
   * attribute in the Ganymede XML document element.  xmlclient won't
   * try to read Ganymede XML files whose major and/or minor numbers
   * are too high.</p> 
   */

  public static final int majorVersion = 1;

  /**
   * <p>This minor version number is compared with the "minor"
   * attribute in the Ganymede XML document element.  xmlclient won't
   * try to read Ganymede XML files whose major and/or minor numbers
   * are too high.</p> 
   */

  public static final int minorVersion = 0;

  // ---

  public String serverHostProperty = null;
  public int    registryPortProperty = 1099;
  public String server_url = null;
  public String propFilename = null;
  public String xmlFilename = null;
  public String username = null;
  public String password = null;
  public boolean disconnectNow = false;

  /**
   * <p>Remote session interface to the Ganymede server, used while
   * loading data objects into the server.</p>
   */

  public Session session = null;

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

  /**
   * <P>Hashtable mapping object type names to
   * hashes which map local object designations to
   * actual {@link arlut.csd.ganymede.client.xmlobject xmlobject}
   * records.</P>
   */

  public Hashtable objectTypes = new Hashtable();

  /**
   * <P>The loader is a thread that obtains information from
   * the server on object type definitions present in the
   * server.  This is used to help guide the interpretation
   * of the XML file when handling the &lt;ganydata&gt; element.</P>
   */

  public Loader loader;

  /**
   * <P>RMI object to handle getting us logged into the server, and to
   * handle asynchronous callbacks from the server on our behalf.  xmlclient
   * implements the
   * {@link arlut.csd.ganymede.client.ClientListener ClientListener} interface
   * in order to receive these callbacks.</P> 
   */

  private ClientBase my_client;

  // ---

  /**
   *
   * main
   *
   */

  public static void main(String argv[])
  {
    xmlclient xclient = new xmlclient(argv);
    xclient.processXML();
  }
  
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
	ok = false;
      }

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

    xmlFilename = argv[argv.length-1];

    xmlFile = new File(xmlFilename);

    if (!xmlFile.exists())
      {
	System.err.println("Ganymede xmlclient: Error, file " + xmlFilename + " does not exist");
	ok = false;
      }

    if (!ok)
      {
 	System.err.println("Usage: java xmlclient properties=<property file> [bufsize=<buffer size>] <xmlfile>");
	System.exit(1);
      }
  }

  /**
   * <p>This method handles the actual XML processing, once the
   * command line arguments have been parsed and handled by the
   * xmlclient constructor.</p>
   */

  public void processXML()
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

	XMLItem docElement = getNextItem();

	if (!docElement.matches("ganymede"))
	  {
	    System.err.println("Error, " + xmlFilename + " does not contain a Ganymede XML file.");
	    System.err.println("Unrecognized XML element: " + docElement);
	    return;
	  }

	Integer majorI = docElement.getAttrInt("major");
	Integer minorI = docElement.getAttrInt("minor");

	if (majorI == null || majorI.intValue() > majorVersion)
	  {
	    System.err.println("Error, the ganymede document element " + docElement +
			       " does not contain a compatible major version number");
	    return;
	  }
	
	if (majorI.intValue() == majorVersion && 
	    (minorI == null || minorI.intValue() > minorVersion))
	  {
	    System.err.println("Error, the ganymede document element " + docElement +
			       " does not contain a compatible minor version number");
	    return;
	  }

	// okay, we're good to go

	XMLItem nextElement = getNextItem();

	if (nextElement.matches("ganyschema"))
	  {
	    processSchema();

	    nextElement = getNextItem();
	  }

	if (nextElement.matches("ganydata"))
	  {
	    processData();

	    nextElement = getNextItem();
	  }

	while (!nextElement.matchesClose("ganymede") && !(nextElement instanceof XMLCloseElement))
	  {
	    System.err.println("Skipping unrecognized element: " + nextElement);
	    nextElement = getNextItem();
	  }
      }
    catch (IOException ex)
      {
	ex.printStackTrace();
      }
    catch (SAXException ex)
      {
	ex.printStackTrace();
      }
    finally
      {
	System.err.println("XML parsing ended");

	if (reader != null)
	  {
	    reader.close();
	  }
	
	System.exit(0);
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

  private XMLItem getNextItem() throws SAXException
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
	    System.err.println(item);
	    item = reader.getNextItem();
	  }
      }

    return item;
  }

  /**
   * <p>This method is called after the &lt;ganyschema&gt; element has been
   * read and consumes everything up to and including the matching
   * &lt;/ganyschema&gt; element, if such is to be found.  Eventually,
   * this method will actually process the contents of the 
   * &lt;ganyschema&gt; element and transmit the schema change information
   * to the server.</p>
   */

  public void processSchema() throws SAXException
  {
    System.err.println("processSchema");

    XMLItem item = getNextItem();

    while (!item.matchesClose("ganyschema") && !(item instanceof XMLEndDocument))
      {
	item = getNextItem();
      }

    System.err.println("/processSchema");
  }

  /**
   * <p>This method is called after the &lt;ganydata&gt; element has been
   * read and consumes everything up to and including the matching
   * &lt;/ganydata&gt; element, if such is to be found.</p>
   *
   * <p>Before starting to read data from the &lt;ganydata&gt; element,
   * this method attempts to connect to the Ganymede server through the
   * normal client {@link arlut.csd.ganymede.Session Session} interface.
   * Once connected, this method will download schema information from
   * the server in order to interpret tags in the &lt;ganydata&gt;
   * element.</p>
   *
   * <p>Assuming that login and schema download was successful, the
   * contents of &lt;ganydata&gt; are scanned, and an in-memory datastructure
   * is constructed in the xmlclient.  All objects are organized in
   * memory by type and id, and inter-object invid references are resolved
   * to the extent possible.</p>
   *
   * <p>If all of that succeeds, processData() will start a transaction
   * on the server, and will start transferring the data from the XML
   * file's &lt;ganydata&gt; element to the server.  If any errors
   * are reported, the returned error message is printed and processData
   * aborts.  If no errors are reported at this stage, a transaction
   * commit is attempted.  Once again, if there are any errors reported
   * from the server, they are printed and processData aborts.  Otherwise,
   * success!</p>
   */

  public void processData() throws SAXException
  {
    System.err.println("processData");

    ReturnVal attempt = null;
    XMLItem item;

    /* -- */

    item = getNextItem();

    while (!item.matchesClose("ganydata") && !(item instanceof XMLEndDocument))
      {
	//	System.err.println(item);
	item = getNextItem();
      }

    System.err.println("/processData");
  }

  /**
   * <p>This private helper method handles logging on to the server as
   * a normal client, and sets up the {@link
   * arlut.csd.ganymede.client.xmlclient#session session} variable.</p>
   */

  private void connectAsClient()
  {
    ReturnVal attempt = null;

    /* -- */

    // after the ClientBase is constructed, we'll be an active RMI
    // server, so we need to always do System.exit() to shut down the
    // VM, from this point forward

    try
      {
	my_client = new ClientBase(server_url, this);
      }
    catch (RemoteException ex)
      {
	System.err.println("Could not connect to server" + ex.getMessage());
	System.exit(1);
      }

    try
      {
	session = my_client.login(username, password);
      }
    catch (RemoteException ex)
      {
	System.err.println("Ganymede xmlclient: couldn't log in for username " + username);
	System.exit(1);
      }

    // from this point on, we'll exit via the finally clause
    // below, so that we do a proper logout from the server

    try
      {
	// Loader is inherited from java.lang.Thread, so we can just
	// create one and start it running so that it will talk to the
	// server and download type and mapping information from the
	// server, in the background

	loader = new Loader(session, true);
	loader.start();

	try
	  {
	    attempt = session.openTransaction("xmlclient client (" + username + ")");

	    if (attempt != null && !attempt.didSucceed())
	      {
		if (attempt.getDialog() != null)
		  {
		    System.err.println("Ganymede xmlclient: couldn't open transaction " + username +
				       ": " + attempt.getDialog().getText());
		  }
		else
		  {
		    System.err.println("Ganymede xmlclient: couldn't open transaction " + username);
		  }

		return;
	      }
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("Ganymede xmlclient: couldn't open transaction " + 
			       username + ": " + ex.getMessage());
	    return;
	  }

	try
	  {
	    attempt = session.commitTransaction(true);

	    if (attempt != null && !attempt.didSucceed())
	      {
		if (attempt.getDialog() != null)
		  {
		    System.err.println("Ganymede xmlclient: couldn't commit transaction " + username +
				       ": " + attempt.getDialog().getText());
		    return;
		  }
		else
		  {
		    System.err.println("Ganymede xmlclient: couldn't commit transaction " + username);
		    return;
		  }
	      }
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("Ganymede.xmlclient: remote exception " + ex.getMessage());
	  }
      }
    finally
      {
	try
	  {
	    my_client.disconnect();
	  }
	catch (RemoteException ex)
	  {
	  }
	finally
	  {
	    System.exit(0);
	  }
      }
  }


  /**
   * <p>Called when the server forces a disconnect.</p>
   *
   * <p>Call getMessage() on the
   * {@link arlut.csd.ganymede.client.ClientEvent ClientEvent} 
   * to get the reason for the disconnect.</p>
   */

  public void disconnected(ClientEvent e)
  {
    disconnectNow = true;
  }

  /**
   * <p>Called when the ClientBase needs to report something
   * to the client.  The client is expected to then put
   * up a dialog or do whatever else is appropriate.</p>
   *
   * <p>Call getMessage() on the
   * {@link arlut.csd.ganymede.client.ClientEvent ClientEvent} 
   * to get the reason for the disconnect.</p>
   */

  public void messageReceived(ClientEvent e)
  {
  }
}

/*
   xmlclient.java

   This is a text client for the Ganymede server.  This client is
   designed to take the filename for an XML file on the command line,
   load the file, parse it, then connect to the server and transfer
   the file to the server for server-side integration into the Ganymede
   database.

   --

   Created: 2 May 2000

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2005
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Properties;

import org.xml.sax.SAXException;

import arlut.csd.Util.ParseArgs;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.XMLEndDocument;
import arlut.csd.Util.XMLError;
import arlut.csd.Util.XMLItem;
import arlut.csd.Util.XMLStartDocument;
import arlut.csd.Util.XMLWarning;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.FileTransmitter;
import arlut.csd.ganymede.rmi.Server;
import arlut.csd.ganymede.rmi.Session;
import arlut.csd.ganymede.rmi.XMLSession;

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
 * @version $Id$
 * @author Jonathan Abbey
 */

public final class xmlclient implements ClientListener, Runnable {

  public static final boolean debug = false;

  /**
   * <p>TranslationService object for handling string localization.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.xmlclient");

  /**
   * <p>This major version number is compared with the "major"
   * attribute in the Ganymede XML document element.  xmlclient won't
   * try to read Ganymede XML files whose major number is too high</p>
   */

  public static final int majorVersion = 1;

  /**
   * <p>This minor version number is provided to the server
   * when handling raw schema files (files whose document element
   * is &lt;ganyschema&gt;.</p>
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
  public String syncChannel = null;
  private boolean dumpSchema = false;
  private boolean dumpData = false;
  private boolean doTest = false;
  private boolean schemaOnly = false;
  private boolean finishedErrStream = false;

  /**
   * RMI reference to a Ganymede server
   */

  private Server server = null;

  /**
   * <p>Remote session interface to the Ganymede server, used while
   * loading data objects into the server.</p>
   */

  public XMLSession xSession = null;

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

  public arlut.csd.Util.XMLReader reader = null;

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
	if (xc.dumpData || xc.dumpSchema)
	  {
	    if (xc.doXMLDump(true, xc.dumpData, xc.dumpSchema))
	      {
		xc.terminate(0);
	      }
	    else
	      {
		xc.terminate(1);		
	      }
	  }

	if (xc.doTest)
	  {
	    xc.runTest();
	    xc.terminate(0);
	  }

	if (xc.doSendChanges(true))
	  {
	    xc.terminate(0);
	  }
	else
	  {
	    System.err.println();
	    // "XML submission failed."
	    System.err.println(ts.l("main.failed"));
	  }
      }
    catch (Exception ex)
      {
	ex.printStackTrace();
      }
    finally
      {
	xc.terminate(1);
      }
  }

  /**
   * This is a convenience method used by the server to get a
   * stack trace from a throwable object in String form.
   */

  static public String stackTrace(Throwable thing)
  {
    StringWriter stringTarget = new StringWriter();
    PrintWriter writer = new PrintWriter(stringTarget);
    
    thing.printStackTrace(writer);
    writer.close();

    return stringTarget.toString();
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
	// "Ganymede xmlclient: Error, must specify properties on Java invocation line"
	System.err.println(ts.l("init.noprops"));
	System.exit(1);
      }
    else
      {
	ok = loadProperties(propFilename);
      }

    // "username"
    username = ParseArgs.getArg(ts.l("global.userArg"), argv);

    if (username == null)
      {
	username = "supergash";
      }

    // "password"
    password = ParseArgs.getArg(ts.l("global.passArg"), argv);
    
    // "bufsize"
    String bufferString = ParseArgs.getArg(ts.l("global.bufArg"), argv);

    if (bufferString != null)
      {
	try
	  {
	    bufferSize = Integer.parseInt(bufferString);
	  }
	catch (NumberFormatException ex)
	  {
	    // "Couldn''t recognize bufsize argument: {0}"
	    System.err.println(ts.l("init.badBuf", bufferString));
	    ok = false;
	  }
      }

    // "dumpschema"
    if (ParseArgs.switchExists(ts.l("global.dumpSchemaArg"), argv))
      {
	dumpSchema = true;
	dumpData = false;
	return;
      }

    // "dumpdata"
    if (ParseArgs.switchExists(ts.l("global.dumpDataArg"), argv))
      {
	dumpData = true;
	dumpSchema = false;

	// "sync"
	syncChannel = ParseArgs.getArg(ts.l("global.syncArg"), argv);

	return;
      }
    
    // "dump"
    if (ParseArgs.switchExists(ts.l("global.dumpArg"), argv))
      {
	dumpData = true;
	dumpSchema = true;
	return;
      }

    // "test"
    if (ParseArgs.switchExists(ts.l("global.testArg"), argv))
      {
	doTest = true;
      }

    xmlFilename = argv[argv.length-1];

    xmlFile = new File(xmlFilename);

    if (!xmlFile.exists())
      {
	// "Ganymede xmlclient: Error, file {0} does not exist"
	System.err.println(ts.l("init.badFile", xmlFilename));
	ok = false;
      }

    if (!ok)
      {
	printUsage();
	System.exit(1);
      }
  }

  public void printUsage()
  {
    // "Usage:\n\
    // 1: xmlclient [username=<username>] [password=<password>] <xmlfile>\n \
    // 2: xmlclient [username=<username>] [password=<password>] -dump\n\
    // 3: xmlclient [username=<username>] [password=<password>] -dumpschema\n\
    // 4: xmlclient [username=<username>] [password=<password>] -dumpdata [sync=<sync channel>]"

    System.err.println(ts.l("printUsage.text"));
  }

  public boolean doXMLDump(boolean commandLine, boolean sendData, boolean sendSchema) throws RemoteException, NotBoundException, MalformedURLException
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
		// Password for "{0}":
		System.err.print(ts.l("global.passPrompt", username));
		password = in.readLine();
		System.err.println();
	      }
	    catch (java.io.IOException ex)
	      {
		// "Exception getting input: {0}"
		throw new RuntimeException(ts.l("global.inputException", ex.toString()));
	      }
	  }
	else
	  {
	    return false;
	  }
      }

    // find the server

    ClientBase client = new ClientBase(server_url, this);

    try
      {
	client.connect();
      }
    catch (Throwable ex)
      {
	// "Error connecting to the server:\n{0}"
	System.err.println(ts.l("global.errorConnecting", stackTrace(ex)));
	return false;
      }

    // since we're only doing data or schema dumping, we don't
    // actually need a GanymedeXMLSession on the server side.

    Session session = client.login(username, password);

    if (session == null)
      {
	// "Error, couldn''t log in to server.. bad username or password?"
	System.err.println(ts.l("global.badLogin"));
	return false;
      }

    // now do what we came for

    ReturnVal retVal = null;

    if (sendData && !sendSchema)
      {
	retVal = session.getDataXML(syncChannel);
      }
    else if (sendSchema && !sendData)
      {
	retVal = session.getSchemaXML();
      }
    else if (sendSchema && sendData)
      {
	retVal = session.getXMLDump();
      }

    if (retVal != null && !retVal.didSucceed())
      {
	String errorMessage = retVal.getDialogText();

	if (errorMessage != null)
	  {
	    System.err.println(errorMessage);
	  }

	return false;
      }

    FileTransmitter transmitter = retVal.getFileTransmitter();

    byte[] bytes = transmitter.getNextChunk();

    try
      {
	while (bytes != null)
	  {
	    System.out.write(bytes);
	    
	    bytes = transmitter.getNextChunk();
	  }
      }
    catch (Exception ex)
      {
	ex.printStackTrace();
      }
    finally
      {
	// and say goodbye

	session.logout();
      }

    return true;
  }

  /**
   * Simple test rig for XMLReader.getNextTree().
   */

  public void runTest()
  {
    try
      {
	reader = new arlut.csd.Util.XMLReader(xmlFilename, bufferSize, true); // skip meaningless whitespace

	XMLItem startDocument = getNextItem();

	if (!(startDocument instanceof XMLStartDocument))
	  {
	    // "XML parser error: first element {0} is not XMLStartDocument"
	    System.err.println(ts.l("global.badBeginParse", startDocument));
	    return;
	  }

	XMLItem docElement = reader.getNextTree();

	if (!docElement.matches("ganymede"))
	  {
	    // "Error, {0} does not contain a Ganymede XML file.\nUnrecognized XML element: {1}"
	    System.err.println(ts.l("global.badDocElement", xmlFilename, docElement));
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
   * <p>This method is used for sending an XML schema edit and/or transaction to
   * the Ganymede server.</p>
   */

  public boolean doSendChanges(boolean commandLine) throws RemoteException, IOException
  {
    // first, check the basic integrity of the xml file and attempt to
    // find the username and password out of it

    if (!scanXML())
      {
	// "Bad XML integrity"
	System.err.println(ts.l("doSendChanges.badScan"));

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

	// "Ganymede xmlclient: Error, must specify Ganymede account name in <ganymede> element or on\ncommand line."

	System.err.println(ts.l("doSendChanges.noUsername"));
	printUsage();
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
		// Password for "{0}":
		System.err.print(ts.l("global.passPrompt", username));
		password = in.readLine();
		System.err.println();
	      }
	    catch (java.io.IOException ex)
	      {
		// "Exception getting input: {0}"
		throw new RuntimeException(ts.l("global.inputException", ex.toString()));
	      }
	  }
	else
	  {
	    return false;
	  }
      }

    // find the server

    ClientBase client = new ClientBase(server_url, this);

    try
      {
	client.connect();
      }
    catch (Throwable ex)
      {
	// "Error connecting to the server:\n{0}"
	System.err.println(ts.l("global.errorConnecting", stackTrace(ex)));
	return false;
      }

    // since we're transmitting changes, we'll need a
    // GanymedeXMLSession on the server side.

    this.xSession = client.xmlLogin(username, password);

    if (xSession == null)
      {
	// "Error, couldn''t log in to server.. bad username or password?"
	System.err.println(ts.l("global.badLogin"));
	return false;
      }

    // "Sending XML to server."
    System.err.println(ts.l("doSendChanges.transmitting"));

    // logged in !  start our error stream retrieval thread

    new Thread(this).start();

    // now we just need to spin through the xml file and send it up to
    // the server

    BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(xmlFilename));

    ReturnVal retVal = null;
    byte[] data = null;
    int oldavail = 0;

    if (schemaOnly)
      {
	String startWrap = "<ganymede major=\"" + majorVersion + "\" minor=\"" + minorVersion + "\">\n";
	data = startWrap.getBytes();

	if (!submitChunk(data))
	  {
	    return false;
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

	if (!submitChunk(data))
	  {
	    return false;
	  }

	// and round and round we go

	avail = inStream.available();
      }

    if (schemaOnly)
      {
	String endWrap = "\n</ganymede>";
	data = endWrap.getBytes();

	if (!submitChunk(data))
	  {
	    return false;
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
	else
	  {
	    // a null ReturnVal signifies a successful result
	    
	    return true;
	  }
      }
    catch (Exception ex)
      {
	// for remote and other
	ex.printStackTrace();

	return false;
      }
  }

  private boolean submitChunk(byte[] data)
  {
    ReturnVal retVal;

    /* -- */

    if (debug)
      {
	System.err.println("submitChunk call");
      }

    try
      {
	retVal = xSession.xmlSubmit(data);

	if (debug)
	  {
	    System.err.println("submitChunk completed");
	  }

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

	return true;
      }
    catch (Exception ex)
      {
	// for remote and other
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
	reader = new arlut.csd.Util.XMLReader(xmlFilename, bufferSize, true); // skip meaningless whitespace

	XMLItem startDocument = getNextItem();

	if (!(startDocument instanceof XMLStartDocument))
	  {
	    // "XML parser error: first element {0} is not XMLStartDocument"
	    System.err.println(ts.l("global.badBeginParse", startDocument));
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
	    // "Error, {0} does not contain a Ganymede XML file.\nUnrecognized XML element: {1}"
	    System.err.println(ts.l("global.badDocElement", xmlFilename, docElement));
	    return false;
	  }

	Integer majorI = docElement.getAttrInt("major");
	Integer minorI = docElement.getAttrInt("minor");

	if (majorI == null || majorI.intValue() > majorVersion)
	  {
	    // "Error, the ganymede Document Element {0} does not contain a compatible major version number."
	    System.err.println(ts.l("scanXML.badMajor", docElement));
	    return false;
	  }

	if (minorI == null)
	  {
	    // "Error, the ganymede Document Element {0} does not contain a minor version number."
	    System.err.println(ts.l("scanXML.badMinor", docElement));
	    return false;
	  }

	if (docElement.getAttrStr("persona") != null)
	  {
	    username = docElement.getAttrStr("persona");
	  }

	if (docElement.getAttrStr("password") != null)
	  {
	    password = docElement.getAttrStr("password");
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
	// "Couldn''t get the server host property"
	System.err.println(ts.l("loadProperties.noServerHost"));
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
	    // "Couldn''t parse the ganymede.registryPort property"
	    System.err.println(ts.l("loadProperties.noRegistryPort", registryPort));
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
	    // "Warning!: {0}"
	    System.err.println(ts.l("getNextItem.warning", item));
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

  /**
   * <p>This method is used when we transmit XML to the server.  It
   * spins on calls to the server's getNextErrChunk() method to pull
   * the error output stream from the server.</p>
   */

  public synchronized void run()
  {
    String result;
    int count = 0;

    /* -- */

    this.finishedErrStream = false;

    while (!this.finishedErrStream)
      {
	try
	  {
	    result = xSession.getNextErrChunk();

	    if (result != null)
	      {
		System.err.print(result);
	      }
	    else
	      {
		this.finishedErrStream = true;
	      }
	  }
	catch (Exception ex)
	  {
	    // for remote and other

	    ex.printStackTrace();

	    // we won't exit our err stream thread on one or two
	    // errors, but if we get a bunch, assume we've lost the
	    // connection and end the thread.

	    if (count++ > 3)
	      {
		return;
	      }
	  }
      }
  }

  /**
   * <p>This method handles terminating the xmlclient.  It is
   * synchronized against the run method which handles pulling the
   * error stream from the server, so that we block termination
   * until the error stream thread completes.</p>
   */

  private synchronized void terminate(int resultCode)
  {
    System.exit(resultCode);
  }
}

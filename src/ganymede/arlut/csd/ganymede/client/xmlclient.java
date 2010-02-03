/*
   xmlclient.java

   This is a text client for the Ganymede server.  This client is
   designed to take the filename for an XML file on the command line,
   load the file, parse it, then connect to the server and transfer
   the file to the server for server-side integration into the Ganymede
   database.

   Created: 2 May 2000

   Module By: Jonathan Abbey

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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
import arlut.csd.ganymede.rmi.Session;
import arlut.csd.ganymede.rmi.XMLSession;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       xmlclient

------------------------------------------------------------------------------*/

/**
 * This is a text client for the Ganymede server.  This client is
 * designed to take the filename for an XML file on the command line,
 * load the file, parse it, then connect to the server and transfer
 * the file to the server for server-side integration into the Ganymede
 * database.
 *
 * @author Jonathan Abbey
 */

public final class xmlclient implements ClientListener, Runnable {

  public static final boolean debug = false;

  /**
   * TranslationService object for handling string localization.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.xmlclient");

  /**
   * This major version number is compared with the "major" attribute
   * in the Ganymede XML document element.  xmlclient won't try to
   * read Ganymede XML files whose major number is too high
   */

  public static final int majorVersion = 1;

  /**
   * This minor version number is provided to the server when handling
   * raw schema files (files whose document element is
   * &lt;ganyschema&gt;.
   */

  public static final int minorVersion = 0;

  // ---

  /**
   * If commandLine is true, the xmlclient will assume that it can
   * interact with the user to prompt for a password through stderr
   * and stdin, if one is not provided on the command line or in an
   * XML input file.
   */

  public boolean commandLine = true;

  public String serverHostProperty = null;
  public int    registryPortProperty = 1099;
  public String server_url = null;
  public String propFilename = null;
  public String xmlFilename = null;
  public String queryString = null;
  public String queryFilename = null;
  public String username = null;
  public String password = null;
  public String syncChannel = null;
  private boolean dumpSchema = false;
  private boolean dumpData = false;
  private boolean doTest = false;
  private boolean schemaOnly = false;
  private boolean includeHistory = false;
  private boolean includeOid = false;
  private boolean finishedErrStream = false;

  /**
   * Remote session interface to the Ganymede server, used while
   * loading data objects into the server.
   */

  public XMLSession xSession = null;

  /**
   * The default buffer size in the {@link arlut.csd.Util.XMLReader XMLReader}.
   * This value determines how far ahead the XMLReader's i/o thread can get in
   * reading from the XML file.  Higher or lower values of this variable may
   * give better performance, depending on the characteristics of the JVM with
   * regards threading, etc.
   */

  public int bufferSize = 20;

  /**
   * Streaming XML reader.  xmlclient creates one of these on startup,
   * and from that point on, all XML reading is done through this
   * object.
   */

  public arlut.csd.Util.XMLReader reader = null;

  /**
   * This StringWriter holds error output generated by the xmlclient
   * during processing.
   */

  private StringWriter errBuf = null;

  /**
   * This PrintWriter is used to handle all debug/error output on
   * behalf of the xmlclient during processing.
   */

  public PrintWriter err = null;

  // ---

  /**
   *
   * main
   *
   */

  public static void main(String argv[])
  {
    xmlclient xc = new xmlclient(true, argv);

    try
      {
        if (xc.dumpData || xc.dumpSchema)
          {
            if (xc.doXMLDump(xc.dumpData, xc.dumpSchema))
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
        else if (xc.queryString != null)
          {
	    if (debug)
	      {
		System.err.println("xmlclient: xc.queryString = " + xc.queryString);
	      }

            if (xc.doQuery(xc.queryString))
              {
                xc.terminate(0);
              }
            else
              {
                xc.terminate(1);
              }
          }
        else if (xc.queryFilename != null)
          {
            if (xc.doQueryFile(xc.queryFilename))
              {
                xc.terminate(0);
              }
            else
              {
                xc.terminate(1);
              }
          }
        else if (xc.doSendChanges())
          {
            xc.terminate(0);
          }
        else
          {
            xc.err.println();

            // "XML submission failed."
            xc.err.println(ts.l("main.failed"));
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
   * This method is used by the interactive Ganymede xmlclient to
   * transmit a Ganymede XML change request to the Ganymede server.
   */

  static public String submitXML(gclient g_client, File inFile)
  {
    xmlclient xc = null;

    try
      {
        xc = new xmlclient(inFile);

        if (xc.doSendChanges_GUI(g_client, inFile))
          {
            return xc.errBuf.toString();
          }
      }
    finally
      {
        synchronized (xc)
          {
            while (!xc.finishedErrStream)
              {
                try
                  {
                    xc.wait();
                  }
                catch (InterruptedException ex)
                  {
                  }
              }

            xc.err.close();
            return xc.errBuf.toString();
          }
      }
  }

  /**
   * This constructor is used to handle XML data submission from the
   * interactive Ganymede client.
   */

  private xmlclient(File inFile)
  {
    this.commandLine = false;
    this.errBuf = new StringWriter();
    this.err = new PrintWriter(errBuf);

    if (!inFile.exists())
      {
	// "Ganymede xmlclient: Error, file {0} does not exist"
	err.println(ts.l("init.badFile", xmlFilename));
      }
  }

  /**
   * This constructor takes care of parsing the command line arguments
   * for xmlclient when run from the command line.
   */

  public xmlclient(boolean hasCommandLine, String argv[])
  {
    boolean ok = true;
    File xmlFile = null;

    /* -- */

    this.err = new PrintWriter(System.err);

    this.commandLine = hasCommandLine;

    // find the properties command line argument

    propFilename = ParseArgs.getArg("properties", argv);

    if (propFilename == null)
      {
	// "Ganymede xmlclient: Error, must specify properties on Java invocation line"
	err.println(ts.l("init.noprops"));
        err.flush();
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
	    err.println(ts.l("init.badBuf", bufferString));
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

	// "includeHistory"
	includeHistory = ParseArgs.switchExists(ts.l("global.includeHistory"), argv);

	// "includeOid"
	includeOid = ParseArgs.switchExists(ts.l("global.includeOid"), argv);

	return;
      }

    // "dump"
    if (ParseArgs.switchExists(ts.l("global.dumpArg"), argv))
      {
	dumpData = true;
	dumpSchema = true;

	// "includeHistory"
	includeHistory = ParseArgs.switchExists(ts.l("global.includeHistory"), argv);

	// "includeOid"
	includeOid = ParseArgs.switchExists(ts.l("global.includeOid"), argv);

	return;
      }

    if (ParseArgs.switchExists(ts.l("global.queryArg"), argv))
      {
	queryString = ParseArgs.decodeArg(argv[argv.length-1]);
	return;
      }

    if (ParseArgs.switchExists(ts.l("global.queryfileArg"), argv))
      {
	queryFilename = ParseArgs.decodeArg(argv[argv.length-1]);
	return;
      }

    // "test"
    if (ParseArgs.switchExists(ts.l("global.testArg"), argv))
      {
	doTest = true;
      }

    xmlFilename = ParseArgs.decodeArg(argv[argv.length-1]);

    xmlFile = new File(xmlFilename);

    if (!xmlFile.exists())
      {
	// "Ganymede xmlclient: Error, file {0} does not exist"
	err.println(ts.l("init.badFile", xmlFilename));
	ok = false;
      }

    if (!ok)
      {
	printUsage();
        err.flush();
	System.exit(1);
      }
  }

  public void printUsage()
  {
    // "Usage:\n\
    // 1: xmlclient [username=<username>] [password=<password>] <xmlfile>\n \
    // 2: xmlclient [username=<username>] [password=<password>] -dump [-includeHistory] [-includeOid]\n\
    // 3: xmlclient [username=<username>] [password=<password>] -dumpschema\n\
    // 4: xmlclient [username=<username>] [password=<password>] -dumpdata [-includeHistory] [-includeOid] [sync=<sync channel>]\n\
    // 5. xmlclient [username=<username>] [password=<password>] -queryfile <queryfile>\n\
    // 6. xmlclient [username=<username>] [password=<password>] -query <query>"

    err.println(ts.l("printUsage.text"));
  }

  public boolean doXMLDump(boolean sendData, boolean sendSchema) throws RemoteException, NotBoundException, MalformedURLException
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
	password = getPassword();
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
	err.println(ts.l("global.errorConnecting", stackTrace(ex)));
	return false;
      }

    // since we're only doing data or schema dumping, we don't
    // actually need a GanymedeXMLSession on the server side.

    Session session = client.login(username, password);

    if (session == null)
      {
	// "Error, couldn''t log in to server.. bad username or password?"
	err.println(ts.l("global.badLogin"));
	return false;
      }

    // now do what we came for

    ReturnVal retVal = null;

    if (sendData && !sendSchema)
      {
	retVal = session.getDataXML(syncChannel, includeHistory, includeOid);
      }
    else if (sendSchema && !sendData)
      {
	retVal = session.getSchemaXML();
      }
    else if (sendSchema && sendData)
      {
	retVal = session.getXMLDump(includeHistory, includeOid);
      }
    else
      {
        throw new RuntimeException("ASSERT: neither sendSchema nor sendData set");
      }

    if (!ReturnVal.didSucceed(retVal))
      {
	String errorMessage = retVal.getDialogText();

	if (errorMessage != null)
	  {
	    err.println(errorMessage);
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
	ex.printStackTrace(err);
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
	    err.println(ts.l("global.badBeginParse", startDocument));
	    return;
	  }

	XMLItem docElement = reader.getNextTree();

	if (docElement == null)
	  {
	    // "Error, {0} does not contain an XML file."
	    err.println(ts.l("global.nullDocElement", xmlFilename));
            return;
	  }

	if (!docElement.matches("ganymede"))
	  {
	    // "Error, {0} does not contain a Ganymede XML file.\nUnrecognized XML element: {1}"
	    err.println(ts.l("global.badDocElement", xmlFilename, docElement));
	    return;
	  }
	else
	  {
	    docElement.debugPrintTree(0);
	  }
      }
    catch (Exception ex)
      {
	ex.printStackTrace(err);

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
   * This method handles submitting a GanyQL query to the Ganymede
   * server and printing the results in XML to stdout.
   */

  public boolean doQuery(String queryString)  throws RemoteException, NotBoundException, MalformedURLException
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
	password = getPassword();
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
	err.println(ts.l("global.errorConnecting", stackTrace(ex)));
	return false;
      }

    // since we're only doing data or schema dumping, we don't
    // actually need a GanymedeXMLSession on the server side.

    Session session = client.login(username, password);

    if (session == null)
      {
	// "Error, couldn''t log in to server.. bad username or password?"
	err.println(ts.l("global.badLogin"));
	return false;
      }

    try
      {
        // now do what we came for

        ReturnVal retVal = session.runXMLQuery(queryString);

        if (!ReturnVal.didSucceed(retVal))
          {
            String errorMessage = retVal.getDialogText();

            if (errorMessage != null)
              {
                err.println(errorMessage);
              }

            return false;
          }

        FileTransmitter transmitter = retVal.getFileTransmitter();

        byte[] bytes = transmitter.getNextChunk();

        while (bytes != null)
          {
            System.out.write(bytes);

            bytes = transmitter.getNextChunk();
          }

        return true;
      }
    catch (Exception ex)
      {
        ex.printStackTrace(err);
        return false;
      }
    finally
      {
        session.logout();       // and say goodbye
      }
  }

  /**
   * Process a GanyGL query contained in a file.
   */

  public boolean doQueryFile(String queryFilename) throws IOException, NotBoundException, RemoteException
  {
    StringBuilder buffer = new StringBuilder();
    FileInputStream in = new FileInputStream(queryFilename);
    BufferedInputStream inBuf = new BufferedInputStream(in);
    int c;

    /* -- */

    // dirt simple buffered read from filename

    c = inBuf.read();

    while (c != -1)
      {
	buffer.append((char) c);
	c = inBuf.read();
      }

    inBuf.close();
    in.close();

    return doQuery(buffer.toString());
  }

  /**
   * This method is used for sending an transaction to the Ganymede
   * server in the context of the interactive graphical client.
   */

  public boolean doSendChanges_GUI(gclient g_client, File inFile) throws RemoteException, IOException
  {
    // first, check the basic integrity of the xml file and attempt to
    // find the username and password out of it

    if (!scanXML(inFile))
      {
	// "Bad XML integrity"
	err.println(ts.l("doSendChanges.badScan"));

	finishedErrStream = true;

	return false;		// malformed in some way
      }

    if (username == null || password == null)
      {
        username = g_client._myglogin.active_username;
        password = g_client._myglogin.active_passwd;
      }

    err.println(ts.l("doSendChanges_GUI.connecting_as", username));

    server_url = g_client._myglogin.server_url;

    // find the server

    ClientBase client = new ClientBase(server_url, this);

    try
      {
	client.connect();
      }
    catch (Throwable ex)
      {
	// "Error connecting to the server:\n{0}"
	err.println(ts.l("global.errorConnecting", stackTrace(ex)));
	return false;
      }

    // since we're transmitting changes, we'll need a
    // GanymedeXMLSession on the server side.

    this.xSession = client.xmlLogin(username, password);

    // N.B. this can fail if the logged in user has changed his
    // password since starting the Ganymede GUI client.. ;-(

    if (xSession == null)
      {
	// "Error, couldn''t log in to server.. bad username or password?"
	err.println(ts.l("global.badLogin"));
	return false;
      }

    // "Sending XML to server."
    err.println(ts.l("doSendChanges.transmitting"));

    // logged in !  start our error stream retrieval thread

    new Thread(this).start();

    // now we just need to spin through the xml file and send it up to
    // the server

    BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(inFile));

    return doSendChanges(inStream);
  }

  /**
   * This method is used for sending an XML schema edit and/or
   * transaction to the Ganymede server from the command line
   * xmlclient.
   */

  public boolean doSendChanges() throws RemoteException, IOException
  {
    // first, check the basic integrity of the xml file and attempt to
    // find the username and password out of it

    if (!scanXML(null))
      {
	// "Bad XML integrity"
	err.println(ts.l("doSendChanges.badScan"));

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

	err.println(ts.l("doSendChanges.noUsername"));
	printUsage();
	return false;
      }

    if (password == null)
      {
	password = getPassword();
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
	err.println(ts.l("global.errorConnecting", stackTrace(ex)));
	return false;
      }

    // since we're transmitting changes, we'll need a
    // GanymedeXMLSession on the server side.

    this.xSession = client.xmlLogin(username, password);

    if (xSession == null)
      {
	// "Error, couldn''t log in to server.. bad username or password?"
	err.println(ts.l("global.badLogin"));
	return false;
      }

    // "Sending XML to server."
    err.println(ts.l("doSendChanges.transmitting"));

    // logged in !  start our error stream retrieval thread

    new Thread(this).start();

    // now we just need to spin through the xml file and send it up to
    // the server

    BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(xmlFilename));

    return doSendChanges(inStream);
  }

  /**
   * This method is used for reading XML from a pre-opened stream for
   * transmitting to the Ganymede server.
   */

  private boolean doSendChanges(InputStream inStream) throws RemoteException, IOException
  {
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
		err.println(message);
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
	ex.printStackTrace(err);

	return false;
      }
  }

  /**
   * Private utility method for acquiring password from the command
   * line if the user did not provide it on the command line.
   */

  private String getPassword()
  {
    if (password != null)
      {
	return password;
      }

    if (this.commandLine)
      {
	java.io.BufferedReader in;

	// get an input stream so we can get the password from the user if we have to

	in = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));

	try
	  {
	    // Password for "{0}":
	    err.print(ts.l("global.passPrompt", username));
            err.flush();
	    password = in.readLine();
	    err.println();
            err.flush();
	  }
	catch (java.io.IOException ex)
	  {
	    // "Exception getting input: {0}"
	    throw new RuntimeException(ts.l("global.inputException", ex.toString()));
	  }
      }
    else
      {
	// "Error, could not acquire password."
	throw new RuntimeException(ts.l("global.nopass"));
      }

    return password;
  }

  private boolean submitChunk(byte[] data)
  {
    ReturnVal retVal;

    /* -- */

    if (debug)
      {
	err.println("submitChunk call");
      }

    try
      {
	retVal = xSession.xmlSubmit(data);

	if (debug)
	  {
	    err.println("submitChunk completed");
	  }

	if (retVal != null)
	  {
	    String message = retVal.getDialogText();

	    if (message != null)
	      {
		err.println(message);
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
	ex.printStackTrace(err);
	return false;
      }
  }

  /**
   * This method validates an XML input file specified in xmlFilename.
   * scanXML() will scan the &lt;ganymede&gt; document element for any
   * username and password information specified therein, as well.
   */

  public boolean scanXML(File inFile)
  {
    String myFilename = null;

    try
      {
        if (inFile == null)
          {
	    myFilename = xmlFilename;
            reader = new arlut.csd.Util.XMLReader(xmlFilename, bufferSize, true, err); // skip meaningless whitespace
          }
        else
          {
	    myFilename = inFile.getName();
            reader = new arlut.csd.Util.XMLReader(inFile, bufferSize, true, err); // skip meaningless whitespace
          }

	XMLItem startDocument = getNextItem();

	if (!(startDocument instanceof XMLStartDocument))
	  {
	    // "XML parser error: first element {0} is not XMLStartDocument"
	    err.println(ts.l("global.badBeginParse", startDocument));
	    return false;
	  }

	XMLItem docElement = getNextItem();

	if (docElement == null)
	  {
	    // "Error, {0} does not contain an XML file."
	    err.println(ts.l("global.nullDocElement", myFilename));
	    return false;
	  }

	if (docElement.matches("ganyschema"))
	  {
	    schemaOnly = true;
	    return true;
	  }
	else if (!docElement.matches("ganymede"))
	  {
	    // "Error, {0} does not contain a Ganymede XML file.\nUnrecognized XML element: {1}"
	    err.println(ts.l("global.badDocElement", myFilename, docElement));
	    return false;
	  }

	Integer majorI = docElement.getAttrInt("major");
	Integer minorI = docElement.getAttrInt("minor");

	if (majorI == null || majorI.intValue() > majorVersion)
	  {
	    // "Error, the ganymede Document Element {0} does not contain a compatible major version number."
	    err.println(ts.l("scanXML.badMajor", docElement));
	    return false;
	  }

	if (minorI == null)
	  {
	    // "Error, the ganymede Document Element {0} does not contain a minor version number."
	    err.println(ts.l("scanXML.badMinor", docElement));
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
	ex.printStackTrace(err);

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
   * This method loads properties from the ganymede.properties
   * file.
   *
   * This method is public so that loader code linked with the
   * Ganymede server code can initialize the properties without
   * going through Ganymede.main().
   */

  public boolean loadProperties(String filename)
  {
    Properties props = new Properties(System.getProperties());
    boolean success = true;

    /* -- */

    BufferedInputStream bis = null;

    try
      {
	bis = new BufferedInputStream(new FileInputStream(filename));
	props.load(bis);
      }
    catch (IOException ex)
      {
	return false;
      }
    finally
      {
	if (bis != null)
	  {
	    try
	      {
		bis.close();
	      }
	    catch (IOException e)
	      {
	      }
	  }
      }

    // make the combined properties file accessible throughout our
    // code.

    System.setProperties(props);

    serverHostProperty = System.getProperty("ganymede.serverhost");

    if (serverHostProperty == null)
      {
	// "Couldn''t get the server host property"
	err.println(ts.l("loadProperties.noServerHost"));
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
	    err.println(ts.l("loadProperties.noRegistryPort", registryPort));
	  }
      }

    if (success)
      {
	server_url = "rmi://" + serverHostProperty + ":" + registryPortProperty + "/ganymede.server";
      }

    return success;
  }

  /**
   * Private helper method to process events from
   * the {@link arlut.csd.Util.XMLReader XMLReader}.  By using
   * this method, the rest of the code in the xmlclient doesn't
   * have to check for error and warning conditions.
   */

  public XMLItem getNextItem() throws SAXException
  {
    XMLItem item = null;

    while (item == null)
      {
	item = reader.getNextItem();

	if (item == null)
	  {
	    return null;
	  }

	if (item instanceof XMLError)
	  {
	    err.println(item);
	    throw new SAXException(item.toString());
	  }

	if (item instanceof XMLWarning)
	  {
	    // "Warning!: {0}"
	    err.println(ts.l("getNextItem.warning", item));
	    item = null;	// trigger retrieval of next, and check for warning
	  }
      }

    return item;
  }

  /**
   * This method is called to consume XML elements until we
   * find the matching close.
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
	err.println(e.getMessage());
      }
  }

  /**
   * Handle a forced disconnect message from the
   * {@link arlut.csd.ganymede.client.ClientBase ClientBase} RMI object.
   */

  public void disconnected(ClientEvent e)
  {
    err.println(e.getMessage());
    err.flush();
    System.exit(1);
  }

  /**
   * This method is used when we transmit XML to the server.  It
   * spins on calls to the server's getNextErrChunk() method to pull
   * the error output stream from the server.
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
		err.print(result);
                err.flush();    // the server will keep things
                                // efficient by the sleeping it does
                                // in getNextErrChunk
	      }
	    else
	      {
		this.finishedErrStream = true;
                this.notifyAll();
	      }
	  }
	catch (Exception ex)
	  {
	    // for remote and other

	    ex.printStackTrace(err);
	    err.flush();

	    this.finishedErrStream = true;
	    this.notifyAll();

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
   * This method handles terminating the xmlclient.  It is
   * synchronized against the run method which handles pulling the
   * error stream from the server, so that we block termination
   * until the error stream thread completes.
   */

  private synchronized void terminate(int resultCode)
  {
    err.flush();
    System.exit(resultCode);
  }
}

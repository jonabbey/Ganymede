/*
   xmlclient.java

   This is a text client for the Ganymede server.  This client is
   designed to take the filename for an XML file on the command line,
   load the file, parse it, then connect to the server and attempt to
   transfer the objects specified in the XML file to the server using
   the standard Ganymede RMI API.

   --

   Created: 2 May 2000
   Version: $Revision: 1.9 $
   Last Mod Date: $Date: 2000/05/27 00:30:27 $
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
 * load the file, parse it, then connect to the server and attempt to
 * transfer the objects specified in the XML file to the server using
 * the standard Ganymede RMI API.</p>
 *
 * @version $Revision: 1.9 $ $Date: 2000/05/27 00:30:27 $ $Name:  $
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

  /**
   * <p>Singleton reference to the instantiated xmlclient object.</p>
   */

  public static xmlclient xc = null;

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
   * <p>Hashtable mapping object type names to
   * hashtables mapping field names to 
   * {@link arlut.csd.ganymede.FieldTemplate FieldTemplate}
   * objects.</p>
   */

  public Hashtable objectTypes = new Hashtable();

  /**
   * <P>Hashtable mapping Short type ids to
   * hashes which map local object designations to
   * actual {@link arlut.csd.ganymede.client.xmlobject xmlobject}
   * records.</P>
   */

  public Hashtable objectStore = new Hashtable();

  /**
   * <p>Vector of {@link arlut.csd.ganymede.client.xmlobject xmlobjects}
   * that correspond to new Ganymede server objects
   * that have been/need to be created by the xmlclient.</p>
   */

  public Vector createdObjects = new Vector();

  /**
   * <p>Vector of {@link arlut.csd.ganymede.client.xmlobject xmlobjects}
   * that correspond to pre-existing Ganymede
   * server objects that have been/need to be checked out for editing by the
   * xmlclient.</p> 
   */

  public Vector editedObjects = new Vector();

  /**
   * <p>Vector of {@link arlut.csd.ganymede.client.xmlobject xmlobjects}
   * that correspond to pre-existing Ganymede
   * server objects that have been/need to be inactivated by the
   * xmlclient.</p> 
   */

  public Vector inactivatedObjects = new Vector();

  /**
   * <p>Vector of {@link arlut.csd.ganymede.client.xmlobject xmlobjects}
   * that correspond to pre-existing Ganymede
   * server objects that have been/need to be deleted by the
   * xmlclient.</p> 
   */

  public Vector deletedObjects = new Vector();

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
    xc = new xmlclient(argv);
    xc.processXML();
  }
  
  public xmlclient(String argv[])
  {
    boolean ok = true;
    File xmlFile = null;
    java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));

    /* -- */

    // find the properties command line argument

    propFilename = ParseArgs.getArg("properties", argv);

    if (propFilename == null)
      {
	System.err.println("Ganymede xmlclient: Error, must specify properties");
	ok = false;
      }
    else
      {
	ok = loadProperties(propFilename);
      }

    username = ParseArgs.getArg("username", argv);

    if (username == null)
      {
	// we would prompt for the username here, but java gives us no
	// portable way to turn character echo on and off.. the script
	// that runs us has character echo off so that we can prompt
	// for the user's password, but since it is off, we can't
	// really prompt for a missing user name here.

	System.err.println("Ganymede xmlclient: Error, must specify ganymede account name");
 	System.err.println("Usage: java xmlclient properties=<property file> username=<username> [password=<password>] [bufsize=<buffer size>] <xmlfile>");
	System.exit(1);
      }

    password = ParseArgs.getArg("password", argv);

    if (password == null)
      {
	try
	  {
	    System.out.print("Password:");
	    password = in.readLine();
	    System.out.println();
	  }
	catch (java.io.IOException ex)
	  {
	    throw new RuntimeException("Exception getting input: " + ex.getMessage());
	  }
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
 	System.err.println("Usage: java xmlclient properties=<property file> username=<username> [password=<password>] [bufsize=<buffer size>] <xmlfile>");
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
    XMLItem item;

    /* -- */

    System.err.println("processData");

    System.err.println("Connecting to server");

    connectAsClient();

    System.err.println("Reading object data");

    item = getNextItem();

    while (!item.matchesClose("ganydata") && !(item instanceof XMLEndDocument))
      {
	if (item.matches("object"))
	  {
	    String mode = item.getAttrStr("action");

	    xmlobject objectRecord = new xmlobject((XMLElement) item);

	    System.err.print(".");

	    //	    System.err.println(item);
	    
	    if (mode == null || mode.equals("create"))
	      {
		createdObjects.addElement(objectRecord);
	      }
	    else if (mode.equals("edit"))
	      {
		editedObjects.addElement(objectRecord);
	      }
	    else if (mode.equals("delete"))
	      {
		deletedObjects.addElement(objectRecord);
	      }
	    else if (mode.equals("inactivate"))
	      {
		inactivatedObjects.addElement(objectRecord);
	      }

	    storeObject(objectRecord);
	  }

	item = getNextItem();
      }

    System.err.print("\n\n");

    System.err.println("Assembling data");

    // not sure if there's much assembling to do yet.. eventually i'm
    // going to have to do the thing with doing invid link-ups, but
    // that'll really be part of transmitData().

    if (false)
      {
	Enumeration enum = objectStore.keys();

	while (enum.hasMoreElements())
	  {
	    Short typeId = (Short) enum.nextElement();
	
	    System.err.println("\n--------------------------------------------------------------------------------\n\n" +
			       "Dumping objects of type " + getTypeName(typeId.shortValue()) + "\n");

	    Hashtable idHash = (Hashtable) objectStore.get(typeId);

	    Enumeration enum2 = idHash.elements();
	
	    while (enum2.hasMoreElements())
	      {
		System.err.println(enum2.nextElement().toString());
	      }
	  }
      }

    transmitData();

    System.err.println("/processData");
  }

  /**
   * <p>This private helper method handles logging on to the server as
   * a normal client, and sets up the {@link
   * arlut.csd.ganymede.client.xmlclient#session session} variable.</p>
   */

  private void connectAsClient()
  {
    // after the ClientBase is constructed, we'll be an active RMI
    // server, so we need to always do System.exit() to shut down the
    // VM, from this point forward

    try
      {
	my_client = new ClientBase(server_url, this);
      }
    catch (RemoteException ex)
      {
	System.err.println("Could not connect to server: " + server_url + ex.getMessage());
	System.exit(1);
      }
    catch (RuntimeException ex)
      {
	System.err.println("Could not connect to server: " + server_url + ex.getMessage());
	System.exit(1);
      }

    System.err.println("Logging into server");

    try
      {
	session = my_client.login(username, password);
      }
    catch (RemoteException ex)
      {
	System.err.println("Ganymede xmlclient: couldn't log in for username " + username);
	System.exit(1);
      }

    // Loader is inherited from java.lang.Thread, so we can just
    // create one and start it running so that it will talk to the
    // server and download type and mapping information from the
    // server, in the background
    
    System.err.println("Creating loader");

    loader = new Loader(session, false);

    System.err.println("Starting loader thread");

    loader.start();

    //    System.err.println("Getting baseList from loader");

    Vector baseList = loader.getBaseList();

    //    System.err.println("Got baseList from loader");

    for (int i = 0; i < baseList.size(); i++)
      {
	BaseDump base = (BaseDump) baseList.elementAt(i);

	// System.err.println("Getting templateVector for " + base.getName());

	Vector templates = loader.getTemplateVector(base.getTypeID());

	Hashtable fieldHash = new Hashtable();
	
	for (int j = 0; j < templates.size(); j++)
	  {
	    FieldTemplate tmpl = (FieldTemplate) templates.elementAt(j);

	    fieldHash.put(tmpl.getName(), tmpl);
	  }

	objectTypes.put(base.getName(), fieldHash);
      }

    System.err.println("Finished downloading object type information from server.");
  }

  /**
   * <p>This method records an xmlobject that has been loaded from the
   * XML file into the xmlclient objectTypes hash.
   */

  public void storeObject(xmlobject object)
  {
    Hashtable objectHash = (Hashtable) objectStore.get(object.type);

    if (objectHash == null)
      {
	objectHash = new Hashtable();
	objectStore.put(object.type, objectHash);
      }

    if (object.id != null)
      {
	objectHash.put(object.id, object);
      }
    else if (object.num != -1)
      {
	objectHash.put(new Integer(object.num), object);
      }
  }

  /**
   * <p>This method resolves an Invid from a type/id pair, talking
   * to the server if the type/id pair has not previously been seen.</p>
   *
   * <p>Returns null on failure to retrieve.</p>
   *
   * @param typeId The object type number of the invid to find
   * @param objId The unique label of the object
   */

  public Invid getInvid(short typeId, String objId)
  {
    Invid invid = null;
    Short typeKey;
    Hashtable objectHash;

    /* -- */

    typeKey = new Short(typeId);
    objectHash = (Hashtable) objectStore.get(typeKey);

    if (objectHash == null)
      {
	objectHash = new Hashtable();
	objectStore.put(typeKey, objectHash);
      }

    Object element = objectHash.get(objId);

    if (element == null)
      {
	try
	  {
	    invid = session.findLabeledObject(objId, typeId);
	  }
	catch (RemoteException ex)
	  {
	  }

	if (invid != null)
	  {
	    // remember it in the cache

	    objectHash.put(objId, invid);
	  }
      }
    else
      {
	if (element instanceof xmlobject)
	  {
	    invid = ((xmlobject) element).invid;

	    // if invid is null at this point, this object hasn't been
	    // created or edited yet on the server, so we can't do
	    // anything other than return null
	  }
	else
	  {
	    // we'll just go ahead and throw a ClassCastException if
	    // we've got something strange in our objectHash

	    invid = (Invid) element;
	  }
      }

    return invid;
  }

  /**
   * <p>This method resolves an Invid from a type/num pair</p>
   *
   * <p>Returns null on failure to retrieve.</p>
   *
   * @param typename The name of the object type, in XML encoded form
   * @param num The numeric id of 
   */

  public Invid getInvid(String typename, int num)
  {
    return new Invid(getTypeNum(typename), num);
  }

  /**
   * <p>This method retrieves an xmlobject that has been previously
   * loaded from the XML file.</p>
   *
   * @param baseName An XML-encoded object type string
   * @param objectID The id string for the object in question
   */

  public xmlobject getObject(String baseName, String objectID)
  {
    return getObject(new Short(getTypeNum(baseName)), objectID);
  }

  /**
   * <p>This method retrieves an xmlobject that has been previously
   * loaded from the XML file.</p>
   *
   * @param baseID a Short holding the number of object type sought
   * @param objectID The id string for the object in question
   */

  public xmlobject getObject(Short baseID, String objectID)
  {
    Hashtable objectHash = (Hashtable) objectStore.get(baseID);

    if (objectHash == null)
      {
	return null;
      }

    return (xmlobject) objectHash.get(objectID);
  }

  /**
   * <p>This method retrieves an xmlobject that has been previously
   * loaded from the XML file.</p>
   *
   * @param baseName An XML-encoded object type string
   * @param objectNum The Integer object number for the object sought
   */

  public xmlobject getObject(String baseName, Integer objectNum)
  {
    return getObject(new Short(getTypeNum(baseName)), objectNum);
  }

  /**
   * <p>This method retrieves an xmlobject that has been previously
   * loaded from the XML file.</p>
   *
   * @param baseID a Short holding the number of object type sought
   * @param objectNum The Integer object number for the object sought
   */

  public xmlobject getObject(Short baseID, Integer objectNum)
  {
    Hashtable objectHash = (Hashtable) objectStore.get(baseID);

    if (objectHash == null)
      {
	return null;
      }

    return (xmlobject) objectHash.get(objectNum);
  }

  /**
   * <p>This helper method returns the short id number of an object
   * type based on its underscore-for-space encoded XML object type
   * name.</p>
   *
   * <p>If the named object type cannot be found, a
   * NullPointerException will be thrown.</p>
   */

  public short getTypeNum(String objectTypeName)
  {
    Hashtable nameHash = loader.getNameToShort();
    Short val = (Short) nameHash.get(XMLUtils.XMLDecode(objectTypeName));
    return val.shortValue();
  }

  /**
   * <p>This helper method returns the object type string for an object
   * type based on its short object type ID number.</p>
   *
   * <p>If the named object type cannot be found, a
   * NullPointerException will be thrown.</p>
   */

  public String getTypeName(short objectTypeID)
  {
    Hashtable baseMap = loader.getBaseMap();
    BaseDump base = (BaseDump) baseMap.get(new Short(objectTypeID));

    return base.getName();
  }

  /**
   * <p>This helper method returns a hash of field names to
   * {@link arlut.csd.ganymede.FieldTemplate FieldTemplate} based
   * on the underscore-for-space XML encoded object type name.</p>
   *
   * <p>The Hashtable returned by this method is intended to be used
   * with the getObjectFieldType method.</p>
   */

  public Hashtable getFieldHash(String objectTypeName)
  {
    return (Hashtable) objectTypes.get(XMLUtils.XMLDecode(objectTypeName));
  }

  /**
   * <p>This helper method takes a hash of field names to
   * {@link arlut.csd.ganymede.FieldTemplate FieldTemplate} and an
   * underscore-for-space XML encoded field name and returns the
   * FieldTemplate for that field, if known.  If not, null is
   * returned.</p> 
   */

  public FieldTemplate getObjectFieldType(Hashtable fieldHash, String fieldName)
  {
    return (FieldTemplate) fieldHash.get(XMLUtils.XMLDecode(fieldName));
  }

  /**
   * <p>This method actually does the work of sending our data to the
   * server.</p>
   */

  private void transmitData()
  {
    boolean success = true;
    ReturnVal attempt;

    /* -- */

    try
      {
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

	    session.enableWizards(false); // we're not interactive, don't give us no wizards
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("Ganymede xmlclient: couldn't open transaction " + 
			       username + ": " + ex.getMessage());
	    return;
	  }

	System.err.println("Creating objects");

	for (int i = 0; success && i < createdObjects.size(); i++)
	  {
	    xmlobject newObject = (xmlobject) createdObjects.elementAt(i);

	    System.err.println("Creating " + newObject);
	
	    attempt = newObject.createOnServer(session);

	    if (attempt != null && !attempt.didSucceed())
	      {
		String msg = attempt.getDialogText();

		if (msg != null)
		  {
		    System.err.println("Error creating " + newObject + ", reason: " + msg);
		  }
		else
		  {
		    System.err.println("Error creating " + newObject + ", no reason given.");
		  }

		success = false;
		continue;
	      }

	    attempt = newObject.registerFields(0); // everything but invids

	    if (attempt != null && !attempt.didSucceed())
	      {
		String msg = attempt.getDialogText();

		if (msg != null)
		  {
		    System.err.println("Error registering fields for " + newObject + ", reason: " + msg);
		  }
		else
		  {
		    System.err.println("Error registering fields for " + newObject + ", no reason given.");
		  }

		success = false;
		continue;
	      }
	  }

	// at this point, all objects we need to create are created,
	// and any non-invid fields in those new objects have been
	// registered.  We now need to register any invid fields in
	// the newly created objects, which should be able to resolve
	// now.

	for (int i = 0; success && i < createdObjects.size(); i++)
	  {
	    xmlobject newObject = (xmlobject) createdObjects.elementAt(i);

	    System.err.println("Resolving invids for " + newObject);
	
	    attempt = newObject.registerFields(1); // just invids

	    if (attempt != null && !attempt.didSucceed())
	      {
		String msg = attempt.getDialogText();

		if (msg != null)
		  {
		    System.err.println("Error registering fields for " + newObject + ", reason: " + msg);
		  }
		else
		  {
		    System.err.println("Error registering fields for " + newObject + ", no reason given.");
		  }

		success = false;
		continue;
	      }
	  }

	// now we need to register fields in the edited objects

	for (int i = 0; success && i < editedObjects.size(); i++)
	  {
	    xmlobject object = (xmlobject) editedObjects.elementAt(i);

	    System.err.println("Editing " + object);

	    attempt = object.editOnServer(session);

	    if (attempt != null && !attempt.didSucceed())
	      {
		String msg = attempt.getDialogText();

		if (msg != null)
		  {
		    System.err.println("Error editing object " + object + ", reason: " + msg);
		  }
		else
		  {
		    System.err.println("Error editing object " + object + ", no reason given.");
		  }

		success = false;
		continue;
	      }
	
	    attempt = object.registerFields(2); // invids and others

	    if (attempt != null && !attempt.didSucceed())
	      {
		String msg = attempt.getDialogText();

		if (msg != null)
		  {
		    System.err.println("Error registering fields for " + object + ", reason: " + msg);
		  }
		else
		  {
		    System.err.println("Error registering fields for " + object + ", no reason given.");
		  }

		success = false;
		continue;
	      }
	  }

	// now we need to inactivate any objects to be inactivated

	for (int i = 0; success && i < inactivatedObjects.size(); i++)
	  {
	    xmlobject object = (xmlobject) inactivatedObjects.elementAt(i);

	    System.err.println("Inactivating " + object);
	    
	    Invid target = object.getInvid();

	    if (target == null)
	      {
		System.err.println("Error, couldn't find Invid for object to be inactivated: " + object);

		success = false;
		continue;
	      }

	    try
	      {
		attempt = session.inactivate_db_object(target);
	      }
	    catch (RemoteException ex)
	      {
		ex.printStackTrace();
		throw new RuntimeException(ex.getMessage());
	      }
	
	    if (attempt != null && !attempt.didSucceed())
	      {
		String msg = attempt.getDialogText();

		if (msg != null)
		  {
		    System.err.println("Error inactivating " + object + ", reason: " + msg);
		  }
		else
		  {
		    System.err.println("Error inactivating  " + object + ", no reason given.");
		  }

		success = false;
		continue;
	      }
	  }

	// and we need to delete any objects to be deleted

	for (int i = 0; success && i < deletedObjects.size(); i++)
	  {
	    xmlobject object = (xmlobject) deletedObjects.elementAt(i);

	    System.err.println("Deleting " + object);
	    
	    Invid target = object.getInvid();

	    if (target == null)
	      {
		System.err.println("Error, couldn't find Invid for object to be deleted: " + object);

		success = false;
		continue;
	      }

	    try
	      {
		attempt = session.remove_db_object(target);
	      }
	    catch (RemoteException ex)
	      {
		ex.printStackTrace();
		throw new RuntimeException(ex.getMessage());
	      }

	    if (attempt != null && !attempt.didSucceed())
	      {
		String msg = attempt.getDialogText();

		if (msg != null)
		  {
		    System.err.println("Error deleting " + object + ", reason: " + msg);
		  }
		else
		  {
		    System.err.println("Error deleting  " + object + ", no reason given.");
		  }

		success = false;
		continue;
	      }
	  }

	// and close up the transaction, one way or another

	try
	  {
	    if (success)
	      {	
		System.err.println("Committing transaction");

		attempt = session.commitTransaction(true);

		if (attempt != null && !attempt.didSucceed())
		  {
		    String msg = attempt.getDialogText();

		    if (msg != null)
		      {
			System.err.println("Error committing transaction, reason: " + msg);
		      }
		    else
		      {
			System.err.println("Error committing transaction, no reason given.");
		      }
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
      }
  }
  
  /**
   * <p>Called when the server forces a disconnect.</p>
   *
   * <p>Call getMessage() on the
   * {@link arlut.csd.ganymede.client.ClientEvent ClientEvent} 
   * to get the reason for the disconnect.</p> */

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
    // this is mostly used so the server can asynchronously notify the
    // client about the server's build activity.  for now we're just
    // ignoring any such messages
  }
}

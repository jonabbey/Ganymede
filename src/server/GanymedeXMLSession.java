/*
   GASH 2

   GanymedeXMLSession.java

   The GANYMEDE object storage system.

   Created: 1 August 2000
   Release: $Name:  $
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 2000/08/28 21:49:18 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

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

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

import arlut.csd.Util.*;
import org.xml.sax.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                              GanymedeXMLSession

------------------------------------------------------------------------------*/

/**
 * <p>This class handles operations and client interactions for the Ganymede
 * server in handling XML file loading.</p>
 */

public final class GanymedeXMLSession implements XMLSession {

  public static final boolean debug = false;

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
   * <p>The working GanymedeSession underlying this XML session.</p>
   */

  GanymedeSession session;

  /**
   * <p>The XML parser object handling XML data from the client</p>
   */

  XMLReader reader;

  /**
   * <p>The data stream used to write data from the client to the
   * XML parser.</p>
   */

  PipedOutputStream pipe;

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
   * <p>Vector of {@link arlut.csd.ganymede.client.xmlobject
   * xmlobjects} that correspond to Ganymede server objects that have
   * been created/checked out for editing during embedded invid field
   * processing, and which need to have their invid fields registered
   * after everything else is done.</p> 
   */

  public Vector embeddedObjects = new Vector();

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
   * <p>This StringBuffer holds output generated by the GanymedeXMLSession's
   * parser thread.</p>
   */

  private StringBuffer parserMessages = new StringBuffer();
  
  /* -- */

  public GanymedeXMLSession(GanymedeSession session)
  {
    this.session = session;

    try
      {
	pipe = new PipedOutputStream();
	reader = new XMLReader(pipe, bufferSize, true);
      }
    catch (IOException ex)
      {
	System.err.println("IO Exception in initializing GanymedeXMLSession.");
	ex.printStackTrace();
	throw new RuntimeException(ex);
      }
  }

  /**
   * <p>This method is called repeatedly by the XML client in order to
   * send the next packet of XML data to the server.  If the server
   * has detected any errors in the already-received XML stream,
   * xmlSubmit() may return a non-null ReturnVal with a description of
   * the failure.  Otherwise, the xmlSubmit() method will enqueue the
   * XML data for the server's continued processing and immediately
   * return a null value, indicating success.  The xmlSubmit() method
   * will only block if the server has filled up its internal buffers
   * and must wait to digest more of the already submitted XML.</p> 
   */

  public ReturnVal xmlSubmit(byte[] bytes)
  {
    try
      {
	pipe.write(bytes);
      }
    catch (IOException ex)
      {
	return Ganymede.createErrorDialog("xmlSubmit i/o error",
					  ex.getMessage());
      }

    String progress = reader.getFailureMessage(true);

    if (!reader.isDone())
      {
	if (progress.length() > 0)
	  {
	    ReturnVal retVal = new ReturnVal(true);
	    retVal.setDialog(new JDialogBuff("XML client messages",
					     progress,
					     "OK",
					     null,
					     "ok.gif"));
	    
	    return retVal;
	  }
	else
	  {
	    return null;	// success, nothing to report
	  }
      }
    else
      {
	return Ganymede.createErrorDialog("XML submit errors",
					  progress);
      }
  }

  /**
   * <p>This method is called by the XML client once the end of the XML
   * stream has been transmitted, whereupon the server will attempt
   * to finalize the XML transaction and return an overall success or
   * failure message in the ReturnVal.  The xmlEnd() method will block
   * until the server finishes processing all the XML data previously
   * submitted by xmlSubmit().</p>
   */

  public ReturnVal xmlEnd()
  {
    return null;
  }

  /**
   * <p>This method is to be used by XML parsing engine code to add
   * an error message to the buffer passed back to the client on
   * subsequent xmlSubmit and xmlEnd calls.</p>
   */

  public void reportError(String message)
  {
  }

  /**
   * <p>This method handles the actual XML processing, once the
   * command line arguments have been parsed and handled by the
   * xmlclient constructor.</p>
   */

  public void run()
  {
    try
      {
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
	    if (nextElement.getAttrStr("persona") != null)
	      {
		username = nextElement.getAttrStr("persona");
	      }

	    if (nextElement.getAttrStr("password") != null)
	      {
		password = nextElement.getAttrStr("password");
	      }

	    if (!processData())
	      {
		// don't both processing rest of XML
		// doc.. just jump down to finally clause

		return; 
	      }

	    nextElement = getNextItem();
	  }

	while (!nextElement.matchesClose("ganymede") && !(nextElement instanceof XMLCloseElement))
	  {
	    System.err.println("Skipping unrecognized element: " + nextElement);
	    nextElement = getNextItem();
	  }
      }
    catch (Exception ex)
      {
	ex.printStackTrace();
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
    System.err.println("Skipping <ganyschema> element (no schema loading support yet)");

    XMLItem item = getNextItem();

    while (!item.matchesClose("ganyschema") && !(item instanceof XMLEndDocument))
      {
	item = getNextItem();
      }

    System.err.println("</ganyschema>");
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
   *
   * @returns true if the &lt;ganydata&gt; element was successfully
   * processed, or false if a fatal error in the XML stream was
   * encountered during processing 
   */

  public boolean processData() throws SAXException
  {
    XMLItem item;
    boolean committedTransaction = false;

    /* -- */

    if (debug)
      {
	System.err.println("processData");
      }

    connectAsClient();

    try
      {
	System.out.print("Reading object data");

	item = getNextItem();

	while (!item.matchesClose("ganydata") && !(item instanceof XMLEndDocument))
	  {
	    if (item.matches("object"))
	      {
		xmlobject objectRecord = null;

		try
		  {
		    objectRecord = new xmlobject((XMLElement) item);
		  }
		catch (NullPointerException ex)
		  {
		    // bad field or object error.. return out of this
		    // method without committing the transaction
		    // our finally clause will log us out

		    return false;
		  }

		System.out.print(".");

		//	    System.err.println(item);

		String mode = objectRecord.getMode();
	    
		if (mode == null || mode.equals("create"))
		  {
		    // if they did specify "create" as the object
		    // action mode, this object definition record will
		    // be forced into a new object, rather than trying
		    // to look for an object on the server with
		    // matching identity attributes

		    // this can be useful if the user wants to create
		    // new objects without worrying about whether
		    // there are id conflicts with the server's state

		    if (mode != null)
		      {
			objectRecord.forceCreate = true;
		      }

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

		if (!storeObject(objectRecord))
		  {
		    System.err.println("\nError, xml object " + objectRecord + " is not unique within the XML file.");
		    
		    // our finally clause will log us out

		    return false;
		  }
	      }

	    item = getNextItem();
	  }

	System.out.print("\n");

	committedTransaction = transmitData();

	return committedTransaction;
      }
    finally
      {
	if (!committedTransaction)
	  {
	    System.out.println("Aborted transaction, logging out.");
	  }

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
   * <p>This private helper method handles logging on to the server as
   * a normal client, and sets up the {@link
   * arlut.csd.ganymede.client.xmlclient#session session} variable.</p>
   */

  private void connectAsClient()
  {
    // Loader is inherited from java.lang.Thread, so we can just
    // create one and start it running so that it will talk to the
    // server and download type and mapping information from the
    // server, in the background

    if (debug)
      {
	System.err.println("Creating loader");
      }

    loader = new Loader(session, false);

    if (debug)
      {
	System.err.println("Starting loader thread");
      }

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

    if (debug)
      {
	System.err.println("Finished downloading object type information from server.");
      }
  }

  /**
   * <p>This method records an xmlobject that has been loaded from the
   * XML file into the xmlclient objectTypes hash.</p>
   *
   * <p>This method returns false if the object to be stored has an id
   * conflict with a previously stored object.</p>
   */

  public boolean storeObject(xmlobject object)
  {
    Hashtable objectHash = (Hashtable) objectStore.get(object.type);

    if (objectHash == null)
      {
	objectHash = new Hashtable();
	objectStore.put(object.type, objectHash);
      }

    if (object.id != null)
      {
	if (objectHash.containsKey(object.id))
	  {
	    return false;
	  }

	objectHash.put(object.id, object);
      }
    else if (object.num != -1)
      {
	Integer intKey = new Integer(object.num);

	if (objectHash.containsKey(intKey))
	  {
	    return false;
	  }

	objectHash.put(intKey, object);
      }
    
    return true;
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
	    if (debug)
	      {
		System.err.println("Calling findLabeledObject() on " + typeId + ":" + objId);
	      }

	    invid = session.findLabeledObject(objId, typeId);

	    if (debug)
	      {
		System.err.println("Returned from findLabeledObject() on " + typeId + ":" + objId);
		System.err.println("findLabeledObject() returned " + invid);
	      }
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
	    invid = ((xmlobject) element).getInvid();

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
   *
   * @returns true if the data was successfully sent to the server and
   * the transaction committed successfully, false if the transaction
   * had problems and was abandoned.
   */

  private boolean transmitData()
  {
    boolean success = true;
    ReturnVal attempt;

    /* -- */

    try
      {
	System.out.println("Opening transaction");

	attempt = session.openTransaction("xmlclient client (" + username + ")", false);
	    
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

	    return false;
	  }

	session.enableWizards(false); // we're not interactive, don't give us no wizards
      }
    catch (RemoteException ex)
      {
	System.err.println("Ganymede xmlclient: couldn't open transaction " + 
			   username + ": " + ex.getMessage());
	return false;
      }

    for (int i = 0; success && i < createdObjects.size(); i++)
      {
	xmlobject newObject = (xmlobject) createdObjects.elementAt(i);
	
	// if the object has enough information that we can look it up
	// on the server (and get an Invid for it), assume that it
	// already exists and go ahead and pull it for editing rather
	// than creating it, unless the forceCreate flag is on.

	if (!newObject.forceCreate && newObject.getInvid() != null)
	  {
	    System.out.println("Editing " + newObject);

	    attempt = newObject.editOnServer(session);

	    if (attempt != null && !attempt.didSucceed())
	      {
		String msg = attempt.getDialogText();

		if (msg != null)
		  {
		    System.err.println("Error editing object " + newObject + ", reason: " + msg);
		  }
		else
		  {
		    System.err.println("Error editing object " + newObject + ", no reason given.");
		  }

		success = false;
		continue;
	      }
	  }
	else
	  {
	    System.out.println("Creating " + newObject);

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
	  }

	// we can't be sure that we can register invid fields
	// until all objects that we need to create are
	// created.. for now, just register non-invid fields

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

	System.out.println("Resolving pointers for " + newObject);
	
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

	System.out.println("Editing " + object);

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

    for (int i = 0; success && i < embeddedObjects.size(); i++)
      {
	xmlobject object = (xmlobject) embeddedObjects.elementAt(i);
	
	attempt = object.registerFields(1); // only non-embedded invids

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

	System.out.println("Inactivating " + object);
	    
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

	System.out.println("Deleting " + object);
	    
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
	    System.out.println("Committing transaction");

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

		success = false;
	      }

	    System.out.println("Transaction successfully committed.");
	    System.out.println("Done.");
	  }
	else
	  {
	    System.err.println("Errors encountered, aborting transaction.");

	    // the disconnect below will abort the transaction and log us out
	  }
      }
    catch (RemoteException ex)
      {
	System.err.println("Ganymede.xmlclient: remote exception " + ex.getMessage());
      }

    return success;
  }
}

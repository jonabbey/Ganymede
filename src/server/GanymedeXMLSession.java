/*
   GASH 2

   GanymedeXMLSession.java

   The GANYMEDE object storage system.

   Created: 1 August 2000
   Release: $Name:  $
   Version: $Revision: 1.12 $
   Last Mod Date: $Date: 2000/09/17 10:04:36 $
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
import arlut.csd.JDialog.JDialogBuff;
import org.xml.sax.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                              GanymedeXMLSession

------------------------------------------------------------------------------*/

/**
 * <p>This class handles operations and client interactions for the Ganymede
 * server in handling XML file loading.</p>
 */

public final class GanymedeXMLSession extends java.lang.Thread implements XMLSession, Unreferenced {

  public static final boolean debug = false;

  /**
   * <p>This major version number is compared with the "major"
   * attribute in the Ganymede XML document element.  We won't
   * try to read Ganymede XML files whose major and/or minor numbers
   * are too high.</p> 
   */

  public static final int majorVersion = 1;

  /**
   * <p>This minor version number is compared with the "minor"
   * attribute in the Ganymede XML document element.  We won't
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
   * <p>Hashtable mapping object type names to
   * hashtables mapping field names to 
   * {@link arlut.csd.ganymede.FieldTemplate FieldTemplate}
   * objects.</p>
   */

  public Hashtable objectTypes = new Hashtable();

  /**
   * <p>Hashtable mapping Short object type ids to
   * hashtables mapping field names to 
   * {@link arlut.csd.ganymede.FieldTemplate FieldTemplate}
   * objects.</p>
   */

  public Hashtable objectTypeIDs = new Hashtable();

  /**
   * <P>Hashtable mapping Short type ids to
   * hashes which map local object designations to
   * actual {@link arlut.csd.ganymede.xmlobject xmlobject}
   * records.</P>
   */

  public Hashtable objectStore = new Hashtable();

  /**
   * <p>Vector of {@link arlut.csd.ganymede.xmlobject xmlobjects}
   * that correspond to new Ganymede server objects
   * that have been/need to be created by this GanymedeXMLSession.</p>
   */

  public Vector createdObjects = new Vector();

  /**
   * <p>Vector of {@link arlut.csd.ganymede.xmlobject xmlobjects}
   * that correspond to pre-existing Ganymede
   * server objects that have been/need to be checked out for editing by this
   * GanymedeXMLSession.</p> 
   */

  public Vector editedObjects = new Vector();

  /**
   * <p>Vector of {@link arlut.csd.ganymede.xmlobject
   * xmlobjects} that correspond to Ganymede server objects that have
   * been created/checked out for editing during embedded invid field
   * processing, and which need to have their invid fields registered
   * after everything else is done.</p> 
   */

  public Vector embeddedObjects = new Vector();

  /**
   * <p>Vector of {@link arlut.csd.ganymede.xmlobject xmlobjects}
   * that correspond to pre-existing Ganymede
   * server objects that have been/need to be inactivated by this
   * GanymedeXMLSession.</p>
   */

  public Vector inactivatedObjects = new Vector();

  /**
   * <p>Vector of {@link arlut.csd.ganymede.xmlobject xmlobjects}
   * that correspond to pre-existing Ganymede
   * server objects that have been/need to be deleted by this
   * GanymedeXMLSession.</p>
   */

  public Vector deletedObjects = new Vector();

  /**
   * <p>This StringWriter holds output generated by the GanymedeXMLSession's
   * parser thread.</p>
   */

  public StringWriter errBuf = new StringWriter();

  /**
   * <p>This PrintWriter is used to handle all debug/error output
   * on behalf of the GanymedeXMLSession.</p>
   */

  public PrintWriter err = new PrintWriter(errBuf);

  /**
   * <p>This flag is used to track whether the background parser thread
   * is active.</p>
   *
   * <p>We set it true here so that we avoid any race conditions.</p>
   */

  private boolean parsing = true;

  /**
   * <p>This flag is used to track whether the background parser thread
   * was successful in committing the transaction.</p>
   */

  private boolean success = false;
  
  /* -- */

  public GanymedeXMLSession(GanymedeSession session)
  {
    this.session = session;

    // tell the GanymedeSession about us, so they can notify us with
    // the stopParser() method if our server login gets forcibly
    // revoked.

    session.setXSession(this);

    initializeLookups();

    try
      {
	// We create a PipedOutputStream that we will write data from
	// the XML client into.  The XMLReader will create a matching
	// PipedInputStream internally, that it will use to read data
	// that we feed into the pipe.

	pipe = new PipedOutputStream();
	reader = new XMLReader(pipe, bufferSize, true);
      }
    catch (IOException ex)
      {
	System.err.println("IO Exception in initializing GanymedeXMLSession.");
	ex.printStackTrace();
	throw new RuntimeException(ex.getMessage());
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
    session.checklogin();

    if (parsing)
      {
	try
	  {
	    pipe.write(bytes);	// can block if the parser thread gets behind
	  }
	catch (IOException ex)
	  {
	    return getReturnVal("pipe writing: " + ex.getMessage(), false);
	  }
      }
    else
      {
	System.err.println("GanymedeXMLSession xmlSubmit, !parsing, skipping writing into pipe");
      }

    // if reader is not done, we're ok to continue

    return getReturnVal(null, !reader.isDone());
  }

  /**
   * <p>This method is called by the XML client once the end of the XML
   * stream has been transmitted, whereupon the server will attempt
   * to finalize the XML transaction and return an overall success or
   * failure message in the ReturnVal.  The xmlEnd() method will block
   * until the server finishes processing all the XML data previously
   * submitted by xmlSubmit().</p>
   * 
   * <p>This method is synchronized to cause it to block until the
   * background parser completes.</p>
   */

  public synchronized ReturnVal xmlEnd()
  {
    // note again that we are synchronized, so that we won't start to
    // execute this method until the (also synchronized) run() method
    // terminates.. in this way, xmlEnd will block until the parsing
    // process completes, and the transaction has been committed or
    // aborted

    // return a summation of what happened

    return getReturnVal(null, success);
  }

  /**
   * <p>This method is for use on the server, and is called by the
   * GanymedeSession to let us know if the server is forcing our login
   * off.</p>
   */

  public void abort()
  {
    if (debug)
      {
	System.err.println("GanymedeXMLSession abort");

	try
	  {
	    throw new RuntimeException("GanymedeXMLSession abort trace");
	  }
	catch (RuntimeException ex)
	  {
	    ex.printStackTrace();
	  }
      }

    // if the parser thread has completed, then parsing will be false
    // and the XML reader will have already been closed

    if (parsing)
      {
	if (debug)
	  {
	    System.err.println("GanymedeXMLSession closing reader");
	  }

	reader.close();		// this will cause the XML Reader to halt
      }
  }


  /**
   * <p>This method is called when the Java RMI system detects that this
   * remote object is no longer referenced by any remote objects.</p>
   *
   * <p>This method handles abnormal logouts and time outs for us.  By
   * default, the 1.1 RMI time-out is 10 minutes.</p>
   *
   * @see java.rmi.server.Unreferenced
   */

  public void unreferenced()
  {
    session.unreferenced();
  }

  /**
   * Something to assist in garbage collection.
   */

  public void cleanup()
  {
    objectTypes.clear();
    objectTypes = null;

    objectStore.clear();
    objectStore = null;
    
    createdObjects.setSize(0);
    createdObjects = null;
    
    editedObjects.setSize(0);
    editedObjects = null;
    
    embeddedObjects.setSize(0);
    embeddedObjects = null;
    
    inactivatedObjects.setSize(0);
    inactivatedObjects = null;
    
    deletedObjects.setSize(0);
    deletedObjects = null;

    if (session != null && session.logged_in)
      {
	session.logout();
	session = null;
      }
  }

  /**
   * <p>This method handles the actual XML processing in the
   * background.  All activity which ultimately draws from
   * the XMLReader will block as necessary to wait for more
   * data from the client.</p>
   */

  public synchronized void run()
  {
    try
      {
	if (debug)
	  {
	    System.err.println("GanymedeXMLSession run getting startDocument");
	  }

	XMLItem startDocument = getNextItem();

	if (!(startDocument instanceof XMLStartDocument))
	  {
	    err.println("XML parser error: first element " + startDocument + 
			" not XMLStartDocument");
	    return;
	  }

	if (debug)
	  {
	    System.err.println("GanymedeXMLSession run getting docElement");
	  }

	XMLItem docElement = getNextItem();

	if (!docElement.matches("ganymede"))
	  {
	    err.println("Error, XML Stream does not contain a Ganymede XML file.");
	    err.println("Unrecognized XML element: " + docElement);
	    return;
	  }

	Integer majorI = docElement.getAttrInt("major");
	Integer minorI = docElement.getAttrInt("minor");

	if (majorI == null || majorI.intValue() > majorVersion)
	  {
	    err.println("Error, the ganymede document element " + docElement +
			" does not contain a compatible major version number");
	    return;
	  }
	
	if (majorI.intValue() == majorVersion && 
	    (minorI == null || minorI.intValue() > minorVersion))
	  {
	    err.println("Error, the ganymede document element " + docElement +
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
	    err.println("Skipping unrecognized element: " + nextElement);
	    nextElement = getNextItem();
	  }
      }
    catch (Exception ex)
      {
	// we may get a SAXException here if the reader gets
	// shutdown before our parsing process is done, or if
	// there is something malformed in the XML

	System.err.println("caught exception for GanymedeXMLSession run()");

	ex.printStackTrace();

	err.println(ex.getMessage());
      }
    finally
      {
	parsing = false;

	if (reader != null)
	  {
	    reader.close();
	  }

	cleanup();
      }
  }

  /**
   * <p>Private helper method to process events from
   * the {@link arlut.csd.Util.XMLReader XMLReader}.  By using
   * this method, the rest of the code in GanymedeXMLSession doesn't
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
	    throw new SAXException(item.toString());
	  }

	if (item instanceof XMLWarning)
	  {
	    err.println("Warning!: " + item);
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
    err.println("Skipping <ganyschema> element (no schema loading support yet)");

    XMLItem item = getNextItem();

    while (!item.matchesClose("ganyschema") && !(item instanceof XMLEndDocument))
      {
	item = getNextItem();
      }
  }

  /**
   * <p>This method is called after the &lt;ganydata&gt; element has been
   * read and consumes everything up to and including the matching
   * &lt;/ganydata&gt; element, if such is to be found.</p>
   *
   * <p>Before starting to read data from the &lt;ganydata&gt; element,
   * this method communicates with the Ganymede server database through the
   * normal client {@link arlut.csd.ganymede.Session Session} interface.</p>
   *
   * <p>The contents of &lt;ganydata&gt; are scanned, and an in-memory
   * datastructure is constructed in the GanymedeXMLSession.  All
   * objects are organized in memory by type and id, and inter-object
   * invid references are resolved to the extent possible.</p>
   *
   * <p>If all of that succeeds, processData() will start a transaction
   * on the server, and will start transferring the data from the XML
   * file's &lt;ganydata&gt; element into the database.  If any errors
   * are reported, the returned error message is printed and processData
   * aborts.  If no errors are reported at this stage, a transaction
   * commit is attempted.  Once again, if there are any errors reported
   * from the server, they are printed and processData aborts.  Otherwise,
   * success!</p>
   *
   * @returns true if the &lt;ganydata&gt; element was successfully
   * processed, or false if a fatal error in the XML stream was
   * encountered during processing */

  public boolean processData() throws SAXException
  {
    XMLItem item;
    boolean committedTransaction = false;

    /* -- */

    if (debug)
      {
	err.println("processData");
      }

    try
      {
	item = getNextItem();

	while (!item.matchesClose("ganydata") && !(item instanceof XMLEndDocument))
	  {
	    if (item.matches("object"))
	      {
		xmlobject objectRecord = null;

		try
		  {
		    objectRecord = new xmlobject((XMLElement) item, this);
		  }
		catch (NullPointerException ex)
		  {
		    // bad field or object error.. return out of this
		    // method without committing the transaction
		    // our finally clause will log us out

		    ex.printStackTrace();

		    return false;
		  }

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
		    err.println("\nError, xml object " + objectRecord + " is not unique within the XML file.");
		    
		    // our finally clause will log us out

		    return false;
		  }
	      }

	    item = getNextItem();
	  }

	committedTransaction = transmitData();

	return committedTransaction;
      }
    catch (Exception ex)
      {
	err.println("Process data.. caught an exception " + ex.getMessage());

	ex.printStackTrace();

	return false;
      }
    finally
      {
	if (!committedTransaction)
	  {
	    err.println("Aborted transaction, logging out.");
	  }

	session.logout();
      }
  }

  /**
   * <p>This private helper method handles logging on to the server as
   * a normal client, and sets up the {@link
   * arlut.csd.ganymede.client.xmlclient#session session} variable.</p>
   */

  private void initializeLookups()
  {
    if (debug)
      {
	System.err.println("GanymedeXMLSession: initializeLookups");
      }

    Vector baseList = Ganymede.db.getBases();

    for (int i = 0; i < baseList.size(); i++)
      {
	DBObjectBase base = (DBObjectBase) baseList.elementAt(i);
	Vector templates = base.getFieldTemplateVector();
	Hashtable fieldHash = new Hashtable();
	
	for (int j = 0; j < templates.size(); j++)
	  {
	    FieldTemplate tmpl = (FieldTemplate) templates.elementAt(j);

	    fieldHash.put(tmpl.getName(), tmpl);
	  }

	objectTypes.put(base.getName(), fieldHash);
	objectTypeIDs.put(new Short(base.getTypeID()), fieldHash);
      }
  }

  /**
   * <p>This method records an xmlobject that has been loaded from the
   * XML file into the GanymedeXMLSession objectStore hash.</p>
   *
   * <p>This method returns false if the object to be stored has an id
   * conflict with a previously stored object.</p>
   */

  public boolean storeObject(xmlobject object)
  {
    if (debug)
      {
	System.err.println("GanymedeXMLSession: storeObject(" + object + ")");
      }

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
	if (debug)
	  {
	    err.println("Calling findLabeledObject() on " + typeId + ":" + objId);
	  }
	
	invid = session.findLabeledObject(objId, typeId);
	
	if (debug)
	  {
	    err.println("Returned from findLabeledObject() on " + typeId + ":" + objId);
	    err.println("findLabeledObject() returned " + invid);
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
    // this is currently using a linear search.. we probably should
    // try to fix this at some point, but the number of object types
    // in the server n is likely to be really quite low, so this
    // probably won't hurt too bad

    DBObjectBase base = Ganymede.db.getObjectBase(XMLUtils.XMLDecode(objectTypeName));
    return base.getTypeID();
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
    DBObjectBase base = Ganymede.db.getObjectBase(objectTypeID);
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
   * <p>This helper method takes a short object type id and an
   * underscore-for-space XML encoded field name and returns the
   * FieldTemplate for that field, if known.  If not, null is
   * returned.</p> */

  public FieldTemplate getFieldTemplate(short type, String fieldName)
  {
    return getFieldTemplate(new Short(type), fieldName);
  }

  /**
   * <p>This helper method takes a short object type id and an
   * underscore-for-space XML encoded field name and returns the
   * FieldTemplate for that field, if known.  If not, null is
   * returned.</p> */

  public FieldTemplate getFieldTemplate(Short type, String fieldName)
  {
    Hashtable fieldHash = (Hashtable) objectTypeIDs.get(type);

    if (fieldHash == null)
      {
	return null;
      }

    return (FieldTemplate) fieldHash.get(XMLUtils.XMLDecode(fieldName));
  }

  public Vector getTemplateVector(Short type)
  {
    DBObjectBase base = Ganymede.db.getObjectBase(type);
    return base.getFieldTemplateVector();
  }

  public Vector getTemplateVector(short type)
  {
    DBObjectBase base = Ganymede.db.getObjectBase(type);
    return base.getFieldTemplateVector();
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
    Hashtable editCount = new Hashtable();
    Hashtable createCount = new Hashtable();
    Hashtable deleteCount = new Hashtable();
    Hashtable inactivateCount = new Hashtable();

    /* -- */

    attempt = session.openTransaction("xmlclient client (" + session.username + ")", false); // non-interactive
	    
    if (attempt != null && !attempt.didSucceed())
      {
	if (attempt.getDialog() != null)
	  {
	    err.println("Ganymede xmlclient: couldn't open transaction " + session.username +
			": " + attempt.getDialog().getText());
	  }
	else
	  {
	    err.println("Ganymede xmlclient: couldn't open transaction " + session.username);
	  }
	
	return false;
      }
    
    session.enableWizards(false); // we're not interactive, don't give us no wizards

    for (int i = 0; success && i < createdObjects.size(); i++)
      {
	xmlobject newObject = (xmlobject) createdObjects.elementAt(i);
	
	// if the object has enough information that we can look it up
	// on the server (and get an Invid for it), assume that it
	// already exists and go ahead and pull it for editing rather
	// than creating it, unless the forceCreate flag is on.

	if (!newObject.forceCreate && newObject.getInvid() != null)
	  {
	    incCount(editCount, newObject.typeString);

	    attempt = newObject.editOnServer(session);

	    if (attempt != null && !attempt.didSucceed())
	      {
		String msg = attempt.getDialogText();

		if (msg != null)
		  {
		    err.println("Error editing object " + newObject + ", reason: " + msg);
		  }
		else
		  {
		    err.println("Error editing object " + newObject + ", no reason given.");
		  }

		success = false;
		continue;
	      }
	  }
	else
	  {
	    incCount(createCount, newObject.typeString);

	    attempt = newObject.createOnServer(session);

	    if (attempt != null && !attempt.didSucceed())
	      {
		String msg = attempt.getDialogText();

		if (msg != null)
		  {
		    err.println("Error creating " + newObject + ", reason: " + msg);
		  }
		else
		  {
		    err.println("Error creating " + newObject + ", no reason given.");
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
		err.println("Error registering fields for " + newObject + ", reason: " + msg);
	      }
	    else
	      {
		err.println("Error registering fields for " + newObject + ", no reason given.");
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

	//	err.println("Resolving pointers for " + newObject);
	
	attempt = newObject.registerFields(1); // just invids

	if (attempt != null && !attempt.didSucceed())
	  {
	    String msg = attempt.getDialogText();

	    if (msg != null)
	      {
		err.println("Error registering fields for " + newObject + ", reason: " + msg);
	      }
	    else
	      {
		err.println("Error registering fields for " + newObject + ", no reason given.");
	      }

	    success = false;
	    continue;
	  }
      }

    // now we need to register fields in the edited objects

    for (int i = 0; success && i < editedObjects.size(); i++)
      {
	xmlobject object = (xmlobject) editedObjects.elementAt(i);

	incCount(editCount, object.typeString);

	attempt = object.editOnServer(session);

	if (attempt != null && !attempt.didSucceed())
	  {
	    String msg = attempt.getDialogText();

	    if (msg != null)
	      {
		err.println("Error editing object " + object + ", reason: " + msg);
	      }
	    else
	      {
		err.println("Error editing object " + object + ", no reason given.");
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
		err.println("Error registering fields for " + object + ", reason: " + msg);
	      }
	    else
	      {
		err.println("Error registering fields for " + object + ", no reason given.");
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
		err.println("Error registering fields for " + object + ", reason: " + msg);
	      }
	    else
	      {
		err.println("Error registering fields for " + object + ", no reason given.");
	      }

	    success = false;
	    continue;
	  }
      }

    // now we need to inactivate any objects to be inactivated

    for (int i = 0; success && i < inactivatedObjects.size(); i++)
      {
	xmlobject object = (xmlobject) inactivatedObjects.elementAt(i);

	incCount(inactivateCount, object.typeString);
	    
	Invid target = object.getInvid();

	if (target == null)
	  {
	    err.println("Error, couldn't find Invid for object to be inactivated: " + object);

	    success = false;
	    continue;
	  }

	attempt = session.inactivate_db_object(target);
	
	if (attempt != null && !attempt.didSucceed())
	  {
	    String msg = attempt.getDialogText();

	    if (msg != null)
	      {
		err.println("Error inactivating " + object + ", reason: " + msg);
	      }
	    else
	      {
		err.println("Error inactivating  " + object + ", no reason given.");
	      }

	    success = false;
	    continue;
	  }
      }

    // and we need to delete any objects to be deleted

    for (int i = 0; success && i < deletedObjects.size(); i++)
      {
	xmlobject object = (xmlobject) deletedObjects.elementAt(i);

	incCount(deleteCount, object.typeString);
	    
	Invid target = object.getInvid();

	if (target == null)
	  {
	    err.println("Error, couldn't find Invid for object to be deleted: " + object);

	    success = false;
	    continue;
	  }

	attempt = session.remove_db_object(target);

	if (attempt != null && !attempt.didSucceed())
	  {
	    String msg = attempt.getDialogText();

	    if (msg != null)
	      {
		err.println("Error deleting " + object + ", reason: " + msg);
	      }
	    else
	      {
		err.println("Error deleting  " + object + ", no reason given.");
	      }

	    success = false;
	    continue;
	  }
      }

    // and close up the transaction, one way or another
    
    if (success)
      {	
	err.println("Committing transaction\n");

	attempt = session.commitTransaction(true);

	if (attempt != null && !attempt.didSucceed())
	  {
	    String msg = attempt.getDialogText();

	    if (msg != null)
	      {
		err.println("Error committing transaction, reason: " + msg);
	      }
	    else
	      {
		err.println("Error committing transaction, no reason given.");
	      }

	    success = false;
	  }
	else
	  {
	    // set the top-level success flag so that xmlEnd() will
	    // return a success value
	    
	    this.success = true;
	  }

	if (createCount.size() > 0)
	  {
	    err.println("Objects created:");

	    Enumeration enum = createCount.keys();

	    while (enum.hasMoreElements())
	      {
		String key = (String) enum.nextElement();

		err.println("\t" + key + ": " + createCount.get(key));
	      }
	  }

	if (editCount.size() > 0)
	  {
	    err.println("Objects edited:");

	    Enumeration enum = editCount.keys();

	    while (enum.hasMoreElements())
	      {
		String key = (String) enum.nextElement();

		err.println("\t" + key + ": " + editCount.get(key));
	      }
	  }

	if (deleteCount.size() > 0)
	  {
	    err.println("Objects deleted:");

	    Enumeration enum = deleteCount.keys();

	    while (enum.hasMoreElements())
	      {
		String key = (String) enum.nextElement();

		err.println("\t" + key + ": " + deleteCount.get(key));
	      }
	  }

	if (inactivateCount.size() > 0)
	  {
	    err.println("Objects inactivated:");

	    Enumeration enum = inactivateCount.keys();

	    while (enum.hasMoreElements())
	      {
		String key = (String) enum.nextElement();

		err.println("\t" + key + ": " + inactivateCount.get(key));
	      }
	  }

	err.println("\nTransaction successfully committed.");
      }
    else
      {
	err.println("Errors encountered, aborting transaction.");
	
	// the disconnect below will abort the transaction and log us out
      }

    return success;
  }

  private void incCount(Hashtable table, String type)
  {
    Integer x = (Integer) table.get(type);

    if (x == null)
      {
	table.put(type, new Integer(1));
      }
    else
      {	
	table.put(type, new Integer(x.intValue() + 1));
      }
  }

  /**
   * <p>This private helper method creates a ReturnVal object to be
   * passed back to the xmlclient.  Any text printed to the err
   * object will be included in the ReturnVal object, followed by
   * the content of message, if any.  If success is true, the
   * ReturnVal returned will encode that.  If success is false,
   * the returned ReturnVal will indicate failure.</p>
   */

  private ReturnVal getReturnVal(String message, boolean success)
  {
    String progress;
    StringBuffer errBuffer = errBuf.getBuffer();

    synchronized (errBuffer)
      {
	progress = errBuffer.toString();
	errBuffer.setLength(0);	// this doesn't actually free memory.. stoopid StringBuffer
      }

    if (message != null && !message.equals(""))
      {
	progress = progress + "\n" + message;
      }

    if (success)
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
}

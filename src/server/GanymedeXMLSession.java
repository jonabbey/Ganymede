/*
   GASH 2

   GanymedeXMLSession.java

   The GANYMEDE object storage system.

   Created: 1 August 2000
   Release: $Name:  $
   Version: $Revision: 1.38 $
   Last Mod Date: $Date: 2002/03/15 02:25:43 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
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
 * <p>This class handles all XML loading operations for the Ganymede
 * server.  GanymedeXMLSession's are created by the {@link
 * arlut.csd.ganymede.GanymedeServer GanymedeServer}'s {@link
 * arlut.csd.ganymede.GanymedeServer#xmlLogin(arlut.csd.ganymede.Client)
 * xmlLogin()} method.  A GanymedeXMLSession is created on top of a
 * {@link arlut.csd.ganymede.GanymedeSession GanymedeSession} and
 * interacts with the database through that session.  A
 * GanymedeXMLSession generally looks to the rest of the server like
 * any other client, except that if the XML file contains a
 * &lt;ganyschema&gt; section, the GanymedeXMLSession will attempt to
 * manipulate the server's login semaphore to force the server into
 * schema editing mode.  This will fail if there are any remote
 * clients connected to the server at the time the XML file is
 * processed.</p>
 *
 * <p>Once xmlLogin creates (and RMI exports) a GanymedeXMLSession, an xmlclient
 * repeatedly calls the {@link arlut.csd.ganymede.GanymedeXMLSession#xmlSubmit(byte[]) xmlSubmit()}
 * method, which writes the bytes received into a pipe.  The GanymedeXMLSession's
 * thread (also initiated by GanymedeServer.xmlLogin()) then loops, reading
 * data off of the pipe with an {@link arlut.csd.Util.XMLReader XMLReader} and
 * doing various schema editing and data loading operations.</p>
 *
 * <p>The &lt;ganydata&gt; processing section was originally written as part
 * of xmlclient, and did all xml parsing on the client side and all data operations
 * remotely over RMI.  Pulling this logic into a server-side
 * GanymedeXMLSession sped things up by a factor of 300 in my testing.</p>
 */

public final class GanymedeXMLSession extends java.lang.Thread implements XMLSession, Unreferenced {

  public static final boolean debug = false;
  public static final boolean schemadebug = true;

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

  public int bufferSize = 100;

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

  /**
   * <p>If we are editing the server's schema from the XML source, this
   * field will hold a reference to a DBSchemaEdit object.</p>
   */

  private DBSchemaEdit editor = null;

  /**
   * <p>This vector is used by the XML schema editing logic to track
   * namespaces from the xml file that need to be added to the current
   * schema.  Elements in this vector are empty XMLElements that contain
   * name and optional case-sensitive attributes.</p>
   */

  private Vector spacesToAdd;

  /**
   * <p>This vector is used by the XML schema editing logic to track
   * namespaces from the xml file that need to be removed from the
   * current schema. Elements in this vector are Strings representing
   * the level of name spaces to be deleted..</p> 
   */

  private Vector spacesToRemove;

  /**
   * <p>This vector is used by the XML schema editing logic to track
   * namespaces from the xml file that need to be edited in the
   * current schema.  Since namespaces can only be edited in the sense
   * of toggling the case sensitivity flag, this vector will only
   * contain XMLElements for namespaces that need to have their case
   * sensitivity toggled. Elements in this vector are empty
   * XMLElements that contain name and optional case-sensitive
   * attributes.</p> 
   */

  private Vector spacesToEdit;

  /**
   * <p>This vector is used by the XML schema editing logic to track
   * object types from the xml file that need to be added to the current
   * schema.  Elements in this vector are XMLItem trees rooted
   * with the appropriate &lt;objectdef&gt; elements.</p>
   */

  private Vector basesToAdd;

  /**
   * <p>This vector is used by the XML schema editing logic to track
   * object types in the current schema that were not mentioned in the
   * xml file and thus need to be removed from the current
   * schema. Elements of this vector are the names of existing bases
   * to be removed.</p>
   */

  private Vector basesToRemove;

  /**
   * <p>This vector is used by the XML schema editing logic to track
   * object types from the xml file that need to be edited in the
   * current schema.  Elements in this vector are XMLItem trees rooted
   * with the appropriate &lt;objectdef&gt; elements.</p> 
   */

  private Vector basesToEdit;

  /**
   * <p>This XMLItem is the XMLElement root of the namespace tree,
   * rooted with the &lt;namespaces&gt; element.  Children of this
   * node will be &lt;namespace&gt; elements.</p>
   */

  private XMLItem namespaceTree = null;

  /**
   * <p>This XMLItem is the XMLElement root of the category tree,
   * rooted with the top-level &lt;category&gt; element.
   * Children of this node will be either &lt;category&gt; or
   * &lt;objectdef&gt; elements.</p> 
   */

  private XMLItem categoryTree = null;

  /**
   * <p>We're using this Vector as a super-simple sempahore thingy,
   * to gate the cleanup() method.</p>
   */

  private Vector cleanedup = new Vector();
  
  /* -- */

  public GanymedeXMLSession(GanymedeSession session)
  {
    this.session = session;

    // tell the GanymedeSession about us, so they can notify us with
    // the stopParser() method if our server login gets forcibly
    // revoked.

    session.setXSession(this);

    try
      {
	// We create a PipedOutputStream that we will write data from
	// the XML client into.  The XMLReader will create a matching
	// PipedInputStream internally, that it will use to read data
	// that we feed into the pipe.

	pipe = new PipedOutputStream();
	reader = new XMLReader(pipe, bufferSize, true, err);
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
	    // the XMLReader may provide our error buffer with more
	    // details about what happened after the parser closes the
	    // pipe, so we'll spin here for a bit until the reader
	    // finishes with everything and closes down.

	    // but, because we're not nuts, we'll not wait more than
	    // 10 seconds

	    // note also that we don't assume that reader is not null here.. if
	    // the parser throws an exception, it's possible that that won't
	    // directly cause our run() method to terminate with an exception,
	    // so we'll have to try and do cleanup ourselves.. if the run()
	    // method does do an exception, we may have already cleaned up
	    // before we get called, so we check to make sure that reader is
	    // not null

	    int waitCount = 0;

	    while (reader != null && !reader.isDone() && waitCount < 10)
	      {
		System.err.println("Waiting for reader to close down:" + waitCount);

		try
		  {
		    Thread.sleep(1000); // sleep for one second
		  }
		catch (InterruptedException ex2)
		  {
		    // ?
		  }

		waitCount++;
	      }

	    cleanup();

	    return getReturnVal(null, false);
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
    if (session != null)
      {
	session.unreferenced();
      }
  }

  /**
   * Something to assist in garbage collection.
   */

  public void cleanup()
  {
    // our ad-hoc semaphore

    synchronized (cleanedup)
      {
	if (cleanedup.size() != 0)
	  {
	    return;
	  }

	cleanedup.addElement("cleaning");
      }

    // note, we must not clear errBuf here, as we may cleanup before
    // calling getReturnVal() to report to the client.

    reader = null;

    objectTypes.clear();
    objectTypes = null;

    objectTypeIDs.clear();
    objectTypeIDs = null;

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

    if (spacesToAdd != null)
      {
	spacesToAdd.setSize(0);
	spacesToAdd = null;
      }

    if (spacesToRemove != null)
      {
	spacesToRemove.setSize(0);
	spacesToRemove = null;
      }

    if (spacesToEdit != null)
      {
	spacesToEdit.setSize(0);
	spacesToEdit = null;
      }

    if (basesToAdd != null)
      {
	basesToAdd.setSize(0);
	basesToAdd = null;
      }

    if (basesToRemove != null)
      {
	basesToRemove.setSize(0);
	basesToRemove = null;
      }

    if (basesToEdit != null)
      {
	basesToEdit.setSize(0);
	basesToEdit = null;
      }

    if (namespaceTree != null)
      {
	namespaceTree.dissolve();
	namespaceTree = null;
      }

    if (categoryTree != null)
      {
	categoryTree.dissolve();
	categoryTree = null;
      }
 
    if (session != null && session.isLoggedIn())
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
	    if (!processSchema(nextElement))
	      {
		return;
	      }
	    else
	      {
		// if all we wind up doing is schema editing, we'll
		// want return a positive success.  in transmitData(),
		// below, we set this.success back to false until we
		// know that we have successfully committed all the
		// data in the <ganydata> block.

		this.success = true; 
	      }

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

    item = reader.getNextItem();
    
    if (item instanceof XMLError)
      {
	throw new SAXException(item.toString());
      }
    
    while (item instanceof XMLWarning)
      {
	err.println("Warning!: " + item);
	
	item = reader.getNextItem();
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

  public boolean processSchema(XMLItem ganySchemaItem) throws SAXException
  {
    boolean _success = false;
    XMLItem _schemaTree = reader.getNextTree(ganySchemaItem);

    try
      {
	// okay, from this point forward, we're going to assume
	// failure unless/until we get to the end of the editing
	// process.  The finally clause for this try block will use
	// the success variable to decide whether to commit or abort
	// the schema edit.

	// the getNextTree() method will have either succeeded or
	// failed in its entirety.. if it found an error along the
	// way, it will have just returned that information, so check
	// that before we get crazy and start messing with the schema

	if ((_schemaTree instanceof XMLError) ||
	    (_schemaTree instanceof XMLEndDocument))
	  {
	    err.println(_schemaTree.toString());
	    return false;
	  }

	if (!session.isSuperGash())
	  {
	    err.println("Skipping <ganyschema> element.. not logged in with supergash privileges");
	    return false;
	  }

	// getNextTree will throw back an XMLError or XMLEndDocument if
	// such is encountered while scanning in the tree's subitems
	
	// try to get a schema editing context

	editor = editSchema();

	if (editor == null)
	  {
	    err.println("Couldn't edit the schema.. other users logged in?");
	    return false;
	  }

	// do the thing

	XMLItem _schemaChildren[] = _schemaTree.getChildren();

	if (_schemaChildren == null)
	  {
	    _success = true;	// no editing to be done
	    return true;
	  }

	// if schemaChildren was not null, XMLReader will guarantee
	// that it has at least one element
	
	int _nextchild = 0;

	if (_schemaChildren[_nextchild].matches("namespaces"))
	  {
	    namespaceTree = _schemaChildren[_nextchild++];
	  }

	if (_schemaChildren.length > _nextchild &&
	    _schemaChildren[_nextchild].matches("object_type_definitions"))
	  {
	    XMLItem _otdItem = _schemaChildren[_nextchild];

	    if (_otdItem.getChildren() == null || _otdItem.getChildren().length != 1)
	      {
		err.println("Error, the object_type_definitions element does not contain a singly-rooted category tree.");
		return false;
	      }

	    categoryTree = _otdItem.getChildren()[0];
	  }
	else
	  {
	    err.println("Couldn't find <object_type_definitions>");
	    return false;
	  }

	// 1.  calculate what name spaces need to be created, edited, or removed

	if (schemadebug)
	  {
	    err.println("1. calculate what name spaces need to be created, edited, or removed");
	  }

	if (namespaceTree != null)
	  {
	    if (!calculateNameSpaces())
	      {
		return false;
	      }
	  }

	// calculateNameSpaces() filled in spacesToAdd, spacesToRemove, and spacesToEdit

	// 2. create new name spaces

	if (schemadebug)
	  {
	    err.println("2. create new name spaces");
	  }

	for (int i = 0; i < spacesToAdd.size(); i++)
	  {
	    XMLItem _space = (XMLItem) spacesToAdd.elementAt(i);

	    String _name = _space.getAttrStr("name");
	    boolean _sensitive = _space.getAttrBoolean("case-sensitive");

	    if (_name == null || _name.equals(""))
	      {
		err.println("Error, namespace item " + _space + " has no name.");
		return false;
	      }

	    err.println("\tCreating " + _name);

	    NameSpace _aNewSpace = editor.createNewNameSpace(_name,!_sensitive);

	    if (_aNewSpace == null)
	      {
		err.println("Couldn't create a new namespace for item " + _space);
		return false;
	      }
	  }

	// 3. calculate what bases we need to create, edit, or remove

	if (schemadebug)
	  {
	    err.println("3. calculate what bases we need to create, edit, or remove");
	  }

	if (categoryTree == null || !calculateBases())
	  {
	    return false;
	  }

	// calculateBases filled in basesToAdd, basesToRemove, and basesToEdit.

	// 4. delete any bases that are not at least mentioned in the XML schema tree

	if (schemadebug)
	  {
	    err.println("4. delete any bases that are not at least mentioned in the XML schema tree");
	  }

	for (int i = 0; i < basesToRemove.size(); i++)
	  {
	    String _basename = (String) basesToRemove.elementAt(i);

	    err.println("\tDeleting " + _basename);

	    if (!handleReturnVal(editor.deleteBase(_basename)))
	      {
		return false;
	      }
	  }

	// 5. rename any bases that need to be renamedn

	if (schemadebug)
	  {
	    err.println("5. rename any bases that need to be renamed");
	  }

	if (!handleBaseRenaming())
	  {
	    return false;
	  }

	// 6. create all bases on the basesToAdd list

	if (schemadebug)
	  {
	    err.println("6. create all bases on the basesToAdd list");
	  }

	for (int i = 0; i < basesToAdd.size(); i++)
	  {
	    XMLItem _entry = (XMLItem) basesToAdd.elementAt(i);

	    err.println("\tCreating " + _entry.getAttrStr("name"));

	    Integer _id = _entry.getAttrInt("id");

	    boolean _embedded = false;

	    XMLItem _children[] = _entry.getChildren();

	    if (_children != null)
	      {
		for (int j = 0; j < _children.length; j++)
		  {
		    if (_children[j].matches("embedded"))
		      {
			_embedded = true;
		      }
		  }
	      }

	    // create the new base, with the requested id.  we'll
	    // specify that the object base is not an embedded one,
	    // since DBObjectBase.setXML() can change that if need be.

	    // also, we'll put it in the root category just so we can
	    // get things in the category tree before we resequence it

	    DBObjectBase _newBase = (DBObjectBase) editor.createNewBase(editor.getRootCategory(),
									_embedded,
									_id.shortValue());

	    // if we failed to create the base, we'll have an
	    // exception thrown.. our finally clause and higher level
	    // catches will handle it

	    // don't yet try to resolve invid links, since we haven't
	    // done a pass through basesToEdit to fix up fields yet

	    if (!handleReturnVal(_newBase.setXML(_entry, false, err)))
	      {
		return false;
	      }
	  }

	// 7. fix up fields in pre-existing bases

	if (schemadebug)
	  {
	    err.println("7. fix up fields in pre-existing bases");
	  }
 
	for (int i = 0; i < basesToEdit.size(); i++)
	  {
	    XMLItem _entry = (XMLItem) basesToEdit.elementAt(i);
	    Integer _id = _entry.getAttrInt("id");

	    DBObjectBase _oldBase = (DBObjectBase) editor.getBase(_id.shortValue());

	    if (_oldBase == null)
	      {
		err.println("Error, couldn't find DBObjectBase for " +
			    _entry.getTreeString() + " in pass 1");
		return false;
	      }

	    if (false)
	      {
		err.println("7. pass 1 - fixups on " + _oldBase);
	      }

	    err.println("\tEditing " + _oldBase);

	    // don't yet try to resolve invid links, since we haven't
	    // done a complete pass through basesToEdit to fix up
	    // fields yet

	    if (!handleReturnVal(_oldBase.setXML(_entry, false, err)))
	      {
		return false;
	      }
	  }

	// now that we have completed our first pass through fields in
	// basesToAdd and basesToEdit, where we created and/or renamed
	// fields, so now we can go back through both lists and finish
	// fixing up invid links.

	for (int i = 0; i < basesToAdd.size(); i++)
	  {
	    XMLItem _entry = (XMLItem) basesToAdd.elementAt(i);
	    Integer _id = _entry.getAttrInt("id");

	    DBObjectBase _oldBase = (DBObjectBase) editor.getBase(_id.shortValue());

	    if (_oldBase == null)
	      {
		err.println("Error, couldn't find DBObjectBase for " +
			    _entry.getTreeString() + " in pass 2");
		return false;
	      }

	    if (schemadebug)
	      {
		err.println("7. pass 2 - fixups on " + _oldBase);
	      }

	    //	    err.println("\tResolving " + _oldBase);

	    if (!handleReturnVal(_oldBase.setXML(_entry, true, err)))
	      {
		return false;
	      }
	  }

	for (int i = 0; i < basesToEdit.size(); i++)
	  {
	    XMLItem _entry = (XMLItem) basesToEdit.elementAt(i);
	    Integer _id = _entry.getAttrInt("id");

	    DBObjectBase _oldBase = (DBObjectBase) editor.getBase(_id.shortValue());

	    if (_oldBase == null)
	      {
		err.println("Error, couldn't find DBObjectBase for " +
			    _entry.getTreeString() + " in pass 3");
		return false;
	      }

	    if (schemadebug)
	      {
		err.println("7. pass 3 - fixups on " + _oldBase);
	      }

	    if (!handleReturnVal(_oldBase.setXML(_entry, true, err)))
	      {
		return false;
	      }
	  }

	// 8. Shuffle the category tree to match the XML file

	if (schemadebug)
	  {
	    err.println("8. Shuffle the category tree to match the XML file");
	  }

	if (!handleReturnVal(reshuffleCategories(categoryTree)))
	  {
	    return false;
	  }

	// 9. Clear out any namespaces that need it

	if (schemadebug)
	  {
	    err.println("9. Clear out any namespaces that need it");
	  }

	for (int i = 0; i < spacesToRemove.size(); i++)
	  {
	    String _name = (String) spacesToRemove.elementAt(i);

	    err.println("\tDeleting " + _name);

	    if (!handleReturnVal(editor.deleteNameSpace(_name)))
	      {
		return false;
	      }
	  }

	// 10. Need to flip case sensitivity on namespaces that
	// need it

	if (schemadebug)
	  {
	    err.println("10. Need to flip case sensitivity on namespaces that need it");
	  }

	for (int i = 0; i < spacesToEdit.size(); i++)
	  {
	    XMLItem _entry = (XMLItem) spacesToEdit.elementAt(i);
	    String _name = _entry.getAttrStr("name");
	    boolean _val = _entry.getAttrBoolean("case-sensitive");

	    err.println("\tFlipping " + _name);

	    NameSpace _space = editor.getNameSpace(_name);

	    _space.setInsensitive(!_val);
	  }

	// 11. Woohoo, Martha, I is a-coming home!

	if (schemadebug)
	  {
	    err.println("Successfully completed XML schema edit.");
	  }

	_success = true;
      }
    catch (Exception ex)
      {
	err.println("Caught exception during xml schema editing");
	err.println(ex.getMessage());
	ex.printStackTrace();

	_success = false;
	return false;
      }
    finally
      {
	// break apart the XML item tree for gc

	ganySchemaItem.dissolve();
	_schemaTree.dissolve();

	// either of these will clear the semaphore lock
	// created by editSchema() above

	if (_success)
	  {
	    err.println("Committing schema edit");
	    editor.commit();
	    this.editor = null;
	    return true;
	  }
	else
	  {
	    err.println("Releasing schema edit");

	    if (editor != null)
	      {
		editor.release();
		this.editor = null;
	      }

	    return false;
	  }
      }
  }

  /**
   * <p>This method fills in spacesToAdd, spacesToRemove, and spacesToEdit.</p>
   */

  private boolean calculateNameSpaces()
  {
    try
      {
	NameSpace[] _list = editor.getNameSpaces();

	Vector _current = new Vector(_list.length);

	for (int i = 0; i < _list.length; i++)
	  {
	    // theoretically possible RemoteException here, due to remote interface

	    _current.addElement(_list[i].getName()); 
	  }

	XMLItem _XNamespaces[] = namespaceTree.getChildren();

	Vector _newSpaces = new Vector(_XNamespaces.length);
	Hashtable _entries = new Hashtable(_XNamespaces.length);

	for (int i = 0; i < _XNamespaces.length; i++)
	  {
	    if (!_XNamespaces[i].matches("namespace"))
	      {
		err.println("unrecognized element: " + _XNamespaces[i] +
			    " when expecting <namespace>");
		return false;
	      }
	    
	    String _name = _XNamespaces[i].getAttrStr("name"); // ditto remote

	    if (_entries.containsKey(_name))
	      {
		err.println("Error, found duplicate <namespace> name '" + _name + "'");
		return false;
	      }

	    _entries.put(_name, _XNamespaces[i]);
	    _newSpaces.addElement(_name);
	  }

	// for spacesToRemove, we just keep the names for the missing
	// name spaces

	spacesToRemove = VectorUtils.difference(_current, _newSpaces);

	// for spacesToAdd and spacesToEdit, we need to first identify
	// names that are new or that were already in our current
	// namespaces list, then look up and save the appropriate
	// XMLItem nodes in the spacesToAdd and spacesToEdit global
	// Vectors.

	Vector _additions = VectorUtils.difference(_newSpaces, _current);

	spacesToAdd = new Vector();

	for (int i = 0; i < _additions.size(); i++)
	  {
	    XMLItem _entry = (XMLItem) _entries.get(_additions.elementAt(i));

	    spacesToAdd.addElement(_entry);
	  }

	Vector _possibleEdits = VectorUtils.intersection(_newSpaces, _current);

	spacesToEdit = new Vector();

	// we are only interested in namespaces to be edited if the
	// case-sensitivity changes.  we could defer this check, but
	// since we know that case-sensitivity is the only thing that
	// can vary in a namespace other than its name, we'll go ahead
	// and filter out no-changes here.

	for (int i = 0; i < _possibleEdits.size(); i++)
	  {
	    XMLItem _entry = (XMLItem) _entries.get(_possibleEdits.elementAt(i));
	    NameSpace _oldEntry = editor.getNameSpace((String) _possibleEdits.elementAt(i));

	    // yes, ==, not !=.. note that the _oldEntry check is for
	    // insensitivity, not sensitivity.

	    if (_entry.getAttrBoolean("case-sensitive") == _oldEntry.isCaseInsensitive())
	      {
		spacesToEdit.addElement(_entry);
	      }
	  }
      }
    catch (RemoteException ex)
      {
	ex.printStackTrace();
	throw new RuntimeException(ex.getMessage());
      }

    return true;
  }

  /**
   * <p>This method fills in basesToAdd, basesToRemove, and basesToEdit.</p>
   */

  private boolean calculateBases()
  {
    // create a list of Short base id's for of bases that we have
    // registered in the schema at present

    Base[] list = editor.getBases();
    Vector current = new Vector(list.length);

    for (int i = 0; i < list.length; i++)
      {
	current.addElement(((DBObjectBase)list[i]).getKey());
      }

    // get a list of objectdef root nodes from our xml tree

    Vector newBases = new Vector();

    findBasesInXMLTree(categoryTree, newBases);

    // get a list of Short id's from our xml tree, record
    // a mapping from those id's to the objectdef nodes in
    // our xml tree

    Vector xmlBases = new Vector();
    Hashtable entries = new Hashtable(); // for Short id's
    Hashtable nameTable = new Hashtable(); // for checking for redundant names

    for (int i = 0; i < newBases.size(); i++)
      {
	XMLItem objectdef = (XMLItem) newBases.elementAt(i);
	
	Integer id = objectdef.getAttrInt("id");
	String name = XMLUtils.XMLDecode(objectdef.getAttrStr("name"));
	
	if (id == null)
	  {
	    err.println("Couldn't get id for object definition item " + objectdef);
	    return false;
	  }

	if (id.shortValue() < 0)
	  {
	    err.println("Can't create or edit an object base type with a negative id number:\n"
			+ objectdef.getTreeString());
	    return false;
	  }

	if (name == null || name.equals(""))
	  {
	    err.println("Couldn't get name for object definition item " + objectdef);
	    return false;
	  }

	Short key = new Short(id.shortValue());
	xmlBases.addElement(key);

	if (entries.containsKey(key))
	  {
	    err.println("Error, found duplicate base id number in <ganyschema>: " + objectdef);
	    return false;
	  }

	if (nameTable.containsKey(name))
	  {
	    err.println("Error, found duplicate base name in <ganyschema>: " + objectdef);
	    return false;
	  }

	entries.put(key, objectdef);
	nameTable.put(name, name); // just need to remember existence
      }

    // We need to calculate basesToRemove.. since the DBSchemaEditor
    // can only delete bases based on their names, we need to 
    // take the Vector of Shorts that we get from difference and
    // put the matching names into basesToRemove

    Vector deletions = VectorUtils.difference(current, xmlBases);

    basesToRemove = new Vector();
    
    for (int i = 0; i < deletions.size(); i++)
      {
	Short id = (Short) deletions.elementAt(i);

	try
	  {
	    // Base.getName() is defined to throw RemoteException
	    
	    basesToRemove.addElement(editor.getBase(id.shortValue()).getName());
	  }
	catch (RemoteException ex)
	  {
	    // should never ever happen

	    ex.printStackTrace();
	    throw new RuntimeException(ex.getMessage());
	  }
      }

    // now calculate basesToAdd and basesToEdit, recording the
    // objectdef XMLItem root for each base in each list
    
    Vector additions = VectorUtils.difference(xmlBases, current);
    Vector edits = VectorUtils.intersection(xmlBases, current);

    basesToAdd = new Vector();
    
    for (int i = 0; i < additions.size(); i++)
      {
	XMLItem entry = (XMLItem) entries.get(additions.elementAt(i));
	
	if (entry.getAttrInt("id").shortValue() < 256)
	  {
	    err.println("Object type id's less than 256 are reserved for new system critical\n");
	    err.println("object types, and may not be created with the xml schema editing support.");
	    return false;
	  }

	basesToAdd.addElement(entry);
      }
    
    basesToEdit = new Vector();
    
    for (int i = 0; i < edits.size(); i++)
      {
	XMLItem entry = (XMLItem) entries.get(edits.elementAt(i));
	
	basesToEdit.addElement(entry);
      }

    return true;
  }

  /**
   * <p>This is a recursive method to do a traversal of an XMLItem
   * tree, adding object base definition roots found to the foundBases
   * vector.</p> 
   */

  private void findBasesInXMLTree(XMLItem treeRoot, Vector foundBases)
  {
    // objectdef's will contain fielddef children, but no more
    // objectdef's, so we treat objectdef's as leaf nodes for our
    // traversal

    if (treeRoot.matches("objectdef"))
      {
	foundBases.addElement(treeRoot);
	return;
      }

    XMLItem children[] = treeRoot.getChildren();

    if (children == null)
      {
	return;
      }

    for (int i = 0; i < children.length; i++)
      {
	findBasesInXMLTree(children[i], foundBases);
      }
  }

  /**
   * <p>This private method takes care of doing any object type
   * renaming, prior to resolving invid field definitions.</p>
   */

  private boolean handleBaseRenaming() throws RemoteException
  {
    XMLItem myBaseItem;
    Base numBaseRef;
    Base nameBaseRef;
    String name;
    
    /* -- */

    for (int i = 0; i < basesToEdit.size(); i++)
      {
	myBaseItem = (XMLItem) basesToEdit.elementAt(i);

	name = XMLUtils.XMLDecode(myBaseItem.getAttrStr("name"));

	numBaseRef = editor.getBase(myBaseItem.getAttrInt("id").shortValue());

	if (name.equals(numBaseRef.getName()))
	  {
	    continue;		// no rename necessary
	  }

	// we need to rename the base pointed to by numBaseRef.. first
	// see if another base already has the name we want

	nameBaseRef = editor.getBase(name);

	// if we found a base with the name we need, switch the two
	// names.  we know from calculateBases() that the user
	// didn't put two bases by the same name in the xml <ganyschema>
	// section, so if swap the names, we'll fix up the second name
	// when we get to it

	err.println("\tRenaming " + numBaseRef.getName() + " to " + name);

	if (nameBaseRef != null)
	  {
	    String swapName = numBaseRef.getName();

	    if (!handleReturnVal(numBaseRef.setName(name)))
	      {
		return false;
	      }

	    if (!handleReturnVal(nameBaseRef.setName(swapName)))
	      {
		return false;
	      }
	  }
	else
	  {
	    if (!handleReturnVal(numBaseRef.setName(name)))
	      {
		return false;
	      }
	  }
      }

    return true;
  }

  /**
   * <p>This method is used by the XML schema editing code
   * in {@link arlut.csd.ganymede.GanymedeXMLSession GanymedeXMLSession}
   * to fix up the category tree to match that specified in the XML
   * &lt;ganyschema&gt; element.</p>
   */

  public synchronized ReturnVal reshuffleCategories(XMLItem categoryRoot)
  {
    Hashtable categoryNames = new Hashtable();

    if (!testXMLCategories(categoryRoot, categoryNames))
      {
	return Ganymede.createErrorDialog("xml",
					  "Error, category names not unique in XML schema");
      }

    DBBaseCategory _rootCategory = buildXMLCategories(categoryRoot);

    if (_rootCategory == null)
      {
	return Ganymede.createErrorDialog("xml",
					  "Error, buildXMLCategories failed somehow");
      }

    editor.rootCategory = _rootCategory;

    return null;		// tada!
  }

  /**
   * <p>This method tests an XML category tree to make sure that all
   * categories in the tree have unique names.</p>
   */

  public boolean testXMLCategories(XMLItem categoryRoot, Hashtable names)
  {
    if (categoryRoot.matches("category"))
      {
	// make sure we don't get duplicate category names

	if (names.containsKey(categoryRoot.getAttrStr("name")))
	  {
	    return false;
	  }
	else
	  {
	    names.put(categoryRoot.getAttrStr("name"), categoryRoot.getAttrStr("name"));
	  }

	XMLItem children[] = categoryRoot.getChildren();

	if (children == null)
	  {
	    return true;
	  }

	for (int i = 0; i < children.length; i++)
	  {
	    if (!testXMLCategories(children[i], names))
	      {
		return false;
	      }
	  }
      }

    return true;
  }

  /**
   * <p>This recursive method takes an XMLItem category tree and returns
   * a new DBBaseCategory tree with all categories and object definitions
   * from the XMLItem category tree ordered correctly.</p>
   */

  public DBBaseCategory buildXMLCategories(XMLItem categoryRoot)
  {
    DBBaseCategory _root;

    /* -- */

    if (!categoryRoot.matches("category"))
      {
	err.println("buildXMLCategories given a non-category categoryRoot, can't build a new category tree, failing");
	return null;
      }

    try
      {
	_root = new DBBaseCategory(editor.store, categoryRoot.getAttrStr("name"));
      }
    catch (RemoteException ex)
      {
	err.println("Caught RMI UnicastRemote initialization error in buildXMLCategories");
	return null;
      }

    XMLItem _children[] = categoryRoot.getChildren();

    if (_children == null)
      {
	return _root;
      }

    for (int i = 0; i < _children.length; i++)
      {
	XMLItem _child = _children[i];

	if (_child.matches("category"))
	  {
	    _root.addNodeAfter(buildXMLCategories(_child), null);
	  }
	else if (_child.matches("objectdef"))
	  {
	    DBObjectBase _base = (DBObjectBase) editor.getBase(_child.getAttrInt("id").shortValue());
	    _root.addNodeAfter(_base, null);
	  }
      }

    return _root;
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

    initializeLookups();

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
		    // if we have already cleaned up as a result of the parser
		    // throwing a pipe write exception, don't report this
		    // exception, as it ultimately came from another thread

		    if (cleanedup.size() > 0)
		      {
			return false;
		      }

		    // otherwise, it was probably due to something in the xmlobject
		    // constructor, and we should report it..

		    // bad field or object error.. return out of this
		    // method without committing the transaction
		    // our finally clause will log us out

		    ex.printStackTrace();

		    return false;
		  }

		System.err.print(".");

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

	System.err.println("\n\n");

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
   * <p>This private method handles data structures initialization for
   * the GanymedeXMLSession, prepping hash lookups that are used
   * to accelerate XML processing.</p>
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
	objectHash = new Hashtable(10001, 0.75f);
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
	// we do this mainly so we can fall through to our if (element
	// == null) logic below.

	objectHash = new Hashtable(10001, 0.75f);
	objectStore.put(typeKey, objectHash);
      }

    Object element = objectHash.get(objId);

    if (element == null)
      {
	// okay, let's look up the given label in the database to see
	// if the user is trying to refer to a pre-existing object.

	// note that we really shouldn't be doing this before we have
	// looped through and done a storeObject() on all objects in
	// the xml <ganydata> section, or else we might prematurely
	// store a reference to a pre-existing object when the xml
	// file meant to reference an object defined in it

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
   * <p>This method actually does the work of integrating our data into the
   * DBStore.</p>
   *
   * @returns true if the data was successfully integrated to the server and
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

    if (cleanedup.size() > 0)
      {
	return false;
      }

    // first, set this.success to false, to reset our success indicator after
    // doing any schema editing

    this.success = false;

    attempt = session.openTransaction("xmlclient", false); // non-interactive
	    
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
	boolean newlyCreated = false;

	xmlobject newObject = (xmlobject) createdObjects.elementAt(i);
	
	// if the object has enough information that we can look it up
	// on the server (and get an Invid for it), assume that it
	// already exists and go ahead and pull it for editing rather
	// than creating it, unless the forceCreate flag is on.

	if (!newObject.forceCreate && newObject.getInvid() != null)
	  {
	    incCount(editCount, newObject.typeString);

	    //	    System.err.println("Editing pre-existing " + newObject);

	    attempt = newObject.editOnServer(session);

	    if (attempt != null && !attempt.didSucceed())
	      {
		String msg = attempt.getDialogText();

		if (msg != null)
		  {
		    err.println("Error editing object " + newObject + ", reason:\n" + msg);
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

	    //	    System.err.println("Creating " + newObject);

	    newlyCreated = true;

	    attempt = newObject.createOnServer(session);

	    if (attempt != null && !attempt.didSucceed())
	      {
		String msg = attempt.getDialogText();

		if (msg != null)
		  {
		    err.println("Error creating " + newObject + ", reason:\n" + msg);
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
		err.print("[1] Error registering fields for ");

		if (newlyCreated)
		  {
		    err.print("newly created object ");
		  }
		else
		  {
		    err.print("edited object ");
		  }

		err.println(newObject + ", reason:\n" + msg);
	      }
	    else
	      {
		err.print("[1] Error registering fields for ");

		if (newlyCreated)
		  {
		    err.print("newly created object ");
		  }
		else
		  {
		    err.print("edited object ");
		  }

		err.println(newObject + ", no reason given.");
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
		err.println("[2] Error registering fields for " + newObject + ", reason:\n" + msg);
	      }
	    else
	      {
		err.println("[2] Error registering fields for " + newObject + ", no reason given.");
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
		err.println("Error editing object " + object + ", reason:\n" + msg);
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
		err.println("[3] Error registering fields for " + object + ", reason:\n" + msg);
	      }
	    else
	      {
		err.println("[3] Error registering fields for " + object + ", no reason given.");
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
		err.println("[4] Error registering fields for " + object + ", reason:\n" + msg);
	      }
	    else
	      {
		err.println("[4] Error registering fields for " + object + ", no reason given.");
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
		err.println("Error inactivating " + object + ", reason:\n" + msg);
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

	Invid target = object.getInvid();

	if (target == null)
	  {
	    err.println("Warning, couldn't find Invid for object to be deleted: " + object);
	    continue;
	  }

	incCount(deleteCount, object.typeString);

	attempt = session.remove_db_object(target);

	if (attempt != null && !attempt.didSucceed())
	  {
	    String msg = attempt.getDialogText();

	    if (msg != null)
	      {
		err.println("Error deleting " + object + ", reason:\n" + msg);
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
		err.println("Error committing transaction, reason:\n" + msg);
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

  /**
   * this private helper method increments a counting
   * integer in table, keyed by type.
   */

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
	    System.out.println(progress);

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
	// we depend on createErrorDialog() to dump the progress to the server log

	return Ganymede.createErrorDialog("XML submit errors",
					  progress);
      }
  }

  /**
   * <p>This is a copy of the editSchema method from the GanymedeAdmin
   * class which has been modified so that it will assert a schema
   * edit lock without requiring that the login semaphore count be
   * zero.  This way we can get a DBSchemaEdit context that we can use
   * to do XML-based schema editing without having to have dropped our
   * GanymedeSession's semaphore increment.  This is safe to do only
   * because we know that the GanymedeXMLSession is single-threaded
   * and will not do any database activity while the schema is opened
   * for editing.</p>
   *
   * @return null if the server could not be put into schema edit mode.
   */

  private DBSchemaEdit editSchema()
  {
    Enumeration enum;
    DBObjectBase base;

    /* -- */

    // first, let's check to see if we're the only session in, and
    // if we are disable the semaphore.  We have to do all of this
    // in a block synchronized on GanymedeServer.lSemaphore so
    // that we won't proceed to approve the schema edit if someone
    // else has logged into the server

    synchronized (GanymedeServer.lSemaphore)
      {
	if (GanymedeServer.lSemaphore.getCount() != 1)
	  {
	    return null;	// someone else is logged in, can't do it
	  }
	
	Ganymede.debug("GanymedeXMLSession entering editSchema");

	// try disabling the semaphore with a false waitForZero value,
	// so that we can go into schema edit mode while still
	// maintaining our GanymedeSession's semaphore increment

	try
	  {
	    String semaphoreCondition = GanymedeServer.lSemaphore.disable("schema edit", false, 0);
	    
	    if (semaphoreCondition != null)
	      {
		Ganymede.debug("GanymedeXMLSession Can't edit schema, semaphore error: " + semaphoreCondition);
		
		return null;
	      }
	  }
	catch (InterruptedException ex)
	  {
	    ex.printStackTrace();
	    throw new RuntimeException(ex.getMessage());
	  }
      }

    // okay at this point we've asserted our interest in editing the
    // schema and made sure that no one else is logged in or can log
    // in.  Now we just need to make sure that we don't have any of
    // the bases locked by anything that is skipping the semaphore,
    // such as tasks.

    // In fact, I believe that the server is now safe against lock
    // races due to all tasks that might involve DBObjectBase access
    // being guarded by the loginSemaphore, but there is little cost
    // in sync'ing here.

    // All the DBLock establish methods synchronize on the DBLockSync
    // object referenced by Ganymede.db.lockSync, so we are safe
    // against lock establish race conditions by synchronizing this
    // section on Ganymede.db.lockSync.

    synchronized (Ganymede.db.lockSync)
      {
	Ganymede.debug("entering editSchema synchronization block");

	enum = Ganymede.db.objectBases.elements();

	if (enum != null)
	  {
	    while (enum.hasMoreElements())
	      {
		base = (DBObjectBase) enum.nextElement();

		if (base.isLocked())
		  {
		    Ganymede.debug("GanymedeXMLSession Can't edit Schema, lock held on " + base.getName());
		    GanymedeServer.lSemaphore.enable("schema edit");
		    return null;
		  }
	      }
	  }

	 // should be okay

	 Ganymede.debug("GanymedeXMLSession Ok to create DBSchemaEdit");

	 GanymedeAdmin.setState("XML Schema Edit In Progress");

	 try
	   {
	     DBSchemaEdit result = new DBSchemaEdit(null);
	     return result;
	   }
	 catch (RemoteException ex)
	   {
	     GanymedeServer.lSemaphore.enable("schema edit");
	     return null;
	   }
      }
  }

  /**
   * <p>Private helper method to print to the client the text of
   * any return val dialog.  Returns true if the retval codes
   * for success, false otherwise.</p>
   */

  private boolean handleReturnVal(ReturnVal retval)
  {
    if (retval != null && retval.getDialogText() != null)
      {
	err.println(retval.getDialogText());
      }

    if (retval == null || retval.didSucceed())
      {
	return true;
      }

    return false;
  }
}

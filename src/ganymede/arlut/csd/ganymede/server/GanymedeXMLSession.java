/*
   GASH 2

   GanymedeXMLSession.java

   The GANYMEDE object storage system.

   Created: 1 August 2000
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

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

package arlut.csd.ganymede.server;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.rmi.server.Unreferenced;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.xml.sax.SAXException;

import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.Util.booleanSemaphore;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;
import arlut.csd.Util.XMLCloseElement;
import arlut.csd.Util.XMLElement;
import arlut.csd.Util.XMLEndDocument;
import arlut.csd.Util.XMLError;
import arlut.csd.Util.XMLItem;
import arlut.csd.Util.XMLStartDocument;
import arlut.csd.Util.XMLUtils;
import arlut.csd.Util.XMLWarning;
import arlut.csd.ganymede.common.FieldTemplate;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.Base;
import arlut.csd.ganymede.rmi.NameSpace;
import arlut.csd.ganymede.rmi.Session;
import arlut.csd.ganymede.rmi.XMLSession;

/*------------------------------------------------------------------------------
                                                                           class
                                                              GanymedeXMLSession

------------------------------------------------------------------------------*/

/**
 * <p>This class handles all XML loading operations for the Ganymede
 * server.  GanymedeXMLSession's are created by the {@link
 * arlut.csd.ganymede.server.GanymedeServer GanymedeServer}'s {@link
 * arlut.csd.ganymede.server.GanymedeServer#xmlLogin(arlut.csd.ganymede.Client)
 * xmlLogin()} method.  A GanymedeXMLSession is created on top of a
 * {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession} and
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
 * repeatedly calls the {@link arlut.csd.ganymede.server.GanymedeXMLSession#xmlSubmit(byte[]) xmlSubmit()}
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
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.GanymedeXMLSession");

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

  arlut.csd.Util.XMLReader reader;

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
   * {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate}
   * objects.</p>
   */

  public Hashtable objectTypes = new Hashtable();

  /**
   * <p>Hashtable mapping Short object type ids to
   * hashtables mapping field names to 
   * {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate}
   * objects.</p>
   */

  public Hashtable objectTypeIDs = new Hashtable();

  /**
   * <P>Hashtable mapping Short type ids to
   * hashes which map local object designations (either id Strings or
   * num Integers from the &lt;object&gt; elements) to
   * actual {@link arlut.csd.ganymede.server.xmlobject xmlobject}
   * records.</P>
   */

  public Hashtable objectStore = new Hashtable();

  /**
   * <p>Vector of {@link arlut.csd.ganymede.server.xmlobject xmlobjects}
   * that correspond to new Ganymede server objects
   * that have been/need to be created by this GanymedeXMLSession.</p>
   */

  public Vector createdObjects = new Vector();

  /**
   * <p>Vector of {@link arlut.csd.ganymede.server.xmlobject xmlobjects}
   * that correspond to pre-existing Ganymede
   * server objects that have been/need to be checked out for editing by this
   * GanymedeXMLSession.</p> 
   */

  public Vector editedObjects = new Vector();

  /**
   * <p>Vector of {@link arlut.csd.ganymede.server.xmlobject
   * xmlobjects} that correspond to Ganymede server objects that have
   * been created/checked out for editing during embedded invid field
   * processing, and which need to have their invid fields registered
   * after everything else is done.</p> 
   */

  public Vector embeddedObjects = new Vector();

  /**
   * <p>Vector of {@link arlut.csd.ganymede.server.xmlobject xmlobjects}
   * that correspond to pre-existing Ganymede
   * server objects that have been/need to be inactivated by this
   * GanymedeXMLSession.</p>
   */

  public Vector inactivatedObjects = new Vector();

  /**
   * <p>Vector of {@link arlut.csd.ganymede.server.xmlobject xmlobjects}
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

  private booleanSemaphore parsing = new booleanSemaphore(true);

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
   * <p>Semaphore to gate the cleanup() method.</p>
   */

  private booleanSemaphore cleanedup = new booleanSemaphore(false);
  
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
	reader = new arlut.csd.Util.XMLReader(pipe, bufferSize, true, err);
      }
    catch (IOException ex)
      {
	System.err.println(ts.l("init.initialization_error", Ganymede.stackTrace(ex)));
	throw new RuntimeException(ex.getMessage());
      }
  }

  /**
   * <p>This method returns a remote reference to the underlying
   * GanymedeSession in use on the server.</p>
   *
   * @see arlut.csd.ganymede.rmi.XMLSession
   */

  public Session getSession()
  {
    return session;
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
   *
   * @see arlut.csd.ganymede.rmi.XMLSession
   */

  public ReturnVal xmlSubmit(byte[] bytes) throws NotLoggedInException
  {
    session.checklogin();

    if (debug)
      {
	System.err.println("xmlSubmit called on server");
      }

    if (parsing.isSet())
      {
	try
	  {
	    if (debug)
	      {
		System.err.println("xmlSubmit byting");
	      }

	    pipe.write(bytes);	// can block if the parser thread gets behind

	    if (debug)
	      {
		System.err.println("xmlSubmit bit");
	      }
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
		// "Waiting for reader to close down: {0,number,#}"
		System.err.println(ts.l("xmlSubmit.waiting_for_reader", new Integer(waitCount)));

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

	    try
	      {
		return getReturnVal(null, false);
	      }
	    finally
	      {
		if (debug)
		  {
		    System.err.println("xmlSubmit call returned on server 1");
		  }
	      }
	  }
      }
    else
      {
	// "GanymedeXMLSession.xmlSubmit(), parser already closed, skipping writing into pipe."
	System.err.println(ts.l("xmlSubmit.parser_already_closed"));
      }

    // if reader is not done, we're ok to continue

    try
      {
	return getReturnVal(null, (reader != null && !reader.isDone()));
      }
    finally
      {
	if (debug)
	  {
	    System.err.println("xmlSubmit call returned on server 2");
	  }
      }
  }

  /**
   * <p>This method is called by the XML client once the end of the XML
   * stream has been transmitted, whereupon the server will attempt
   * to finalize the XML transaction and return an overall success or
   * failure message in the ReturnVal.</p>
   *
   * @see arlut.csd.ganymede.rmi.XMLSession
   */

  public ReturnVal xmlEnd()
  {
    if (debug)
      {
	System.err.println("xmlEnd() called");
      }

    parsing.waitForCleared();

    return getReturnVal(null, success);
  }

  /**
   * <p>This method is called by the XML client on a dedicated thread
   * to pull stderr messages from the server.</p>
   *
   * <p>This call will block on the server until err stream data is
   * available, but will always block for at least half a second so
   * that the client doesn't loop on getNextErrChunk() too fast.</p>
   *
   * <p>This method will return null after the server closes its error
   * stream.</p>
   *
   * @see arlut.csd.ganymede.rmi.XMLSession
   */

  public String getNextErrChunk()
  {
    String progress = null;
    StringBuffer errBuffer = errBuf.getBuffer();
    boolean done = false;

    /* -- */

    while (!done)
      {
	synchronized (errBuffer)
	  {
	    progress = errBuffer.toString();
	    errBuffer.setLength(0);	// this doesn't actually free memory.. stoopid StringBuffer
	  }

	if (progress.length() != 0)
	  {
	    done = true;

	    System.err.print(progress);

	    try
	      {
		Thread.sleep(500); // sleep for one half second to slow the client/server spin loop down
	      }
	    catch (InterruptedException ex2)
	      {
		// ?
	      }

	    // Now that we've waited, collect any additional
	    // accumulation and we'll return

	    synchronized (errBuffer)
	      {
		progress = progress + errBuffer.toString();
		errBuffer.setLength(0);
	      }
	  }
	else
	  {
	    if (!parsing.isSet())
	      {
		return null;
	      }

	    try
	      {
		Thread.sleep(1000); // sleep for one second to allow more err stream data to accumulate
	      }
	    catch (InterruptedException ex2)
	      {
		// ?
	      }
	  }
      }

    return progress;
  }

  /**
   * <p>This method is for use on the server, and is called by the
   * GanymedeSession to let us know if the server is forcing our login
   * off.</p>
   *
   * @see arlut.csd.ganymede.rmi.XMLSession
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

    if (parsing.isSet())
      {
	if (debug)
	  {
	    System.err.println("GanymedeXMLSession closing reader");
	  }

	// "Abort called, closing reader."
	System.err.println(ts.l("abort.aborting"));

	reader.close();		// this will cause the XML Reader to halt
      }
    else
      {
	if (debug)
	  {
	    System.err.println("GanymedeXMLSession closing already closed reader");
	  }
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
	// set our underlying GanymedeSession's xSession to null so
	// that it will take things seriously when we tell it that it
	// is unreferenced.

	session.setXSession(null);
	session.unreferenced();
      }
  }

  /**
   * <p>This method handles cleanup post-schema edit.</p>
   */

  public void cleanupSchemaEdit()
  {
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
  }

  /**
   * Something to assist in garbage collection.
   */

  public void cleanup()
  {
    if (debug)
      {
	System.err.println("Entering cleanup");
      }

    if (cleanedup.set(true))
      {
	return;
      }

    // note, we must not clear errBuf here, as we may cleanup before
    // calling getReturnVal() to report to the client.

    reader.close();
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
	    // "XML parser error: first element {0} not an XMLStartDocument"
	    err.println(ts.l("run.not_start_element", startDocument));

	    return;
	  }

	if (debug)
	  {
	    System.err.println("GanymedeXMLSession run getting docElement");
	  }

	XMLItem docElement = getNextItem();

	if (!docElement.matches("ganymede"))
	  {
	    // "Error, XML Stream does not contain a Ganymede XML file.\nUnrecognized XML element: {0}"
	    err.println(ts.l("run.bad_start_element", docElement));

	    return;
	  }

	Integer majorI = docElement.getAttrInt("major");
	Integer minorI = docElement.getAttrInt("minor");

	if (majorI == null || majorI.intValue() > majorVersion)
	  {
	    // "Error, the Ganymede document element {0} does not contain a compatible major version number."u
	    err.println(ts.l("run.bad_major_version", docElement));

	    return;
	  }
	
	if (majorI.intValue() == majorVersion && 
	    (minorI == null || minorI.intValue() > minorVersion))
	  {
	    // "Error, the Ganymede document element {0} does not contain a compatible minor version number."
	    err.println(ts.l("run.bad_minor_version", docElement));

	    return;
	  }

	// okay, we're good to go

	XMLItem nextElement = getNextItem();

	if (nextElement.matches("ganyschema"))
	  {
	    boolean schemaOk = false;

	    try
	      {
		schemaOk = processSchema(nextElement);
	      }
	    finally
	      {
		cleanupSchemaEdit();
	      }
	    
	    if (!schemaOk)
	      {
		return;
	      }
	    else
	      {
		// if all we wind up doing is schema editing, we'll
		// want return a positive success.  in
		// integrateXMLTransaction(), below, we set
		// this.success back to false until we know that we
		// have successfully committed all the data in the
		// <ganydata> block.

		this.success = true; 
	      }

	    nextElement = getNextItem();
	  }

	if (nextElement.matches("ganydata"))
	  {
	    if (!processData())
	      {
		// don't both processing rest of XML doc.. just jump
		// down to finally clause

		return;
	      }

	    nextElement = getNextItem();
	  }

	while (!nextElement.matchesClose("ganymede") && !(nextElement instanceof XMLCloseElement))
	  {
	    // "Skipping unrecognized element: {0}"
	    err.println(ts.l("run.skipping", nextElement));

	    nextElement = getNextItem();
	  }
      }
    catch (Exception ex)
      {
	// we may get a SAXException here if the reader gets
	// shutdown before our parsing process is done, or if
	// there is something malformed in the XML

	// "Caught exception for GanymedeXMLSession.run():\n{0}"

	err.println(ts.l("run.exception", Ganymede.stackTrace(ex)));
      }
    finally
      {
	if (debug)
	  {
	    System.err.println("run() terminating");
	  }

	parsing.set(false);

	cleanupSchemaEdit();
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
	// "Warning!: {0}"
	err.println(ts.l("getNextItem.warning", item));
	
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
	    // "Skipping <ganyschema> element.. not logged in with supergash privileges."
	    err.println(ts.l("processSchema.bad_permissions"));

	    return false;
	  }

	// getNextTree will throw back an XMLError or XMLEndDocument if
	// such is encountered while scanning in the tree's subitems
	
	// try to get a schema editing context

	editor = editSchema();

	if (editor == null)
	  {
	    // "Couldn''t edit the schema.. other users logged in?"
	    err.println(ts.l("processSchema.editing_blocked"));

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
		// "Error, the object_type_definitions element does not contain a single-rooted category tree."
		err.println(ts.l("processSchema.bad_category_tree"));

		return false;
	      }

	    categoryTree = _otdItem.getChildren()[0];
	  }
	else
	  {
	    // "Couldn''t find <object_type_definitions>."
	    err.println(ts.l("processSchema.no_object_type_definitions"));

	    return false;
	  }

	// 1.  calculate what name spaces need to be created, edited, or removed

	if (schemadebug)
	  {
	    // "1.  Calculate what name spaces need to be created, edited, or removed"
	    err.println(ts.l("processSchema.schemadebug_1"));
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
	    // "2.  Create new name spaces."
	    err.println(ts.l("processSchema.schemadebug_2"));
	  }

	for (int i = 0; i < spacesToAdd.size(); i++)
	  {
	    XMLItem _space = (XMLItem) spacesToAdd.elementAt(i);

	    String _name = _space.getAttrStr("name");

	    if (_name == null || _name.equals(""))
	      {
		// "Error, namespace item {0} has no name attribute."
		err.println(ts.l("processSchema.no_name_namespace", _space));

		return false;
	      }

	    // make sure we have a case-sensitive attribute, just to
	    // get in the user's face a bit so he doesn't have the
	    // system doing something unexpected without warning

	    if (_space.getAttrStr("case-sensitive") == null)
	      {
		// "Warning, namespace item {0} has no case-sensitive attribute.  {0} will be created as case insensitive."
		err.println(ts.l("processSchema.no_case_namespace", _space));
	      }

	    boolean _sensitive = _space.getAttrBoolean("case-sensitive");

	    // "\tCreating namespace {0}."
	    err.println(ts.l("processSchema.creating_namespace", _name));

	    NameSpace _aNewSpace = editor.createNewNameSpace(_name,!_sensitive);

	    if (_aNewSpace == null)
	      {
		// "Couldn''t create a new namespace for item {0}."
		err.println(ts.l("processSchema.failed_namespace_create", _space));

		return false;
	      }
	  }

	// 3. calculate what bases we need to create, edit, or remove

	if (schemadebug)
	  {
	    // "3.  Calculate what object bases we need to create, edit, or remove."
	    err.println(ts.l("processSchema.schemadebug_3"));
	  }

	if (categoryTree == null || !calculateBases())
	  {
	    return false;
	  }

	// calculateBases filled in basesToAdd, basesToRemove, and basesToEdit.

	// 4. delete any bases that are not at least mentioned in the XML schema tree

	if (schemadebug)
	  {
	    // "4.  Delete any object bases that are not at least mentioned in the XML schema tree."
	    err.println(ts.l("processSchema.schemadebug_4"));
	  }

	for (int i = 0; i < basesToRemove.size(); i++)
	  {
	    String _basename = (String) basesToRemove.elementAt(i);

	    // "\tDeleting object base {0}."
	    err.println(ts.l("processSchema.deleting_base", _basename));

	    if (!handleReturnVal(editor.deleteBase(_basename)))
	      {
		return false;
	      }
	  }

	// 5. rename any bases that need to be renamedn

	if (schemadebug)
	  {
	    // "5.  Rename any object bases that need to be renamed."
	    err.println(ts.l("processSchema.schemadebug_5"));
	  }

	if (!handleBaseRenaming())
	  {
	    return false;
	  }

	// 6. create all bases on the basesToAdd list

	if (schemadebug)
	  {
	    // "6.  Create all object bases on the basesToAdd list."
	    err.println(ts.l("processSchema.schemadebug_6"));
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
	    // "7.  Fix up fields in pre-existing object bases."
	    err.println(ts.l("processSchema.schemadebug_7"));
	  }
 
	for (int i = 0; i < basesToEdit.size(); i++)
	  {
	    XMLItem _entry = (XMLItem) basesToEdit.elementAt(i);
	    Integer _id = _entry.getAttrInt("id");

	    DBObjectBase _oldBase = (DBObjectBase) editor.getBase(_id.shortValue());

	    if (_oldBase == null)
	      {
		// " Error, couldn''t find DBObjectBase for {0} in pass {1,number,#}."
		err.println(ts.l("processSchema.bad_base", _entry.getTreeString(), new Integer(1)));

		return false;
	      }

	    if (false)
	      {
		// "7.  pass 1 - fixups on {0}"
		err.println(ts.l("processSchema.schemadebug_7_1", _oldBase));
	      }

	    // "\tEditing object base {0}"
	    err.println(ts.l("processSchema.editing_objectbase", _oldBase));

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
		// " Error, couldn''t find DBObjectBase for {0} in pass {1,number,#}."
		err.println(ts.l("processSchema.bad_base", _entry.getTreeString(), new Integer(2)));

		return false;
	      }

	    if (schemadebug)
	      {
		// "7.  pass 2 - fixups on object base {0}"
		err.println(ts.l("processSchema.schemadebug_7_2", _oldBase));
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
		// " Error, couldn''t find DBObjectBase for {0} in pass {1,number,#}."
		err.println(ts.l("processSchema.bad_base", _entry.getTreeString(), new Integer(3)));

		return false;
	      }

	    if (schemadebug)
	      {
		// "7.  pass 3 - fixups on object base {0}"
		err.println(ts.l("processSchema.schemadebug_7_3", _oldBase));
	      }

	    if (!handleReturnVal(_oldBase.setXML(_entry, true, err)))
	      {
		return false;
	      }
	  }

	// 8. Shuffle the category tree to match the XML file

	if (schemadebug)
	  {
	    // "8.  Shuffle the Category tree to match the XML schema."
	    err.println(ts.l("processSchema.schemadebug_8"));
	  }

	if (!handleReturnVal(reshuffleCategories(categoryTree)))
	  {
	    return false;
	  }

	// 9. Clear out any namespaces that need it

	if (schemadebug)
	  {
	    // "9.  Clear out any name spaces that need it."
	    err.println(ts.l("processSchema.schemadebug_9"));
	  }

	for (int i = 0; i < spacesToRemove.size(); i++)
	  {
	    String _name = (String) spacesToRemove.elementAt(i);

	    // "\tDeleting name space {0}."
	    err.println(ts.l("processSchema.deleting_namespace", _name));

	    if (!handleReturnVal(editor.deleteNameSpace(_name)))
	      {
		return false;
	      }
	  }

	// 10. Need to flip case sensitivity on namespaces that
	// need it

	if (schemadebug)
	  {
	    // "10.  Need to flip case sensitivity on namespaces that need it."
	    err.println(ts.l("processSchema.schemadebug_10"));
	  }

	for (int i = 0; i < spacesToEdit.size(); i++)
	  {
	    XMLItem _entry = (XMLItem) spacesToEdit.elementAt(i);
	    String _name = _entry.getAttrStr("name");
	    boolean _val = _entry.getAttrBoolean("case-sensitive");

	    // "\tFlipping name space {0}."
	    err.println(ts.l("processSchema.flipping_namespace", _name));

	    NameSpace _space = editor.getNameSpace(_name);

	    _space.setInsensitive(!_val);
	  }

	// 11. Woohoo, Martha, I is a-coming home!

	if (schemadebug)
	  {
	    // "Successfully completed XML schema edit."
	    err.println(ts.l("processSchema.schemadebug_success"));
	  }

	_success = true;
      }
    catch (Throwable ex)
      {
	// "Caught Exception during XML schema editing.\n{0}"
	err.println(ts.l("processSchema.exception", Ganymede.stackTrace(ex)));

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
	    // "Committing schema edit."
	    err.println(ts.l("processSchema.committing"));
	    editor.commit();
	    this.editor = null;
	    return true;
	  }
	else
	  {
	    // "Releasing schema edit."
	    err.println(ts.l("processSchema.releasing"));

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
		// "Error, unrecognized element: {0} when expecting <namespace>."
		err.println(ts.l("calculateNameSpaces.not_a_namespace", _XNamespaces[i]));

		return false;
	      }
	    
	    String _name = _XNamespaces[i].getAttrStr("name"); // ditto remote

	    if (_entries.containsKey(_name))
	      {
		// "Error, found duplicate <namespace> name ''{0}''."
		err.println(ts.l("calculateNameSpaces.duplicate_namespace", _name));

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
	    // "Error, couldn''t get id number for object definition item: {0}."
	    err.println(ts.l("calculateBases.missing_id", objectdef));

	    return false;
	  }

	if (id.shortValue() < 0)
	  {
	    // "Error, can''t create or edit an object base with a negative id number: {0}."
	    err.println(ts.l("calculateBases.negative_id", objectdef));

	    return false;
	  }

	if (name == null || name.equals(""))
	  {
	    // "Error, couldn''t get name for object definition item: {0}."
	    err.println(ts.l("calculateBases.missing_name", objectdef));

	    return false;
	  }

	Short key = new Short(id.shortValue());
	xmlBases.addElement(key);

	if (entries.containsKey(key))
	  {
	    // "Error, found duplicate object base id number in <ganyschema>: {0}."
	    err.println(ts.l("calculateBases.duplicate_id", objectdef));

	    return false;
	  }

	if (nameTable.containsKey(name))
	  {
	    // "Error, found duplicate object base name in <ganyschema>: {0}."
	    err.println(ts.l("calculateBases.duplicate_name", objectdef));

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
	    // "Error, object type ids of less than 256 are reserved for new system-defined
	    // object types, and may not be created with the xml schema editing system: {0}."
	    err.println(ts.l("calculateBases.reserved_object_base_id", entry));

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

	// "\tRenaming {0} to {1}."
	err.println(ts.l("handleBaseRenaming.renaming_base", numBaseRef.getName(), name));

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
   * in {@link arlut.csd.ganymede.server.GanymedeXMLSession GanymedeXMLSession}
   * to fix up the category tree to match that specified in the XML
   * &lt;ganyschema&gt; element.</p>
   */

  public synchronized ReturnVal reshuffleCategories(XMLItem categoryRoot)
  {
    Hashtable categoryNames = new Hashtable();

    if (!testXMLCategories(categoryRoot, categoryNames))
      {
	// "Error, category names not unique in XML schema."
	return Ganymede.createErrorDialog(ts.l("reshuffleCategories.duplicate_category"));
      }

    DBBaseCategory _rootCategory = buildXMLCategories(categoryRoot);

    if (_rootCategory == null)
      {
	// "Error, buildXMLCategories() was not able to create a new category tree."
	return Ganymede.createErrorDialog(ts.l("reshuffleCategories.failed_categories"));
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
	// "buildXMLCategories() called with a bad XML element.  Expecting <category> element, found {0}."
	err.println(ts.l("buildXMLCategories.bad_root", categoryRoot));

	return null;
      }

    try
      {
	_root = new DBBaseCategory(editor.store, categoryRoot.getAttrStr("name"));
      }
    catch (RemoteException ex)
      {
	// "Caught RMI export error in buildXMLCategories():\n{0}"
	err.println(ts.l("buildXMLCategories.exception", Ganymede.stackTrace(ex)));

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
   * normal client {@link arlut.csd.ganymede.rmi.Session Session} interface.</p>
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
    int modCount = 0;
    int totalCount = 0;

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

		    if (cleanedup.isSet())
		      {
			return false;
		      }

		    // otherwise, it was probably due to something in the xmlobject
		    // constructor, and we should report it..

		    // bad field or object error.. return out of this
		    // method without committing the transaction
		    // our finally clause will log us out

		    // "Error constructing xmlobject for {0}:\n{1}"
		    err.println(ts.l("processData.xmlobject_init_failure", item, Ganymede.stackTrace(ex)));

		    return false;
		  }

		if (modCount == 9)
		  {
		    System.err.print(".");
		    modCount = 0;
		  }
		else
		  {
		    modCount++;
		  }

		totalCount++;

		String mode = objectRecord.getMode();
	    
		if (mode == null || mode.equals("create"))
		  {
		    // if no mode was specified, we'll tentatively
		    // identify it as an object that needs to be
		    // created.. but when it comes time to look at
		    // that, we'll look up the object identifier
		    // attributes, and if we find a pre-existing
		    // match, we'll edit that instead.

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
		    err.println();

		    // "Error, xml object {0} is not uniquely identified within the XML file."
		    err.println(ts.l("processData.duplicate_xmlobject", objectRecord));
		    
		    // our finally clause will log us out

		    return false;
		  }
	      }

	    item = getNextItem();
	  }

	err.println();

	// "Done scanning XML for data elements.  Integrating transaction for {0,number,#} <object> elements."
	err.println(ts.l("processData.integrating", new Integer(totalCount)));

	err.println();

	committedTransaction = integrateXMLTransaction();

	if (committedTransaction)
	  {
	    // "Finished integrating XML data transaction."
	    err.println(ts.l("processData.committed"));
	  }

	return committedTransaction;
      }
    catch (Exception ex)
      {
	// "Error, processData() caught an exception:\n{0}"
	err.println(ts.l("processData.exception", Ganymede.stackTrace(ex)));

	return false;
      }
    finally
      {
	if (!committedTransaction)
	  {
	    // "Aborted XML data transaction, logging out."
	    err.println(ts.l("processData.aborted"));
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
    if (false)
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

  public Invid getInvid(short typeId, String objId) throws NotLoggedInException
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

	if (false)
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

	    if (debug)
	      {
		err.println("GanymedeXMLSession.getInvid() found xmlobject in objectHash for " + typeId + ":" + objId);
		err.println("Found xmlobject is " + element.toString());
	      }

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
    return Invid.createInvid(getTypeNum(typename), num);
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

    if (base == null)
      {
	throw new NullPointerException("Oh, why won't you let my people look up: " + objectTypeName + ", oh my lord?");
      }

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
   * {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate} based
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
   * {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate} and an
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

  private boolean integrateXMLTransaction() throws NotLoggedInException
  {
    boolean success = true;
    ReturnVal attempt;
    HashMap editCount = new HashMap();
    HashMap createCount = new HashMap();
    HashMap deleteCount = new HashMap();
    HashMap inactivateCount = new HashMap();

    /* -- */

    if (cleanedup.isSet())
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
	    // "GanymedeXMLSession Error: couldn''t open transaction {0}: {1}"
	    err.println(ts.l("integrateXMLTransaction.failed_open_msg", session.username, attempt.getDialog().getText()));
	  }
	else
	  {
	    // "GanymedeXMLSession Error: couldn''t open transaction {0}."
	    err.println(ts.l("integrateXMLTransaction.failed_open_no_msg", session.username));
	  }
	
	return false;
      }
    
    session.enableWizards(false); // we're not interactive, don't give us no wizards

    // go through all of our 'possibly created' objects and do the
    // Invid lookups on them before we do any edits, so that we can
    // resolve the labels with the pre-edit names in place

    for (int i = 0; i < createdObjects.size(); i++)
      {
	xmlobject newObject = (xmlobject) createdObjects.elementAt(i);

	newObject.getInvid();
      }

    // now that we've made sure to resolve all Invids for the
    // possibly-newly-created objects, we can go ahead and create
    // those that we really couldn't find for editing.

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

	    if (debug)
	      {
		System.err.println("Editing pre-existing " + newObject);
	      }

	    attempt = newObject.editOnServer(session);

	    if (attempt != null && !attempt.didSucceed())
	      {
		String msg = attempt.getDialogText();

		if (msg != null)
		  {
		    // "GanymedeXMLSession Error editing object {0}:\n{1}"
		    err.println(ts.l("integrateXMLTransaction.editing_error_msg", newObject, msg));
		  }
		else
		  {
		    // "GanymedeXMLSession Error detected editing object {0}, but no specific error message was generated."
		    err.println(ts.l("integrateXMLTransaction.editing_error_no_msg", newObject));
		  }

		success = false;
		continue;
	      }
	  }
	else
	  {
	    incCount(createCount, newObject.typeString);

	    if (debug)
	      {
		System.err.println("Creating " + newObject);
	      }

	    newlyCreated = true;

	    attempt = newObject.createOnServer(session);

	    if (attempt != null && !attempt.didSucceed())
	      {
		String msg = attempt.getDialogText();

		if (msg != null)
		  {
		    // "GanymedeXMLSession Error creating object {0}:\n{1}"
		    err.println(ts.l("integrateXMLTransaction.creating_error_msg", newObject, msg));
		  }
		else
		  {
		    // "GanymedeXMLSession Error detected creating object {0}, but no specific error message was generated."
		    err.println(ts.l("integrateXMLTransaction.creating_error_no_msg", newObject));
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
		if (newlyCreated)
		  {
		    // "[1] Error registering fields for newly created object {0}:\n{1}"
		    err.println(ts.l("integrateXMLTransaction.error_new_registering", newObject, msg));
		  }
		else
		  {
		    // "[1] Error registering fields for edited object {0}:\n{1}"
		    err.println(ts.l("integrateXMLTransaction.error_old_registering", newObject, msg));
		  }
	      }
	    else
	      {
		if (newlyCreated)
		  {
		    // "[1] Error detected registering fields for newly created object {0}."
		    err.println(ts.l("integrateXMLTransaction.error_new_registering_no_msg", newObject));
		  }
		else
		  {
		    // "[1] Error detected registering fields for edited object {0}."
		    err.println(ts.l("integrateXMLTransaction.error_old_registering_no_msg", newObject));
		  }
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
		// "[{0,number,#}] Error registering fields for {1}:\n{2}"
		err.println(ts.l("integrateXMLTransaction.error_registering", new Integer(2), newObject, msg));
	      }
	    else
	      {
		// "[{0,number,#}] Error detected registering fields for {1}."
		err.println(ts.l("integrateXMLTransaction.error_registering_no_msg", new Integer(2), newObject));
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
		// "GanymedeXMLSession Error editing object {0}:\n{1}"
		err.println(ts.l("integrateXMLTransaction.editing_error_msg", object, msg));
	      }
	    else
	      {
		// "GanymedeXMLSession Error detected editing object {0}, but no specific error message was generated."
		err.println(ts.l("integrateXMLTransaction.editing_error_no_msg", object));
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
		// "[{0,number,#}] Error registering fields for {1}:\n{2}"
		err.println(ts.l("integrateXMLTransaction.error_registering", new Integer(3), object, msg));
	      }
	    else
	      {
		// "[{0,number,#}] Error detected registering fields for {1}."
		err.println(ts.l("integrateXMLTransaction.error_registering_no_msg", new Integer(3), object));
	      }

	    success = false;
	    continue;
	  }
      }

    // XXX hm.. what happens here if we created an object in the
    // createdObjects vector (or edited it in the editedObjects
    // vector) which is meant to link to an embedded object?  since
    // we're not registering the embedded objects until we get to this
    // point, it seems that we'd have a problem, there.. ?? XXX

    for (int i = 0; success && i < embeddedObjects.size(); i++)
      {
	xmlobject object = (xmlobject) embeddedObjects.elementAt(i);
	
	attempt = object.registerFields(1); // only non-embedded invids

	if (attempt != null && !attempt.didSucceed())
	  {
	    String msg = attempt.getDialogText();

	    if (msg != null)
	      {
		// "[{0,number,#}] Error registering fields for {1}:\n{2}"
		err.println(ts.l("integrateXMLTransaction.error_registering", new Integer(4), object, msg));
	      }
	    else
	      {
		// "[{0,number,#}] Error detected registering fields for {1}."
		err.println(ts.l("integrateXMLTransaction.error_registering_no_msg", new Integer(4), object));
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
	    // "Error, couldn''t find Invid for object to be inactivated: {0}"
	    err.println(ts.l("integrateXMLTransaction.what_invid_to_inactivate", object));

	    success = false;
	    continue;
	  }

	attempt = session.inactivate_db_object(target);
	
	if (attempt != null && !attempt.didSucceed())
	  {
	    String msg = attempt.getDialogText();

	    if (msg != null)
	      {
		// "Error inactivating {0}:\n{1}"
		err.println(ts.l("integrateXMLTransaction.bad_inactivation", object, msg));
	      }
	    else
	      {
		// "Error detected inactivating {0}."
		err.println(ts.l("integrateXMLTransaction.bad_inactivation_no_msg", object));
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
	    // "Error, couldn''t find Invid for object to be deleted: {0}"
	    err.println(ts.l("integrateXMLTransaction.what_invid_to_delete", object));

	    continue;
	  }

	incCount(deleteCount, object.typeString);

	attempt = session.remove_db_object(target);

	if (attempt != null && !attempt.didSucceed())
	  {
	    String msg = attempt.getDialogText();

	    if (msg != null)
	      {
		// "Error deleting {0}:\n{1}"
		err.println(ts.l("integrateXMLTransaction.bad_deletion", object, msg));
	      }
	    else
	      {
		// "Error detected deleting {0}."
		err.println(ts.l("integrateXMLTransaction.bad_deletion_no_msg", object));
	      }

	    success = false;
	    continue;
	  }
      }

    // and close up the transaction, one way or another
    
    if (success)
      {
	// "Committing transaction."
	err.println(ts.l("integrateXMLTransaction.committing"));
	err.println();

	attempt = session.commitTransaction(true);

	if (attempt != null && !attempt.didSucceed())
	  {
	    String msg = attempt.getDialogText();

	    if (msg != null)
	      {
		// "Error, could not successfully commit this XML data transaction:\n{0}"
		err.println(ts.l("integrateXMLTransaction.commit_error", msg));
	      }
	    else
	      {
		// "Error detected committing XML data transaction."
		err.println(ts.l("integrateXMLTransaction.commit_error_no_msg"));
	      }

	    success = false;
	  }
	else
	  {
	    // set the top-level success flag so that xmlEnd() will
	    // return a success value
	    
	    this.success = true;
	  }

	if (success)
	  {
	    if (createCount.size() > 0)
	      {
		// "Objects created:"
		err.println(ts.l("integrateXMLTransaction.objects_created"));
		
		Iterator iter = createCount.keySet().iterator();
		
		while (iter.hasNext())
		  {
		    String key = (String) iter.next();
		    
		    // "\t{0}: {1,number,#}"
		    err.println(ts.l("integrateXMLTransaction.object_count", key, createCount.get(key)));
		  }
	      }
	    
	    if (editCount.size() > 0)
	      {
		// "Objects edited:"
		err.println(ts.l("integrateXMLTransaction.objects_edited"));
		
		Iterator iter = editCount.keySet().iterator();
		
		while (iter.hasNext())
		  {
		    String key = (String) iter.next();

		    // "\t{0}: {1,number,#}"
		    err.println(ts.l("integrateXMLTransaction.object_count", key, createCount.get(key)));		    
		  }
	      }
	    
	    if (deleteCount.size() > 0)
	      {
		// "Objects deleted:"
		err.println(ts.l("integrateXMLTransaction.objects_deleted"));
		
		Iterator iter = deleteCount.keySet().iterator();
		
		while (iter.hasNext())
		  {
		    String key = (String) iter.next();

		    // "\t{0}: {1,number,#}"
		    err.println(ts.l("integrateXMLTransaction.object_count", key, createCount.get(key)));		    
		  }
	      }
	    
	    if (inactivateCount.size() > 0)
	      {
		// "Objects inactivated:"
		err.println(ts.l("integrateXMLTransaction.objects_inactivated"));
		
		Iterator iter = inactivateCount.keySet().iterator();
		
		while (iter.hasNext())
		  {
		    String key = (String) iter.next();

		    // "\t{0}: {1,number,#}"
		    err.println(ts.l("integrateXMLTransaction.object_count", key, createCount.get(key)));		    
		  }
	      }

	    // "Transaction successfully committed."
	    err.println(ts.l("integrateXMLTransaction.thrill_of_victory"));

	    return success;
	  }
      }

    // we should only get here if success == false

    // "Errors encountered, aborting transaction. "
    err.println(ts.l("integrateXMLTransaction.agony_of_defeat"));

    return success;
  }

  /**
   * this private helper method increments a counting
   * integer in table, keyed by type.
   */

  private void incCount(HashMap table, String type)
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
    if (success)
      {
	if (message != null && message.length() > 0)
	  {
	    System.out.println(message);

	    ReturnVal retVal = new ReturnVal(true);

	    retVal.setDialog(new JDialogBuff(ts.l("getReturnVal.default_title"), // "XML client message"
					     message,
					     ts.l("getReturnVal.ok_button"), // "OK"
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
	if (message == null)
	  {
	    return new ReturnVal(false);
	  }
	else
	  {
	    // we depend on createErrorDialog() to dump the progress to the server log

	    return Ganymede.createErrorDialog(ts.l("getReturnVal.failure_title"), // "XML client error"
					      message);
	  }
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
    Enumeration en;
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

	// "GanymedeXMLSession entering editSchema"
	Ganymede.debug(ts.l("editSchema.entering"));

	// try disabling the semaphore with a false waitForZero value,
	// so that we can go into schema edit mode while still
	// maintaining our GanymedeSession's semaphore increment

	try
	  {
	    String semaphoreCondition = GanymedeServer.lSemaphore.disable("schema edit", false, 0);
	    
	    if (semaphoreCondition != null)
	      {
		// "GanymedeXMLSession Can''t edit schema, semaphore error: {0}"
		Ganymede.debug(ts.l("editSchema.semaphore_blocked", semaphoreCondition));
		
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
	// "GanymedeXMLSession entering editSchema synchronization block"
	Ganymede.debug(ts.l("editSchema.entering_synchronized"));

	en = Ganymede.db.objectBases.elements();

	if (en != null)
	  {
	    while (en.hasMoreElements())
	      {
		base = (DBObjectBase) en.nextElement();

		if (base.isLocked())
		  {
		    // "GanymedeXMLSession Can''t edit schema, previous lock held on object base {0}"
		    Ganymede.debug(ts.l("editSchema.base_blocked", base.getName()));

		    GanymedeServer.lSemaphore.enable("schema edit");
		    return null;
		  }
	      }
	  }

	// should be okay
	
	// "GanymedeXMLSession Ok to create DBSchemaEdit"
	Ganymede.debug(ts.l("editSchema.ok_to_edit"));

	// "XML Schema Edit In Progress"	
	GanymedeAdmin.setState(ts.l("editSchema.admin_notify"));

	try
	  {
	    DBSchemaEdit result = new DBSchemaEdit();
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

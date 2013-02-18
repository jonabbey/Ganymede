/*
   GASH 2

   GanymedeXMLSession.java

   The GANYMEDE object storage system.

   Created: 1 August 2000

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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

package arlut.csd.ganymede.server;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.rmi.server.Unreferenced;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
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
import arlut.csd.ganymede.common.FieldType;
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
 * arlut.csd.ganymede.rmi.Server Server}'s {@link
 * arlut.csd.ganymede.rmi.Server#xmlLogin(java.lang.String username,
 * java.lang.String password) xmlLogin()} method.  A
 * GanymedeXMLSession is created on top of a {@link
 * arlut.csd.ganymede.server.GanymedeSession GanymedeSession} and
 * interacts with the database through that session.  A
 * GanymedeXMLSession generally looks to the rest of the server like
 * any other client, except that if the XML file contains a
 * &lt;ganyschema&gt; section, the GanymedeXMLSession will attempt to
 * manipulate the server's login semaphore to force the server into
 * schema editing mode.  This will fail if there are any remote
 * clients connected to the server at the time the XML file is
 * processed.</p>
 *
 * <p>Once xmlLogin creates (and RMI exports) a GanymedeXMLSession, an
 * xmlclient repeatedly calls the {@link
 * arlut.csd.ganymede.server.GanymedeXMLSession#xmlSubmit(byte[])
 * xmlSubmit()} method, which writes the bytes received into a pipe.
 * The GanymedeXMLSession's thread (also initiated by
 * GanymedeServer.xmlLogin()) then loops, reading data off of the pipe
 * with an {@link arlut.csd.Util.XMLReader XMLReader} and doing
 * various schema editing and data loading operations.</p>
 *
 * <p>The &lt;ganydata&gt; processing section was originally written
 * as part of xmlclient, and did all xml parsing on the client side
 * and all data operations remotely over RMI.  Pulling this logic into
 * a server-side GanymedeXMLSession sped things up by a factor of 300
 * in my testing.</p>
 */

public final class GanymedeXMLSession extends java.lang.Thread implements XMLSession, Unreferenced {

  public static final boolean debug = false;
  public static final boolean schemadebug = true;

  /**
   * How big shall we make our default invid/xmlobject hash size when
   * we're processing objects in an object base?  This should be small
   * enough not to be too great a waste, but big enough that we won't
   * have to worry about lots of hashtable re-hashing during
   * transaction processing.
   */

  private static final int OBJECTHASHSIZE = 10001;

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.GanymedeXMLSession");

  /**
   * This major version number is compared with the "major"
   * attribute in the Ganymede XML document element.  We won't
   * try to read Ganymede XML files whose major and/or minor numbers
   * are too high.
   */

  public static final int majorVersion = 1;

  /**
   * This minor version number is compared with the "minor"
   * attribute in the Ganymede XML document element.  We won't
   * try to read Ganymede XML files whose major and/or minor numbers
   * are too high.
   */

  public static final int minorVersion = 1;

  /**
   * The working GanymedeSession underlying this XML session.
   */

  GanymedeSession session;

  /**
   * The XML parser object handling XML data from the client
   */

  arlut.csd.Util.XMLReader reader;

  /**
   * The data stream used to write data from the client to the
   * XML parser.
   */

  private PipedOutputStream pipe;

  /**
   * The default buffer size in the {@link arlut.csd.Util.XMLReader XMLReader}.
   * This value determines how far ahead the XMLReader's i/o thread can get in
   * reading from the XML file.  Higher or lower values of this variable may
   * give better performance, depending on the characteristics of the JVM with
   * regards threading, etc.
   */

  private int bufferSize = 100;

  /**
   * Hashtable mapping object type names to
   * hashtables mapping field names to
   * {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate}
   * objects.
   */

  private Hashtable<String, Hashtable<String, FieldTemplate>> objectTypes =
    new Hashtable<String, Hashtable<String, FieldTemplate>>();

  /**
   * Hashtable mapping Short object type ids to
   * hashtables mapping field names to
   * {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate}
   * objects.
   */

  private Hashtable<Short, Hashtable<String, FieldTemplate>> objectTypeIDs =
    new Hashtable<Short, Hashtable<String, FieldTemplate>>();

  /**
   * <p>Rather overloaded Hashtable mapping Short type ids to hashes
   * which map local object designations (either id Strings or num
   * Integers from the &lt;object&gt; elements) either to actual
   * {@link arlut.csd.ganymede.server.xmlobject xmlobject} records or
   * to raw {@link arlut.csd.ganymede.common.Invid Invids}.</p>
   *
   * <p>The purpose of this structure is to efficiently (time-wise) track
   * targets for the &lt;invid&gt; elements that are encountered
   * during processing of an XML transaction stream.  In many cases,
   * these targets will be &lt;object&gt; elements that have not yet
   * been created in the server's persistent data store.  Not all,
   * however.  In the cases where they properly refer to pre-existing
   * objects on the server that are not edited by &lt;object&gt;
   * elements in the XML transaction, the inner hashtable structures
   * will contain simple Invid objects rather than xmlobjects.</p>
   */

  private Hashtable<Short, Hashtable> objectStore = new Hashtable<Short, Hashtable>();

  /**
   * HashSet used to detect &lt;object&gt; elements that map to the same Invid
   * in the DBStore.
   */

  private HashSet<Invid> duplications = null;

  /**
   * Vector of {@link arlut.csd.ganymede.server.xmlobject xmlobjects}
   * that correspond to new Ganymede server objects
   * that have been/need to be created by this GanymedeXMLSession.
   */

  private Vector<xmlobject> createdObjects = new Vector<xmlobject>();

  /**
   * Vector of {@link arlut.csd.ganymede.server.xmlobject xmlobjects}
   * that correspond to pre-existing Ganymede
   * server objects that have been/need to be checked out for editing by this
   * GanymedeXMLSession.
   */

  private Vector<xmlobject> editedObjects = new Vector<xmlobject>();

  /**
   * Vector of {@link arlut.csd.ganymede.server.xmlobject
   * xmlobjects} that correspond to Ganymede server objects that have
   * been created/checked out for editing during embedded invid field
   * processing, and which need to have their invid fields registered
   * after everything else is done.
   */

  private Vector<xmlobject> embeddedObjects = new Vector<xmlobject>();

  /**
   * Vector of {@link arlut.csd.ganymede.server.xmlobject xmlobjects}
   * that correspond to pre-existing Ganymede
   * server objects that have been/need to be inactivated by this
   * GanymedeXMLSession.
   */

  private Vector<xmlobject> inactivatedObjects = new Vector<xmlobject>();

  /**
   * Vector of {@link arlut.csd.ganymede.server.xmlobject xmlobjects}
   * that correspond to pre-existing Ganymede
   * server objects that have been/need to be deleted by this
   * GanymedeXMLSession.
   */

  private Vector<xmlobject> deletedObjects = new Vector<xmlobject>();

  /**
   * This StringWriter holds output generated by the GanymedeXMLSession's
   * parser thread.
   */

  private StringWriter errBuf = new StringWriter();

  /**
   * This PrintWriter is used to handle all debug/error output
   * on behalf of the GanymedeXMLSession.
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
   * This flag is used to track whether the background parser thread
   * was successful in committing the transaction.
   */

  private boolean success = false;

  /**
   * If we are editing the server's schema from the XML source, this
   * field will hold a reference to a DBSchemaEdit object.
   */

  private DBSchemaEdit editor = null;

  /**
   * This vector is used by the XML schema editing logic to track
   * namespaces from the xml file that need to be added to the current
   * schema.  Elements in this vector are empty XMLElements that contain
   * name and optional case-sensitive attributes.
   */

  private Vector<XMLItem> spacesToAdd;

  /**
   * This vector is used by the XML schema editing logic to track
   * namespaces from the xml file that need to be removed from the
   * current schema. Elements in this vector are Strings representing
   * the level of name spaces to be deleted..
   */

  private Vector<String> spacesToRemove;

  /**
   * This vector is used by the XML schema editing logic to track
   * namespaces from the xml file that need to be edited in the
   * current schema.  Since namespaces can only be edited in the sense
   * of toggling the case sensitivity flag, this vector will only
   * contain XMLElements for namespaces that need to have their case
   * sensitivity toggled. Elements in this vector are empty
   * XMLElements that contain name and optional case-sensitive
   * attributes.
   */

  private Vector<XMLItem> spacesToEdit;

  /**
   * This vector is used by the XML schema editing logic to track
   * object types from the xml file that need to be added to the current
   * schema.  Elements in this vector are XMLItem trees rooted
   * with the appropriate &lt;objectdef&gt; elements.
   */

  private Vector<XMLItem> basesToAdd;

  /**
   * This vector is used by the XML schema editing logic to track
   * object types in the current schema that were not mentioned in the
   * xml file and thus need to be removed from the current
   * schema. Elements of this vector are the names of existing bases
   * to be removed.
   */

  private Vector<String> basesToRemove;

  /**
   * This vector is used by the XML schema editing logic to track
   * object types from the xml file that need to be edited in the
   * current schema.  Elements in this vector are XMLItem trees rooted
   * with the appropriate &lt;objectdef&gt; elements.
   */

  private Vector<XMLItem> basesToEdit;

  /**
   * This XMLItem is the XMLElement root of the namespace tree,
   * rooted with the &lt;namespaces&gt; element.  Children of this
   * node will be &lt;namespace&gt; elements.
   */

  private XMLItem namespaceTree = null;

  /**
   * This XMLItem is the XMLElement root of the category tree,
   * rooted with the top-level &lt;category&gt; element.
   * Children of this node will be either &lt;category&gt; or
   * &lt;objectdef&gt; elements.
   */

  private XMLItem categoryTree = null;

  /**
   * Comment for the <ganydata> transaction commit, if any is
   * provided.
   */

  private String comment = null;

  /**
   * Semaphore to gate the cleanup() method.
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
        //
        // Used only for processing input from xmlclient.  If the
        // xmlclient only wanted to dump data, it would just use a
        // GanymedeSession and call one of that class' getXML()
        // methods

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
   * This method returns a remote reference to the underlying
   * GanymedeSession in use on the server.
   *
   * @see arlut.csd.ganymede.rmi.XMLSession
   */

  public Session getSession()
  {
    return session;
  }

  /**
   * This method is called repeatedly by the XML client in order to
   * send the next packet of XML data to the server.  If the server
   * has detected any errors in the already-received XML stream,
   * xmlSubmit() may return a non-null ReturnVal with a description of
   * the failure.  Otherwise, the xmlSubmit() method will enqueue the
   * XML data for the server's continued processing and immediately
   * return a null value, indicating success.  The xmlSubmit() method
   * will only block if the server has filled up its internal buffers
   * and must wait to digest more of the already submitted XML.
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

            pipe.write(bytes);  // can block if the parser thread gets behind

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
                System.err.println(ts.l("xmlSubmit.waiting_for_reader", Integer.valueOf(waitCount)));

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
   * This method is called by the XML client once the end of the XML
   * stream has been transmitted, whereupon the server will attempt
   * to finalize the XML transaction and return an overall success or
   * failure message in the ReturnVal.
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
            errBuffer.setLength(0);     // this doesn't actually free memory.. stoopid StringBuffer
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
                String finalBit = errBuffer.toString();
                errBuffer.setLength(0);

                progress = progress + finalBit;
                System.err.print(finalBit);
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
   * This method is for use on the server, and is called by the
   * GanymedeSession to let us know if the server is forcing our login
   * off.
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
            Ganymede.logError(ex);
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

        reader.close();         // this will cause the XML Reader to halt
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
   * <p>The RMI timeout can be modified by setting the system property
   * sun.rmi.transport.proxy.connectTimeout.</p>
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
   * This method handles cleanup post-schema edit.
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
   * This method handles the actual XML processing in the
   * background.  All activity which ultimately draws from
   * the XMLReader will block as necessary to wait for more
   * data from the client.
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
            // "Error, the Ganymede document element {0} does not contain a compatible major version number."
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
                // don't bother processing rest of XML doc.. just jump
                // down to finally clause

                return;
              }

            nextElement = getNextItem();
          }

        while ((!nextElement.matchesClose("ganymede") && !(nextElement instanceof XMLCloseElement)) && !(nextElement instanceof XMLEndDocument))
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
   * Helper method to process events from the {@link
   * arlut.csd.Util.XMLReader XMLReader}.  By using this method, the
   * rest of the code in GanymedeXMLSession doesn't have to check for
   * error and warning conditions.
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
   * Helper method to peek at the next event from the {@link
   * arlut.csd.Util.XMLReader XMLReader}.  If the peek finds an
   * XMLError item, a SAXException will be thrown.  If the peek finds
   * any XMLWarning items, they will be consumed and the contents of
   * the warning text passed to err.
   */

  public XMLItem peekNextItem() throws SAXException
  {
    XMLItem item = null;

    item = reader.peekNextItem();

    if (item instanceof XMLError)
      {
        throw new SAXException(item.toString());
      }

    while (item instanceof XMLWarning)
      {
        // "Warning!: {0}"
        err.println(ts.l("getNextItem.warning", item));

        reader.getNextItem();   // consume the peeked warning item
        item = reader.peekNextItem();
      }

    return item;
  }

  /**
   * This method is called after the &lt;ganyschema&gt; element has been
   * read and consumes everything up to and including the matching
   * &lt;/ganyschema&gt; element, if such is to be found.  Eventually,
   * this method will actually process the contents of the
   * &lt;ganyschema&gt; element and transmit the schema change information
   * to the server.
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

        if (!session.getPermManager().isSuperGash())
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
            _success = true;    // no editing to be done
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

        for (XMLItem _space: spacesToAdd)
          {
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

        for (String _basename: basesToRemove)
          {
            // "\tDeleting object base {0}."
            err.println(ts.l("processSchema.deleting_base", _basename));

            if (!handleReturnVal(editor.deleteBase(_basename)))
              {
                return false;
              }
          }

        // 5. rename any bases that need to be renamed

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

        for (XMLItem _entry: basesToAdd)
          {
            // "\tCreating object base {0}"
            err.println(ts.l("processSchema.creating_objectbase", _entry.getAttrStr("name")));

            Integer _id = _entry.getAttrInt("id");

            boolean _embedded = false;

            XMLItem _children[] = _entry.getChildren();

            if (_children != null)
              {
                for (XMLItem _child: _children)
                  {
                    if (_child.matches("embedded"))
                      {
                        _embedded = true;
                        break;
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

        for (XMLItem _entry: basesToEdit)
          {
            Integer _id = _entry.getAttrInt("id");

            DBObjectBase _oldBase = (DBObjectBase) editor.getBase(_id.shortValue());

            if (_oldBase == null)
              {
                // " Error, couldn''t find DBObjectBase for {0} in pass {1,number,#}."
                err.println(ts.l("processSchema.bad_base", _entry.getTreeString(), Integer.valueOf(1)));

                return false;
              }

            if (false)
              {
                // "7.  pass 1 - fixups on {0}"
                err.println(ts.l("processSchema.schemadebug_7_1", _oldBase.getName()));
              }

            // "\tEditing object base {0}"
            err.println(ts.l("processSchema.editing_objectbase", _oldBase.getName()));

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

        for (XMLItem _entry: basesToAdd)
          {
            Integer _id = _entry.getAttrInt("id");

            DBObjectBase _oldBase = (DBObjectBase) editor.getBase(_id.shortValue());

            if (_oldBase == null)
              {
                // " Error, couldn''t find DBObjectBase for {0} in pass {1,number,#}."
                err.println(ts.l("processSchema.bad_base", _entry.getTreeString(), Integer.valueOf(2)));

                return false;
              }

            if (schemadebug)
              {
                // "7.  pass 2 - fixups on object base {0}"
                err.println(ts.l("processSchema.schemadebug_7_2", _oldBase.getName()));
              }

            //      err.println("\tResolving " + _oldBase);

            if (!handleReturnVal(_oldBase.setXML(_entry, true, err)))
              {
                return false;
              }
          }

        for (XMLItem _entry: basesToEdit)
          {
            Integer _id = _entry.getAttrInt("id");

            DBObjectBase _oldBase = (DBObjectBase) editor.getBase(_id.shortValue());

            if (_oldBase == null)
              {
                // " Error, couldn''t find DBObjectBase for {0} in pass {1,number,#}."
                err.println(ts.l("processSchema.bad_base", _entry.getTreeString(), Integer.valueOf(3)));

                return false;
              }

            if (schemadebug)
              {
                // "7.  pass 3 - fixups on object base {0}"
                err.println(ts.l("processSchema.schemadebug_7_3", _oldBase.getName()));
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

        for (String _name: spacesToRemove)
          {
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

        for (XMLItem _entry: spacesToEdit)
          {
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
   * This method fills in spacesToAdd, spacesToRemove, and spacesToEdit.
   */

  private boolean calculateNameSpaces()
  {
    try
      {
        NameSpace[] _list = editor.getNameSpaces();

        Vector<String> _current = new Vector<String>(_list.length);

        for (NameSpace _ns: _list)
          {
            // theoretically possible RemoteException here, due to remote interface

            _current.add(_ns.getName());
          }

        XMLItem _XNamespaces[] = namespaceTree.getChildren();

        Vector<String> _newSpaces = new Vector<String>(_XNamespaces.length);
        Hashtable<String, XMLItem> _entries = new Hashtable<String, XMLItem>(_XNamespaces.length);

        for (XMLItem _xns: _XNamespaces)
          {
            if (!_xns.matches("namespace"))
              {
                // "Error, unrecognized element: {0} when expecting <namespace>."
                err.println(ts.l("calculateNameSpaces.not_a_namespace", _xns));

                return false;
              }

            String _name = _xns.getAttrStr("name"); // ditto remote

            if (_entries.containsKey(_name))
              {
                // "Error, found duplicate <namespace> name ''{0}''."
                err.println(ts.l("calculateNameSpaces.duplicate_namespace", _name));

                return false;
              }

            _entries.put(_name, _xns);
            _newSpaces.add(_name);
          }

        // for spacesToRemove, we just keep the names for the missing
        // name spaces

        spacesToRemove = VectorUtils.difference(_current, _newSpaces);

        // for spacesToAdd and spacesToEdit, we need to first identify
        // names that are new or that were already in our current
        // namespaces list, then look up and save the appropriate
        // XMLItem nodes in the spacesToAdd and spacesToEdit global
        // Vectors.

        Vector<String> _additions = VectorUtils.difference(_newSpaces, _current);

        spacesToAdd = new Vector<XMLItem>();

        for (String _name: _additions)
          {
            spacesToAdd.add(_entries.get(_name));
          }

        Vector<String> _possibleEdits = VectorUtils.intersection(_newSpaces, _current);

        spacesToEdit = new Vector<XMLItem>();

        // we are only interested in namespaces to be edited if the
        // case-sensitivity changes.  we could defer this check, but
        // since we know that case-sensitivity is the only thing that
        // can vary in a namespace other than its name, we'll go ahead
        // and filter out no-changes here.

        for (String _name: _possibleEdits)
          {
            XMLItem _entry = _entries.get(_name);
            NameSpace _oldEntry = editor.getNameSpace(_name);

            // yes, ==, not !=.. note that the _oldEntry check is for
            // insensitivity, not sensitivity.

            if (_entry.getAttrBoolean("case-sensitive") == _oldEntry.isCaseInsensitive())
              {
                spacesToEdit.add(_entry);
              }
          }
      }
    catch (RemoteException ex)
      {
        Ganymede.logError(ex);
        throw new RuntimeException(ex.getMessage());
      }

    return true;
  }

  /**
   * This method fills in basesToAdd, basesToRemove, and basesToEdit.
   */

  private boolean calculateBases()
  {
    // create a list of Short base id's for of bases that we have
    // registered in the schema at present

    DBObjectBase[] list = (DBObjectBase[]) editor.getBases();
    Vector<Short> current = new Vector(list.length);

    for (DBObjectBase base: list)
      {
        current.add(base.getKey());
      }

    // get a list of objectdef root nodes from our xml tree

    Vector<XMLItem> newBases = new Vector<XMLItem>();

    findBasesInXMLTree(categoryTree, newBases);

    // get a list of Short id's from our xml tree, record
    // a mapping from those id's to the objectdef nodes in
    // our xml tree

    Vector<Short> xmlBases = new Vector<Short>();
    Hashtable<Short, XMLItem> entries = new Hashtable<Short, XMLItem>(); // for Short id's
    HashSet<String> nameTable = new HashSet<String>(); // for checking for redundant names

    for (XMLItem objectdef: newBases)
      {
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

        Short key = Short.valueOf(id.shortValue());
        xmlBases.add(key);

        if (entries.containsKey(key))
          {
            // "Error, found duplicate object base id number in <ganyschema>: {0}."
            err.println(ts.l("calculateBases.duplicate_id", objectdef));

            return false;
          }

        if (nameTable.contains(name))
          {
            // "Error, found duplicate object base name in <ganyschema>: {0}."
            err.println(ts.l("calculateBases.duplicate_name", objectdef));

            return false;
          }

        entries.put(key, objectdef);
        nameTable.add(name);
      }

    // We need to calculate basesToRemove.. since the DBSchemaEditor
    // can only delete bases based on their names, we need to
    // take the Vector of Shorts that we get from difference and
    // put the matching names into basesToRemove

    Vector<Short> deletions = VectorUtils.difference(current, xmlBases);

    basesToRemove = new Vector<String>();

    for (Short id: deletions)
      {
        try
          {
            // Base.getName() is defined to throw RemoteException

            basesToRemove.add(editor.getBase(id.shortValue()).getName());
          }
        catch (RemoteException ex)
          {
            // should never ever happen

            Ganymede.logError(ex);
            throw new RuntimeException(ex.getMessage());
          }
      }

    // now calculate basesToAdd and basesToEdit, recording the
    // objectdef XMLItem root for each base in each list

    Vector<Short> additions = VectorUtils.difference(xmlBases, current);
    Vector<Short> edits = VectorUtils.intersection(xmlBases, current);

    basesToAdd = new Vector<XMLItem>();

    for (Short id: additions)
      {
        XMLItem entry = entries.get(id);

        if (entry.getAttrInt("id").shortValue() < 256)
          {
            // "Error, object type ids of less than 256 are reserved for new system-defined
            // object types, and may not be created with the xml schema editing system: {0}."
            err.println(ts.l("calculateBases.reserved_object_base_id", entry));

            return false;
          }

        basesToAdd.add(entry);
      }

    basesToEdit = new Vector<XMLItem>();

    for (Short id: edits)
      {
        XMLItem entry = entries.get(id);

        basesToEdit.add(entry);
      }

    return true;
  }

  /**
   * This is a recursive method to do a traversal of an XMLItem
   * tree, adding object base definition roots found to the foundBases
   * vector.
   */

  private void findBasesInXMLTree(XMLItem treeRoot, Vector<XMLItem> foundBases)
  {
    // objectdef's will contain fielddef children, but no more
    // objectdef's, so we treat objectdef's as leaf nodes for our
    // traversal

    if (treeRoot.matches("objectdef"))
      {
        foundBases.add(treeRoot);
        return;
      }

    XMLItem children[] = treeRoot.getChildren();

    if (children == null)
      {
        return;
      }

    for (XMLItem childRoot: children)
      {
        findBasesInXMLTree(childRoot, foundBases);
      }
  }

  /**
   * This private method takes care of doing any object type
   * renaming, prior to resolving invid field definitions.
   */

  private boolean handleBaseRenaming() throws RemoteException
  {
    Base numBaseRef;
    Base nameBaseRef;
    String name;

    /* -- */

    for (XMLItem myBaseItem: basesToEdit)
      {
        name = XMLUtils.XMLDecode(myBaseItem.getAttrStr("name"));

        numBaseRef = editor.getBase(myBaseItem.getAttrInt("id").shortValue());

        if (name.equals(numBaseRef.getName()))
          {
            continue;           // no rename necessary
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
   * This method is used by the XML schema editing code
   * in {@link arlut.csd.ganymede.server.GanymedeXMLSession GanymedeXMLSession}
   * to fix up the category tree to match that specified in the XML
   * &lt;ganyschema&gt; element.
   */

  public synchronized ReturnVal reshuffleCategories(XMLItem categoryRoot)
  {
    HashSet<String> categoryNames = new HashSet<String>();

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

    return null;                // tada!
  }

  /**
   * This method tests an XML category tree to make sure that all
   * categories in the tree have unique names.
   */

  public boolean testXMLCategories(XMLItem categoryRoot, HashSet<String> names)
  {
    if (categoryRoot.matches("category"))
      {
        // make sure we don't get duplicate category names

        if (names.contains(categoryRoot.getAttrStr("name")))
          {
            return false;
          }
        else
          {
            names.add(categoryRoot.getAttrStr("name"));
          }

        XMLItem children[] = categoryRoot.getChildren();

        if (children == null)
          {
            return true;
          }

        for (XMLItem childRoot: children)
          {
            if (!testXMLCategories(childRoot, names))
              {
                return false;
              }
          }
      }

    return true;
  }

  /**
   * This recursive method takes an XMLItem category tree and returns
   * a new DBBaseCategory tree with all categories and object definitions
   * from the XMLItem category tree ordered correctly.
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
        _root = new DBBaseCategory(Ganymede.db, categoryRoot.getAttrStr("name"));
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

    for (XMLItem _child: _children)
      {
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
   * <p>This method is called after the &lt;ganydata&gt; element has
   * been read and consumes everything up to and including the
   * matching &lt;/ganydata&gt; element, if such is to be found.</p>
   *
   * <p>Before starting to read data from the &lt;ganydata&gt;
   * element, this method communicates with the Ganymede server
   * database through the normal client {@link
   * arlut.csd.ganymede.rmi.Session Session} interface.</p>
   *
   * <p>The contents of &lt;ganydata&gt; are scanned, and an in-memory
   * datastructure is constructed in the GanymedeXMLSession.  All
   * objects are organized in memory by type and id, and inter-object
   * invid references are resolved to the extent possible.</p>
   *
   * <p>If all of that succeeds, processData() will start a
   * transaction on the server, and will start transferring the data
   * from the XML file's &lt;ganydata&gt; element into the database.
   * If any errors are reported, the returned error message is printed
   * and processData aborts.  If no errors are reported at this stage,
   * a transaction commit is attempted.  Once again, if there are any
   * errors reported from the server, they are printed and processData
   * aborts.  Otherwise, success!</p>
   *
   * @return true if the &lt;ganydata&gt; element was successfully
   * processed, or false if a fatal error in the XML stream was
   * encountered during processing
   */

  public boolean processData() throws SAXException
  {
    XMLItem item = null;
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
            if (item.matches("comment") && reader.isNextCharData())
              {
                this.comment = reader.getFollowingString(item, true);
              }
            else if (item.matches("object"))
              {
                xmlobject objectRecord = null;

                try
                  {
                    objectRecord = new xmlobject((XMLElement) item, this, null);
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

                    createdObjects.add(objectRecord);
                  }
                else if (mode.equals("edit"))
                  {
                    editedObjects.add(objectRecord);
                  }
                else if (mode.equals("delete"))
                  {
                    deletedObjects.add(objectRecord);
                  }
                else if (mode.equals("inactivate"))
                  {
                    inactivatedObjects.add(objectRecord);
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
        err.println(ts.l("processData.integrating", Integer.valueOf(totalCount)));

        err.println();

        try
          {
            this.duplications = new HashSet<Invid>();

            committedTransaction = integrateXMLTransaction();
          }
        finally
          {
            this.duplications = null;
          }

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
        reader.pushbackItem(item);  // let the run() method see what we ran into at the end

        if (!committedTransaction)
          {
            // "Aborted XML data transaction, logging out."
            err.println(ts.l("processData.aborted"));
          }

        session.logout();
      }
  }

  /**
   * This private method handles data structures initialization for
   * the GanymedeXMLSession, prepping hash lookups that are used
   * to accelerate XML processing.
   */

  private void initializeLookups()
  {
    if (debug)
      {
        System.err.println("GanymedeXMLSession: initializeLookups");
      }

    for (DBObjectBase base: Ganymede.db.getBases())
      {
        Vector<FieldTemplate> templates = base.getFieldTemplateVector();
        Hashtable<String, FieldTemplate> fieldHash = new Hashtable<String, FieldTemplate>();

        for (FieldTemplate tmpl: templates)
          {
            fieldHash.put(tmpl.getName(), tmpl);
          }

        objectTypes.put(base.getName(), fieldHash);
        objectTypeIDs.put(Short.valueOf(base.getTypeID()), fieldHash);
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

    Hashtable objectHash = objectStore.get(object.type);

    if (objectHash == null)
      {
        objectHash = new Hashtable(OBJECTHASHSIZE, 0.75f);
        objectStore.put(object.type, objectHash);
      }

    if (object.id != null)
      {
        if (objectHash.containsKey(object.id))
          {
            Object thing = objectHash.get(object.id);

            if (thing instanceof xmlobject)
              {
                // we've already got an xmlobject with that id stored

                return false;
              }
            else if (thing instanceof Invid)
              {
                // we've got a previously cached Invid associated with
                // this object's id.. go ahead and replace it with an
                // actual xmlobject.

                Invid objectInvid;

                try
                  {
                    objectInvid = object.getInvid();
                  }
                catch (NotLoggedInException ex)
                  {
                    throw new RuntimeException(ex); // really can't happen
                  }

                if (!thing.equals(objectInvid))
                  {
                    if (objectInvid == null)
                      {
                        object.setInvid((Invid) thing);
                      }
                    else
                      {
                        // ugh!  we seem to be storing an xmlobject
                        // that thinks it belongs to an Invid that
                        // doesn't match a previous one associated
                        // with this slot.  that can't possibly be
                        // right, can it?

                        return false;
                      }
                  }

                objectHash.put(object.id, object);
              }
            else
              {
                throw new ClassCastException();
              }
          }
        else
          {
            objectHash.put(object.id, object);
          }
      }
    else if (object.num != -1)
      {
        Integer intKey = Integer.valueOf(object.num);

        if (objectHash.containsKey(intKey))
          {
            Object thing = objectHash.get(intKey);

            if (thing instanceof xmlobject)
              {
                return false;
              }
            else if (thing instanceof Invid)
              {
                // overwrite the cached Invid.  Note that since the
                // object being stored has its Invid forced with the
                // use of the num field, there's no way that this
                // xmlobject we're storing can't match the Invid
                // already stored in this slot.

                objectHash.put(intKey, object);
              }
            else
              {
                throw new ClassCastException();
              }
          }
        else
          {
            objectHash.put(intKey, object);
          }
      }

    return true;
  }

  /**
   * This method is used to look up an xmlobject that we have seen,
   * in order to get a partial resolution of an invid target that we
   * have found in our XML processing.  It is called by {@link
   * arlut.csd.ganymede.server.xInvid#getInvid()} in the event that an
   * &gt;invid&lt; element is found which does not resolve to a
   * pre-existing object in the server.
   */

  public xmlobject getXMLObjectTarget(short typeId, String objectId)
  {
    Hashtable objectHash = objectStore.get(Short.valueOf(typeId));

    if (objectHash == null)
      {
        return null;
      }

    Object result = objectHash.get(objectId);

    if (result != null && result instanceof xmlobject)
      {
        return (xmlobject) result;
      }
    else
      {
        return null;
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

  public Invid getInvid(short typeId, String objId) throws NotLoggedInException
  {
    Invid invid = null;
    Short typeKey;
    Hashtable objectHash;

    /* -- */

    typeKey = Short.valueOf(typeId);
    objectHash = objectStore.get(typeKey);

    if (objectHash == null)
      {
        // we do this mainly so we can fall through to our if (element
        // == null) logic below.

        objectHash = new Hashtable(OBJECTHASHSIZE, 0.75f);
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
            // cache it in our objectStore so that we won't have to do
            // (relatively) expensive lookups from here on out.

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
   * This method retrieves an xmlobject that has been previously
   * loaded from the XML file.
   *
   * @param baseName An XML-encoded object type string
   * @param objectID The id string for the object in question
   */

  public xmlobject getObject(String baseName, String objectID)
  {
    return getObject(Short.valueOf(getTypeNum(baseName)), objectID);
  }

  /**
   * This method retrieves an xmlobject that has been previously
   * loaded from the XML file.
   *
   * @param baseID a Short holding the number of object type sought
   * @param objectID The id string for the object in question
   */

  public xmlobject getObject(Short baseID, String objectID)
  {
    Hashtable objectHash = objectStore.get(baseID);

    if (objectHash == null)
      {
        return null;
      }

    Object thing = objectHash.get(objectID);

    if (thing != null && thing instanceof xmlobject)
      {
        return (xmlobject) thing;
      }

    return null;
  }

  /**
   * This method retrieves an xmlobject that has been previously
   * loaded from the XML file.
   *
   * @param baseName An XML-encoded object type string
   * @param objectNum The Integer object number for the object sought
   */

  public xmlobject getObject(String baseName, Integer objectNum)
  {
    return getObject(Short.valueOf(getTypeNum(baseName)), objectNum);
  }

  /**
   * This method retrieves an xmlobject that has been previously
   * loaded from the XML file.
   *
   * @param baseID a Short holding the number of object type sought
   * @param objectNum The Integer object number for the object sought
   */

  public xmlobject getObject(Short baseID, Integer objectNum)
  {
    Hashtable objectHash = objectStore.get(baseID);

    if (objectHash == null)
      {
        return null;
      }

    Object thing = objectHash.get(objectNum);

    if (thing != null && thing instanceof xmlobject)
      {
        return (xmlobject) thing;
      }

    return null;
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

  public Hashtable<String, FieldTemplate> getFieldHash(String objectTypeName)
  {
    return objectTypes.get(XMLUtils.XMLDecode(objectTypeName));
  }

  /**
   * This helper method takes a hash of field names to
   * {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate} and an
   * underscore-for-space XML encoded field name and returns the
   * FieldTemplate for that field, if known.  If not, null is
   * returned.
   */

  public FieldTemplate getObjectFieldType(Hashtable<String, FieldTemplate> fieldHash, String fieldName)
  {
    return fieldHash.get(XMLUtils.XMLDecode(fieldName));
  }

  /**
   * This helper method takes a short object type id and an
   * underscore-for-space XML encoded field name and returns the
   * FieldTemplate for that field, if known.  If not, null is
   * returned.
   */

  public FieldTemplate getFieldTemplate(short type, String fieldName)
  {
    return getFieldTemplate(Short.valueOf(type), fieldName);
  }

  /**
   * This helper method takes a short object type id and an
   * underscore-for-space XML encoded field name and returns the
   * FieldTemplate for that field, if known.  If not, null is
   * returned.
   */

  public FieldTemplate getFieldTemplate(Short type, String fieldName)
  {
    Hashtable<String, FieldTemplate> fieldHash = objectTypeIDs.get(type);

    if (fieldHash == null)
      {
        return null;
      }

    return fieldHash.get(XMLUtils.XMLDecode(fieldName));
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

  public boolean haveSeenInvid(Invid paramInvid)
  {
    return this.duplications.contains(paramInvid);
  }

  public void rememberSeenInvid(Invid paramInvid)
  {
    this.duplications.add(paramInvid);
  }

  public void rememberEmbeddedObject(xmlobject object)
  {
    this.embeddedObjects.addElement(object);
  }

  /**
   * This method actually does the work of integrating our data into the
   * DBStore.
   *
   * @return true if the data was successfully integrated to the server and
   * the transaction committed successfully, false if the transaction
   * had problems and was abandoned.
   */

  private boolean integrateXMLTransaction() throws NotLoggedInException
  {
    ReturnVal retVal;
    HashMap<String, Integer> editCount = new HashMap<String, Integer>();
    HashMap<String, Integer> createCount = new HashMap<String, Integer>();
    HashMap<String, Integer> deleteCount = new HashMap<String, Integer>();
    HashMap<String, Integer> inactivateCount = new HashMap<String, Integer>();

    /* -- */

    if (cleanedup.isSet())
      {
        return false;
      }

    retVal = session.openTransaction("xmlclient", false); // non-interactive

    if (!ReturnVal.didSucceed(retVal))
      {
        if (retVal.getDialog() != null)
          {
            // "GanymedeXMLSession Error: couldn''t open transaction {0}: {1}"
            err.println(ts.l("integrateXMLTransaction.failed_open_msg", session.getSessionName(), retVal.getDialog().getText()));
          }
        else
          {
            // "GanymedeXMLSession Error: couldn''t open transaction {0}."
            err.println(ts.l("integrateXMLTransaction.failed_open_no_msg", session.getSessionName()));
          }

        return false;
      }

    session.enableWizards(false); // we're not interactive, don't give us no wizards

    // we first need to try to resolve all objects in our various
    // queues to find their invids.  if we find ones that don't match
    // with objects pre-existing on the server, but do match other
    // <object> elements in the file, we'll provisionally link them to
    // the xmlobject representing the object in question.

    knitInvidReferences();

    try
      {
        for (xmlobject newObject: createdObjects)
          {
            boolean newlyCreated = false;

            // if the object has enough information that we can look it up
            // on the server (and get an Invid for it), assume that it
            // already exists and go ahead and pull it for editing rather
            // than creating it, unless the forceCreate flag is on.

            if (newObject.forceCreate || newObject.getInvid() == null)
              {
                incCount(createCount, newObject.typeString);

                if (debug)
                  {
                    System.err.println("Creating " + newObject);
                  }

                newlyCreated = true;

                retVal = newObject.createOnServer(session);

                if (!ReturnVal.didSucceed(retVal))
                  {
                    String msg = retVal.getDialogText();

                    if (msg != null)
                      {
                        // "GanymedeXMLSession Error creating object {0}:\n{1}"
                        throw new XMLIntegrationException(ts.l("integrateXMLTransaction.creating_error_msg", newObject, msg));
                      }
                    else
                      {
                        // "GanymedeXMLSession Error detected creating object {0}, but no specific error message was generated."
                        throw new XMLIntegrationException(ts.l("integrateXMLTransaction.creating_error_no_msg", newObject));
                      }
                  }
              }
            else
              {
                incCount(editCount, newObject.typeString);

                if (debug)
                  {
                    System.err.println("Editing pre-existing " + newObject);
                  }

                retVal = newObject.editOnServer(session);

                if (!ReturnVal.didSucceed(retVal))
                  {
                    String msg = retVal.getDialogText();

                    if (msg != null)
                      {
                        // "GanymedeXMLSession Error editing object {0}:\n{1}"
                        throw new XMLIntegrationException(ts.l("integrateXMLTransaction.editing_error_msg", newObject, msg));
                      }
                    else
                      {
                        // "GanymedeXMLSession Error detected editing object {0}, but no specific error message was generated."
                        throw new XMLIntegrationException(ts.l("integrateXMLTransaction.editing_error_no_msg", newObject));
                      }
                  }
              }

            // we can't be sure that we can register invid fields
            // until all objects that we need to create are
            // created.. for now, just register non-invid fields

            retVal = newObject.registerFields(0); // everything but invids

            if (!ReturnVal.didSucceed(retVal))
              {
                String msg = retVal.getDialogText();

                if (msg != null)
                  {
                    if (newlyCreated)
                      {
                        // "[1] Error registering fields for newly created object {0}:\n{1}"
                        throw new XMLIntegrationException(ts.l("integrateXMLTransaction.error_new_registering", newObject, msg));
                      }
                    else
                      {
                        // "[1] Error registering fields for edited object {0}:\n{1}"
                        throw new XMLIntegrationException(ts.l("integrateXMLTransaction.error_old_registering", newObject, msg));
                      }
                  }
                else
                  {
                    if (newlyCreated)
                      {
                        // "[1] Error detected registering fields for newly created object {0}."
                        throw new XMLIntegrationException(ts.l("integrateXMLTransaction.error_new_registering_no_msg", newObject));
                      }
                    else
                      {
                        // "[1] Error detected registering fields for edited object {0}."
                        throw new XMLIntegrationException(ts.l("integrateXMLTransaction.error_old_registering_no_msg", newObject));
                      }
                  }
              }
          }

        // the created (or possibly) created objects are created and/or
        // edited, and their non-invid fields are fixed up.  we need to do
        // the same for definitely edited objects

        for (xmlobject object: editedObjects)
          {
            incCount(editCount, object.typeString);

            retVal = object.editOnServer(session);

            if (!ReturnVal.didSucceed(retVal))
              {
                String msg = retVal.getDialogText();

                if (msg != null)
                  {
                    // "GanymedeXMLSession Error editing object {0}:\n{1}"
                    throw new XMLIntegrationException(ts.l("integrateXMLTransaction.editing_error_msg", object, msg));
                  }
                else
                  {
                    // "GanymedeXMLSession Error detected editing object {0}, but no specific error message was generated."
                    throw new XMLIntegrationException(ts.l("integrateXMLTransaction.editing_error_no_msg", object));
                  }
              }

            retVal = object.registerFields(0); // everything but non-embedded invid fields

            if (!ReturnVal.didSucceed(retVal))
              {
                String msg = retVal.getDialogText();

                if (msg != null)
                  {
                    // "[{0,number,#}] Error registering fields for {1}:\n{2}"
                    throw new XMLIntegrationException(ts.l("integrateXMLTransaction.error_registering", Integer.valueOf(2), object, msg));
                  }
                else
                  {
                    // "[{0,number,#}] Error detected registering fields for {1}."
                    throw new XMLIntegrationException(ts.l("integrateXMLTransaction.error_registering_no_msg", Integer.valueOf(2), object));
                  }
              }
          }

        // at this point, all objects we need to create are created,
        // and any non-invid fields in those new objects have been
        // registered.  We now need to register any invid fields in
        // the newly created objects, which should be able to resolve
        // now.

        for (xmlobject newObject: createdObjects)
          {
            retVal = newObject.registerFields(1); // just invids

            if (!ReturnVal.didSucceed(retVal))
              {
                String msg = retVal.getDialogText();

                if (msg != null)
                  {
                    // "[{0,number,#}] Error registering fields for {1}:\n{2}"
                    throw new XMLIntegrationException(ts.l("integrateXMLTransaction.error_registering", Integer.valueOf(3), newObject, msg));
                  }
                else
                  {
                    // "[{0,number,#}] Error detected registering fields for {1}."
                    throw new XMLIntegrationException(ts.l("integrateXMLTransaction.error_registering_no_msg", Integer.valueOf(3), newObject));
                  }
              }
          }

        // now we need to register fields in the edited objects

        for (xmlobject object: editedObjects)
          {
            retVal = object.registerFields(1); // just invids, everything else we already did

            if (!ReturnVal.didSucceed(retVal))
              {
                String msg = retVal.getDialogText();

                if (msg != null)
                  {
                    // "[{0,number,#}] Error registering fields for {1}:\n{2}"
                    throw new XMLIntegrationException(ts.l("integrateXMLTransaction.error_registering", Integer.valueOf(4), object, msg));
                  }
                else
                  {
                    // "[{0,number,#}] Error detected registering fields for {1}."
                    throw new XMLIntegrationException(ts.l("integrateXMLTransaction.error_registering_no_msg", Integer.valueOf(4), object));
                  }
              }
          }

        // finally we need to do the same for the objects we checked out
        // or created when handling embedded objects

        for (xmlobject object: embeddedObjects)
          {
            retVal = object.registerFields(1); // only non-embedded invids

            if (!ReturnVal.didSucceed(retVal))
              {
                String msg = retVal.getDialogText();

                if (msg != null)
                  {
                    // "[{0,number,#}] Error registering fields for {1}:\n{2}"
                    throw new XMLIntegrationException(ts.l("integrateXMLTransaction.error_registering", Integer.valueOf(5), object, msg));
                  }
                else
                  {
                    // "[{0,number,#}] Error detected registering fields for {1}."
                    throw new XMLIntegrationException(ts.l("integrateXMLTransaction.error_registering_no_msg", Integer.valueOf(5), object));
                  }
              }
          }

        // now we need to inactivate any objects to be inactivated

        for (xmlobject object: inactivatedObjects)
          {
            incCount(inactivateCount, object.typeString);

            Invid target = object.getInvid();

            if (target == null)
              {
                // "Error, couldn''t find Invid for object to be inactivated: {0}"
                throw new XMLIntegrationException(ts.l("integrateXMLTransaction.what_invid_to_inactivate", object));
              }

            retVal = session.inactivate_db_object(target);

            if (!ReturnVal.didSucceed(retVal))
              {
                String msg = retVal.getDialogText();

                if (msg != null)
                  {
                    // "Error inactivating {0}:\n{1}"
                    throw new XMLIntegrationException(ts.l("integrateXMLTransaction.bad_inactivation", object, msg));
                  }
                else
                  {
                    // "Error detected inactivating {0}."
                    throw new XMLIntegrationException(ts.l("integrateXMLTransaction.bad_inactivation_no_msg", object));
                  }
              }
          }

        // and we need to delete any objects to be deleted

        for (xmlobject object: deletedObjects)
          {
            Invid target = object.getInvid();

            if (target == null)
              {
                // "Error, couldn''t find Invid for object to be deleted: {0}"
                err.println(ts.l("integrateXMLTransaction.what_invid_to_delete", object));

                continue;
              }

            incCount(deleteCount, object.typeString);

            retVal = session.remove_db_object(target);

            if (!ReturnVal.didSucceed(retVal))
              {
                String msg = retVal.getDialogText();

                if (msg != null)
                  {
                    // "Error deleting {0}:\n{1}"
                    throw new XMLIntegrationException(ts.l("integrateXMLTransaction.bad_deletion", object, msg));
                  }
                else
                  {
                    // "Error detected deleting {0}."
                    throw new XMLIntegrationException(ts.l("integrateXMLTransaction.bad_deletion_no_msg", object));
                  }
              }
          }

        // "Committing transaction."
        err.println(ts.l("integrateXMLTransaction.committing"));
        err.println();

        retVal = session.commitTransaction(true, this.comment);

        if (!ReturnVal.didSucceed(retVal))
          {
            String msg = retVal.getDialogText();

            if (msg != null)
              {
                // "Error, could not successfully commit this XML data transaction:\n{0}"
                throw new XMLIntegrationException(ts.l("integrateXMLTransaction.commit_error", msg));
              }
            else
              {
                // "Error detected committing XML data transaction."
                throw new XMLIntegrationException(ts.l("integrateXMLTransaction.commit_error_no_msg"));
              }
          }

        if (createCount.size() > 0)
          {
            // "Objects created:"
            err.println(ts.l("integrateXMLTransaction.objects_created"));

            for (Map.Entry<String, Integer> item: createCount.entrySet())
              {
                // "\t{0}: {1,number,#}"
                err.println(ts.l("integrateXMLTransaction.object_count", item.getKey(), item.getValue()));
              }
          }

        if (editCount.size() > 0)
          {
            // "Objects edited:"
            err.println(ts.l("integrateXMLTransaction.objects_edited"));

            for (Map.Entry<String, Integer> item: editCount.entrySet())
              {
                // "\t{0}: {1,number,#}"
                err.println(ts.l("integrateXMLTransaction.object_count", item.getKey(), item.getValue()));
              }
          }

        if (deleteCount.size() > 0)
          {
            // "Objects deleted:"
            err.println(ts.l("integrateXMLTransaction.objects_deleted"));

            for (Map.Entry<String, Integer> item: deleteCount.entrySet())
              {
                // "\t{0}: {1,number,#}"
                err.println(ts.l("integrateXMLTransaction.object_count", item.getKey(), item.getValue()));
              }
          }

        if (inactivateCount.size() > 0)
          {
            // "Objects inactivated:"
            err.println(ts.l("integrateXMLTransaction.objects_inactivated"));

            for (Map.Entry<String, Integer> item: inactivateCount.entrySet())
              {
                // "\t{0}: {1,number,#}"
                err.println(ts.l("integrateXMLTransaction.object_count", item.getKey(), item.getValue()));
              }
          }

        // "Transaction successfully committed."
        err.println(ts.l("integrateXMLTransaction.thrill_of_victory"));

        return true;
      }
    catch (Exception ex)
      {
        if (!(ex instanceof XMLIntegrationException))
          {
            err.println(ex.getStackTrace());
          }

        err.println(ex.getMessage());

        // "Errors encountered, aborting transaction. "
        err.println(ts.l("integrateXMLTransaction.agony_of_defeat"));

        return false;
      }
  }

  /**
   * <p>This private helper method is responsible for working through
   * the objectStore hash and dereferencing any xInvids contained
   * therein to objects in the XML file and/or server, before any
   * actual edits are performed.</p>
   *
   * <p>This is necessary so that we can deal with the possibility of
   * objects being renamed before we have all of our invid field
   * updates made.  By looking up all Invids that we can before we
   * start editing anything (but after we've done a storeObject() on
   * all objects that we are editing or creating), we can be sure that
   * we will resolve Invid references in xInvid objects to the proper,
   * pre-rename object labels.</p>
   */

  private void knitInvidReferences() throws NotLoggedInException
  {
    for (Map.Entry<Short, Hashtable> entry: objectStore.entrySet())
      {
        Short type = entry.getKey();
        Hashtable<Object, Object> objectHash = entry.getValue();

        for (Map.Entry<Object, Object> innerEntry: objectHash.entrySet())
          {
            Object key = innerEntry.getKey();
            Object thing = innerEntry.getValue();

            if (thing instanceof xmlobject)
              {
                xmlobject storedObject = (xmlobject) thing;

                // Let's try to get the invid for this storedObject,
                // as it exists before we might possibly do any
                // renaming.  The call to getInvid() on the xmlobject
                // may involve a lookup in the server's persistent
                // data store if we haven't previously resolved it.

                Invid invid = storedObject.getInvid();

                if (invid == null)
                  {
                    if (storedObject.getMode() == null || storedObject.getMode().equals("create"))
                      {
                        // we may need to create this object, so we'll
                        // clear the knownNonExistent flag.

                        storedObject.knownNonExistent = false;
                      }
                    else
                      {
                        // "Error, could not look up pre-existing {0} object with label {1}.  Did you mean to use the create action?"
                        throw new RuntimeException(ts.l("knitInvidReferences.no_such_object",
                                                        getTypeName(type.shortValue()), key));
                      }
                  }
              }
          }
      }

    // now that we have forced the lookup and resolution of all
    // labeled objects, we need to go through all objects that we've
    // seen reference to and try to look up all <invid> elements
    // contained therein.  <invid> elements that point to objects we
    // have just looked up above will be able to dereference those
    // Invids by looking in this very objectStore hashing structure,
    // even for objects that we have labeled but not yet created on
    // the server.

    for (Map.Entry<Short, Hashtable> entry: objectStore.entrySet())
      {
        Short type = entry.getKey();
        Hashtable objectHash = entry.getValue();

        for (Object thing: objectHash.values())
          {
            if (thing instanceof xmlobject)
              {
                xmlobject storedObject = (xmlobject) thing;

                // an xmlobject to be deleted may not actually have
                // any fields stored in it

                if (storedObject.fields != null)
                  {
                    // now go through the stored object and do lookups for
                    // any invid fields contained thereunder.

                    Enumeration fieldEnum = storedObject.fields.elements();

                    while (fieldEnum.hasMoreElements())
                      {
                        xmlfield field = (xmlfield) fieldEnum.nextElement();

                        if (field.getType() == FieldType.INVID && !field.fieldDef.isEditInPlace())
                          {
                            field.dereferenceInvids();
                          }
                      }
                  }
              }
          }
      }
  }

  /**
   * this private helper method increments a counting
   * integer in table, keyed by type.
   */

  private void incCount(HashMap<String, Integer> table, String type)
  {
    Integer x = table.get(type);

    if (x == null)
      {
        table.put(type, Integer.valueOf(1));
      }
    else
      {
        table.put(type, Integer.valueOf(x.intValue() + 1));
      }
  }

  /**
   * This private helper method creates a ReturnVal object to be
   * passed back to the xmlclient.  Any text printed to the err
   * object will be included in the ReturnVal object, followed by
   * the content of message, if any.  If success is true, the
   * ReturnVal returned will encode that.  If success is false,
   * the returned ReturnVal will indicate failure.
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
            return null;        // success, nothing to report
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
            return null;        // someone else is logged in, can't do it
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
            Ganymede.logError(ex);
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
   * Private helper method to print to the client the text of
   * any return val dialog.  Returns true if the retval codes
   * for success, false otherwise.
   */

  private boolean handleReturnVal(ReturnVal retval)
  {
    if (retval != null && retval.getDialogText() != null)
      {
        err.println(retval.getDialogText());
      }

    if (ReturnVal.didSucceed(retval))
      {
        return true;
      }

    return false;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                         XMLIntegrationException

------------------------------------------------------------------------------*/

/**
 * An internal exception used for flow control in
 * GanymedeXMLSession.integrateXMLTransaction().
 */

class XMLIntegrationException extends RuntimeException {

  public XMLIntegrationException()
  {
    super();
  }

  public XMLIntegrationException(String message)
  {
    super(message);
  }
}

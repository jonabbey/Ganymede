/*
   GASH 2

   DBStore.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Release: $Name:  $
   Version: $Revision: 1.146 $
   Last Mod Date: $Date: 2001/08/18 06:16:27 $
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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.text.*;
import java.rmi.*;

import com.jclark.xml.output.*;
import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                         DBStore

------------------------------------------------------------------------------*/

/**
 * <p>DBStore is the main Ganymede database class. DBStore is responsible for
 * actually handling the Ganymede database, and manages
 * database loads and dumps, locking (in conjunction with 
 * {@link arlut.csd.ganymede.DBSession DBSession} and
 * {@link arlut.csd.ganymede.DBLock DBLock}), 
 * the {@link arlut.csd.ganymede.DBJournal journal}, and schema dumping.</p>
 *
 * <P>The DBStore class holds the server's namespace and schema
 * dictionaries, in the form of a collection of
 * {@link arlut.csd.ganymede.DBNameSpace DBNameSpace} and {@link
 * arlut.csd.ganymede.DBObjectBase DBObjectBase} objects.  Each
 * DBObjectBase contains schema information for an object type,
 * including field definitions for all fields that may be stored in
 * objects of that type.</P>
 *
 * <p>In addition to holding schema information, each DBObjectBase
 * contains an {@link arlut.csd.ganymede.Invid Invid}-keyed hash of
 * {@link arlut.csd.ganymede.DBObject DBObject}'s of that type in
 * memory after the database loading is complete at start-up.  Changes
 * made to the DBStore are done in {@link arlut.csd.ganymede.DBEditSet
 * transactional contexts} using DBSession, which is responsible for
 * initiating journal changes when individual transactions are
 * committed to the database.  Periodically, the Ganymede server's
 * {@link arlut.csd.ganymede.GanymedeScheduler GanymedeScheduler} task
 * engine will schedule a full {@link
 * arlut.csd.ganymede.DBStore#dump(java.lang.String,boolean,boolean)
 * dump} to consolidate the journal and update the on-disk database
 * file.  The server will also do a dump when the server's admin
 * console {@link arlut.csd.ganymede.GanymedeAdmin GanymedeAdmin}
 * interface initiates a server shutdown.</p>
 *
 * <p>DBStore was originally written with the intent of being able to serve as
 * a stand-alone in-process transactional object storage system, but in practice, DBStore
 * is now thoroughly integrated with the rest of the Ganymede server, and particularly
 * with {@link arlut.csd.ganymede.GanymedeSession GanymedeSession}.  Various
 * component classes ({@link arlut.csd.ganymede.DBSession DBSession},
 * {@link arlut.csd.ganymede.DBObject DBObject}, and
 * {@link arlut.csd.ganymede.DBField DBField}), assume that there is usually
 * an associated GanymedeSession to be consulted for permissions and the like.</P>
 *
 * @version $Revision: 1.146 $ %D%
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT 
 */

public final class DBStore {

  // type identifiers used in the object store

  /**
   * All ganymede.db and ganymede.schema files will start with this string
   */

  static final String id_string = "Gstore";

  /**
   * Major file version id.. will be first byte in ganymede.db/ganymede.schema
   * after id_string
   */

  static final byte major_version = 2;

  /**
   * Minor file version id.. will be second byte in ganymede.db/ganymede.schema
   * after id_string
   */

  static final byte minor_version = 2;

  /**
   * XML version major id
   */

  static final byte major_xml_version = 1;

  /**
   * XML version minor id
   */

  static final byte minor_xml_version = 0;

  /**
   * Enable/disable debug in the DBStore methods
   */

  static boolean debug = false;

  /**
   * We're going to just have a singleton
   */

  static DBStore db = null;

  /**
   * <p>Convenience function to find and return objects from the database
   * without having to go through the GanymedeSession and DBSession layers.</p>
   *
   * <p>This method provides a direct reference to an object in the DBStore,
   * without exporting it for remote RMI reference and without doing any
   * permissions setting.</p>
   */

  public static DBObject viewDBObject(Invid invid)
  {
    if (DBStore.db == null)
      {
	throw new IllegalStateException("DBStore not initialized");
      }

    DBObjectBase base;

    base = DBStore.db.getObjectBase(invid.getType());

    if (base == null)
      {
	return null;
      }

    return base.getObject(invid);
  }

  /**
   * <p>Convenience synchronized function to set the singleton DBStore
   * db member in the DBStore class.</p>
   */

  private static synchronized boolean setDBSingleton(DBStore store)
  {
    if (DBStore.db != null)
      {
	return false;
      }

    DBStore.db = store;

    return true;
  }

  /* --- */

  /*
    All of the following should only be modified/accessed
    in a critical section synchronized on the DBStore object.
   */
  
  /**
   * hash mapping object type to DBObjectBase's
   */
	
  Hashtable objectBases;

  /** 
   * <p>hash mapping {@link arlut.csd.ganymede.Invid Invid}'s to Hashtables
   * of Invid's.  Used to record the set of Invids that point to a
   * given Invid.</p>
   *
   * <p>That is, backPointers.get(anInvid) returns a hashtable whose keys
   * are the Invid's that point to that Invid via an asymmetric link.</p>
   */

  Hashtable backPointers;

  /**
   * <p>Vector of {@link arlut.csd.ganymede.DBSession DBSession} objects.</p>
   */

  private Vector sessions;

  /** 
   * A collection of {@link arlut.csd.ganymede.DBNameSpace
   * DBNameSpaces} registered in this DBStore.  
   */

  Vector nameSpaces;

  /** 
   * if true, {@link arlut.csd.ganymede.DBObjectBase DBObjectBase}
   * set methods will be enabled 
   */

  boolean loading = false;

  /**
   * Root of the category tree defined in this database
   */

  DBBaseCategory rootCategory;

  /**
   * Major revision number of the database file loaded by this
   * DBStore object.
   */

  byte file_major;

  /**
   * Minor revision number of the database file loaded by this
   * DBStore object.
   */

  byte file_minor;

  /**
   * The Journal for this database, initialized when the database is
   * loaded.
   */

  DBJournal journal = null;

  /**
   * A separate object to act as a synchronization monitor for DBLock
   * code.
   */

  public DBLockSync lockSync = new DBLockSync();

  // debugging info

  /**
   * A count of how many database objects in this DBStore are currently
   * checked out for creation, editing, or removal.
   */

  int objectsCheckedOut = 0;

  /* -- */

  /**
   * <p>This is the constructor for DBStore.</p>
   *
   * <p>Currently, once you construct a DBStore object, all you can do to
   * initialize it is call load().  This API needs to be extended to
   * provide for programmatic bootstrapping, or another tool needs
   * to be produced for the purpose.</p>
   */

  public DBStore()
  {
    debug = Ganymede.debug;

    objectBases = new Hashtable(20); // default 
    backPointers = new Hashtable(1000);	// default
    nameSpaces = new Vector();
    sessions = new Vector();

    try
      {
	rootCategory = new DBBaseCategory(this, "Categories");
      }
    catch (RemoteException ex)
      {
	// we shouldn't ever get here unless there is something wrong
	// with the RMI system or the Java environment

	System.err.println("Caught DBBaseCategory ex: " + ex.getMessage());
	ex.printStackTrace();
	throw new Error("couldn't initialize rootCategory");
      }

    // set the static pointer after we have at least created the basic
    // data structures

    if (!setDBSingleton(this))
      {
	throw new IllegalStateException("DBStore already created");
      }

    GanymedeAdmin.setState("Normal Operation");
  }

  /**
   * <p>Load the database from disk.</p>
   *
   * <p>This method loads both the database type definition and database
   * contents from a single disk file.</p>
   *
   * @param filename Name of the database file
   * @see arlut.csd.ganymede.DBJournal
   */

  public void load(String filename)
  {
    load(filename, true);
  }

  /**
   * <p>Load the database from disk.</p>
   *
   * <p>This method loads both the database type definition and database
   * contents from a single disk file.</p>
   *
   * @param filename Name of the database file
   * @param loadJournal if true, process and consolidate the journal
   * on loading.
   * @see arlut.csd.ganymede.DBJournal
   */

  public synchronized void load(String filename, boolean loadJournal)
  {
    FileInputStream inStream = null;
    BufferedInputStream bufStream = null;
    DataInputStream in;

    DBObjectBase tempBase;
    short baseCount, namespaceCount, categoryCount;
    String namespaceID;
    boolean caseInsensitive;
    String file_id;

    /* -- */

    loading = true;

    nameSpaces.removeAllElements();

    try
      {
	inStream = new FileInputStream(filename);
	bufStream = new BufferedInputStream(inStream);
	in = new DataInputStream(bufStream);

	try
	  {
	    file_id = in.readUTF();
	    
	    if (!file_id.equals(id_string))
	      {
		System.err.println("DBStore initialization error: DBStore id mismatch for " + filename);
		throw new RuntimeException("DBStore initialization error (" + filename + ")");
	      }
	  }
	catch (IOException ex)
	  {
 	    System.err.println("DBStore initialization error: DBStore id read failure for " + filename);
	    System.err.println("IOException: ");
	    ex.printStackTrace();

	    throw new RuntimeException("DBStore initialization error (" + filename + ")");
	  }

	file_major = in.readByte();
	file_minor = in.readByte();

	if (debug)
	  {
	    System.err.println("DBStore load(): file version " + file_major + "." + file_minor);
	  }

	if (file_major > major_version)
	  {
	    System.err.println("DBStore initialization error: major version mismatch");
	    throw new Error("DBStore initialization error (" + filename + ")");
	  }

	if (file_major == major_version && file_minor > minor_version)
	  {
	    System.err.println("*** Error, this ganymede.db file is too new for this version of the Ganymede server.");
	    System.err.println("*** There may be errors in loading the data.");
	  }

	// read in the namespace definitions

	namespaceCount = in.readShort();

	if (debug)
	  {
	    System.err.println("DBStore load(): loading " + namespaceCount + " namespaces");
	  }

	for (int i = 0; i < namespaceCount; i++)
	  {
	    nameSpaces.addElement(new DBNameSpace(in));
	  }

	// read in the object categories

	if (debug)
	  {
	    System.err.println("DBStore load(): loading  category definitions");
	  }

	if (file_major > 1 || (file_major == 1 && file_minor >= 3))
	  {
	    rootCategory = new DBBaseCategory(this, in);
	  }
	
	// previous to 2.0, we wrote out the DBObjectBase structures
	// to ganymede.db as a separate step from the category loading

	if (file_major == 1)
	  {
	    baseCount = in.readShort();

	    if (debug)
	      {
		System.err.println("DBStore load(): loading " + baseCount + " bases");
	      }

	    if (baseCount > 0)
	      {
		objectBases = new Hashtable(baseCount);
	      }
	    else
	      {
		objectBases = new Hashtable();	
	      }

	    // Actually read in the object bases
	
	    for (short i = 0; i < baseCount; i++)
	      {
		tempBase = new DBObjectBase(in, this);
	    
		setBase(tempBase);

		if (debug)
		  {
		    System.err.println("loaded base " + tempBase.getTypeID());
		  }
	      }

	    // we're only using the resort() method because we're
	    // handling an old file that might not have the categories
	    // in display order.  This code is intended to fall out of
	    // use.

	    rootCategory.resort();
	  }
      }
    catch (IOException ex)
      {
	System.err.println("DBStore initialization error: couldn't properly process " + filename);
	System.err.println("IOException: " + ex);
	System.err.println("Stack Trace: " + Ganymede.stackTrace(ex));
	throw new RuntimeException("DBStore initialization error (" + filename + ")");
      }
    finally
      {
	if (bufStream != null)
	  {
	    try
	      {
		bufStream.close();
	      }
	    catch (IOException ex)
	      {
	      }
	  }

	if (inStream != null)
	  {
	    try
	      {
		inStream.close();
	      }
	    catch (IOException ex)
	      {
	      }
	  }
      }

    if (loadJournal)
      {
	try 
	  {
	    journal = new DBJournal(this, Ganymede.journalProperty);
	  }
	catch (IOException ex)
	  {
	    // what do we really want to do here?

	    ex.printStackTrace();

	    throw new RuntimeException("couldn't initialize journal:" + ex.getMessage());
	  }

	if (!journal.isClean())
	  {
	    try
	      {
		if (!journal.load())
		  {
		    // if the journal wasn't in a totally consistent
		    // state, print out a warning.  we'll still
		    // continue to do everything we normally would,
		    // however.

		    System.err.println("\nError, couldn't load entire journal.. " +
				       "final transaction in journal not processed.\n");
		  }

		// update the DBObjectBase iterationSets, since the journal
		// loading bypasses the transaction mechanism where this is
		// normally done

		Enumeration enum = objectBases.elements();

		while (enum.hasMoreElements())
		  {
		    DBObjectBase base = (DBObjectBase) enum.nextElement();

		    base.updateIterationSet();
		  }

		// go ahead and consolidate the journal into the DBStore
		// before we really get under way.

		// Notice that we are going to archive a copy of the
		// existing db file each time we start up the server
		
		if (!journal.isClean())
		  {
		    dump(filename, true, true);
		  }
	      }
	    catch (IOException ex)
	      {
		// what do we really want to do here?

		throw new RuntimeException("couldn't load journal");
	      }
	  }
      }

    loading = false;
  }

  /**
   * <p>Dumps the database to disk</p>
   *
   * <p>This method dumps the entire database to disk.  The thread that calls the
   * dump method will be suspended until there are no threads performing update
   * writes to the in-memory database.  In practice this will likely never be
   * a long interval.  Note that this method *will* dump the database, even
   * if no changes have been made.  You should check the DBStore journal's 
   * isClean() method to determine whether or not a dump is really needed,
   * if you're not sure.</p>
   *
   * <p>The dump is guaranteed to be transaction consistent.</p>
   *
   * @param filename Name of the database file to emit
   * @param releaseLock boolean.  If releaseLock==false, dump() will not release
   *                              the dump lock when it is done with the dump.  This
   *                              is intended to allow for a clean shut down.  For
   *                              non-terminal dumps, releaseLock should be true.
   * @param archiveIt If true, dump will create a zipped copy of the previously existing
   *                  ganymede.db file in an 'old' directory under the location where
   *                  ganymede.db is held.
   *
   * @see arlut.csd.ganymede.DBEditSet
   * @see arlut.csd.ganymede.DBJournal
   */

  public synchronized void dump(String filename, boolean releaseLock,
				boolean archiveIt) throws IOException
  {
    File dbFile = null;
    FileOutputStream outStream = null;
    BufferedOutputStream bufStream = null;
    DataOutputStream out = null;
    
    FileOutputStream textOutStream = null;
    PrintWriter textOut = null;
    
    short baseCount, namespaceCount, categoryCount;
    DBDumpLock lock = null;
    DBNameSpace ns;
    DBBaseCategory bc;
    
    /* -- */
    
    if (debug)
      {
	System.err.println("DBStore: Dumping");
      }

    lock = new DBDumpLock(this);

    if (debug)
      {
	System.err.println("DBStore: establishing dump lock");
      }

    try
      {
	lock.establish("System");	// wait until we get our lock 
      }
    catch (InterruptedException ex)
      {
      }

    if (debug)
      {
	System.err.println("DBStore: dump lock established");
      }
    
    // Move the old version of the file to a backup
    
    try
      {
	dbFile = new File(filename);

	// first thing we do is zip up the old ganymede.db file if
	// archiveIt is true.

	if (dbFile.exists())
	  {
	    if (archiveIt)
	      {
		if (debug)
		  {
		    System.err.println("DBStore: zipping old db");
		  }

		String directoryName = dbFile.getParent();

		File directory = new File(directoryName);

		if (!directory.isDirectory())
		  {
		    throw new IOException("Error, couldn't find output directory to backup: " + 
					  directoryName);
		  }

		File oldDirectory = new File(directoryName + File.separator + "old");
		    
		if (!oldDirectory.exists())
		  {
		    oldDirectory.mkdir();
		  }

		DateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", 
							    java.util.Locale.US);
		String label = formatter.format(new Date());

		String zipFileName = directoryName + File.separator + "old" + File.separator +
		  label + "db.zip";

		Vector fileNameVect = new Vector();
		fileNameVect.addElement(filename);

		zipIt.createZipFile(zipFileName, fileNameVect);
	      }
	  }

	// and dump the whole thing to ganymede.db.new

	if (debug)
	  {
	    System.err.println("DBStore: writing new db");
	  }

	outStream = new FileOutputStream(filename + ".new");
	bufStream = new BufferedOutputStream(outStream);
	out = new DataOutputStream(bufStream);

	// okay, then!  let's get started!

	out.writeUTF(id_string);
	out.writeByte(major_version);
	out.writeByte(minor_version);

	namespaceCount = (short) nameSpaces.size();

	out.writeShort(namespaceCount);

	for (int i = 0; i < namespaceCount; i++)
	  {
	    ns = (DBNameSpace) nameSpaces.elementAt(i);
	    ns.emit(out);
	  }

	rootCategory.emit(out);	// writes out categories and bases

	out.close();
	out = null;

	// ok, we've successfully dumped to ganymede.db.new.. move
	// the old file to ganymede.db.bak

	if (debug)
	  {
	    System.err.println("DBStore: renaming new db");
	  }

	dbFile.renameTo(new File(filename + ".bak"));

	// and move ganymede.db.new to ganymede.db.. note that we do
	// have a very slight vulnerability here if we are interrupted
	// between the above rename and this one.  If this happens,
	// the Ganymede manager will have to manually move
	// ganymede.db.new to ganymede.db before starting the server.

	// In all other circumstances, the server will be able to come
	// up and handle things cleanly without loss of data.

	new File(filename + ".new").renameTo(dbFile);

	// and dump the schema out in a human readable form.. note
	// that this is far less critical than all of the above,
	// so if we get an exception above we won't worry about
	// completing this part.
	
	if (Ganymede.htmlProperty != null)
	  {
	    textOutStream = new FileOutputStream(Ganymede.htmlProperty);
	    textOut = new PrintWriter(textOutStream);
		
	    printCategoryTreeHTML(textOut);
	  }
      }
    catch (IOException ex)
      {
	System.err.println("DBStore error dumping to " + filename);
	
	throw ex;
      }
    finally
      {
	if (releaseLock)
	  {
	    if (lock != null)
	      {
		if (debug)
		  {
		    System.err.println("DBStore: releasing dump lock");
		  }

		lock.release();
	      }
	  }

	if (out != null)
	  {
	    out.close();
	  }

	if (bufStream != null)
	  {
	    bufStream.close();
	  }
	   
	if (outStream != null)
	  {
	    outStream.close();
	  }

	if (textOut != null)
	  {
	    textOut.close();
	  }

	if (textOutStream != null)
	  {
	    textOutStream.close();
	  }
      }

    // if our thread was terminated above, we won't get here.  If
    // we've got here, we had an ok dump, and we don't need to
    // worry about the journal.. if it isn't truncated properly
    // somebody can just remove it and ganymede will recover
    // ok.
    
    if (journal != null)
      {
	journal.reset();
      }
    
    GanymedeAdmin.updateLastDump(new Date());

    if (Ganymede.log != null)
      {
	Ganymede.log.logSystemEvent(new DBLogEvent("dump",
						   "Database dumped",
						   null,
						   null,
						   null,
						   null));
      }
  }

  /**
   * <p>Dumps the database to disk as an XML file</p>
   *
   * <p>This method dumps the entire database to disk.  The thread that calls the
   * dump method will be suspended until there are no threads performing update
   * writes to the in-memory database.  In practice this will likely never be
   * a long interval.  Note that this method *will* dump the database, even
   * if no changes have been made.  You should check the DBStore journal's 
   * isClean() method to determine whether or not a dump is really needed,
   * if you're not sure.</p>
   *
   * <p>The dump is guaranteed to be transaction consistent.</p>
   *
   * @param filename Name of the database file to emit
   *
   * @param outStream Stream to write the XML to @param
   * dumpDataObjects if false, only the schema definition will be
   * written
   *
   * @param dumpDataObjects If true, the emitted file will include
   * the objects in the Ganymede database.
   *
   * @param dumpSchema If true, the emitted file will include the
   * schema definition
   *
   * @see arlut.csd.ganymede.DBEditSet
   * @see arlut.csd.ganymede.DBJournal
   */

  public void dumpXML(String filename, boolean dumpDataObjects, boolean dumpSchema) throws IOException
  {
    FileOutputStream outStream = null;
    BufferedOutputStream bufStream = null;

    /* -- */

    outStream = new FileOutputStream(filename);
    bufStream = new BufferedOutputStream(outStream);

    this.dumpXML(bufStream, dumpDataObjects, dumpSchema);
  }

  /**
   * <p>Dumps the database and/or database schema to an OutputStream
   * as an XML file</p>
   *
   * <p>This method dumps the entire database to the OutputStream.
   * The thread that calls the dump method will be suspended until
   * there are no threads performing update writes to the in-memory
   * database.  In practice this will likely never be a long interval.
   * Note that this method *will* dump the database, even if no
   * changes have been made.  You should check the DBStore journal's
   * isClean() method to determine whether or not a dump is really
   * needed, if you're not sure.</p>
   *
   * <p>The dump is guaranteed to be transaction consistent.</p>
   *
   * @param outStream Stream to write the XML to @param
   * dumpDataObjects if false, only the schema definition will be
   * written
   *
   * @param dumpDataObjects If true, the emitted file will include
   * the objects in the Ganymede database.
   *
   * @param dumpSchema If true, the emitted file will include the
   * schema definition
   *
   * @see arlut.csd.ganymede.DBEditSet
   * @see arlut.csd.ganymede.DBJournal
   */

  public synchronized void dumpXML(OutputStream outStream, boolean dumpDataObjects, boolean dumpSchema) throws IOException
  {
    XMLDumpContext xmlOut = null;
    
    Enumeration basesEnum;
    DBDumpLock lock = null;
    DBNameSpace ns;
    DBBaseCategory bc;
    
    /* -- */
    
    if (debug)
      {
	System.err.println("DBStore: Dumping XML");
      }

    if (!dumpDataObjects && !dumpSchema)
      {
	throw new IllegalArgumentException("not dumping data objects, not dumping schema.. what should i do?");
      }

    lock = new DBDumpLock(this);

    try
      {
	lock.establish("System");	// wait until we get our lock 
      }
    catch (InterruptedException ex)
      {
      }

    if (debug)
      {
	System.err.println("DBStore.dumpXML(): got dump lock");
      }

    try
      {
	xmlOut = new XMLDumpContext(new UTF8XMLWriter(outStream, UTF8XMLWriter.MINIMIZE_EMPTY_ELEMENTS),
				    false, // don't dump plaintext passwords needlessly
				    false); // don't include creator/modifier data

	if (debug)
	  {
	    System.err.println("DBStore.dumpXML(): created XMLDumpContext");
	  }

	// start writing

	if (dumpDataObjects)
	  {
	    xmlOut.startElement("ganymede");
	    xmlOut.attribute("major", Byte.toString(major_xml_version));
	    xmlOut.attribute("minor", Byte.toString(minor_xml_version));
	  }

	if (dumpSchema)
	  {
	    if (dumpDataObjects)
	      {
		xmlOut.indentOut();
	      }

	    xmlOut.startElementIndent("ganyschema");

	    xmlOut.indentOut();
	    xmlOut.startElementIndent("namespaces");
	    
	    xmlOut.indentOut();
	    
	    for (int i = 0; i < nameSpaces.size(); i++)
	      {
		ns = (DBNameSpace) nameSpaces.elementAt(i);
		ns.emitXML(xmlOut);
	      }
	    
	    xmlOut.indentIn();
	    xmlOut.endElementIndent("namespaces");
	    
	    // write out our category tree
	    
	    xmlOut.skipLine();
	    
	    xmlOut.startElementIndent("object_type_definitions");
    
	    xmlOut.indentOut();
	    rootCategory.emitXML(xmlOut);
	    
	    xmlOut.indentIn();
	    xmlOut.endElementIndent("object_type_definitions");
    
	    xmlOut.indentIn();
	    xmlOut.endElementIndent("ganyschema");
	  }
    
	if (dumpDataObjects)
	  {
	    if (!dumpSchema)
	      {
		xmlOut.indentOut();
	      }

	    xmlOut.startElementIndent("ganydata");
	    xmlOut.indentOut();

	    Vector bases = getBases();
	    
	    for (int i = 0; i < bases.size(); i++)
	      {
		DBObjectBase base = (DBObjectBase) bases.elementAt(i);

		if (base.isEmbedded())
		  {
		    continue;
		  }

		Enumeration enum = base.objectTable.elements();

		while (enum.hasMoreElements())
		  {
		    DBObject x = (DBObject) enum.nextElement();

		    x.emitXML(xmlOut);
		  }
	      }

	    xmlOut.indentIn();
	    xmlOut.endElementIndent("ganydata");

	    xmlOut.indentIn();
	    xmlOut.endElementIndent("ganymede");
	  }

	xmlOut.write("\n");
	xmlOut.close();
	xmlOut = null;
      }
    finally
      {
	if (debug)
	  {
	    System.err.println("DBStore.dumpXML(): finally!");
	  }

	if (lock != null)
	  {
	    lock.release();
	  }

	if (xmlOut != null)
	  {
	    xmlOut.close();
	  }

	if (outStream != null)
	  {
	    outStream.close();
	  }
      }
  }

  /**
   * <p>Adds a session to our records.</p>
   */

  public synchronized void login(DBSession session)
  {
    this.sessions.addElement(session);
  }

  /**
   * <p>Removes a session from our records.</p>
   */

  public synchronized void logout(DBSession session)
  {
    this.sessions.removeElement(session);
  }

  /** 
   * <p>Dumps an HTML representation of this database's category
   * hierarchy, starting at the root 
   * {@link arlut.csd.ganymede.DBBaseCategory DBBaseCategory}.</p>
   *
   * @param out PrintStream to print to 
   */

  public synchronized void printCategoryTreeHTML(PrintWriter out)
  {
    out.println("<HTML><HEAD><TITLE>Ganymede Schema Dump -- " + new Date() + "</TITLE></HEAD>");
    out.println("<BODY BGCOLOR=\"#FFFFFF\"><H1>Ganymede Schema Dump -- " + new Date() + "</H1>");
    out.println("<HR>");

    rootCategory.printHTML(out);

    out.println("<HR>");
    out.println("Emitted: " + new Date());
    out.println("</BODY>");
  }

  /** 
   * <p>Dumps a text representation of this database's category
   * hierarchy, starting at the root 
   * {@link arlut.csd.ganymede.DBBaseCategory DBBaseCategory}.</p>
   *
   * @param out PrintStream to print to 
   */

  public synchronized void printCategoryTree(PrintWriter out)
  {
    rootCategory.print(out, "");
  }

  /**
   * <p>Do a printable dump of the object databases</p>
   *
   * @param out PrintStream to print to
   */

  public void printBases(PrintWriter out)
  {
    Enumeration enum;

    /* -- */

    synchronized (objectBases)
      {
	enum = objectBases.elements();
	
	while (enum.hasMoreElements())
	  {
	    ((DBObjectBase) enum.nextElement()).print(out, "");
	  }
      }
  }

  /**
   * Returns a vector of Strings, the names of the bases currently
   * defined in this DBStore.
   */

  public Vector getBaseNameList()
  {
    Vector result = new Vector();
    Enumeration enum;

    /* -- */

    synchronized (objectBases)
      {
	enum = objectBases.elements();
	
	while (enum.hasMoreElements())
	  {
	    result.addElement(((DBObjectBase) enum.nextElement()).getName());
	  }
      }
    
    return result;
  }

  /**
   * Returns a vector of {@link arlut.csd.ganymede.DBObjectBase DBObjectBases} currently
   * defined in this DBStore.
   */

  public Vector getBases()
  {
    Vector result = new Vector();
    Enumeration enum;

    /* -- */

    synchronized (objectBases)
      {
	enum = objectBases.elements();
	
	while (enum.hasMoreElements())
	  {
	    result.addElement(enum.nextElement());
	  }
      }
    
    return result;
  }

  /**
   * Returns the object definition class for the id class.
   *
   * @param id Type id for the base to be returned
   */

  public DBObjectBase getObjectBase(Short id)
  {
    return (DBObjectBase) objectBases.get(id);
  }

  /**
   * Returns the object definition class for the id class.
   *
   * @param id Type id for the base to be returned
   */

  public DBObjectBase getObjectBase(short id)
  {
    return (DBObjectBase) objectBases.get(new Short(id));
  }

  /**
   * Returns the object definition class for the id class.
   *
   * @param baseName Name of the base to be returned
   */

  public DBObjectBase getObjectBase(String baseName)
  {
    DBObjectBase base;
    Enumeration enum;

    /* -- */

    synchronized (objectBases)
      {
	enum = objectBases.elements();
	
	while (enum.hasMoreElements())
	  {
	    base = (DBObjectBase) enum.nextElement();
	    
	    if (base.getName().equals(baseName))
	      {
		return base;
	      }
	  }
      }

    return null;
  }

  /**
   * 
   * Method to replace/add a DBObjectBase in the DBStore.
   *
   */

  public synchronized void setBase(DBObjectBase base)
  {
    objectBases.put(base.getKey(), base);
  }

  /**
   *
   * Method to locate a registered namespace by name.
   *
   */

  public DBNameSpace getNameSpace(String name)
  {
    DBNameSpace namespace;

    /* -- */

    synchronized (nameSpaces)
      {
	for (int i = 0; i < nameSpaces.size(); i++)
	  {
	    namespace = (DBNameSpace) nameSpaces.elementAt(i);
	    
	    if (namespace.getName().equals(name))
	      {
		return namespace;
	      }
	  }
      }
	
    return null;
  }

  /**
   *
   * Method to get a category from the category list, by
   * it's full path name.
   *
   */

  public synchronized CategoryNode getCategoryNode(String pathName)
  {
    DBBaseCategory 
      bc;

    int
      tok;

    /* -- */

    if (pathName == null)
      {
	throw new IllegalArgumentException("can't deal with null pathName");
      }

    // System.err.println("DBStore.getCategory(): searching for " + pathName);
    
    StringReader reader = new StringReader(pathName);
    StreamTokenizer tokens = new StreamTokenizer(reader);
    
    tokens.wordChars(Integer.MIN_VALUE, Integer.MAX_VALUE);
    tokens.ordinaryChar('/');
    
    tokens.slashSlashComments(false);
    tokens.slashStarComments(false);
    
    try
      {
	tok = tokens.nextToken();

	bc = rootCategory;

	// The path is going to include the name of the root node
	// itself (unlike in the UNIX filesystem, where the root node
	// has no 'name' of its own), so we need to skip into the
	// root node.

	if (tok == '/')
	  {
	    tok = tokens.nextToken();
	  }

	if (tok == StreamTokenizer.TT_WORD && tokens.sval.equals(rootCategory.getName()))
	  {
	    tok = tokens.nextToken();
	  }

	while (tok != StreamTokenizer.TT_EOF && bc != null)
	  {
	    // note that slashes are the only non-word token we
	    // should ever get, so they are implicitly separators.
	    
	    if (tok == StreamTokenizer.TT_WORD)
	      {
		// System.err.println("DBStore.getCategory(): Looking for node " + tokens.sval);

		CategoryNode cn = bc.getNode(tokens.sval);

		if (cn instanceof DBBaseCategory)
		  {
		    bc = (DBBaseCategory) cn;
		  }
		else if (cn instanceof DBObjectBase)
		  {
		    return cn;
		  }
		else
		  {
		    throw new RuntimeException("Found unknown thing in category tree.." + cn.toString());
		  }

		if (bc == null)
		  {
		    System.err.println("DBStore.getCategory(): found null");
		  }
	      }
	    
	    tok = tokens.nextToken();
	  }
      }
    catch (IOException ex)
      {
	throw new RuntimeException("parse error in getCategory: " + ex);
      }
    
    return bc;
  }

  /**
   * <p>Initialization method for a newly created DBStore.. this
   * method creates a new Schema from scratch, defining the
   * mandatory Ganymede object types, registering their customization
   * classes, defining fields, and all the rest.</p>
   *
   * <p>Note that we don't go through a 
   * {@link arlut.csd.ganymede.DBSchemaEdit DBSchemaEdit}
   * here, we just initialize the {@link arlut.csd.ganymede.DBObjectBase DBObjectBase}/
   * {@link arlut.csd.ganymede.DBObjectBaseField DBObjectBaseField} structures
   * manually.</p>
   */

  void initializeSchema()
  {
    DBObjectBase b;
    DBObjectBaseField bf;
    DBNameSpace ns;

    /* -- */

    loading = true;

    try
      {
	DBBaseCategory adminCategory = new DBBaseCategory(this, "Admin-Level Objects", rootCategory);
	rootCategory.addNodeAfter(adminCategory, null);

	ns = new DBNameSpace("ownerbase", true);
	nameSpaces.addElement(ns);
	
	ns = new DBNameSpace("username", true);
	nameSpaces.addElement(ns);

	ns = new DBNameSpace("access", true);
	nameSpaces.addElement(ns);

	ns = new DBNameSpace("persona", true);
	nameSpaces.addElement(ns);

	ns = new DBNameSpace("eventtoken", true);
	nameSpaces.addElement(ns);

	ns = new DBNameSpace("buildertask", true);
	nameSpaces.addElement(ns);

	DBBaseCategory permCategory = new DBBaseCategory(this, "Permissions", adminCategory);
	adminCategory.addNodeAfter(permCategory, null);

	// create owner base

	b = new DBObjectBase(this, false);
	b.object_name = "Owner Group";
	b.classname = "arlut.csd.ganymede.ownerCustom";
	b.type_code = (short) SchemaConstants.OwnerBase; // 0

	permCategory.addNodeAfter(b, null);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.OwnerNameField;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Name";
	bf.loading = true;
	bf.setNameSpace("ownerbase");
	bf.loading = false;
	bf.comment = "The name of this ownership group";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.OwnerMembersField;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Members";
	bf.array = true;
	bf.allowedTarget = SchemaConstants.PersonaBase;
	bf.targetField = SchemaConstants.PersonaGroupsField;
	bf.comment = "List of admin personae that are members of this owner set";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.OwnerCcAdmins;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Cc All Admins";
	bf.loading = false;
	bf.comment = "If checked, mail to this owner group will be sent to the admins";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.OwnerExternalMail;
	bf.field_type = FieldType.STRING;
	bf.field_name = "External Mail List";
	bf.array = true;
	bf.comment = "What external email addresses should be notified of changes to objects owned?";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.OwnerObjectsOwned;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Objects owned";
	bf.allowedTarget = -2;	// any
	bf.targetField = SchemaConstants.OwnerListField;	// owner list field
	bf.array = true;
	bf.comment = "What objects are owned by this owner set";
	b.addFieldToEnd(bf);

	b.setLabelField(SchemaConstants.OwnerNameField);

	// link in the class we specified

	b.createHook();

	// and link in the base to this DBStore

	setBase(b);

	// create persona base

	b = new DBObjectBase(this, false);
	b.object_name = "Admin Persona";
	b.classname = "arlut.csd.ganymede.adminPersonaCustom";
	b.type_code = (short) SchemaConstants.PersonaBase; // 1

	permCategory.addNodeAfter(b, null); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.PersonaNameField;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Name";
	bf.comment = "Descriptive label for this admin persona";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.PersonaPasswordField;
	bf.field_type = FieldType.PASSWORD;
	bf.field_name = "Password";
	bf.maxLength = 32;
	bf.crypted = true;
	bf.comment = "Persona password";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.PersonaGroupsField;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Owner Sets";
	bf.allowedTarget = SchemaConstants.OwnerBase;	// any
	bf.targetField = SchemaConstants.OwnerMembersField;	// owner list field
	bf.array = true;
	bf.comment = "What owner sets are this persona members of?";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.PersonaAssocUser;
	bf.field_type = FieldType.INVID;
	bf.field_name = "User";
	bf.allowedTarget = SchemaConstants.UserBase;	// any
	bf.targetField = SchemaConstants.UserAdminPersonae;	// owner list field
	bf.array = false;
	bf.comment = "What user is this admin persona associated with?";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.PersonaPrivs;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Privilege Sets";
	bf.allowedTarget = SchemaConstants.RoleBase;
	bf.targetField = SchemaConstants.RolePersonae;
	bf.array = true;
	bf.comment = "What permission matrices are this admin persona associated with?";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.PersonaAdminConsole;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Admin Console";
	bf.array = false;
	bf.comment = "If true, this persona can be used to access the admin console";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.PersonaAdminPower;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Full Console";
	bf.array = false;
	bf.comment = "If true, this persona can kill users and edit the schema";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.PersonaMailAddr;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Email Address";
	bf.array = false;
	bf.comment = "Where email to this administrator should be sent";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.PersonaLabelField;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Label";
	bf.array = false;
	bf.loading = true;
	bf.setNameSpace("persona");
	bf.loading = false;
	b.addFieldToEnd(bf);

	b.setLabelField(SchemaConstants.PersonaLabelField);

	// link in the class we specified

	b.createHook();

	// and link in the base to this DBStore

	setBase(b);

	// create Role base

	b = new DBObjectBase(this, false);
	b.object_name = "Role";
	b.classname = "arlut.csd.ganymede.permCustom";
	b.type_code = (short) SchemaConstants.RoleBase; // 2

	permCategory.addNodeAfter(b, null); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.RoleName;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Name";
	bf.loading = true;
	bf.setNameSpace("access");
	bf.loading = false;
	bf.comment = "The name of this permission matrix";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.RoleDelegatable;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Delegatable Role";
	bf.comment = "If true, this role can be granted to admins created/edited by Personae with this role.";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.RoleMatrix;
	bf.field_type = FieldType.PERMISSIONMATRIX;
	bf.field_name = "Objects Owned Access Bits";
	bf.comment = "Access bits, by object type for objects owned by admins using this permission object";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.RoleDefaultMatrix;
	bf.field_type = FieldType.PERMISSIONMATRIX;
	bf.field_name = "Default Access Bits";
	bf.comment = "Access bits, by object type for all objects on the part of admins using this permission object";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.RolePersonae;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Persona entities";
	bf.allowedTarget = SchemaConstants.PersonaBase;
	bf.targetField = SchemaConstants.PersonaPrivs;
	bf.array = true;
	bf.comment = "What personae are using this permission matrix?";
	b.addFieldToEnd(bf);

	b.setLabelField(SchemaConstants.RoleName);

	// link in the class we specified

	b.createHook();

	// and link in the base to this DBStore

	setBase(b);

	// create System Events base

	DBBaseCategory eventCategory = new DBBaseCategory(this, "Events", adminCategory);
	adminCategory.addNodeAfter(eventCategory, null);

	b = new DBObjectBase(this, false);
	b.object_name = "System Event";
	b.classname = "arlut.csd.ganymede.eventCustom";
	b.type_code = (short) SchemaConstants.EventBase;  

	eventCategory.addNodeAfter(b, null); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.EventToken;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Event Token";
	bf.badChars = " :";
	bf.loading = true;
	bf.setNameSpace("eventtoken");
	bf.loading = false;
	bf.comment = "Single-word token to identify this event type in Ganymede source code";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.EventName;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Event Name";
	bf.badChars = ":";
	bf.comment = "Short name for this event class, suitable for an email message title";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.EventDescription;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Event Description";
	bf.badChars = ":";
	bf.comment = "Fuller description for this event class, suitable for an email message body";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.EventMailBoolean;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Send Mail";
	bf.comment = "If true, occurrences of this event will be emailed";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.EventMailToSelf;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Cc Admin";
	bf.comment = "If true, mail for this event will always be cc'ed to the admin performing the action";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.EventMailOwners;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Cc Owners";
	bf.comment = "If true, mail for this event will always be cc'ed to administrators in the owner group";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.EventExternalMail;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Mail List";
	bf.array = true;
	bf.comment = "List of email addresses to send this event to";
	b.addFieldToEnd(bf);

	b.setLabelField(SchemaConstants.EventToken);
    
	// link in the class we specified

	b.createHook();

	// and link in the base to this DBStore

	setBase(b);

	// create Object Events base

	b = new DBObjectBase(this, false);
	b.object_name = "Object Event";
	b.classname = "arlut.csd.ganymede.objectEventCustom";
	b.type_code = (short) SchemaConstants.ObjectEventBase;  

	eventCategory.addNodeAfter(b, null); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.ObjectEventToken;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Event Token";
	bf.badChars = " :";
	bf.loading = true;
	bf.setNameSpace("eventtoken");
	bf.loading = false;
	bf.comment = "Single-word token to identify this event type in Ganymede source code";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.ObjectEventObjectName;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Object Type Name";
	bf.comment = "The name of the object that this event is tracking";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.ObjectEventName;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Event Name";
	bf.badChars = ":";
	bf.comment = "Short name for this event class, suitable for an email message title";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.ObjectEventDescription;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Event Description";
	bf.badChars = ":";
	bf.comment = "Fuller description for this event class, suitable for an email message body";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.ObjectEventMailToSelf;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Cc Admin";
	bf.comment = "If true, mail for this event will always be cc'ed to the admin performing the action";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.ObjectEventMailOwners;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Cc Owner Groups";
	bf.comment = "If true, mail for this event will always be cc'ed to the owner groups owning the object";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.ObjectEventExternalMail;
	bf.field_type = FieldType.STRING;
	bf.field_name = "External Email";
	bf.array = true;
	bf.comment = "Email addresses not stored in Ganymede";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.ObjectEventObjectType;
	bf.field_type = FieldType.NUMERIC;
	bf.field_name = "Object Type ID";
	bf.visibility = false;	// we don't want this to be seen by the client
	bf.comment = "The type code of the object that this event is tracking";
	b.addFieldToEnd(bf);

	// link in the class we specified

	b.createHook();

	// and link in the base to this DBStore

	setBase(b);

	// create user base

	DBBaseCategory userCategory = new DBBaseCategory(this, "User-Level Objects", rootCategory);
	rootCategory.addNodeAfter(userCategory, null);

	b = new DBObjectBase(this, false);
	b.object_name = "User";
	b.type_code = (short) SchemaConstants.UserBase; // 2

	userCategory.addNodeAfter(b, null); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.UserUserName;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Username";
	bf.minLength = 2;
	bf.maxLength = 8;
	bf.badChars = " :=><|+[]\\/*;:.,?\""; // See p.252, teach yourself WinNT Server 4 in 14 days
	bf.loading = true;
	bf.setNameSpace("username");
	bf.loading = false;
	bf.comment = "User name for an individual privileged to log into Ganymede and/or the network";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.UserPassword;
	bf.field_type = FieldType.PASSWORD;
	bf.field_name = "Password";
	bf.maxLength = 32;
	bf.crypted = true;
	bf.isCrypted();
	bf.comment = "Password for an individual privileged to log into Ganymede and/or the network";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.UserAdminPersonae;
	bf.field_type = FieldType.INVID;
	bf.allowedTarget = SchemaConstants.PersonaBase;
	bf.targetField = SchemaConstants.PersonaAssocUser;
	bf.field_name = "Admin Personae";
	bf.array = true;
	bf.comment = "A list of admin personae this user can assume";
	b.addFieldToEnd(bf);

	b.setLabelField(SchemaConstants.UserUserName);
    
	setBase(b);

	// create Task Base

	b = new DBObjectBase(this, false);
	b.object_name = "Task";
	b.classname = "arlut.csd.ganymede.taskCustom";
	b.type_code = (short) SchemaConstants.TaskBase; // 5

	adminCategory.addNodeAfter(b, null); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.TaskName;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Task Name";
	bf.loading = true;
	bf.setNameSpace("buildertask");
	bf.loading = false;
	bf.comment = "Name of this task, as shown in task monitor";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.TaskClass;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Task Class";
	bf.badChars = "/:";
	bf.comment = "Name of the plug-in class to load on server restart to handle this task";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.TaskRunOnCommit;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Run On Transaction Commit";
	bf.comment = "If true, this task will be run on transaction commit";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.TaskRunPeriodically;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Run Periodically";
	bf.comment = "If true, this task will be scheduled for periodic execution";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.TaskPeriodUnit;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Period Unit";
	bf.comment = "What is the unit of time we're using?";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.TaskPeriodCount;
	bf.field_type = FieldType.NUMERIC;
	bf.field_name = "Period Count";
	bf.comment = "How many time units between task runs?";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.TaskPeriodAnchor;
	bf.field_type = FieldType.DATE;
	bf.field_name = "Period Anchor";
	bf.comment = "When do we start counting period intervals from?";
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.TaskOptionStrings;
	bf.field_type = FieldType.STRING;
	bf.array = true;
	bf.field_name = "Option Strings";
	bf.comment = "Optional task parameters, interpreted by specific tasks if needed";
	b.addFieldToEnd(bf);

	b.setLabelField(SchemaConstants.TaskName);

    	// link in the class we specified

	b.createHook();

	// and record the base

	setBase(b);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("remote :" + ex);
      }

    loading = false;
  }

  /**
   * <p>Creates required objects when a new database is created
   * from scratch, or if a pre-existing but damaged database file
   * is loaded..</p>
   */

  void initializeObjects()
  {
    DBEditObject eObj;
    StringDBField s;
    PasswordDBField p;
    InvidDBField i;
    BooleanDBField b;
    GanymedeSession gSession = null;
    DBSession session;
    PermissionMatrixDBField pm;
    ReturnVal retVal;
    boolean success = false;

    Invid supergashOwner = new Invid(SchemaConstants.OwnerBase, SchemaConstants.OwnerSupergash);
    Invid supergash = new Invid(SchemaConstants.PersonaBase, SchemaConstants.PersonaSupergashObj);
    Invid monitor = new Invid(SchemaConstants.PersonaBase, SchemaConstants.PersonaMonitorObj);
    Invid defaultRole = new Invid(SchemaConstants.RoleBase, SchemaConstants.RoleDefaultObj);

    /* -- */

    // manually insert the root (supergash) admin object

    try
      {
	gSession = new GanymedeSession();
	gSession.enableOversight(false);
      }
    catch (RemoteException ex)
      {
	throw new Error("RMI system could not initialize GanymedeSession");
      }

    session = gSession.session;
    session.openTransaction("DBStore bootstrap initialization");

    try
      {
	// make sure the supergash owner group exists

	if (!exists(session, supergashOwner))
	  {
	    System.err.println("Creating supergash Owner Group");

	    retVal = session.createDBObject(SchemaConstants.OwnerBase, supergashOwner, null);

	    if (retVal == null || !retVal.didSucceed())
	      {
		throw new RuntimeException("Couldn't create supergash owner group.");
	      }

	    eObj = (DBEditObject) retVal.getObject();

	    s = (StringDBField) eObj.getField("Name");
	    s.setValueLocal(Ganymede.rootname);
	  }

	// make sure the supergash admin persona object exists

	if (!exists(session, supergash))
	  {
	    System.err.println("Creating supergash persona object");
	
	    retVal = session.createDBObject(SchemaConstants.PersonaBase, supergash, null);
	
	    if (retVal == null || !retVal.didSucceed())
	      {
		throw new RuntimeException("Couldn't create supergash admin persona.");
	      }
	
	    eObj = (DBEditObject) retVal.getObject();
	
	    // set the user visible name

	    s = (StringDBField) eObj.getField(SchemaConstants.PersonaNameField);
	    s.setValueLocal(Ganymede.rootname);

	    // check to make sure the invisible label field was properly set

	    s = (StringDBField) eObj.getField(SchemaConstants.PersonaLabelField);

	    if (!s.getValueString().equals(Ganymede.rootname))
	      {
		System.err.println("*** Error, supergash label field not automatically set..");
		System.err.println("*** problem in adminPersonaCustom? " + s.getValueString());

		s.setValueLocal(Ganymede.rootname);
	      }

	    p = (PasswordDBField) eObj.getField(SchemaConstants.PersonaPasswordField);
	    p.setPlainTextPass(Ganymede.defaultrootpassProperty); // default supergash password
	
	    b = (BooleanDBField) eObj.getField(SchemaConstants.PersonaAdminConsole);
	    b.setValueLocal(Boolean.TRUE);
	
	    b = (BooleanDBField) eObj.getField(SchemaConstants.PersonaAdminPower);
	    b.setValueLocal(Boolean.TRUE);
	  }
	else
	  {
	    eObj = session.editDBObject(supergash);

	    if (eObj == null)
	      {
		throw new RuntimeException("Couldn't edit supergash admin persona.");
	      }

	    if (Ganymede.rootname != null && !Ganymede.rootname.equals(""))
	      {
		s = (StringDBField) eObj.getField(SchemaConstants.PersonaNameField);
		s.setValueLocal(Ganymede.rootname);
	      }
        
	    if (Ganymede.resetadmin)
	      {
		p = (PasswordDBField) eObj.getField("Password");
		p.setPlainTextPass(Ganymede.defaultrootpassProperty); // default supergash password
	      }
	
	    b = (BooleanDBField) eObj.getField(SchemaConstants.PersonaAdminConsole);
	    b.setValueLocal(Boolean.TRUE);
	
	    b = (BooleanDBField) eObj.getField(SchemaConstants.PersonaAdminPower);
	    b.setValueLocal(Boolean.TRUE);
	  }

	// make sure the supergash admin persona and supergash owner group are linked.

	eObj = session.editDBObject(supergash);

	i = (InvidDBField) eObj.getField(SchemaConstants.PersonaGroupsField);

	if (!i.containsElement(supergashOwner))
	  {
	    System.err.println("Linking supergash Persona object to supergash Owner Group");
	    i.addElementLocal(supergashOwner);
	  }

	// make sure the monitor object exists if the properties file defines one

	if (!exists(session, monitor) && Ganymede.monitornameProperty != null && 
	    Ganymede.defaultmonitorpassProperty != null)
	  {
	    System.err.println("Creating monitor persona");
	
	    retVal = session.createDBObject(SchemaConstants.PersonaBase, monitor, null);

	    if (retVal == null || !retVal.didSucceed())
	      {
		throw new RuntimeException("Couldn't create monitor admin persona.");
	      }
	
	    if (Ganymede.monitornameProperty == null)
	      {
		throw new NullPointerException("monitor name property not loaded, can't initialize monitor account");
	      }

	    eObj = (DBEditObject) retVal.getObject();
	
	    s = (StringDBField) eObj.getField(SchemaConstants.PersonaNameField);
	    s.setValueLocal(Ganymede.monitornameProperty);

	    // check our autonomic functioning

	    s = (StringDBField) eObj.getField(SchemaConstants.PersonaLabelField);

	    if (!s.getValueString().equals(Ganymede.monitornameProperty))
	      {
		System.err.println("*** Error, monitor label field not automatically set..");
		System.err.println("*** problem in adminPersonaCustom? " + s.getValueString());

		s.setValueLocal(Ganymede.monitornameProperty);
	      }

	    //	    s.setValueLocal(Ganymede.monitornameProperty);
    
	    p = (PasswordDBField) eObj.getField(SchemaConstants.PersonaPasswordField);
	
	    if (Ganymede.defaultmonitorpassProperty != null)
	      {
		p.setPlainTextPass(Ganymede.defaultmonitorpassProperty); // default monitor password
	      }
	    else
	      {
		throw new NullPointerException("monitor password property not loaded, can't initialize monitor account");
	      }

	    b = (BooleanDBField) eObj.getField(SchemaConstants.PersonaAdminConsole);
	    b.setValueLocal(Boolean.TRUE);

	    b = (BooleanDBField) eObj.getField(SchemaConstants.PersonaAdminPower);
	    b.setValueLocal(Boolean.FALSE);
	  }

	// make sure we have the default role object

	if (!exists(session, defaultRole))
	  {
	    System.err.println("Creating default Role object");

	    retVal = session.createDBObject(SchemaConstants.RoleBase, defaultRole, null);

	    if (retVal == null || !retVal.didSucceed())
	      {
		throw new RuntimeException("Couldn't create permissions default object.");
	      }

	    eObj = (DBEditObject) retVal.getObject();
	
	    s = (StringDBField) eObj.getField(SchemaConstants.RoleName);
	    s.setValueLocal("Default");
	
	    // what can users do with objects they own?  Includes users themselves
	
	    pm = (PermissionMatrixDBField) eObj.getField(SchemaConstants.RoleMatrix);

	    // view self, nothing else

	    pm.setPerm(SchemaConstants.UserBase, PermEntry.getPermEntry(true, false, false, false));
	  }

	createSysEventObj(session, "abnormallogout", "Unusual Logout", null, false);

	createSysEventObj(session, "adminconnect", "Admin Console Attached", 
			  "Admin Console Attached", false);

	createSysEventObj(session, "admindisconnect", "Admin Console Disconnected", 
			  "Admin Console Disconnected", false);

	createSysEventObj(session, "badpass", "Failed login attempt", 
			  "Bad username and/or password", true);

	createSysEventObj(session, "deleteobject", "Object Deleted", 
			  "This object has been deleted.", true);

	createSysEventObj(session, "dump", "Database Dump", "Database Dump", false);

	createSysEventObj(session, "expirationwarn", "Expiration Warning", 
			  "This object is going to expire soon.", false);

	createSysEventObj(session, "expirenotify", "Expiration Notification", 
			  "This object has been expired.", false);

	createSysEventObj(session, "finishtransaction", "transaction end", null, false);

	createSysEventObj(session, "goodlogin", "Successful login", null, false);

	createSysEventObj(session, "inactivateobject", "Object Inactivation", 
			  "This object has been inactivated", true);

	createSysEventObj(session, "journalreset", "Journal File Reset", "Journal file reset", false);

	createSysEventObj(session, "normallogout", "Normal Logout", null, false);

	createSysEventObj(session, "objectchanged", "Object Changed", "Object Changed", true);

	createSysEventObj(session, "objectcreated", "Object Created", "Object Created", true);

	createSysEventObj(session, "reactivateobject", "Object Reactivation", 
			  "This object has been reactivated", true);

	createSysEventObj(session, "removalwarn", "Removal Warning", "This object is going to be removed", false);

	createSysEventObj(session, "removenotify", "Removal Notification", "This object has been removed", false);

	createSysEventObj(session, "restart", "Server Restarted", "The Ganymede server was restarted", false);

	createSysEventObj(session, "shutdown", "Server shutdown", 
			  "The Ganymede server was cleanly shut down", false);

	// Create the start transaction oBject.  This object is
	// consulted by the DBLog class to guide how mail should be
	// handled for transaction logs.

	DBEditObject transactionEvent = createSysEventObj(session, 
							  "starttransaction", 
							  "transaction start", null, false);

	if (transactionEvent != null)
	  {
	    transactionEvent.setFieldValueLocal(SchemaConstants.EventMailBoolean, new Boolean(true));
	    transactionEvent.setFieldValueLocal(SchemaConstants.EventMailToSelf, new Boolean(true));
	    transactionEvent.setFieldValueLocal(SchemaConstants.EventMailOwners, new Boolean(true));
	    transactionEvent.setFieldValueLocal(SchemaConstants.NotesField, 
						"This system event object is consulted to determine whether " +
						"mail should be sent\nout when a transaction is committed.\n\n" +
						"If the 'Event Mail' checkbox is set to false, no transaction log " +
						"mail will be sent\nfrom the Ganymede server, ever.\n\nIf the " +
						"'Cc: Admin' checkbox is set to true, the admin who committed the " +
						"transaction will receive a copy of the transaction log.\n\n" +
						"If the 'Cc: Owner Groups' checkbox is set to true, the members of " +
						"the owner groups whose objects were affected by the transaction " +
						"committed will each receive a copy of that portion of the transaction " +
						"log that concerns objects owned by those admins.");
	  }

	// we commit the transaction at the DBStore level because we don't
	// want to mess with running builder tasks

	retVal = session.commitTransaction();

	// if the DBSession commit failed, we won't get an automatic
	// abort..  do that here.

	if (retVal != null && !retVal.didSucceed())
	  {
	    try
	      {
		session.abortTransaction();
	      }
	    catch (Throwable ex)
	      {
		ex.printStackTrace();
	      }
	    finally
	      {
		success=true;	// true enough, anyway
		gSession.logout();
	      }
	  }

	gSession.logout();
	success = true;
      }
    catch (Throwable ex)
      {
	ex.printStackTrace();
      }
    finally
      {
	if (!success)
	  {
	    session.abortTransaction();
	    gSession.logout();
	  }
      }
  }

  /**
   * Convenience method for initializeObjects().
   */

  private boolean exists(DBSession session, Invid invid)
  {
    return (session.viewDBObject(invid) != null);
  }

  /**
   * Convenience method for initializeObjects().
   */

  private DBEditObject createSysEventObj(DBSession session, 
					 String token, String name, String description,
					 boolean ccAdmin)
				 
  {
    DBEditObject eO = null;
    ReturnVal retVal;

    if (session.getGSession().findLabeledObject(token, SchemaConstants.EventBase) != null)
      {
	return null;
      }

    System.err.println("Creating " + token + " system event object");

    retVal = session.createDBObject(SchemaConstants.EventBase, null);

    if (retVal == null || !retVal.didSucceed())
      {
	throw new RuntimeException("Error, could not create system event object " + token);
      }

    eO = (DBEditObject) retVal.getObject();

    retVal = eO.setFieldValueLocal(SchemaConstants.EventToken, token);

    if (retVal != null && !retVal.didSucceed())
      {
	throw new RuntimeException("Error, could not set token for system event object " + token);
      }

    retVal = eO.setFieldValueLocal(SchemaConstants.EventName, name);

    if (retVal != null && !retVal.didSucceed())
      {
	throw new RuntimeException("Error, could not set name for system event object " + token);
      }

    if (description != null)
      {
	retVal = eO.setFieldValueLocal(SchemaConstants.EventDescription, description);
	
	if (retVal != null && !retVal.didSucceed())
	  {
	    throw new RuntimeException("Error, could not set description system event object " + token);
	  }
      }

    return eO;
  }

  /*

    -- The following methods are used to keep track of DBStore state and
       are reflected in updates to any connected Admin consoles.  These
       methods are for statistics keeping only. --

   */

  /**
   * <p>Increments the count of checked-out objects for the admin consoles.</p>
   */

  void checkOut()
  {
    objectsCheckedOut++;
    GanymedeAdmin.updateCheckedOut();
  }

  /**
   * <p>Decrements the count of checked-out objects for the admin consoles.</p>
   */

  void checkIn()
  {
    objectsCheckedOut--;

    GanymedeAdmin.updateCheckedOut();

    if (objectsCheckedOut < 0)
      {
	throw new RuntimeException("Objects checked out has gone negative"); 
      }
  }

  /**
   * <p>Increments the count of held locks for the admin consoles.</p>
   */

  void addLock()
  {
    lockSync.addLock();
  }

  /**
   * <p>Decrements the count of held locks for the admin consoles.</p>
   */

  void removeLock()
  {
    lockSync.removeLock();
  }

  public void debugBackPointers()
  {
    synchronized (backPointers)
      {
	Enumeration enum = backPointers.keys();

	while (enum.hasMoreElements())
	  {
	    Invid objInvid = (Invid) enum.nextElement();

	    System.err.println("Object: " + describe(objInvid));

	    Hashtable backs = (Hashtable) backPointers.get(objInvid);

	    Enumeration backenum = backs.keys();

	    while (backenum.hasMoreElements())
	      {
		System.err.println("\t" + describe((Invid) backenum.nextElement()));
	      }

	    System.err.println("\n");
	  }
      }
  }

  private final String describe(Invid x)
  {
    // very little synchronization involved here aside from
    // backpointers in the debugBackPointers() method above, so we're
    // not very worried about deadlocks here

    return Ganymede.internalSession.describe(x);
  }
}

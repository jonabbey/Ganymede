/*
   GASH 2

   DBStore.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Release: $Name:  $
   Version: $Revision: 1.85 $
   Last Mod Date: $Date: 1999/07/21 05:38:20 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

   Contact information

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
import java.text.*;
import java.rmi.*;

import arlut.csd.Util.zipIt;

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
 * <P>In addition to handling loads, dumps, and schema initialization
 * for cold server start-ups, DBStore also acts as a synchronization
 * monitor object for all {@link arlut.csd.ganymede.DBLock DBLock}
 * activity.  Because of this, it is critical that all synchronized
 * methods in this class do a this.notifyAll() on exiting a
 * synchronized block because the {@link arlut.csd.ganymede.DBLock
 * DBLock} classes wait on DBStore as their synchronization monitor.
 * Generally, whenever the DBLock classes wait on DBStore, they do so
 * in a loop with a timeout specified on the wait() to avoid accidental
 * thread-lock, but it is still important to do a notifyAll() to avoid
 * unnecessary delays.</P>
 *
 * @version $Revision: 1.85 $ %D%
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT 
 */

public class DBStore {

  // type identifiers used in the object store

  static final String id_string = "Gstore";
  static final byte major_version = 1;
  static final byte minor_version = 12;

  static boolean debug = false;

  /* - */

  /*
    All of the following should only be modified/accessed
    in a critical section synchronized on the DBStore object.
   */
  
  /**
   * lock for schema revision
   */

  boolean schemaEditInProgress;

  /** 
   * lock for invid sweep
   */

  boolean sweepInProgress;	
  
  /**
   * to keep track of what ID to assign to new bases
   */

  short maxBaseId = 256;
  
  /**
   * hash mapping object type to DBObjectBase's
   */
	
  Hashtable objectBases;

  /** 
   * <P>Identifier keys for current {@link arlut.csd.ganymede.DBLock
   * DBLocks}.</P>
   *
   * <P>This hash is used by the establish() method in various DBLock
   * subclasses to guarantee that only one lock will established by
   * a client at a time, to prevent any possibility of DBLock deadlock.</P>
   *
   * <P>The values in this hash may either be scalar DBLock objects, or
   * in the case of readers (where it is permissible for a single client
   * to have several distinct reader locks), a Vector of DBLocks.</P>
   */

  Hashtable lockHash;

  /**
   * unique valued hashes
   */

  Vector nameSpaces;

  /**
   * if true, DBObjectBase set methods will be enabled
   */

  boolean loading = false;

  /**
   * Root of the category tree defined in this database
   */

  DBBaseCategory rootCategory;

  byte file_major, file_minor;

  /**
   * The Journal for this database, initialized when the database is
   * loaded.
   */

  DBJournal journal = null;

  // debugging info

  int objectsCheckedOut = 0;
  int locksHeld = 0;

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
    lockHash = new Hashtable(20); // default
    nameSpaces = new Vector();

    try
      {
	rootCategory = new DBBaseCategory(this, "Categories");
      }
    catch (RemoteException ex)
      {
	throw new Error("couldn't initialize rootCategory");
      }

    schemaEditInProgress = false;
    GanymedeAdmin.setState("Normal Operation");
  }

  /**
   * <p>Load the database from disk.</p>
   *
   * <p>This method loads both the database type definition and database
   * contents from a single disk file.</p>
   *
   * <p>Note that this method does _not_ do a this.notifyAll() upon
   * returning, this is acceptable because we are assuming we will
   * never call load() on a DBStore with locks held.</p>
   *
   * @param filename Name of the database file
   * @param reallyLoad if true, we'll actually fully load the database.
   * If false, we'll just get the schema loaded so we can report on it.
   * @see arlut.csd.ganymede.DBJournal
   */

  public synchronized void load(String filename, boolean reallyLoad)
  {
    load(filename, reallyLoad, true);
  }

  /**
   * <p>Load the database from disk.</p>
   *
   * <p>This method loads both the database type definition and database
   * contents from a single disk file.</p>
   *
   * <p>Note that this method does _not_ do a this.notifyAll() upon
   * returning, this is acceptable because we are assuming we will
   * never call load() on a DBStore with locks held.</p>
   *
   * @param filename Name of the database file
   * @param reallyLoad if true, we'll actually fully load the database.
   * If false, we'll just get the schema loaded so we can report on it.
   * @param loadJournal if true, process and consolidate the journal
   * on loading.
   * @see arlut.csd.ganymede.DBJournal
   */

  public synchronized void load(String filename, boolean reallyLoad,
				boolean loadJournal)
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
	    System.err.println("IOException: " + ex);
	    throw new RuntimeException("DBStore initialization error (" + filename + ")");
	  }

	file_major = in.readByte();
	file_minor = in.readByte();

	if (debug)
	  {
	    System.err.println("DBStore load(): file version " + file_major + "." + file_minor);
	  }

	if (file_major != major_version)
	  {
	    System.err.println("DBStore initialization error: major version mismatch");
	    throw new Error("DBStore initialization error (" + filename + ")");
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

	if (file_major >= 1 && file_minor >= 3)
	  {
	    rootCategory = new DBBaseCategory(this, in);
	  }
	
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
	    tempBase = new DBObjectBase(in, this, reallyLoad);
	    
	    setBase(tempBase);

	    if (debug)
	      {
		System.err.println("loaded base " + tempBase.getTypeID());
	      }

	    if (tempBase.getTypeID() > maxBaseId)
	      {
		maxBaseId = tempBase.getTypeID();
	      }
	  }
      }
    catch (IOException ex)
      {
	System.err.println("DBStore initialization error: couldn't properly process " + filename);
	System.err.println("IOException: " + ex);
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

    lockHash = new Hashtable(baseCount); // reset lockHash

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

	if (!journal.clean())
	  {
	    try
	      {
		if (!journal.load())
		  {
		    System.err.println("\nError, couldn't load entire journal.. " +
				       "final transaction in journal not processed.\n");
		  }
		else
		  {
		    // go ahead and consolidate the journal into the DBStore
		    // before we really get under way.

		    // Notice that we are going to archive a copy of the
		    // existing db file each time we start up the server

		    if (!journal.clean())
		      {
			dump(filename, true, true);
		      }
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
   * clean() method to determine whether or not a dump is really needed.</p>
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
    try
      {
	if (schemaEditInProgress)
	  {
	    Ganymede.debug("DBStore.dumpSchema(): schema being edited, dump aborted");
	  }

	File dbFile = null;
	FileOutputStream outStream = null;
	BufferedOutputStream bufStream = null;
	DataOutputStream out = null;

	FileOutputStream textOutStream = null;
	PrintWriter textOut = null;

	Enumeration basesEnum;
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

	try
	  {
	    lock.establish("System");	// wait until we get our lock 
	  }
	catch (InterruptedException ex)
	  {
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
		    String directoryName = dbFile.getParent();

		    File directory = new File(directoryName);

		    if (!directory.isDirectory())
		      {
			throw new IOException("Error, couldn't find output directory to backup: " + directoryName);
		      }

		    File oldDirectory = new File(directoryName + File.separator + "old");
		    
		    if (!oldDirectory.exists())
		      {
			oldDirectory.mkdir();
		      }

		    DateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", 
								java.util.Locale.US);
		    String label = formatter.format(new Date());

		    String zipFileName = directoryName + File.separator + "old" + File.separator + label + "db.zip";

		    Vector fileNameVect = new Vector();
		    fileNameVect.addElement(filename);

		    zipIt.createZipFile(zipFileName, fileNameVect);
		  }
	      }

	    // and dump the whole thing to ganymede.db.new

	    outStream = new FileOutputStream(filename + ".new");
	    bufStream = new BufferedOutputStream(outStream);
	    out = new DataOutputStream(bufStream);

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

	    if (major_version >= 1 && minor_version >= 3)
	      {
		rootCategory.emit(out);
	      }

	    baseCount = (short) objectBases.size();

	    out.writeShort(baseCount);
	
	    basesEnum = objectBases.elements();

	    while (basesEnum.hasMoreElements())
	      {
		((DBObjectBase) basesEnum.nextElement()).emit(out, true);
	      } 

	    out.close();
	    out = null;

	    // ok, we've successfully dumped to ganymede.db.new.. move
	    // the old file to ganymede.db.bak

	    dbFile.renameTo(new File(filename + ".bak"));

	    // and move ganymede.db.new to ganymede.db.. note that we
	    // do have a very slight vulnerability here if we are
	    // interrupted between the above rename and this one.

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
    finally
      {
	this.notifyAll();
      }
  }

  /**
   * <p>Dump the schema to disk</p>
   *
   * <p>This method dumps the entire database to disk, minus any actual objects.</p>
   *
   * <p>The thread that calls the dump method will be suspended until
   * there are no threads performing update writes to the in-memory
   * database.  In practice this will likely never be a long interval.</p>
   *
   * @param filename Name of the database file to emit
   * @param releaseLock boolean.  If releaseLock==false, dump() will not release
   *                              the dump lock when it is done with the dump.  This
   *                              is intended to allow for a clean shut down.  For
   *                              non-terminal dumps, releaseLock should be true.
   *
   * @see arlut.csd.ganymede.DBEditSet
   * @see arlut.csd.ganymede.DBJournal
   * @see arlut.csd.ganymede.adminSession
   */

  public synchronized void dumpSchema(String filename, boolean releaseLock) throws IOException
  {
    try
      {
	if (schemaEditInProgress)
	  {
	    Ganymede.debug("DBStore.dumpSchema(): schema being edited, dump aborted");
	  }

	File dbFile = null;
	FileOutputStream outStream = null;
	BufferedOutputStream bufStream = null;
	DataOutputStream out = null;

	FileOutputStream textOutStream = null;
	PrintWriter textOut = null;
	Enumeration basesEnum;
	short baseCount, namespaceCount, categoryCount;
	DBDumpLock lock = null;
	DBNameSpace ns;
	DBBaseCategory bc;
	DBObjectBase base;

	/* -- */

	if (debug)
	  {
	    System.err.println("DBStore: Dumping");
	  }

	lock = new DBDumpLock(this);

	try
	  {
	    lock.establish("System");	// wait until we get our lock 
	  }
	catch (InterruptedException ex)
	  {
	    Ganymede.debug("DBStore.dumpSchema(): dump lock establish interrupted, schema not dumped");
	    return;
	  }
    
	// Move the old version of the file to a backup
    
	try
	  {
	    dbFile = new File(filename);

	    if (dbFile.isFile())
	      {
		dbFile.renameTo(new File(filename + ".bak"));
	      }

	    // and dump the whole thing

	    outStream = new FileOutputStream(filename);
	    bufStream = new BufferedOutputStream(outStream);
	    out = new DataOutputStream(bufStream);

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

	    if (major_version >= 1 && minor_version >= 3)
	      {
		rootCategory.emit(out);
	      }

	    baseCount = (short) objectBases.size();

	    out.writeShort(baseCount);
	
	    basesEnum = objectBases.elements();

	    while (basesEnum.hasMoreElements())
	      {
		base = (DBObjectBase) basesEnum.nextElement();

		if (base.type_code == SchemaConstants.OwnerBase ||
		    base.type_code == SchemaConstants.PersonaBase ||
		    base.type_code == SchemaConstants.RoleBase ||
		    base.type_code == SchemaConstants.EventBase)
		  {
		    base.partialEmit(out); // gotta retain admin login ability
		  }
		else if (base.type_code == SchemaConstants.TaskBase)
		  {
		    base.emit(out, true); // save the builder information
		  }
		else
		  {
		    base.emit(out, false); // just write out the schema info
		  }
	      } 

	    // and dump the schema out in a human readable form
	
	    if (Ganymede.htmlProperty != null)
	      {
		textOutStream = new FileOutputStream(Ganymede.htmlProperty);
		textOut = new PrintWriter(textOutStream);
		
		printCategoryTreeHTML(textOut);
	      }
	  }
	catch (IOException ex)
	  {
	    System.err.println("DBStore error dumping schema to " + filename);
	    throw ex;
	  }
	finally
	  {
	    if (releaseLock)
	      {
		if (lock != null)
		  {
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
      }
    finally
      {
	this.notifyAll();
      }
  }

  /**
   * <p>Creates a {@link arlut.csd.ganymede.DBSession DBSession}
   * handle on this database.  Each {@link arlut.csd.ganymede.GanymedeSession GanymedeSession}
   * created in the Ganymede Server will also have a DBSession.</p>
   *
   * @param key Identifying key
   */

  protected synchronized DBSession login(Object key)
  {
    try
      {
	DBSession retVal;

	/* -- */

	if (schemaEditInProgress)
	  {
	    throw new RuntimeException("can't login, the server's in schema edit mode");
	  }

	if (sweepInProgress)
	  {
	    throw new RuntimeException("can't login, the server is performing an invid sweep");
	  }

	retVal = new DBSession(this, null, key);

	return retVal;
      }
    finally
      {
	this.notifyAll();
      }
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
    try
      {
	out.println("<HTML><HEAD><TITLE>Ganymede Schema Dump -- " + new Date() + "</TITLE></HEAD>");
	out.println("<BODY BGCOLOR=\"#FFFFFF\"><H1>Ganymede Schema Dump -- " + new Date() + "</H1>");
	out.println("<HR>");

	rootCategory.printHTML(out);

	out.println("<HR>");
	out.println("Emitted: " + new Date());
	out.println("</BODY>");
      }
    finally
      {
	this.notifyAll();
      }
  }

  /** 
   * <p>Dumps a text representation of this database's category
   * hierarchy, starting at the root 
   * {@link arlut.csd.ganymede.DBBaseCategory DBBaseCategory}.</p>
   *
   * @param out PrintStream to print to 
   * @param showBuiltIns If false, don't display built-in field definitions
   */

  public synchronized void printCategoryTree(PrintWriter out, boolean showBuiltIns)
  {
    try
      {
	rootCategory.print(out, "", showBuiltIns);
      }
    finally
      {
	this.notifyAll();
      }
  }

  /**
   * <p>Do a printable dump of the object databases</p>
   *
   * @param out PrintStream to print to
   */

  public synchronized void printBases(PrintWriter out)
  {
    try
      {
	Enumeration enum;

	/* -- */

	enum = objectBases.elements();

	while (enum.hasMoreElements())
	  {
	    ((DBObjectBase) enum.nextElement()).print(out, "", true);
	  }
      }
    finally
      {
	this.notifyAll();
      }
  }

  /**
   * Returns a vector of Strings, the names of the bases currently
   * defined in this DBStore.
   */

  public synchronized Vector getBaseNameList()
  {
    Vector result = new Vector();

    try
      {
	Enumeration enum;

	/* -- */

	enum = objectBases.elements();

	while (enum.hasMoreElements())
	  {
	    result.addElement(((DBObjectBase) enum.nextElement()).getName());
	  }
      }
    finally
      {
	this.notifyAll();
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

  public synchronized DBObjectBase getObjectBase(String baseName)
  {
    try
      {
	DBObjectBase base;
	Enumeration enum;

	/* -- */

	enum = objectBases.elements();

	while (enum.hasMoreElements())
	  {
	    base = (DBObjectBase) enum.nextElement();
	
	    if (base.getName().equals(baseName))
	      {
		return base;
	      }
	  }

	return null;
      }
    finally
      {
	this.notifyAll();
      }
  }

  /**
   *
   * Returns a base id for a newly created base
   * 
   */

  public synchronized short getNextBaseID()
  {
    try
      {
	return maxBaseId++;
      }
    finally
      {
	this.notifyAll();
      }
  }

  /**
   *
   * Let go of a baseId if the base create was not
   * committed.
   * 
   */

  public synchronized void releaseBaseID(short id)
  {
    try
      {
	if (id == maxBaseId)
	  {
	    maxBaseId--;
	  }
      }
    finally
      {
	this.notifyAll();
      }
  }

  /**
   * 
   * Method to replace/add a DBObjectBase in the DBStore.
   *
   */

  public synchronized void setBase(DBObjectBase base)
  {
    try
      {
	objectBases.put(base.getKey(), base);
      }
    finally
      {
	this.notifyAll();
      }
  }

  /**
   *
   * Method to locate a registered namespace by name.
   *
   */

  public synchronized DBNameSpace getNameSpace(String name)
  {
    DBNameSpace namespace;

    /* -- */

    for (int i = 0; i < nameSpaces.size(); i++)
      {
	namespace = (DBNameSpace) nameSpaces.elementAt(i);

	if (namespace.name.equals(name))
	  {
	    return namespace;
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

  public synchronized DBBaseCategory getCategory(String pathName)
  {
    try
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

		    bc = (DBBaseCategory) bc.getNode(tokens.sval);

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
    finally
      {
	this.notifyAll();
      }
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
	rootCategory.addNode(adminCategory, false, false);

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
	adminCategory.addNode(permCategory, false, false);

	// create owner base

	b = new DBObjectBase(this, false);
	b.object_name = "Owner Group";
	b.classname = "arlut.csd.ganymede.custom.ownerCustom";
	b.type_code = (short) SchemaConstants.OwnerBase; // 0
	b.displayOrder = b.type_code;

	permCategory.addNode(b, false, false);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.OwnerNameField;
	bf.field_order = 1;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Name";
	bf.loading = true;
	bf.setNameSpace("ownerbase");
	bf.loading = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "The name of this ownership group";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.OwnerMembersField;
	bf.field_order = 2;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Members";
	bf.array = true;
	bf.removable = false;
	bf.editable = false;
	bf.allowedTarget = SchemaConstants.PersonaBase;
	bf.targetField = SchemaConstants.PersonaGroupsField;
	bf.comment = "List of admin personae that are members of this owner set";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.OwnerCcAdmins;
	bf.field_order = 3;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Cc: All Admins";
	bf.loading = true;
	bf.loading = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "If checked, mail to this owner group will be sent to the admins";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.OwnerExternalMail;
	bf.field_order = 4;
	bf.field_type = FieldType.STRING;
	bf.field_name = "External Mail List";
	bf.removable = false;
	bf.editable = false;
	bf.array = true;
	bf.comment = "What external email addresses should be notified of changes to objects owned?";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.OwnerObjectsOwned;
	bf.field_order = 999;	// the custom class makes this a hidden field
	bf.field_type = FieldType.INVID;
	bf.field_name = "Objects owned";
	bf.allowedTarget = -2;	// any
	bf.targetField = SchemaConstants.OwnerListField;	// owner list field
	bf.removable = false;
	bf.editable = false;
	bf.array = true;
	bf.comment = "What objects are owned by this owner set";
	b.fieldTable.put(bf);

	b.setLabelField(SchemaConstants.OwnerNameField);

	// link in the class we specified

	b.createHook();

	// and link in the base to this DBStore

	setBase(b);

	// create persona base

	b = new DBObjectBase(this, false);
	b.object_name = "Admin Persona";
	b.classname = "arlut.csd.ganymede.custom.adminPersonaCustom";
	b.type_code = (short) SchemaConstants.PersonaBase; // 1
	b.displayOrder = b.type_code;

	permCategory.addNode(b, false, false); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.PersonaNameField;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Name";
	bf.field_order = 0;
	bf.loading = true;
	bf.setNameSpace("persona");
	bf.loading = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "The unique name for this admin persona";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.PersonaPasswordField;
	bf.field_type = FieldType.PASSWORD;
	bf.field_name = "Password";
	bf.maxLength = 32;
	bf.field_order = 1;
	bf.removable = false;
	bf.editable = false;
	bf.crypted = true;
	bf.comment = "Persona password";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.PersonaGroupsField;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Owner Sets";
	bf.allowedTarget = SchemaConstants.OwnerBase;	// any
	bf.targetField = SchemaConstants.OwnerMembersField;	// owner list field
	bf.removable = false;
	bf.editable = false;
	bf.array = true;
	bf.comment = "What owner sets are this persona members of?";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.PersonaAssocUser;
	bf.field_type = FieldType.INVID;
	bf.field_name = "User";
	bf.allowedTarget = SchemaConstants.UserBase;	// any
	bf.targetField = SchemaConstants.UserAdminPersonae;	// owner list field
	bf.removable = false;
	bf.editable = false;
	bf.array = false;
	bf.comment = "What user is this admin persona associated with?";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.PersonaPrivs;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Privilege Sets";
	bf.allowedTarget = SchemaConstants.RoleBase;
	bf.targetField = SchemaConstants.RolePersonae;
	bf.array = true;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "What permission matrices are this admin persona associated with?";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.PersonaAdminConsole;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Admin Console";
	bf.array = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "If true, this persona can be used to access the admin console";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.PersonaAdminPower;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Full Console";
	bf.array = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "If true, this persona can kill users and edit the schema";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.PersonaMailAddr;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Email Address";
	bf.array = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Where email to this administrator should be sent";
	b.fieldTable.put(bf);

	b.setLabelField(SchemaConstants.PersonaNameField);

	// link in the class we specified

	b.createHook();

	// and link in the base to this DBStore

	setBase(b);

	// create Role base

	b = new DBObjectBase(this, false);
	b.object_name = "Role";
	b.classname = "arlut.csd.ganymede.custom.permCustom";
	b.type_code = (short) SchemaConstants.RoleBase; // 2
	b.displayOrder = b.type_code;

	permCategory.addNode(b, false, false); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.RoleName;
	bf.field_order = 1;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Name";
	bf.loading = true;
	bf.setNameSpace("access");
	bf.loading = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "The name of this permission matrix";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.RoleDelegatable;
	bf.field_order = 2;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Delegatable Role?";
	bf.removable = false;
	bf.editable = false;
	bf.comment = "If true, this role can be granted to admins created/edited by Personae with this role.";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.RoleMatrix;
	bf.field_order = 3;
	bf.field_type = FieldType.PERMISSIONMATRIX;
	bf.field_name = "Objects Owned Access Bits";
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Access bits, by object type for objects owned by admins using this permission object";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.RoleDefaultMatrix;
	bf.field_order = 4;
	bf.field_type = FieldType.PERMISSIONMATRIX;
	bf.field_name = "Default Access Bits";
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Access bits, by object type for all objects on the part of admins using this permission object";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.RolePersonae;
	bf.field_order = 5;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Persona entities";
	bf.allowedTarget = SchemaConstants.PersonaBase;
	bf.targetField = SchemaConstants.PersonaPrivs;
	bf.array = true;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "What personae are using this permission matrix?";
	b.fieldTable.put(bf);

	b.setLabelField(SchemaConstants.RoleName);

	// link in the class we specified

	b.createHook();

	// and link in the base to this DBStore

	setBase(b);

	// create System Events base

	DBBaseCategory eventCategory = new DBBaseCategory(this, "Events", adminCategory);
	adminCategory.addNode(eventCategory, false, false);

	b = new DBObjectBase(this, false);
	b.object_name = "System Event";
	b.classname = "arlut.csd.ganymede.custom.eventCustom";
	b.type_code = (short) SchemaConstants.EventBase;  
	b.displayOrder = b.type_code;

	eventCategory.addNode(b, false, false); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.EventToken;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Event Token";
	bf.badChars = " :";
	bf.field_order = 1;
	bf.loading = true;
	bf.setNameSpace("eventtoken");
	bf.loading = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Single-word token to identify this event type in Ganymede source code";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.EventName;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Event Name";
	bf.badChars = ":";
	bf.field_order = 2;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Short name for this event class, suitable for an email message title";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.EventDescription;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Event Description";
	bf.badChars = ":";
	bf.field_order = 3;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Fuller description for this event class, suitable for an email message body";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.EventMailBoolean;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Send Mail?";
	bf.field_order = 4;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "If true, occurrences of this event will be emailed";
	b.fieldTable.put(bf);


	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.EventMailToSelf;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Cc: Admin?";
	bf.field_order = 6;

	bf.removable = false;
	bf.editable = false;
	bf.comment = "If true, mail for this event will always be cc'ed to the admin performing the action";
	b.fieldTable.put(bf);

	b.setLabelField(SchemaConstants.EventToken);
    
	// link in the class we specified

	b.createHook();

	// and link in the base to this DBStore

	setBase(b);

	// create Object Events base

	b = new DBObjectBase(this, false);
	b.object_name = "Object Event";
	b.classname = "arlut.csd.ganymede.custom.objectEventCustom";
	b.type_code = (short) SchemaConstants.ObjectEventBase;  
	b.displayOrder = b.type_code;

	eventCategory.addNode(b, false, false); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.ObjectEventToken;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Event Token";
	bf.badChars = " :";
	bf.field_order = 1;
	bf.loading = true;
	bf.setNameSpace("eventtoken");
	bf.loading = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Single-word token to identify this event type in Ganymede source code";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.ObjectEventObjectName;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Object Type Name";
	bf.field_order = 2;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "The name of the object that this event is tracking";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.ObjectEventName;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Event Name";
	bf.badChars = ":";
	bf.field_order = 3;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Short name for this event class, suitable for an email message title";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.ObjectEventDescription;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Event Description";
	bf.badChars = ":";
	bf.field_order = 4;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Fuller description for this event class, suitable for an email message body";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.ObjectEventMailToSelf;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Cc: Admin?";
	bf.field_order = 6;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "If true, mail for this event will always be cc'ed to the admin performing the action";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.ObjectEventMailOwners;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Cc: Owner Groups?";
	bf.field_order = 7;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "If true, mail for this event will always be cc'ed to the owner groups owning the object";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.ObjectEventExternalMail;
	bf.field_type = FieldType.STRING;
	bf.field_name = "External Email";
	bf.field_order = 8;
	bf.array = true;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Email addresses not stored in Ganymede";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.ObjectEventObjectType;
	bf.field_type = FieldType.NUMERIC;
	bf.field_name = "Object Type ID";
	bf.removable = false;
	bf.editable = false;
	bf.visibility = false;	// we don't want this to be seen by the client
	bf.comment = "The type code of the object that this event is tracking";
	b.fieldTable.put(bf);


	// link in the class we specified

	b.createHook();

	// and link in the base to this DBStore

	setBase(b);

	// create user base

	DBBaseCategory userCategory = new DBBaseCategory(this, "User-Level Objects", rootCategory);
	rootCategory.addNode(userCategory, false, false);

	b = new DBObjectBase(this, false);
	b.object_name = "User";
	b.type_code = (short) SchemaConstants.UserBase; // 2
	b.displayOrder = b.type_code;

	userCategory.addNode(b, false, false); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.UserUserName;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Username";
	bf.minLength = 2;
	bf.maxLength = 8;
	bf.badChars = " :=><|+[]\\/*;:.,?\""; // See p.252, teach yourself WinNT Server 4 in 14 days
	bf.field_order = 2;
	bf.loading = true;
	bf.setNameSpace("username");
	bf.loading = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "User name for an individual privileged to log into Ganymede and/or the network";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.UserPassword;
	bf.field_type = FieldType.PASSWORD;
	bf.field_name = "Password";
	bf.maxLength = 32;
	bf.field_order = 3;
	bf.removable = false;
	bf.editable = false;
	bf.crypted = true;
	bf.isCrypted();
	bf.comment = "Password for an individual privileged to log into Ganymede and/or the network";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.UserAdminPersonae;
	bf.field_type = FieldType.INVID;
	bf.allowedTarget = SchemaConstants.PersonaBase;
	bf.targetField = SchemaConstants.PersonaAssocUser;
	bf.field_name = "Admin Personae";
	bf.field_order = bf.field_code;
	bf.removable = false;
	bf.editable = false;
	bf.array = true;
	bf.comment = "A list of admin personae this user can assume";
	b.fieldTable.put(bf);

	b.setLabelField(SchemaConstants.UserUserName);
    
	setBase(b);

	// create Task Base

	b = new DBObjectBase(this, false);
	b.object_name = "Task";
	b.classname = "arlut.csd.ganymede.custom.taskCustom";
	b.type_code = (short) SchemaConstants.TaskBase; // 5
	b.displayOrder = b.type_code;

	adminCategory.addNode(b, false, false); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.TaskName;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Task Name";
	bf.field_order = 1;
	bf.loading = true;
	bf.setNameSpace("buildertask");
	bf.loading = false;
	bf.removable = false;
	bf.editable = false;
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.TaskClass;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Task Class";
	bf.badChars = "/:";
	bf.field_order = 2;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Name of the plug-in class to load on server restart to handle this task";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.TaskRunOnCommit;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Run On Transaction Commit";
	bf.field_order = 3;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "If true, this task will be run on transaction commit";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.TaskRunPeriodically;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Run Periodically";
	bf.field_order = 4;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "If true, this task will be scheduled for periodic execution";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.TaskPeriodUnit;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Period Unit";
	bf.field_order = 5;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "What is the unit of time we're using?";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.TaskPeriodCount;
	bf.field_type = FieldType.NUMERIC;
	bf.field_name = "Period Count";
	bf.field_order = 6;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "How many time units between task runs?";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.TaskPeriodAnchor;
	bf.field_type = FieldType.DATE;
	bf.field_name = "Period Anchor";
	bf.field_order = 7;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "When do we start counting period intervals from?";
	b.fieldTable.put(bf);

	b.setLabelField(SchemaConstants.TaskName);
    
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
   * from scratch.</p>
   */

  void initializeObjects()
  {
    DBEditObject eO;
    Invid inv;
    StringDBField s;
    PasswordDBField p;
    InvidDBField i;
    BooleanDBField b;
    GanymedeSession gSession = null;
    DBSession session;
    PermissionMatrixDBField pm;
    
    /* -- */

    // manually insert the root (supergash) admin object

    try
      {
	gSession = new GanymedeSession();
      }
    catch (RemoteException ex)
      {
	throw new Error("RMI system could not initialize GanymedeSession");
      }

    session = gSession.session;
    session.openTransaction("DBStore bootstrap initialization");

    eO =(DBEditObject) session.createDBObject(SchemaConstants.OwnerBase, null); // create a new owner group 
    inv = eO.getInvid();

    s = (StringDBField) eO.getField("Name");
    s.setValueLocal(Ganymede.rootname);
    
    // create a supergash admin persona object 

    eO =(DBEditObject) session.createDBObject(SchemaConstants.PersonaBase, null);

    s = (StringDBField) eO.getField("Name");
    s.setValueLocal(Ganymede.rootname);
    
    p = (PasswordDBField) eO.getField("Password");
    p.setPlainTextPass(Ganymede.defaultrootpassProperty); // default supergash password

    i = (InvidDBField) eO.getField(SchemaConstants.PersonaGroupsField);
    i.addElementLocal(inv);

    b = (BooleanDBField) eO.getField(SchemaConstants.PersonaAdminConsole);
    b.setValueLocal(Boolean.TRUE);

    b = (BooleanDBField) eO.getField(SchemaConstants.PersonaAdminPower);
    b.setValueLocal(Boolean.TRUE);

    // create a monitor admin persona object 

    eO =(DBEditObject) session.createDBObject(SchemaConstants.PersonaBase, null);

    s = (StringDBField) eO.getField("Name");

    if (Ganymede.monitornameProperty != null)
      {
	s.setValueLocal(Ganymede.monitornameProperty);
      }
    else
      {
	throw new NullPointerException("monitor name property not loaded, can't initialize monitor account");
      }
    
    p = (PasswordDBField) eO.getField("Password");

    if (Ganymede.defaultmonitorpassProperty != null)
      {
	p.setPlainTextPass(Ganymede.defaultmonitorpassProperty); // default monitor password
      }
    else
      {
	throw new NullPointerException("monitor password property not loaded, can't initialize monitor account");
      }

    b = (BooleanDBField) eO.getField(SchemaConstants.PersonaAdminConsole);
    b.setValueLocal(Boolean.TRUE);

    b = (BooleanDBField) eO.getField(SchemaConstants.PersonaAdminPower);
    b.setValueLocal(Boolean.FALSE);

    // create SchemaConstants.PermDefaultObj

    eO =(DBEditObject) session.createDBObject(SchemaConstants.RoleBase, null); 

    s = (StringDBField) eO.getField(SchemaConstants.RoleName);
    s.setValueLocal("Default");

    // what can users do with objects they own?  Includes users themselves

    pm = (PermissionMatrixDBField) eO.getField(SchemaConstants.RoleMatrix);
    pm.setPerm(SchemaConstants.UserBase, new PermEntry(true, false, false, false)); // view self, nothing else

    // what can arbitrary users do with objects they don't own?  nothing by default

    // pm = (PermissionMatrixDBField) eO.getField(SchemaConstants.RoleDefaultMatrix);

    createSysEventObj(session, "abnormallogout", "Unusual Logout", null, false);
    createSysEventObj(session, "adminconnect", "Admin Console Attached", "Admin Console Attached", false);
    createSysEventObj(session, "admindisconnect", "Admin Console Disconnected", "Admin Console Disconnected", false);
    createSysEventObj(session, "badpass", "Failed login attempt", "Bad username and/or password", true);
    createSysEventObj(session, "deleteobject", "Object Deleted", "This object has been deleted.", true);
    createSysEventObj(session, "dump", "Database Dump", "Database Dump", false);
    createSysEventObj(session, "expirationwarn", "Expiration Warning", "This object is going to expire soon.", false);
    createSysEventObj(session, "expirenotify", "Expiration Notification", "This object has been expired.", false);
    createSysEventObj(session, "finishtransaction", "transaction end", null, false);
    createSysEventObj(session, "goodlogin", "Successful login", null, false);
    createSysEventObj(session, "inactivateobject", "Object Inactivation", "This object has been inactivated", true);
    createSysEventObj(session, "journalreset", "Journal File Reset", "Journal file reset", false);
    createSysEventObj(session, "normallogout", "Normal Logout", null, false);
    createSysEventObj(session, "objectchanged", "Object Changed", "Object Changed", true);
    createSysEventObj(session, "objectcreated", "Object Created", "Object Created", true);
    createSysEventObj(session, "reactivateobject", "Object Reactivation", "This object has been reactivated", true);
    createSysEventObj(session, "removalwarn", "Removal Warning", "This object is going to be removed", false);
    createSysEventObj(session, "removenotify", "Removal Notification", "This object has been removed", false);
    createSysEventObj(session, "restart", "Server Restarted", "The Ganymede server was restarted", false);
    createSysEventObj(session, "shutdown", "Server shutdown", "The Ganymede server was cleanly shut down", false);
    createSysEventObj(session, "starttransaction", "transaction start", null, false);

    session.commitTransaction();
    gSession.logout();
  }

  private void createSysEventObj(DBSession session, 
				 String token, String name, String description,
				 boolean ccAdmin)
				 
  {
    DBEditObject eO;
    ReturnVal retVal;

    eO = (DBEditObject) session.createDBObject(SchemaConstants.EventBase, null);

    if (eO == null)
      {
	throw new RuntimeException("Error, could not create system event object " + token);
      }

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
  }

  /**
   * <p>Increments the count of held locks for the admin consoles.</p>
   */

  void addLock()
  {
    locksHeld++;
    GanymedeAdmin.updateLocksHeld();
  }

  /**
   * <p>Decrements the count of held locks for the admin consoles.</p>
   */

  void removeLock()
  {
    locksHeld--;
    GanymedeAdmin.updateLocksHeld();
  }

}

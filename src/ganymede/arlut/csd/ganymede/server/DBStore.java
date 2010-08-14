/*
   GASH 2

   DBStore.java

   The GANYMEDE object storage system.

   Created: 2 July 1996

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

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

package arlut.csd.ganymede.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import arlut.csd.Util.JythonMap;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.zipIt;
import arlut.csd.ganymede.common.FieldType;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.InvidPool;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.PermEntry;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.CategoryNode;

import com.jclark.xml.output.UTF8XMLWriter;

/*------------------------------------------------------------------------------
                                                                           class
                                                                         DBStore

------------------------------------------------------------------------------*/

/**
 * DBStore is the main Ganymede database class. DBStore is responsible for
 * actually handling the Ganymede database, and manages
 * database loads and dumps, locking (in conjunction with 
 * {@link arlut.csd.ganymede.server.DBSession DBSession} and
 * {@link arlut.csd.ganymede.server.DBLock DBLock}), 
 * the {@link arlut.csd.ganymede.server.DBJournal journal}, and schema dumping.
 *
 * The DBStore class holds the server's namespace and schema
 * dictionaries, in the form of a collection of
 * {@link arlut.csd.ganymede.server.DBNameSpace DBNameSpace} and {@link
 * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} objects.  Each
 * DBObjectBase contains schema information for an object type,
 * including field definitions for all fields that may be stored in
 * objects of that type.
 *
 * In addition to holding schema information, each DBObjectBase
 * contains an {@link arlut.csd.ganymede.common.Invid Invid}-keyed hash of
 * {@link arlut.csd.ganymede.server.DBObject DBObject}'s of that type in
 * memory after the database loading is complete at start-up.  Changes
 * made to the DBStore are done in {@link arlut.csd.ganymede.server.DBEditSet
 * transactional contexts} using DBSession, which is responsible for
 * initiating journal changes when individual transactions are
 * committed to the database.  Periodically, the Ganymede server's
 * {@link arlut.csd.ganymede.server.GanymedeScheduler GanymedeScheduler} task
 * engine will schedule a full {@link
 * arlut.csd.ganymede.server.DBStore#dump(java.lang.String,boolean,boolean)
 * dump} to consolidate the journal and update the on-disk database
 * file.  The server will also do a dump when the server's admin
 * console {@link arlut.csd.ganymede.server.GanymedeAdmin GanymedeAdmin}
 * interface initiates a server shutdown.
 *
 * DBStore was originally written with the intent of being able to serve as
 * a stand-alone in-process transactional object storage system, but in practice, DBStore
 * is now thoroughly integrated with the rest of the Ganymede server, and particularly
 * with {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}.  Various
 * component classes ({@link arlut.csd.ganymede.server.DBSession DBSession},
 * {@link arlut.csd.ganymede.server.DBObject DBObject}, and
 * {@link arlut.csd.ganymede.server.DBField DBField}), assume that there is usually
 * an associated GanymedeSession to be consulted for permissions and the like.
 *
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT 
 */

public final class DBStore implements JythonMap {

  // type identifiers used in the object store

  /**
   * All ganymede.db files will start with this string
   */

  static final String id_string = "Gstore";

  /**
   * Major file version id.. will be first byte in ganymede.db file
   * after id_string
   */

  static final byte major_version = 2;

  /**
   * Minor file version id.. will be second byte in ganymede.db file
   * after id_string
   */

  static final byte minor_version = 20;

  /**
   * Enable/disable debug in the DBStore methods
   */

  static boolean debug = false;

  /**
   * We're going to just have a singleton
   */

  static DBStore db = null;

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBStore");

  /**
   * Translated string for normal operation.
   *
   * One of the values that can be shown as 'Server State' on the
   * admin console.
   */

  public static final String normal_state = ts.l("init.okaystate"); // "Normal Operation"

  /**
   * Monotonically increasing transaction number.
   */

  private int transactionNumber = 0;

  /**
   * Object we're using as a monitor for atomically increasing
   * transactionNumber.
   */

  private Object transactionNumberLock = new Object();

  /**
   * Convenience function to find and return objects from the database
   * without having to go through the GanymedeSession and DBSession layers.
   *
   * This method provides a direct reference to an object in the DBStore,
   * without exporting it for remote RMI reference and without doing any
   * permissions setting.
   */

  public static DBObject viewDBObject(Invid invid)
  {
    if (DBStore.db == null)
      {
	throw new IllegalStateException(ts.l("global.notinit")); // "DBStore not initialized"
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
   * Convenience synchronized function to set the singleton DBStore
   * db member in the DBStore class.
   */

  private static synchronized void setDBSingleton(DBStore store)
  {
    if (DBStore.db != null)
      {
	throw new IllegalStateException(ts.l("setDBSingleton.exception")); // "DBStore already created"
      }

    DBStore.db = store;
  }

  /* --- */

  /*
    All of the following should only be modified/accessed
    in a critical section synchronized on the DBStore object.
   */
  
  /**
   * hash mapping object type to DBObjectBase's
   */
	
  Hashtable<Short,DBObjectBase> objectBases;

  /** 
   * Tracks invids which point to specific objects via asymmetric
   * links in the Ganymede persistent data store.
   */

  DBLinkTracker aSymLinkTracker;

  /** 
   * A collection of {@link arlut.csd.ganymede.server.DBNameSpace
   * DBNameSpaces} registered in this DBStore.  
   */

  Vector<DBNameSpace> nameSpaces;

  /** 
   * if true, {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
   * set methods will be enabled 
   */

  private boolean loading = false;

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

  /**
   * We use this boolean to work around a design flaw in password
   * field storage in DBStore revision 2.19.
   */

  public boolean journalLoading = false;

  /**
   * A count of how many database objects in this DBStore are currently
   * checked out for creation, editing, or removal.
   */

  int objectsCheckedOut = 0;

  /* -- */

  /**
   * This is the constructor for DBStore.
   *
   * Currently, once you construct a DBStore object, all you can do to
   * initialize it is call load().  This API needs to be extended to
   * provide for programmatic bootstrapping, or another tool needs
   * to be produced for the purpose.
   */

  public DBStore()
  {
    debug = Ganymede.debug;

    objectBases = new Hashtable<Short, DBObjectBase>(20); // default 
    aSymLinkTracker = new DBLinkTracker();
    nameSpaces = new Vector<DBNameSpace>();

    try
      {
	rootCategory = new DBBaseCategory(this, ts.l("init.rootcategory"));  // "Categories"
      }
    catch (RemoteException ex)
      {
	// we shouldn't ever get here unless there is something wrong
	// with the RMI system or the Java environment

	// "Caught DBBaseCategory exception: {0}"
	System.err.println(ts.l("init.exception", ex.getMessage()));

	Ganymede.logError(ex);

	// "Couldn''t initialize rootCategory"
	throw new Error(ts.l("init.error")); // XXX wrap earlier exception if we are JDK 1.4+ only?
      }

    // set the static pointer after we have at least created the basic
    // data structures.

    setDBSingleton(this);

    GanymedeAdmin.setState(DBStore.normal_state); // "Normal Operation"
  }

  /**
   * This method returns true if the disk file being loaded by this DBStore
   * has a version number greater than major.minor.
   */

  public boolean isAtLeast(int major, int minor)
  {
    return (this.file_major > major || 
	    (this.file_major == major && this.file_minor >= minor));
  }

  /**
   * This method returns true if the disk file being loaded by this DBStore
   * has a version number earlier than major.minor.
   */

  public boolean isLessThan(int major, int minor)
  {
    return (this.file_major < major || 
	    (this.file_major == major && this.file_minor < minor));
  }

  /**
   * This method returns true if the disk file being loaded by this DBStore
   * has a version number equal to major.minor.
   */

  public boolean isAtRev(int major, int minor)
  {
    return (this.file_major == major && this.file_minor == minor);
  }

  /**
   * This method returns true if the disk file being loaded by this
   * DBStore has a version number between greater than or equal to
   * major1.minor1 and less than major2.minor2.
   */

  public boolean isBetweenRevs(int major1, int minor1, int major2, int minor2)
  {
    if (this.file_major == major1 && this.file_minor >= minor1)
      {
	return true;
      }

    if (this.file_major == major2 && this.file_minor < minor2)
      {
	return true;
      }

    if (this.file_major > major1 && this.file_major < major2)
      {
	return true;
      }

    return false;
  }

  /**
   * Returns true if we're in the middle of loading our database from
   * disk.
   */

  public boolean isLoading()
  {
    return loading;
  }

  /**
   * Load the database from disk.
   *
   * This method loads both the database type definition and database
   * contents from a single disk file.
   *
   * @param filename Name of the database file
   * @see arlut.csd.ganymede.server.DBJournal
   */

  public void load(String filename)
  {
    load(filename, true);
  }

  /**
   * Load the database from disk.
   *
   * This method loads both the database type definition and database
   * contents from a single disk file.
   *
   * @param filename Name of the database file
   * @param loadJournal if true, process and consolidate the journal
   * on loading.
   * @see arlut.csd.ganymede.server.DBJournal
   */

  public synchronized void load(String filename, boolean loadJournal)
  {
    FileInputStream inStream = null;
    BufferedInputStream bufStream = null;
    DataInputStream in;

    DBObjectBase tempBase;
    short baseCount, namespaceCount;
    int invidPoolSize;
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
		System.err.println(ts.l("load.versionfail", filename));
		throw new RuntimeException(ts.l("load.initerror", filename));
	      }
	  }
	catch (IOException ex)
	  {
 	    System.err.println("DBStore initialization error: DBStore id read failure for " + filename);
	    System.err.println("IOException: ");
	    Ganymede.logError(ex);

	    throw new RuntimeException(ts.l("load.initerror", filename));
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

	// at version 2.8, we started tracking monotonic transaction
	// id numbers

	if (this.isAtLeast(2, 8))
	  {
	    transactionNumber = in.readInt();
	  }

	// at version 2.9, we started recording the size of our invid
	// pool

	if (this.isAtLeast(2, 9))
	  {
	    invidPoolSize = in.readInt();
	  }
	else
	  {
	    invidPoolSize = -1;
	  }

	if (invidPoolSize != -1)
	  {
	    Invid.setAllocator(new InvidPool(invidPoolSize*2 + 1)); // let's make sure we don't immediately rehash
	  }
	else
	  {
	    Invid.setAllocator(new InvidPool());
	  }

	// read in the namespace definitions

	namespaceCount = in.readShort();

	if (debug)
	  {
	    // "DBStore.load(): loading {0,number,#} namespaces"
	    System.err.println(ts.l("load.namespaces", Integer.valueOf(namespaceCount)));
	  }

	for (int i = 0; i < namespaceCount; i++)
	  {
	    nameSpaces.addElement(new DBNameSpace(in));
	  }

	// read in the object categories

	if (debug)
	  {
	    // "DBStore.load(): loading category definitions"
	    System.err.println(ts.l("load.categories"));
	  }

	DBField.fieldCount = 0;
	DBObject.objectCount = 0;

	if (isAtLeast(1,3))
	  {
	    rootCategory = new DBBaseCategory(this, in);
	  }
	
	// previous to 2.0, we wrote out the DBObjectBase structures
	// to ganymede.db as a separate step from the category loading

	if (isLessThan(2,0))
	  {
	    baseCount = in.readShort();

	    if (debug)
	      {
		System.err.println("DBStore load(): loading " + baseCount + " bases");
	      }

	    if (baseCount > 0)
	      {
		objectBases = new Hashtable<Short, DBObjectBase>(baseCount);
	      }
	    else
	      {
		objectBases = new Hashtable<Short, DBObjectBase>();
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

	// print out loading statistics

	if (debug)
	  {
	    // "DBStore.load(): Loaded {0,number,#} fields in {1,number,#} objects"
	    System.err.println(ts.l("load.statistics", Integer.valueOf(DBField.fieldCount), Integer.valueOf(DBObject.objectCount)));
	  }

	// make sure that we have the new object bases and fields
	// that have come into use since Ganymede 1.0.12.

	verifySchema2_0();

	// make sure that all object bases have namespace-constrained
	// label fields.  This is a new requirement with Ganymede 2.0
	// (DBStore rev 2.11), and not one that we can really automate
	// in any way.  So we just warn the admin and hope that they
	// start up the schema editor so that we can force them to fix
	// it.

	if (!verify_label_fields())
	  {
	    /*
	     *
	     *
	     * WARNING: DBStore load error: one or more object bases are missing
	     *          namespace constrained label fields.
	     *
	     *          Ganymede 2.0 now requires all object types to have
	     *          namespace-constrained label fields.
	     *
	     *          You MUST edit the schema before proceeding and define label fields
	     *          for all object types, or else Ganymede will behave unreliably.
	     *
	     */

	    System.err.println(ts.l("load.missing_labels"));
	  }
      }
    catch (IOException ex)
      {
	System.err.println("DBStore initialization error: couldn't properly process " + filename);
	System.err.println("IOException: " + ex);
	System.err.println("Stack Trace: " + Ganymede.stackTrace(ex));
	throw new RuntimeException(ts.l("load.initerror", filename));
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

	    Ganymede.logError(ex);

	    throw new RuntimeException("couldn't initialize journal:" + ex.getMessage());
	  }

	if (!journal.isClean())
	  {
	    try
	      {
		journalLoading = true;

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

		for (DBObjectBase base: objectBases.values())
		  {
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

		Ganymede.logError(ex);
		throw new RuntimeException("couldn't load journal");
	      }
	    catch (InterruptedException ex)
	      {
		// we got interrupted while waiting to lock
		// the database.. unlikely in the extreme here.

		Ganymede.logError(ex);
		throw new RuntimeException("couldn't dump journal");
	      }
	    finally
	      {
		journalLoading = false;
	      }
	  }
      }

    loading = false;
  }

  /**
   * Dumps the database to disk
   *
   * This method dumps the entire database to disk.  The thread that calls the
   * dump method will be suspended until there are no threads performing update
   * writes to the in-memory database.  In practice this will likely never be
   * a long interval.  Note that this method *will* dump the database, even
   * if no changes have been made.  You should check the DBStore journal's 
   * isClean() method to determine whether or not a dump is really needed,
   * if you're not sure.
   *
   * The dump is guaranteed to be transaction consistent.
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
   * @see arlut.csd.ganymede.server.DBEditSet
   * @see arlut.csd.ganymede.server.DBJournal
   */

  public synchronized void dump(String filename, boolean releaseLock,
				boolean archiveIt) throws IOException, InterruptedException
  {
    File dbFile = null;
    FileOutputStream outStream = null;
    BufferedOutputStream bufStream = null;
    DataOutputStream out = null;
    
    short namespaceCount;
    DBDumpLock lock = null;
    DBNameSpace ns;
    
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

    lock.establish("System");	// wait until we get our lock 

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

                String oldDirName = directoryName + File.separator + "old";

		File oldDirectory = new File(oldDirName);
		    
		if (!oldDirectory.exists())
		  {
		    if (!oldDirectory.mkdir())
                      {
                        throw new IOException("Couldn't mkdir " + oldDirName);
                      }
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

	out.writeInt(transactionNumber); // version 2.8
	out.writeInt(Invid.getCount());	// version 2.9

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

	if (dbFile.exists())
	  {
	    // ok, we've successfully dumped to ganymede.db.new.. move
	    // the old file to ganymede.db.bak

	    if (!dbFile.renameTo(new File(filename + ".bak")))
	      {
		throw new IOException("Couldn't rename " + filename + " to " + filename + ".bak");
	      }
	  }

	// and move ganymede.db.new to ganymede.db.. note that we do
	// have a very slight vulnerability here if we are interrupted
	// between the above rename and this one.  If this happens,
	// the Ganymede manager will have to manually move
	// ganymede.db.new to ganymede.db before starting the server.

	// In all other circumstances, the server will be able to come
	// up and handle things cleanly without loss of data.

	if (debug)
	  {
	    System.err.println("DBStore: renaming new db");
	  }

        File newFile = new File(filename + ".new");

        if (!newFile.renameTo(dbFile))
          {
            throw new IOException("Couldn't rename " + filename + ".new to " + filename);
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
      }

    // if our thread was terminated above, we won't get here.  If
    // we've got here, we had an ok dump, and we don't need to
    // worry about the journal.. if it isn't truncated properly
    // somebody can just remove it and ganymede will recover
    // ok.
    
    if (journal != null)
      {
	journal.reset();	// ** sync **
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
   * Dumps the database to disk as an XML file
   *
   * This method dumps the entire database to disk.  The thread that calls the
   * dump method will be suspended until there are no threads performing update
   * writes to the in-memory database.  In practice this will likely never be
   * a long interval.  Note that this method *will* dump the database, even
   * if no changes have been made.  You should check the DBStore journal's 
   * isClean() method to determine whether or not a dump is really needed,
   * if you're not sure.
   *
   * The dump is guaranteed to be transaction consistent.
   *
   * @param filename Name of the database file to emit
   *
   * @param dumpDataObjects If true, the emitted file will include
   * the objects in the Ganymede database.
   *
   * @param dumpSchema If true, the emitted file will include the
   * schema definition
   *
   * @param syncChannel The name of the sync channel whose constraints
   * we want to apply to this dump.  May be null if the client wants
   * an unfiltered dump.
   *
   * @param includeHistory If true, the historical fields (creation
   * date & info, last modification date & info) will be included in
   * the xml stream.
   *
   * @see arlut.csd.ganymede.server.DBEditSet
   * @see arlut.csd.ganymede.server.DBJournal
   */

  public void dumpXML(String filename, boolean dumpDataObjects, boolean dumpSchema, 
		      String syncChannel, boolean includeHistory, boolean includeOid) throws IOException
  {
    FileOutputStream outStream = null;
    BufferedOutputStream bufStream = null;

    /* -- */

    outStream = new FileOutputStream(filename);
    bufStream = new BufferedOutputStream(outStream);

    this.dumpXML(bufStream, dumpDataObjects, dumpSchema, syncChannel, includeHistory, includeOid);
  }

  /**
   * Dumps the database and/or database schema to an OutputStream
   * as an XML file
   *
   * This method dumps the entire database to the OutputStream.
   * The thread that calls the dump method will be suspended until
   * there are no threads performing update writes to the in-memory
   * database.  In practice this will likely never be a long interval.
   * Note that this method *will* dump the database, even if no
   * changes have been made.  You should check the DBStore journal's
   * isClean() method to determine whether or not a dump is really
   * needed, if you're not sure.
   *
   * The dump is guaranteed to be transaction consistent.
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
   * @param syncChannel The name of the sync channel whose constraints
   * we want to apply to this dump.  May be null if the client wants
   * an unfiltered dump.
   *
   * @param includeHistory If true, the historical fields (creation
   * date & info, last modification date & info) will be included in
   * the xml stream.
   *
   * @param includeOid If true, the objects written out to the xml
   * stream will include an "oid" attribute which contains the precise
   * Invid of the object.
   *
   * @see arlut.csd.ganymede.server.DBEditSet
   * @see arlut.csd.ganymede.server.DBJournal
   */

  public synchronized void dumpXML(OutputStream outStream, boolean dumpDataObjects, boolean dumpSchema, String syncChannel,
				   boolean includeHistory, boolean includeOid) throws IOException
  {
    XMLDumpContext xmlOut = null;
    
    DBDumpLock lock = null;
    DBNameSpace ns;
    SyncRunner syncConstraint = null;
    boolean includePlaintext = false;

    /* -- */
    
    if (debug)
      {
	System.err.println("DBStore: Dumping XML");
      }

    try
      {
	if (!dumpDataObjects && !dumpSchema)
	  {
	    // "One of dumpDataObjects and dumpSchema must be true."
	    throw new IllegalArgumentException(ts.l("dumpXML.doNothing"));
	  }

	xmlOut = new XMLDumpContext(new UTF8XMLWriter(outStream, UTF8XMLWriter.MINIMIZE_EMPTY_ELEMENTS),
				    includePlaintext,
				    includeHistory,
				    syncConstraint,
				    includeOid);

	// we don't need a lock if we're just dumping the schema,
	// because the GanymedeServer loginSemaphore will keep the
	// schema from changing under us.

	if (!dumpDataObjects)
	  {
	    dumpSchemaXML(xmlOut);
	    return;
	  }

	// we must be dumping objects, possibly with schema data as well

	xmlOut.setDumpPasswords(true); // we're doing a full dump, so passwords are allowed

	lock = new DBDumpLock(this);

	try
	  {
	    lock.establish("System");	// wait until we get our lock 
	  }
	catch (InterruptedException ex)
	  {
	    // "DBStore.dumpXML(): Interrupted waiting for dump lock:\n\n{0}"
	    Ganymede.debug(ts.l("dumpXML.interrupted", Ganymede.stackTrace(ex)));
	    throw new RuntimeException(ex);
	  }

	if (false)
	  {
	    System.err.println("DBStore.dumpXML(): got dump lock");
	  }

	if (dumpDataObjects && !dumpSchema && syncChannel != null)
	  {
	    syncConstraint = Ganymede.getSyncChannel(syncChannel);
	    
	    if (syncConstraint == null)
	      {
		// "No such sync channel defined: {0}"
		throw new IllegalArgumentException(ts.l("dumpXML.badSyncChannel", syncChannel));
	      }
	    else
	      {
		includePlaintext = syncConstraint.includePlaintextPasswords();
	      }
	  }

	if (false)
	  {
	    System.err.println("DBStore.dumpXML(): created XMLDumpContext");
	  }
	    
	// start writing
	    
	xmlOut.startElement("ganymede");
	xmlOut.attribute("major", Integer.toString(GanymedeXMLSession.majorVersion));
	xmlOut.attribute("minor", Integer.toString(GanymedeXMLSession.minorVersion));

	xmlOut.indentOut();

	if (dumpSchema)
	  {
	    dumpSchemaXML(xmlOut);
	  }

	xmlOut.startElementIndent("ganydata");
	xmlOut.indentOut();

	for (DBObjectBase base: getBases())
	  {
	    if (base.isEmbedded())
	      {
		continue;
	      }
		
	    for (DBObject x: base.getObjects())
	      {
		if (xmlOut.mayInclude(x))
		  {
		    x.emitXML(xmlOut);
		  }
	      }
	  }

	xmlOut.indentIn();
	xmlOut.endElementIndent("ganydata");

	xmlOut.indentIn();
	xmlOut.endElementIndent("ganymede");

	xmlOut.write("\n");
	xmlOut.close();
	xmlOut = null;
      }
    finally
      {
	if (false)
	  {
	    System.err.println("DBStore.dumpXML(): finally!");
	  }

	if (lock != null)
	  {
	    lock.release();
	  }
	    
	if (xmlOut != null)
	  {
	    try
	      {
		xmlOut.close();
	      }
	    catch (IOException ex)
	      {
		Ganymede.logError(ex);
	      }
	  }
	
	if (outStream != null)
	  {
	    try
	      {
		outStream.close();
	      }
	    catch (IOException ex)
	      {
		Ganymede.logError(ex);
	      }
	  }
      }
  }

  /**
   * Dumps the schema definition to xmlOut.
   */

  public synchronized void dumpSchemaXML(XMLDumpContext xmlOut) throws IOException
  {
    xmlOut.startElementIndent("ganyschema");
	    
    xmlOut.indentOut();
    xmlOut.startElementIndent("namespaces");
	    
    xmlOut.indentOut();
	    
    for (DBNameSpace ns: nameSpaces)
      {
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

  /**
   * Returns a vector of Strings, the names of the bases currently
   * defined in this DBStore.
   */

  public Vector<String> getBaseNameList()
  {
    Vector<String> result = new Vector<String>();

    /* -- */

    synchronized (objectBases)
      {
	for (DBObjectBase base: objectBases.values())
	  {
	    result.add(base.getName());
	  }
      }
    
    return result;
  }

  /**
   * Returns a Vector of {@link arlut.csd.ganymede.server.DBObjectBase
   * DBObjectBases} currently defined in this DBStore.
   */

  public Vector<DBObjectBase> getBases()
  {
    Vector<DBObjectBase> result;

    synchronized (objectBases)
      {
	result = new Vector<DBObjectBase>(objectBases.values());
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
    return objectBases.get(id);
  }

  /**
   * Returns the object definition class for the id class.
   *
   * @param id Type id for the base to be returned
   */

  public DBObjectBase getObjectBase(short id)
  {
    return objectBases.get(Short.valueOf(id));
  }

  /**
   * Returns the object definition class for the id class.
   *
   * @param baseName Name of the base to be returned
   */

  public DBObjectBase getObjectBase(String baseName)
  {
    synchronized (objectBases)
      {
	for (DBObjectBase base: objectBases.values())
	  {
	    if (base.getName().equalsIgnoreCase(baseName))
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
    base.setEditingMode(DBObjectBase.EditingMode.LOCKED);

    objectBases.put(base.getKey(), base);
  }

  /**
   * Increments and returns the server's monotonic
   * transactionNumber.
   */

  public int getNextTransactionNumber()
  {
    synchronized (transactionNumberLock)
      {
	return ++transactionNumber;
      }
  }

  /**
   * Returns the most recent transaction number allocated.
   */

  public int getTransactionNumber()
  {
    return transactionNumber;
  }

  /**
   * This method is used by the
   * {@link arlut.csd.ganymede.server.DBJournal#undoTransaction(arlut.csd.ganymede.server.DBJournalTransaction)}
   * method to put back a transaction number when it is undone.
   */

  public void undoNextTransactionNumber(int number)
  {
    synchronized (transactionNumberLock)
      {
	if (number == transactionNumber)
	  {
	    --transactionNumber;
	  }
      }
  }

  /**
   * This method is used when reading journal entries to
   * bump up the transaction number.  If the nextNumber provided
   * isn't actually the next number in our transaction sequence,
   * we'll throw an IntegrityConstraintException.
   */

  public void updateTransactionNumber(int nextNumber) throws IntegrityConstraintException
  {
    synchronized (transactionNumberLock)
      {
	if (transactionNumber > 0 && nextNumber != transactionNumber + 1)
	  {
	    if (nextNumber == transactionNumber)
	      {
		// Inconsistent transaction number ({0}) detected while reading transaction from journal, expected {1}.
		// This probably means that the server was terminated in the middle of a dump process,
		// and that the journal file is older than the ganymede.db file.  You should be able to
		// remove the journal file and restart the server.

		throw new IntegrityConstraintException(ts.l("updateTransactionNumber.lingeringJournal", Integer.valueOf(nextNumber), Integer.valueOf(transactionNumber+1)));
	      }
	    else
	      {
		// "Inconsistent transaction number ({0}) detected while reading transaction from journal, expected {1}.  Throwing up."
		throw new IntegrityConstraintException(ts.l("updateTransactionNumber.badnumber", Integer.valueOf(nextNumber), Integer.valueOf(transactionNumber+1)));
	      }
	  }

	transactionNumber = nextNumber;
      }
  }

  /**
   * Method to obtain the object from the DBStore represented by the
   * given Invid.
   *
   * NOTE: this method will give you a direct reference to the actual
   * DBObject, not a clone. So be careful, and treat the returned
   * objects as "read only".
   */

  public DBObject getObject(Invid invid)
  {
    return getObjectBase(invid.getType()).getObject(invid.getNum());
  }

  /**
   *
   * Method to locate a registered namespace by name.
   *
   */

  public DBNameSpace getNameSpace(String name)
  {
    synchronized (nameSpaces)
      {
	for (DBNameSpace namespace: nameSpaces)
	  {
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
		    throw new RuntimeException("Found unknown/null thing in category tree.." + cn.toString());
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
   * Initialization method for a newly created DBStore.. this
   * method creates a new Schema from scratch, defining the
   * mandatory Ganymede object types, registering their customization
   * classes, defining fields, and all the rest.
   *
   * Note that we don't go through a 
   * {@link arlut.csd.ganymede.server.DBSchemaEdit DBSchemaEdit}
   * here, we just initialize the {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}/
   * {@link arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField} structures
   * manually.
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

	ns = new DBNameSpace("rolespace", true);
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
	b.setName("Owner Group");
	b.setClassInfo("arlut.csd.ganymede.server.ownerCustom", null);
	b.setTypeID(SchemaConstants.OwnerBase); // 0

	permCategory.addNodeAfter(b, null);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.OwnerNameField);
	bf.setType(FieldType.STRING);
	bf.setName("Name");
	bf.setNameSpace("ownerbase");
	bf.setComment("The name of this ownership group");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.OwnerMembersField);
	bf.setType(FieldType.INVID);
	bf.setName("Members");
	bf.setArray(true);
	bf.setTargetBase(SchemaConstants.PersonaBase);
	bf.setTargetField(SchemaConstants.PersonaGroupsField);
	bf.setComment("List of admin personae that are members of this owner set");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.OwnerCcAdmins);
	bf.setType(FieldType.BOOLEAN);
	bf.setName("Cc All Admins");
	bf.setComment("If checked, mail to this owner group will be sent to the admins");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.OwnerExternalMail);
	bf.setType(FieldType.STRING);
	bf.setName("External Mail List");
	bf.setArray(true);
	bf.setComment("What external email addresses should be notified of changes to objects owned?");
	b.addFieldToEnd(bf);

	/*  XXX As of DBStore 2.7, we don't use this field any more
	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.OwnerObjectsOwned);
	bf.setType(FieldType.INVID);
	bf.setName("Objects owned");
	bf.setTargetBase(-2);	// any
	bf.setTargetField(SchemaConstants.OwnerListField);
	bf.setArray(true);
	bf.setComment("What objects are owned by this owner set");
	b.addFieldToEnd(bf);
	*/

	b.setLabelField(SchemaConstants.OwnerNameField);

	// link in the class we specified

	b.createHook();

	// and link in the base to this DBStore

	setBase(b);

	// create persona base

	b = new DBObjectBase(this, false);
	b.setName("Admin Persona");
	b.setClassInfo("arlut.csd.ganymede.server.adminPersonaCustom", null);
	b.setTypeID(SchemaConstants.PersonaBase); // 1

	permCategory.addNodeAfter(b, null); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.PersonaNameField);
	bf.setType(FieldType.STRING);
	bf.setName("Name");
	bf.setComment("Descriptive label for this admin persona");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.PersonaPasswordField);
	bf.setType(FieldType.PASSWORD);
	bf.setName("Password");
	bf.setMaxLength((short)32);
	bf.setCrypted(false);
	bf.setShaUnixCrypted(true);
	bf.setShaUnixCrypted512(true);
	bf.setComment("Persona password");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.PersonaGroupsField);
	bf.setType(FieldType.INVID);
	bf.setName("Owner Sets");
	bf.setTargetBase(SchemaConstants.OwnerBase);
	bf.setTargetField(SchemaConstants.OwnerMembersField);
	bf.setArray(true);
	bf.setComment("What owner sets are this persona members of?");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.PersonaAssocUser);
	bf.setType(FieldType.INVID);
	bf.setName("User");
	bf.setTargetBase(SchemaConstants.UserBase);
	bf.setTargetField(SchemaConstants.UserAdminPersonae);
	bf.setArray(false);
	bf.setComment("What user is this admin persona associated with?");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.PersonaPrivs);
	bf.setType(FieldType.INVID);
	bf.setName("Privilege Sets");
	bf.setTargetBase(SchemaConstants.RoleBase);
	bf.setTargetField(SchemaConstants.RolePersonae);
	bf.setArray(true);
	bf.setComment("What permission matrices are this admin persona associated with?");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.PersonaAdminConsole);
	bf.setType(FieldType.BOOLEAN);
	bf.setName("Admin Console");
	bf.setArray(false);
	bf.setComment("If true, this persona can be used to access the admin console");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.PersonaAdminPower);
	bf.setType(FieldType.BOOLEAN);
	bf.setName("Full Console");
	bf.setArray(false);
	bf.setComment("If true, this persona can kill users and edit the schema");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.PersonaMailAddr);
	bf.setType(FieldType.STRING);
	bf.setName("Email Address");
	bf.setArray(false);
	bf.setComment("Where email to this administrator should be sent");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.PersonaLabelField);
	bf.setType(FieldType.STRING);
	bf.setName("Label");
	bf.setArray(false);
	bf.setNameSpace("persona");
	b.addFieldToEnd(bf);

	b.setLabelField(SchemaConstants.PersonaLabelField);

	// link in the class we specified

	b.createHook();

	// and link in the base to this DBStore

	setBase(b);

	// create Role base

	b = new DBObjectBase(this, false);
	b.setName("Role");
	b.setClassInfo("arlut.csd.ganymede.server.permCustom", null);
	b.setTypeID(SchemaConstants.RoleBase); // 2

	permCategory.addNodeAfter(b, null); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.RoleName);
	bf.setType(FieldType.STRING);
	bf.setName("Name");
	bf.setNameSpace("rolespace");
	bf.setComment("The name of this permission matrix");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.RoleDelegatable);
	bf.setType(FieldType.BOOLEAN);
	bf.setName("Delegatable Role");
	bf.setComment("If true, this role can be granted to admins created/edited by Personae with this role.");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.RoleMatrix);
	bf.setType(FieldType.PERMISSIONMATRIX);
	bf.setName("Objects Owned Access Bits");
	bf.setComment("Access bits, by object type for objects owned by admins using this permission object");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.RoleDefaultMatrix);
	bf.setType(FieldType.PERMISSIONMATRIX);
	bf.setName("Default Access Bits");
	bf.setComment("Access bits, by object type for all objects on the part of admins using this permission object");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.RolePersonae);
	bf.setType(FieldType.INVID);
	bf.setName("Persona entities");
	bf.setTargetBase(SchemaConstants.PersonaBase);
	bf.setTargetField(SchemaConstants.PersonaPrivs);
	bf.setArray(true);
	bf.setComment("What personae are using this permission matrix?");
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
	b.setName("System Event");
	b.setClassInfo("arlut.csd.ganymede.server.eventCustom", null);
	b.setTypeID(SchemaConstants.EventBase);

	eventCategory.addNodeAfter(b, null); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.EventToken);
	bf.setType(FieldType.STRING);
	bf.setName("Event Token");
	bf.setBadChars(" :");
	bf.setNameSpace("eventtoken");
	bf.setComment("Single-word token to identify this event type in Ganymede source code");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.EventName);
	bf.setType(FieldType.STRING);
	bf.setName("Event Name");
	bf.setBadChars(":");
	bf.setComment("Short name for this event class, suitable for an email message title");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.EventDescription);
	bf.setType(FieldType.STRING);
	bf.setName("Event Description");
	bf.setBadChars(":");
	bf.setComment("Fuller description for this event class, suitable for an email message body");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.EventMailBoolean);
	bf.setType(FieldType.BOOLEAN);
	bf.setName("Send Mail");
	bf.setComment("If true, occurrences of this event will be emailed");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.EventMailToSelf);
	bf.setType(FieldType.BOOLEAN);
	bf.setName("Cc Admin");
	bf.setComment("If true, mail for this event will always be cc'ed to the admin performing the action");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.EventMailOwners);
	bf.setType(FieldType.BOOLEAN);
	bf.setName("Cc Owners");
	bf.setComment("If true, mail for this event will always be cc'ed to administrators in the owner group");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.EventExternalMail);
	bf.setType(FieldType.STRING);
	bf.setName("Mail List");
	bf.setArray(true);
	bf.setComment("List of email addresses to send this event to");
	b.addFieldToEnd(bf);

	b.setLabelField(SchemaConstants.EventToken);
    
	// link in the class we specified

	b.createHook();

	// and link in the base to this DBStore

	setBase(b);

	// create Object Events base

	b = new DBObjectBase(this, false);
	b.setName("Object Event");
	b.setClassInfo("arlut.csd.ganymede.server.objectEventCustom", null);
	b.setTypeID(SchemaConstants.ObjectEventBase);

	eventCategory.addNodeAfter(b, null); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.ObjectEventLabel);
	bf.setType(FieldType.STRING);
	bf.setName("Hidden Label");
	bf.setNameSpace("eventtoken");
	bf.setVisibility(false);	// hidden
	bf.setComment("Hidden composite label field.  The contents of this label field is automatically set from the Event Token and Object Type Name fields.");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.ObjectEventToken);
	bf.setType(FieldType.STRING);
	bf.setName("Event Token");
	bf.setBadChars(" :");
	bf.setComment("Single-word token to identify this event type in Ganymede source code");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.ObjectEventObjectName);
	bf.setType(FieldType.STRING);
	bf.setName("Object Type Name");
	bf.setComment("The name of the object that this event is tracking");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.ObjectEventName);
	bf.setType(FieldType.STRING);
	bf.setName("Event Name");
	bf.setBadChars(":");
	bf.setComment("Short name for this event class, suitable for an email message title");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.ObjectEventDescription);
	bf.setType(FieldType.STRING);
	bf.setName("Event Description");
	bf.setBadChars(":");
	bf.setComment("Fuller description for this event class, suitable for an email message body");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.ObjectEventMailToSelf);
	bf.setType(FieldType.BOOLEAN);
	bf.setName("Cc Admin");
	bf.setComment("If true, mail for this event will always be cc'ed to the admin performing the action");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.ObjectEventMailOwners);
	bf.setType(FieldType.BOOLEAN);
	bf.setName("Cc Owner Groups");
	bf.setComment("If true, mail for this event will always be cc'ed to the owner groups owning the object");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.ObjectEventExternalMail);
	bf.setType(FieldType.STRING);
	bf.setName("External Email");
	bf.setArray(true);
	bf.setComment("Email addresses not stored in Ganymede");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.ObjectEventObjectType);
	bf.setType(FieldType.NUMERIC);
	bf.setName("Object Type ID");
	bf.setVisibility(false);	// we don't want this to be seen by the client
	bf.setComment("The type code of the object that this event is tracking");
	b.addFieldToEnd(bf);

	// set the label field

	b.setLabelField(SchemaConstants.ObjectEventLabel);

	// link in the class we specified

	b.createHook();

	// and link in the base to this DBStore

	setBase(b);

	// create user base

	DBBaseCategory userCategory = new DBBaseCategory(this, "User-Level Objects", rootCategory);
	rootCategory.addNodeAfter(userCategory, null);

	b = new DBObjectBase(this, false);
	b.setName("User");
	b.setTypeID(SchemaConstants.UserBase); // 2

	userCategory.addNodeAfter(b, null); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.UserUserName);
	bf.setType(FieldType.STRING);
	bf.setName("Username");
	bf.setMinLength((short)2);
	bf.setMaxLength((short)8);
	bf.setBadChars(" :=><|+[]\\/*;:.,?\""); // See p.252, teach yourself WinNT Server 4 in 14 days
	bf.setNameSpace("username");
	bf.setComment("User name for an individual privileged to log into Ganymede and/or the network");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.UserPassword);
	bf.setType(FieldType.PASSWORD);
	bf.setName("Password");
	bf.setMaxLength((short)32);
	bf.setCrypted(true);
	bf.setComment("Password for an individual privileged to log into Ganymede and/or the network");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.UserAdminPersonae);
	bf.setType(FieldType.INVID);
	bf.setTargetBase(SchemaConstants.PersonaBase);
	bf.setTargetField(SchemaConstants.PersonaAssocUser);
	bf.setName("Admin Personae");
	bf.setArray(true);
	bf.setComment("A list of admin personae this user can assume");
	b.addFieldToEnd(bf);

	b.setLabelField(SchemaConstants.UserUserName);
    
	setBase(b);

	// create Task Base

	b = new DBObjectBase(this, false);
	b.setName("Task");
	b.setClassInfo("arlut.csd.ganymede.server.taskCustom", null);
	b.setTypeID(SchemaConstants.TaskBase); // 5

	adminCategory.addNodeAfter(b, null); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.TaskName);
	bf.setType(FieldType.STRING);
	bf.setName("Task Name");
	bf.setNameSpace("buildertask");
	bf.setComment("Name of this task, as shown in task monitor");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.TaskClass);
	bf.setType(FieldType.STRING);
	bf.setName("Task Class");
	bf.setBadChars("/:");
	bf.setComment("Name of the plug-in class to load on server restart to handle this task");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.TaskRunOnCommit);
	bf.setType(FieldType.BOOLEAN);
	bf.setName("Run On Transaction Commit");
	bf.setComment("If true, this task will be run on transaction commit");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.TaskRunPeriodically);
	bf.setType(FieldType.BOOLEAN);
	bf.setName("Run Periodically");
	bf.setComment("If true, this task will be scheduled for periodic execution");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.TaskPeriodUnit);
	bf.setType(FieldType.STRING);
	bf.setName("Period Unit");
	bf.setComment("What is the unit of time we're using?");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.TaskPeriodCount);
	bf.setType(FieldType.NUMERIC);
	bf.setName("Period Count");
	bf.setComment("How many time units between task runs?");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.TaskPeriodAnchor);
	bf.setType(FieldType.DATE);
	bf.setName("Period Anchor");
	bf.setComment("When do we start counting period intervals from?");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.TaskOptionStrings);
	bf.setType(FieldType.STRING);
	bf.setArray(true);
	bf.setName("Option Strings");
	bf.setComment("Optional task parameters, interpreted by specific tasks if needed");
	b.addFieldToEnd(bf);

	b.setLabelField(SchemaConstants.TaskName);

    	// link in the class we specified

	b.createHook();

	// and record the base

	setBase(b);

	// create Sync Channel Base

	b = new DBObjectBase(this, false);
	b.setName("Sync Channel");
	b.setClassInfo("arlut.csd.ganymede.server.syncChannelCustom", null);
	b.setTypeID(SchemaConstants.SyncChannelBase); // 7

	adminCategory.addNodeAfter(b, null); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.SyncChannelName);
	bf.setType(FieldType.STRING);
	bf.setName("Name");
	bf.setNameSpace("buildertask");	// same namespace as tasks
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.SyncChannelTypeString);
	bf.setType(FieldType.STRING);
	bf.setName("Sync Channel Type");
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.SyncChannelClassName);
	bf.setType(FieldType.STRING);
	bf.setName("Sync Master Classname");
	bf.setComment(ts.l("initializeSchema.syncmaster_comment")); // "Added a descriptive comment for the new Sync Master Classname field in the Sync Channel object definition."
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.SyncChannelDirectory);
	bf.setType(FieldType.STRING);
	bf.setName("Queue Directory");
	bf.setComment(ts.l("initializeSchema.syncqueuedir_comment")); // "Location of the sync channel directory on disk."
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.SyncChannelFullStateFile);
	bf.setType(FieldType.STRING);
	bf.setName("Full State File");
	bf.setComment(ts.l("initializeSchema.syncfullstatefile_comment")); // "Path to the file to use for full-state XML dumps."
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.SyncChannelServicer);
	bf.setType(FieldType.STRING);
	bf.setName("Service Program");
	bf.setComment(ts.l("initializeSchema.syncprogram_comment")); // "The location of the program to service this sync channel."
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.SyncChannelFields);
	bf.setType(FieldType.FIELDOPTIONS);
	bf.setName("Sync Data");
	bf.setComment(ts.l("initializeSchema.syncdata_comment")); // "The definitions for what object and fields we want to include in this sync channel."
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.SyncChannelPlaintextOK);
	bf.setType(FieldType.BOOLEAN);
	bf.setName("Allow Plaintext Passwords");
	bf.setComment(ts.l("initializeSchema.syncplaintext_comment")); // "Allow Plaintext Passwords to be written to this Sync Channel."
	b.addFieldToEnd(bf);

	bf = new DBObjectBaseField(b);
	bf.setID(SchemaConstants.SyncChannelTypeNum);
	bf.setType(FieldType.NUMERIC);
	bf.setName("Channel Type Index");
	b.addFieldToEnd(bf);

	b.setLabelField(SchemaConstants.SyncChannelName);

    	// link in the class we specified

	b.createHook();

	// and record the base

	setBase(b);

	// and lock in the namespaces

	for (DBNameSpace namespace: nameSpaces)
	  {
	    namespace.schemaEditCommit();
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("remote :" + ex);
      }

    loading = false;
  }

  /**
   * This method is designed to transition from a Ganymede 1.0
   * database schema to a Ganymede 2.0 database schema, by adding the
   * new built-in object and fields that 1.0 lacked.
   */

  void verifySchema2_0()
  {
    DBObjectBase b = null;
    DBObjectBaseField bf = null;

    /* -- */

    try
      {
	if (getNameSpace("buildertask") == null)
	  {
	    Ganymede.debug("Adding buildertask name space");

	    // note!  we have had buildertask in the server DBStore
	    // since 1998.. if we're dealing with a ganymede.db file
	    // that doesn't have it, it must be old indeed!

	    DBNameSpace ns = new DBNameSpace("buildertask", true);
	    nameSpaces.addElement(ns);
	  }

	if (getObjectBase(SchemaConstants.SyncChannelBase) == null)
	  {
	    Ganymede.debug("Adding Sync Channel ObjectBase");

	    // create Sync Channel Base

	    b = new DBObjectBase(this, false);
	    b.setName("Sync Channel");
	    b.setClassInfo("arlut.csd.ganymede.server.syncChannelCustom", null);
	    b.setTypeID(SchemaConstants.SyncChannelBase); // 7

	    DBBaseCategory adminCategory = (DBBaseCategory) getCategoryNode("/Admin-Level Objects");

	    adminCategory.addNodeAfter(b, null); // add it to the end is ok

	    bf = new DBObjectBaseField(b);
	    bf.setID(SchemaConstants.SyncChannelName);
	    bf.setType(FieldType.STRING);
	    bf.setName("Name");
	    bf.setNameSpace("buildertask");	// same namespace as tasks
	    b.addFieldToEnd(bf);

	    bf = new DBObjectBaseField(b);
	    bf.setID(SchemaConstants.SyncChannelTypeString);
	    bf.setType(FieldType.STRING);
	    bf.setName("Sync Channel Type");
	    b.addFieldToEnd(bf);

	    bf = new DBObjectBaseField(b);
	    bf.setID(SchemaConstants.SyncChannelClassName);
	    bf.setType(FieldType.STRING);
	    bf.setName("Sync Master Classname");
	    b.addFieldToEnd(bf);

	    bf = new DBObjectBaseField(b);
	    bf.setID(SchemaConstants.SyncChannelDirectory);
	    bf.setType(FieldType.STRING);
	    bf.setName("Queue Directory");
	    bf.setComment("Location of the sync channel directory on disk");
	    b.addFieldToEnd(bf);

	    bf = new DBObjectBaseField(b);
	    bf.setID(SchemaConstants.SyncChannelFullStateFile);
	    bf.setType(FieldType.STRING);
	    bf.setName("Full State File");
	    bf.setComment("Path to the file to use for full-state XML dumps");
	    b.addFieldToEnd(bf);

	    bf = new DBObjectBaseField(b);
	    bf.setID(SchemaConstants.SyncChannelServicer);
	    bf.setType(FieldType.STRING);
	    bf.setName("Service Program");
	    bf.setComment("The location of the program to service this sync channel");
	    b.addFieldToEnd(bf);

	    bf = new DBObjectBaseField(b);
	    bf.setID(SchemaConstants.SyncChannelFields);
	    bf.setType(FieldType.FIELDOPTIONS);
	    bf.setName("Sync Data");
	    bf.setComment("The definitions for what object and fields we want to include in this sync channel");
	    b.addFieldToEnd(bf);

	    bf = new DBObjectBaseField(b);
	    bf.setID(SchemaConstants.SyncChannelPlaintextOK);
	    bf.setType(FieldType.BOOLEAN);
	    bf.setName("Allow Plaintext Passwords");
	    bf.setComment("Allow Plaintext Passwords to be written to this Sync Channel");
	    b.addFieldToEnd(bf);

	    bf = new DBObjectBaseField(b);
	    bf.setID(SchemaConstants.SyncChannelTypeNum);
	    bf.setType(FieldType.NUMERIC);
	    bf.setName("Channel Type Index");
	    b.addFieldToEnd(bf);

	    b.setLabelField(SchemaConstants.SyncChannelName);

	    // link in the class we specified

	    b.createHook();

	    setBase(b);
	  }
	else
	  {
	    // we added the SyncChannelTypeString, SyncChannelTypeNum
	    // and SyncChannelFullStateFile fields after releasing a
	    // version without, check to see if we need to add them

	    // note that we redefined the SyncChannel type to have
	    // these fields in DBStore version 2.11.

	    DBObjectBase syncBase = (DBObjectBase) getObjectBase(SchemaConstants.SyncChannelBase);
	    syncBase.setEditingMode(DBObjectBase.EditingMode.LOADING);

	    try
	      {
		if (syncBase.getField(SchemaConstants.SyncChannelTypeString) == null)
		  {
		    bf = new DBObjectBaseField(syncBase);
		    bf.setID(SchemaConstants.SyncChannelTypeString);
		    bf.setType(FieldType.STRING);
		    bf.setName("Sync Channel Type");
		    syncBase.addFieldAfter(bf, SchemaConstants.SyncChannelName);
		  }

		if (syncBase.getField(SchemaConstants.SyncChannelClassName) == null)
		  {
		    bf = new DBObjectBaseField(syncBase);
		    bf.setID(SchemaConstants.SyncChannelClassName);
		    bf.setType(FieldType.STRING);
		    bf.setName("Sync Master Classname");
		    syncBase.addFieldAfter(bf, SchemaConstants.SyncChannelTypeString);
		  }

		if (syncBase.getField(SchemaConstants.SyncChannelFullStateFile) == null)
		  {
		    bf = new DBObjectBaseField(syncBase);
		    bf.setID(SchemaConstants.SyncChannelFullStateFile);
		    bf.setType(FieldType.STRING);
		    bf.setName("Full State File");
		    bf.setComment("Path to the file to use for full-state XML dumps");
		    syncBase.addFieldAfter(bf, SchemaConstants.SyncChannelDirectory);
		  }

		if (syncBase.getField(SchemaConstants.SyncChannelTypeNum) == null)
		  {
		    bf = new DBObjectBaseField(syncBase);
		    bf.setID(SchemaConstants.SyncChannelTypeNum);
		    bf.setType(FieldType.NUMERIC);
		    bf.setName("Channel Type Index");
		    syncBase.addFieldToEnd(bf);
		  }
	      }
	    finally
	      {
		syncBase.setEditingMode(DBObjectBase.EditingMode.LOCKED);
	      }
	  }

	// now make sure that the Task base has the TaskOptionStrings field

	DBObjectBase taskBase = (DBObjectBase) getObjectBase(SchemaConstants.TaskBase);

	if (taskBase.getField(SchemaConstants.TaskOptionStrings) == null)
	  {
	    Ganymede.debug("Adding TaskOptionStrings to task base");

	    bf = new DBObjectBaseField(taskBase);
	    bf.setID(SchemaConstants.TaskOptionStrings);
	    bf.setType(FieldType.STRING);
	    bf.setArray(true);
	    bf.setName("Option Strings");
	    bf.setComment("Optional task parameters, interpreted by specific tasks if needed");
	    taskBase.addFieldToEnd(bf);
	  }

	// make sure that the ObjectEvent type has the new hidden label field

	DBObjectBase objectEventBase = (DBObjectBase) getObjectBase(SchemaConstants.ObjectEventBase);

	if (objectEventBase.getField(SchemaConstants.ObjectEventLabel) == null)
	  {
	    Ganymede.debug("Adding ObjectEventLabel to ObjectEvent base");

	    bf = new DBObjectBaseField(objectEventBase);
	    bf.setID(SchemaConstants.ObjectEventLabel);
	    bf.setName("Hidden Label");
	    bf.setType(FieldType.STRING);
	    bf.setNameSpace("eventtoken");
	    bf.setVisibility(false);	// hidden
	    bf.setComment("Hidden composite label field.  The contents of this label field is automatically set from the Event Token and Object Type Name fields.");
	    objectEventBase.addFieldToStart(bf);
	  }

	objectEventBase.setLabelField(SchemaConstants.ObjectEventLabel);

	// and this last check is for the old ARL database, which
	// somehow did not get namespace constrained on the task name
	// field

	bf = (DBObjectBaseField) taskBase.getField(SchemaConstants.TaskName);

	if (bf.getNameSpace() == null)
	  {
	    Ganymede.debug("Applying namespace constraint to task name");

	    bf.setNameSpace("buildertask");
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException(ex);
      }
  }

  /**
   * This method scans through all of the object bases defined and
   * verifies that all bases have a designated label field that is
   * namespace constrained.
   */

  boolean verify_label_fields()
  {
    boolean ok = true;

    for (DBObjectBase base: objectBases.values())
      {
	if (base.getLabelField() == -1)
	  {
	    // "Error, object base {0} has no label field defined."
	    System.err.println(ts.l("verify_label_fields.no_label", base.getName()));
	    ok = false;
	  }
	else
	  {
	    DBObjectBaseField labelFieldDef = (DBObjectBaseField) base.getField(base.getLabelField());

	    if (labelFieldDef.getNameSpace() == null)
	      {
		// "Error, object base {0}''s label field ({1}) must be namespace-constrained."
		System.err.println(ts.l("verify_label_fields.no_namespace", base.getName(), labelFieldDef.getName()));
		ok = false;
	      }
	  }
      }

    return ok;
  }

  /**
   * Creates required objects when a new database is created
   * from scratch, or if a pre-existing but damaged database file
   * is loaded..
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

    Invid supergashOwner = Invid.createInvid(SchemaConstants.OwnerBase, SchemaConstants.OwnerSupergash);
    Invid supergash = Invid.createInvid(SchemaConstants.PersonaBase, SchemaConstants.PersonaSupergashObj);
    Invid monitor = Invid.createInvid(SchemaConstants.PersonaBase, SchemaConstants.PersonaMonitorObj);
    Invid defaultRole = Invid.createInvid(SchemaConstants.RoleBase, SchemaConstants.RoleDefaultObj);

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

    try
      {
	// make sure the supergash owner group exists

	if (!exists(session, supergashOwner))
	  {
	    System.err.println("Creating supergash Owner Group");

	    retVal = session.createDBObject(SchemaConstants.OwnerBase, supergashOwner, null);

	    if (!ReturnVal.didSucceed(retVal))
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
	
	    if (!ReturnVal.didSucceed(retVal))
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
		p = (PasswordDBField) eObj.getField(SchemaConstants.PersonaPasswordField);
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

	    if (!ReturnVal.didSucceed(retVal))
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

	    if (!ReturnVal.didSucceed(retVal))
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

        createSysEventObj(session, "externalerror", "Error Running External Process",
                          "The Ganymede Server encountered an error when attempting to run an external process.",
                          false);

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
	    transactionEvent.setFieldValueLocal(SchemaConstants.EventMailBoolean, Boolean.TRUE);
	    transactionEvent.setFieldValueLocal(SchemaConstants.EventMailToSelf, Boolean.TRUE);
	    transactionEvent.setFieldValueLocal(SchemaConstants.EventMailOwners, Boolean.TRUE);
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

	if (!ReturnVal.didSucceed(retVal))
	  {
	    try
	      {
		session.abortTransaction();
	      }
	    catch (Throwable ex)
	      {
		Ganymede.logError(ex);
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
	Ganymede.logError(ex);
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

    try
      {
	if (session.getGSession().findLabeledObject(token, SchemaConstants.EventBase) != null)
	  {
	    return null;
	  }
      }
    catch (NotLoggedInException ex)
      {
	throw new Error("Mysterious not logged in exception: " + ex.getMessage());
      }

    System.err.println("Creating " + token + " system event object");

    retVal = session.createDBObject(SchemaConstants.EventBase, null);

    if (!ReturnVal.didSucceed(retVal))
      {
	throw new RuntimeException("Error, could not create system event object " + token);
      }

    eO = (DBEditObject) retVal.getObject();

    retVal = eO.setFieldValueLocal(SchemaConstants.EventToken, token);

    if (!ReturnVal.didSucceed(retVal))
      {
	throw new RuntimeException("Error, could not set token for system event object " + token);
      }

    retVal = eO.setFieldValueLocal(SchemaConstants.EventName, name);

    if (!ReturnVal.didSucceed(retVal))
      {
	throw new RuntimeException("Error, could not set name for system event object " + token);
      }

    if (description != null)
      {
	retVal = eO.setFieldValueLocal(SchemaConstants.EventDescription, description);
	
	if (!ReturnVal.didSucceed(retVal))
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
   * Increments the count of checked-out objects for the admin consoles.
   */

  void checkOut()
  {
    objectsCheckedOut++;
    GanymedeAdmin.updateCheckedOut();
  }

  /**
   * Decrements the count of checked-out objects for the admin consoles.
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
   * Increments the count of held locks for the admin consoles.
   */

  void addLock()
  {
    lockSync.addLock();
  }

  /**
   * Decrements the count of held locks for the admin consoles.
   */

  void removeLock()
  {
    lockSync.removeLock();
  }

  /* *************************************************************************
   *
   * The following methods are for Jython/Map support
   * 
   * For this object, the Map interface allows for indexing based on either
   * the name or the type ID of a DBObjectBase. Indexing by type id, however,
   * is only supported for "direct" access to the Map; the type id numbers
   * won't appear in the list of keys for the Map.
   *
   * EXAMPLE:
   * MyDBStoreObject.get("Users") will return the DBObjectBase with the label
   * of "Users".
   *
   */

  public boolean containsKey(Object key)
  {
    return keySet().contains(key);
  }
  
  public boolean has_key(Object key)
  {
    return containsKey(key);
  }
  
  public boolean containsValue(Object value)
  {
    return getBases().contains(value);
  }

  public Set<Entry> entrySet()
  {
    Set<Entry> entrySet = new HashSet<Entry>();

    synchronized( objectBases)
      {
	for (DBObjectBase base: objectBases.values())
	  {
	    entrySet.add(new Entry(base));
	  }
      }
    
    return entrySet;
  }

  public Object get(Object key)
  {
    if (key instanceof Short)
      {
        return getObjectBase((Short) key);
      }
    else if (key instanceof String)
      {
        return getObjectBase((String) key);
      }
    else
      {
        return null;
      }
  }

  public Set<String> keySet()
  {
    return new HashSet<String>(getBaseNameList());
  }

  public Set<String> keys()
  {
    return keySet();
  }
  
  public List items()
  {
    List list = new ArrayList();
    Object[] tuple;
    
    for (DBObjectBase base: objectBases.values())
      {
        tuple = new Object[2];
        tuple[0] = base.getName();
        tuple[1] = base;
        list.add(tuple);
      }
    
    return list;
  }
  
  public int size()
  {
    return getBaseNameList().size();
  }

  public Collection values()
  {
    return new ArrayList(getBases());
  }

  public String toString()
  {
    return keySet().toString();
  }
  
  /**
   * Implements key/value pairs for use in a 
   * {@link java.util.Map}'s {@link java.util.Map#entrySet()} method.
   */

  static class Entry implements Map.Entry
  {
    Object key, value;
    
    public Entry( DBObjectBase base )
    {
      key = base.getName();
      value = base;
    }
    
    public Object getKey()
    {
      return key;
    }

    public Object getValue()
    {
      return value;
    }

    public Object setValue(Object value)
    {
      return null;
    }
  }

  /* 
   * These methods are are no-ops since we don't want this object
   * messed with via the Map interface.
   */
    
  public Object put(Object key, Object value)
  {
    return null;
  }
  
  public void putAll(Map t)
  {
    return;
  }
  
  public Object remove(Object key)
  {
    return null;
  }
  
  public void clear()
  {
    return;
  }
  
  public boolean isEmpty()
  {
    return getBaseNameList().isEmpty();
  }
}
